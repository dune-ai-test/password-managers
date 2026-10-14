package com.aegis.vault.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps the SQLCipher passphrase behind Android Keystore.
 *
 * The wrapping key lives in the device's secure hardware and can only be used
 * after fresh biometric authentication (no validity window), so the wrapped blob
 * is useless to anyone who doesn't pass BiometricPrompt first. minSdk is 26, so
 * `setUserAuthenticationParameters` (API 30) and the older
 * `setUserAuthenticationRequired` path are both handled.
 */
object BiometricGate {

    private const val KEY_ALIAS = "aegis_biometric_wrap_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        generator.init(spec.build())
        return generator.generateKey()
    }

    /**
     * Biometric-gated encrypt. Must be called while a CryptoObject from a
     * successful BiometricPrompt is available (i.e. from the authentication
     * callback), otherwise the key refuses to be used.
     */
    fun cipherForWrap(): Cipher = prepareWrapCipher()

    /**
     * Prepares the decrypt cipher for an existing wrapped passphrase.
     *
     * With this app's Keystore settings (zero validity window), the very first
     * `init` throws `UserNotAuthenticatedException` AFTER the AES/GCM spec has been
     * recorded on the cipher — so the same cipher instance is then safe to hand to
     * `BiometricPrompt.CryptoObject`: the framework completes initialization
     * during the authenticated operation, and the cipher returned via the success
     * callback is ready for [unwrap]. Do not re-init it afterwards.
     */
    fun prepareUnwrapCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        } catch (e: Exception) {
            // UserNotAuthenticatedException is expected on the first call; the
            // prompt below will finish the operation.
        }
        return cipher
    }

    /** Prepares the encrypt cipher used when turning on biometric unlock. */
    fun prepareWrapCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        } catch (e: Exception) {
            // Same as above: the prompt finishes the operation.
        }
        return cipher
    }

    /** Layout of a wrapped passphrase: iv || ciphertext, base64'd for the prefs. */
    fun wrap(passphrase: ByteArray, cipher: Cipher): String {
        val encrypted = cipher.doFinal(passphrase)
        return com.aegis.vault.data.crypto.VaultCrypto.toBase64(cipher.iv + encrypted)
    }

    /**
     * Decrypts a wrapped passphrase using the cipher from the BiometricPrompt
     * success callback. The cipher must be the one handed to the prompt — never
     * re-initialized after authentication.
     */
    fun unwrap(wrapped: String, cipher: Cipher): ByteArray {
        val blob = com.aegis.vault.data.crypto.VaultCrypto.fromBase64(wrapped)
        val body = blob.copyOfRange(IV_LENGTH, blob.size)
        return cipher.doFinal(body)
    }

    fun ivOf(wrapped: String): ByteArray {
        val blob = com.aegis.vault.data.crypto.VaultCrypto.fromBase64(wrapped)
        return blob.copyOfRange(0, IV_LENGTH)
    }

    /** Drops the key (invalidating all wrapped passphrases). */
    fun reset() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }
}
