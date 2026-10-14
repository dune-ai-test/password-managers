package com.aegis.vault.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aegis.vault.data.crypto.VaultCrypto

/**
 * Owns everything needed to verify a master password and to open the vault with
 * biometrics.
 *
 * On disk (EncryptedSharedPreferences):
 *  - salt:  random per-vault PBKDF2 salt (public by design)
 *  - verifier: a PBKDF2 hash of the passphrase, so a wrong master password is
 *    rejected without touching the database
 *  - wrapped passphrase: the SQLCipher passphrase itself, AES-GCM encrypted with
 *    a Keystore key that requires fresh biometric authentication. Unwrapping
 *    only succeeds right after a successful BiometricPrompt.
 */
class MasterPasswordManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** True once the user has completed onboarding and set a master password. */
    val hasMasterPassword: Boolean
        get() = prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFIER)

    val biometricUnlockEnabled: Boolean
        get() = prefs.contains(KEY_WRAPPED_PASSPHRASE) && plainPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    /** Saves the master password: derives the passphrase, stores salt + verifier. */
    fun setMasterPassword(password: String): ByteArray {
        val salt = VaultCrypto.randomSalt()
        val passphrase = VaultCrypto.derivePassphrase(password.toCharArray(), salt)
        prefs.edit()
            .putString(KEY_SALT, VaultCrypto.toBase64(salt))
            .putString(KEY_VERIFIER, VaultCrypto.toBase64(hashPassphrase(passphrase)))
            .apply()
        return passphrase
    }

    /**
     * Verifies a candidate master password and returns the derived passphrase.
     * Returns null when the password is wrong.
     */
    fun unlockWithPassword(password: String): ByteArray? {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return null
        val verifierB64 = prefs.getString(KEY_VERIFIER, null) ?: return null
        val salt = VaultCrypto.fromBase64(saltB64)
        val passphrase = VaultCrypto.derivePassphrase(password.toCharArray(), salt)
        val hash = hashPassphrase(passphrase)
        if (!hash.contentEquals(VaultCrypto.fromBase64(verifierB64))) return null
        return passphrase
    }

    /** Stores the wrapped passphrase produced by [BiometricGate] after a successful prompt. */
    fun enableBiometricUnlock(wrapped: String) {
        plainPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, true).apply()
        prefs.edit().putString(KEY_WRAPPED_PASSPHRASE, wrapped).apply()
    }

    fun disableBiometricUnlock() {
        plainPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, false).apply()
        prefs.edit().remove(KEY_WRAPPED_PASSPHRASE).apply()
    }

    fun wrappedPassphrase(): String? =
        if (biometricUnlockEnabled) prefs.getString(KEY_WRAPPED_PASSPHRASE, null) else null

    /**
     * Changes the master password. Caller must have already re-keyed the database
     * (SQLCipher changePassword); here we only store the new salt + verifier so
     * subsequent unlocks use the new password.
     */
    fun changeMasterPassword(new: String): ByteArray {
        val salt = VaultCrypto.randomSalt()
        val newPassphrase = VaultCrypto.derivePassphrase(new.toCharArray(), salt)
        prefs.edit()
            .putString(KEY_SALT, VaultCrypto.toBase64(salt))
            .putString(KEY_VERIFIER, VaultCrypto.toBase64(hashPassphrase(newPassphrase)))
            .apply()
        return newPassphrase
    }

    // ------------------------------------------------------- app settings

    var autoLockSeconds: Int
        get() = plainPrefs.getInt(KEY_AUTOLOCK, 60)
        set(value) = plainPrefs.edit().putInt(KEY_AUTOLOCK, value).apply()

    var showStorageNotice: Boolean
        get() = plainPrefs.getBoolean(KEY_STORAGE_NOTICE, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_STORAGE_NOTICE, value).apply()

    fun appVersionLabel(): String = plainPrefs.getString(KEY_VERSION, null) ?: DEFAULT_VERSION

    /** When the last encrypted backup was written, or null when never. */
    var lastBackupAtMillis: Long?
        get() = plainPrefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0L }
        set(value) = plainPrefs.edit().putLong(KEY_LAST_BACKUP, value ?: 0L).apply()

    private fun hashPassphrase(passphrase: ByteArray): ByteArray =
        java.security.MessageDigest.getInstance("SHA-256").digest(passphrase)

    companion object {
        private const val PREFS_NAME = "aegis_vault_keys"
        private const val PLAIN_PREFS_NAME = "aegis_settings"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_WRAPPED_PASSPHRASE = "wrapped_passphrase"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTOLOCK = "autolock_seconds"
        private const val KEY_STORAGE_NOTICE = "storage_notice"
        private const val KEY_VERSION = "version_label"
        private const val KEY_LAST_BACKUP = "last_backup_at"
        const val DEFAULT_VERSION = "4.2.0 (128)"

        val AUTOLOCK_OPTIONS = listOf("Immediately", "30 seconds", "1 minute", "5 minutes", "15 minutes")

        fun autoLockSecondsForOption(option: String): Int = when (option) {
            "Immediately" -> 0
            "30 seconds" -> 30
            "1 minute" -> 60
            "5 minutes" -> 300
            "15 minutes" -> 900
            else -> 60
        }

        fun optionForAutoLockSeconds(seconds: Int): String = when (seconds) {
            0 -> "Immediately"
            30 -> "30 seconds"
            300 -> "5 minutes"
            900 -> "15 minutes"
            else -> "1 minute"
        }
    }
}
