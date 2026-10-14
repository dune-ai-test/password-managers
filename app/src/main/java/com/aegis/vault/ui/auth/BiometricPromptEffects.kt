package com.aegis.vault.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Bridges the AuthViewModel's prompt requests to the system BiometricPrompt.
 *
 * Two flows share it:
 *  - unlock: the user taps "Unlock with fingerprint" on L3
 *  - wrap: right after creating a master password on L2 with the toggle on
 *
 * Navigation is NOT triggered here — the screens observe `vaultReady` and
 * navigate once it flips, so a prompt that resolves after the composable would
 * have been disposed of can never be missed. Labels are Android-native, per the
 * platform convention (no "Face ID" wording).
 */
@Composable
fun BiometricPromptEffects(
    viewModel: AuthViewModel,
    host: BiometricPromptHost,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unlockCipher by viewModel.biometricUnlockCipher.collectAsStateWithLifecycle()

    LaunchedEffect(unlockCipher) {
        val cipher = unlockCipher ?: return@LaunchedEffect
        host.show(
            cipher = cipher,
            title = "Unlock Aegis",
            subtitle = "Verify your identity to open the vault",
            negativeButton = "Use master password",
            onSuccess = { result -> viewModel.finishBiometricUnlock(result) },
            onError = { message -> viewModel.onBiometricError(message) },
        )
    }

    val wrapCipher = state.biometricWrapCipher
    LaunchedEffect(wrapCipher) {
        val cipher = wrapCipher ?: return@LaunchedEffect
        host.show(
            cipher = cipher,
            title = "Enable fingerprint unlock",
            subtitle = "Confirm it's you to finish setting up Aegis",
            negativeButton = "Not now",
            onSuccess = { result -> viewModel.completeBiometricWrap(result) },
            onError = { _ -> viewModel.cancelBiometricWrap() },
        )
    }
}
