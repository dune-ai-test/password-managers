package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.security.BreachDatabase
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.ItemRow
import com.aegis.vault.ui.components.ScoreRing
import com.aegis.vault.ui.components.SectionHeader
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L11 — Security: score ring, the four stat cards, breach/reuse alerts, and the
 * weak-passwords list. All numbers are computed on-device from the vault.
 */
@Composable
fun SecurityScreen(
    locator: ServiceLocator,
    onOpenLogin: (String) -> Unit,
) {
    val colors = AegisTheme.colors
    val scanner = locator.securityScanner
    val repo = locator.vaultRepository

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    val report = remember(version) { repo.securityReport() }
    val logins = remember(version) { repo.logins() }
    val reusedGroups = remember(version) {
        logins.groupBy { it.password }.filter { it.value.size > 1 }
    }
    val breachedLogins = remember(version) {
        logins.filter { BreachDatabase.isBreached(it.domain) }
    }
    val oldestLogin = remember(version) { logins.minByOrNull { it.lastModified } }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 170.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AegisText("Security", style = AegisType.title)

            // Score card.
            AegisCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp, contentPadding = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ScoreRing(score = report.score, label = report.scoreLabel)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AegisText("Security score", style = AegisType.bodyStrong)
                        AegisText(
                            text = report.scoreDescription,
                            style = AegisType.footnote,
                            color = colors.textSecondary,
                        )
                        AegisText(
                            text = "Last checked locally today",
                            style = AegisType.caption,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            // Stats grid.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        accent = colors.amber,
                        icon = AegisIcon.Shield,
                        number = report.weakCount,
                        label = "Weak",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        accent = colors.amber,
                        icon = AegisIcon.RefreshCw,
                        number = report.reusedCount,
                        label = "Reused",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        accent = colors.blue,
                        icon = AegisIcon.ShieldCheck,
                        number = report.missing2faCount,
                        label = "Missing 2FA",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        accent = colors.red,
                        icon = AegisIcon.Bell,
                        number = report.breachedCount,
                        label = "Breached",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Alerts.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                breachedLogins.firstOrNull()?.let { login ->
                    AlertCard(
                        accent = colors.red,
                        icon = AegisIcon.Bell,
                        title = "${login.domain} breach detected",
                        description = "This password appeared in a known leak. Change it now.",
                        buttonLabel = "Review",
                        onAction = { onOpenLogin(login.id) },
                    )
                }
                reusedGroups.entries.firstOrNull()?.let { (_, members) ->
                    val first = members.first()
                    AlertCard(
                        accent = colors.amber,
                        icon = AegisIcon.RefreshCw,
                        title = "Reuse detected",
                        description = "${members.size} accounts share the same password. Update takes ~2 min.",
                        buttonLabel = "Fix",
                        onAction = { onOpenLogin(first.id) },
                    )
                }
            }

            // Weak passwords list.
            if (breachedLogins.isNotEmpty() || oldestLogin != null) {
                SectionHeader(title = "Weak passwords")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    breachedLogins.take(2).forEach { login ->
                        ItemRow(
                            title = login.domain,
                            subtitle = "Leaked in ${BreachDatabase.breachLabelFor(login.domain) ?: "a breach"}",
                            badgeLetter = login.domain.take(1),
                            badgeColor = badgeColorFor(login.domain),
                            onClick = { onOpenLogin(login.id) },
                        )
                    }
                    oldestLogin?.let { login ->
                        ItemRow(
                            title = login.domain,
                            subtitle = "Created in ${yearOf(login.createdAt)}",
                            badgeLetter = login.domain.take(1),
                            badgeColor = badgeColorFor(login.domain),
                            onClick = { onOpenLogin(login.id) },
                        )
                    }
                }
            }

            if (report.totalIssues == 0) {
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                    AegisText(
                        text = "No weak, reused, breached, or two-factor-less logins. Nice.",
                        style = AegisType.body,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    accent: androidx.compose.ui.graphics.Color,
    icon: AegisIcon,
    number: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = AegisTheme.colors
    AegisCard(
        modifier = modifier,
        contentPadding = 14.dp,
        color = colors.card,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.tintFor(accent)),
                contentAlignment = Alignment.Center,
            ) {
                AegisIconGraphic(icon = icon, contentDescription = null, tint = accent, size = 17.dp)
            }
            AegisText(
                text = number.toString(),
                style = AegisType.titleCompact.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
            AegisText(label, style = AegisType.footnote, color = colors.textSecondary)
        }
    }
}

@Composable
private fun AlertCard(
    accent: androidx.compose.ui.graphics.Color,
    icon: AegisIcon,
    title: String,
    description: String,
    buttonLabel: String,
    onAction: () -> Unit,
) {
    val colors = AegisTheme.colors
    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.tintFor(accent)),
                contentAlignment = Alignment.Center,
            ) {
                AegisIconGraphic(icon = icon, contentDescription = null, tint = accent, size = 20.dp)
            }
            Box(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AegisText(title, style = AegisType.bodyStrong)
                Box(Modifier.size(2.dp))
                AegisText(
                    text = description,
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )
            }
            Box(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.tintFor(accent))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                AegisText(
                    text = buttonLabel,
                    style = AegisType.captionStrong,
                    color = accent,
                )
            }
        }
    }
}

/** Calendar year of an epoch-millis timestamp (minSdk 26 → java.time is native). */
internal fun yearOf(timestamp: Long): Int =
    java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).year
