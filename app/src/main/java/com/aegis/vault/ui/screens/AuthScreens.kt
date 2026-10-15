package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.ui.auth.AuthViewModel
import com.aegis.vault.ui.auth.BiometricPromptEffects
import com.aegis.vault.ui.auth.rememberBiometricPromptHost
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisLogo
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.IosToggle
import com.aegis.vault.ui.components.PasswordField
import com.aegis.vault.ui.components.PressableSurface
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.RequirementCheck
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.StrengthMeter
import com.aegis.vault.ui.components.TextLink
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L2 — Create master password. Strength meter, three requirement checks, the
 * un-recoverable warning, and the fingerprint-unlock opt-in.
 */
/**
 * True while the software keyboard is on screen. With edge-to-edge + SDK 35 the
 * window does not resize for the IME, so screens observe this to scroll their
 * primary action into view.
 */
@Composable
private fun imeVisible(): Boolean {
    val density = LocalDensity.current
    val bottom = WindowInsets.ime.getBottom(density)
    return bottom > 0
}

@Composable
fun CreateMasterPasswordScreen(
    locator: ServiceLocator,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: AuthViewModel = viewModel(
        factory = viewModelFactory { initializer { AuthViewModel(locator) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AegisTheme.colors
    val promptHost = rememberBiometricPromptHost()
    val scrollState = rememberScrollState()
    val imeOpen = imeVisible()

    BiometricPromptEffects(viewModel = viewModel, host = promptHost)

    // Navigate once the vault is open and any biometric prompt has resolved.
    LaunchedEffect(state.vaultReady) {
        if (state.vaultReady) onReady()
    }

    // With the keyboard up, bring Continue above it automatically.
    LaunchedEffect(imeOpen) {
        if (imeOpen) scrollState.animateScrollTo(scrollState.maxValue)
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "", onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.tintFor(colors.blue)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AegisIconGraphic(
                            icon = AegisIcon.Lock,
                            contentDescription = null,
                            tint = colors.blue,
                            size = 28.dp,
                        )
                    }
                }
                AegisText(
                    text = "Set your master password",
                    style = AegisType.titleCompact,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                AegisText(
                    text = "It's the only password you'll ever need to remember. Your vault will live in an encrypted local database.",
                    style = AegisType.subhead,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    showPassword = state.showPassword,
                    onToggleShow = { viewModel.setShowPassword(!state.showPassword) },
                    onSubmit = { viewModel.createMasterPassword(state.password) },
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AegisText("Password strength", style = AegisType.footnote, color = colors.textSecondary)
                        AegisText(
                            text = state.strengthLabel,
                            style = AegisType.footnoteStrong,
                            color = when (state.strengthLevel) {
                                0, 1 -> colors.red
                                2 -> colors.amber
                                3 -> colors.blue
                                else -> colors.green
                            },
                        )
                    }
                    StrengthMeter(level = state.strengthLevel, segmentCount = 4)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RequirementRow(met = state.meetsLength, text = "At least 12 characters")
                    RequirementRow(met = state.meetsCases, text = "Upper and lowercase letters")
                    RequirementRow(met = state.meetsNumberOrSymbol, text = "A number or symbol")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.amberTint)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AegisIconGraphic(
                        icon = AegisIcon.Shield,
                        contentDescription = null,
                        tint = colors.amber,
                        size = 18.dp,
                    )
                    AegisText(
                        text = "If you forget your master password, no one — not even us — can restore your vault.",
                        style = AegisType.footnote,
                        color = Color(0xFF8A4B00),
                        modifier = Modifier.weight(1f),
                    )
                }

                PrimaryButton(
                    text = "Continue",
                    onClick = { viewModel.createMasterPassword(state.password) },
                    modifier = Modifier.fillMaxWidth(),
                )

                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.tintFor(colors.blue)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AegisIconGraphic(
                                icon = AegisIcon.ScanFace,
                                contentDescription = null,
                                tint = colors.blue,
                                size = 20.dp,
                            )
                        }
                        Box(Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            AegisText("Enable fingerprint unlock", style = AegisType.bodyStrong)
                            Box(Modifier.size(2.dp))
                            AegisText(
                                text = "Skip the password on this device",
                                style = AegisType.footnote,
                                color = colors.textSecondary,
                            )
                        }
                        Box(Modifier.size(10.dp))
                        IosToggle(
                            checked = state.enableBiometric,
                            onCheckedChange = viewModel::setBiometricToggle,
                        )
                    }
                }

                state.error?.let { error ->
                    AegisText(
                        text = error,
                        style = AegisType.footnote,
                        color = colors.red,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * L3 — Unlock. Master-password entry plus the biometric unlock path, with the
 * same masked-password treatment as L2.
 */
@Composable
fun UnlockScreen(
    locator: ServiceLocator,
    onUnlocked: () -> Unit,
    itemCount: Int = 0,
) {
    val viewModel: AuthViewModel = viewModel(
        factory = viewModelFactory { initializer { AuthViewModel(locator) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AegisTheme.colors
    val promptHost = rememberBiometricPromptHost()
    var forgotNote by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val imeOpen = imeVisible()

    BiometricPromptEffects(viewModel = viewModel, host = promptHost)

    // Navigate once the vault is open (password or biometric path).
    LaunchedEffect(state.vaultReady) {
        if (state.vaultReady) onUnlocked()
    }

    // Keep Unlock above the keyboard.
    LaunchedEffect(imeOpen) {
        if (imeOpen) scrollState.animateScrollTo(scrollState.maxValue)
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AegisLogo(size = 76.dp)
            AegisText("Welcome back", style = AegisType.titleCompact)
            AegisText(
                text = if (itemCount > 0) {
                    "Enter your master password to unlock your local vault — $itemCount items on this device."
                } else {
                    "Enter your master password to unlock your local vault."
                },
                style = AegisType.subhead,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                showPassword = state.showPassword,
                onToggleShow = { viewModel.setShowPassword(!state.showPassword) },
                onSubmit = { viewModel.unlockWithPassword(state.password) },
            )

            PrimaryButton(
                text = "Unlock",
                onClick = { viewModel.unlockWithPassword(state.password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(colors.separator),
                )
                AegisText("or", style = AegisType.footnote, color = colors.textTertiary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(colors.separator),
                )
            }

            if (state.biometricConfigured) {
                PressableSurface(
                    onClick = { viewModel.beginBiometricUnlock() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AegisIconGraphic(
                            icon = AegisIcon.ScanFace,
                            contentDescription = null,
                            tint = colors.blue,
                            size = 22.dp,
                        )
                        AegisText(
                            text = "Unlock with fingerprint",
                            style = AegisType.bodyStrong,
                            color = colors.blue,
                        )
                    }
                }
            }

            state.error?.let { error ->
                AegisText(error, style = AegisType.footnote, color = colors.red, textAlign = TextAlign.Center)
            }

            if (forgotNote) {
                AegisText(
                    text = "There is no reset — the master password is the only key to this vault.",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            TextLink(
                text = "Forgot master password?",
                onClick = { forgotNote = true },
                color = colors.textSecondary,
                style = AegisType.footnote,
            )
        }
    }
}

@Composable
private fun RequirementRow(met: Boolean, text: String) {
    val colors = AegisTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RequirementCheck(met = met)
        AegisText(
            text = text,
            style = AegisType.footnote,
            color = if (met) colors.textPrimary else colors.textSecondary,
        )
    }
}
