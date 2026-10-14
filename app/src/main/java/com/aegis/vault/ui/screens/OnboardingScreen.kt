package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisLogo
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.TextLink
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L1 — Onboarding. The first screen of a fresh install: the Aegis proposition,
 * three feature cards, and the two ways in (create master password / existing vault).
 */
@Composable
fun OnboardingScreen(
    onCreateMasterPassword: () -> Unit,
    onUnlockExisting: () -> Unit,
) {
    val colors = AegisTheme.colors
    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.size(24.dp))
            AegisLogo(size = 88.dp)
            AegisText(
                text = "Every password, one tap away",
                style = AegisType.hero,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AegisText(
                text = "Aegis keeps your logins, cards and notes encrypted on this device — no account, no cloud, no servers.",
                style = AegisType.subhead.copy(lineHeight = 22.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureCard(
                    icon = AegisIcon.ScanFace,
                    iconTint = colors.blue,
                    title = "Biometric unlock",
                    text = "Open your vault in half a second.",
                )
                FeatureCard(
                    icon = AegisIcon.Lock,
                    iconTint = colors.green,
                    title = "Local-only vault",
                    text = "Everything lives in an encrypted local database on this device.",
                )
                FeatureCard(
                    icon = AegisIcon.Bell,
                    iconTint = colors.purple,
                    title = "Breach alerts",
                    text = "Alerts come from a bundled offline breach database.",
                )
            }

            PrimaryButton(
                text = "Create master password",
                onClick = onCreateMasterPassword,
                modifier = Modifier.fillMaxWidth(),
            )
            TextLink(
                text = "I already have a vault",
                onClick = onUnlockExisting,
                color = colors.blue,
                style = AegisType.subhead.copy(fontWeight = FontWeight.SemiBold),
            )
            AegisText(
                text = "By continuing you agree to the Terms and Privacy Policy",
                style = AegisType.mini,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: AegisIcon,
    iconTint: Color,
    title: String,
    text: String,
) {
    val colors = AegisTheme.colors
    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.tintFor(iconTint)),
                contentAlignment = Alignment.Center,
            ) {
                AegisIconGraphic(icon = icon, contentDescription = null, tint = iconTint, size = 20.dp)
            }
            Box(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                AegisText(text = title, style = AegisType.bodyStrong)
                Box(Modifier.size(2.dp))
                AegisText(text = text, style = AegisType.footnote, color = colors.textSecondary)
            }
        }
    }
}
