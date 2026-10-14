package com.aegis.vault.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * All key material for the vault.
 *
 * The master password never leaves memory as a String: it is expanded with
 * PBKDF2WithHmacSHA256 (210k iterations, per-vault random salt) into a 256-bit
 * passphrase, and that passphrase is what opens the SQLCipher database. A wrong
 * password produces a wrong passphrase and SQLCipher simply refuses to read the
 * database, so the salt+verifier pair only exists to fail fast.
 */
object VaultCrypto {

    const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12
    private val secureRandom = SecureRandom()

    /** Derives the SQLCipher passphrase from the master password. */
    fun derivePassphrase(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun derivePassphraseHex(password: String, salt: ByteArray): String =
        derivePassphrase(password.toCharArray(), salt).toHexString()

    fun randomSalt(): ByteArray = ByteArray(16).also { secureRandom.nextBytes(it) }

    fun randomUuid(): String = java.util.UUID.randomUUID().toString()

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    /**
     * AES-256-GCM encrypts [plaintext] with a key derived from [password] using a
     * fresh 16-byte salt and 12-byte IV. Output layout is salt || iv || ciphertext.
     * Used for encrypted backups.
     */
    fun encryptWithPassword(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = randomSalt()
        val key = SecretKeySpec(derivePassphrase(password, salt), "AES")
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plaintext)
        return salt + iv + cipherText
    }

    /**
     * Decrypts a blob produced by [encryptWithPassword]. Returns null if the
     * password is wrong or the blob was tampered with (GCM authentication).
     */
    fun decryptWithPassword(blob: ByteArray, password: CharArray): ByteArray? {
        if (blob.size < 16 + IV_LENGTH + 16) return null
        return try {
            val salt = blob.copyOfRange(0, 16)
            val iv = blob.copyOfRange(16, 16 + IV_LENGTH)
            val cipherText = blob.copyOfRange(16 + IV_LENGTH, blob.size)
            val key = SecretKeySpec(derivePassphrase(password, salt), "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            null
        }
    }
}

internal fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
