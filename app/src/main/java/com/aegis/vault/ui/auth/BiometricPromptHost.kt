package com.aegis.vault.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * True when at least one biometric (or device-credential) authenticator is
 * enrolled and available.
 */
fun canAuthenticateBiometric(context: android.content.Context): Boolean {
    val status = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    return status == BiometricManager.BIOMETRIC_SUCCESS
}

/**
 * Hosts BiometricPrompt for the auth screens. Built from the enclosing activity
 * (MainActivity is a FragmentActivity) and chooses Android-native labels, per the
 * app's platform conventions.
 */
class BiometricPromptHost(private val activity: FragmentActivity?) {

    val isAvailable: Boolean get() = activity != null

    fun show(
        cipher: Cipher,
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
    ) {
        val current = activity ?: run {
            onError("No activity to host the prompt")
            return
        }
        val executor = ContextCompat.getMainExecutor(current)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val resultCipher = result.cryptoObject?.cipher
                if (resultCipher != null) {
                    onSuccess(resultCipher)
                } else {
                    onError("Authentication succeeded but no cipher was returned")
                }
            }

            override fun onAuthenticationError(code: Int, message: CharSequence) {
                onError(message.toString())
            }

            override fun onAuthenticationFailed() {
                // A failed attempt — the system UI stays up and asks again.
            }
        }
        val prompt = BiometricPrompt(current, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}

@Composable
fun rememberBiometricPromptHost(): BiometricPromptHost {
    val context = LocalContext.current
    return remember(context) { BiometricPromptHost(context as? FragmentActivity) }
}
