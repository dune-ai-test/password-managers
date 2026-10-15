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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.CategoryTile
import com.aegis.vault.ui.components.ItemRow
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SectionHeader
import com.aegis.vault.ui.components.StorageStrip
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L4 — the Passwords (vault) tab: header, search, storage strip, the four category
 * tiles, Recently used, and Needs attention. The tab bar and FAB are drawn by the
 * nav host.
 */
@Composable
fun VaultScreen(
    locator: ServiceLocator,
    onOpenCategory: (ItemType) -> Unit,
    onOpenLogin: (String) -> Unit,
    onOpenSearch: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNewItem: () -> Unit,
) {
    val colors = AegisTheme.colors
    val repo = locator.vaultRepository

    // Re-read from the database whenever the repository's data version changes.
    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) {
        repo.dataVersion.collect { version = it }
    }

    val stats = remember(version) { repo.stats() }
    val recentLogins = remember(version) { repo.recentLogins(limit = 5) }
    val attentionRows = remember(version) { buildAttentionRows(repo.logins()) }
    val dbSize = remember(version) { repo.databaseSizeBytes() }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 170.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: screen title + avatar.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AegisText("Passwords", style = AegisType.title)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.blue),
                    contentAlignment = Alignment.Center,
                ) {
                    AegisText(
                        "A",
                        style = AegisType.bodyStrong.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onAccent,
                    )
                }
            }

            // Search bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .height(46.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSearch,
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AegisIconGraphic(
                    icon = AegisIcon.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    size = 18.dp,
                )
                Box(Modifier.size(10.dp))
                AegisText(
                    text = "Search passwords, cards, notes",
                    style = AegisType.body,
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }

            StorageStrip(
                storageText = "Local-only vault · ${formatStorageSize(dbSize)} on this device",
            )

            // Category tiles.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryTile(
                    name = "Logins",
                    count = "${stats.logins} items",
                    icon = AegisIcon.KeyRound,
                    accent = colors.blue,
                    onClick = { onOpenCategory(ItemType.Login) },
                    modifier = Modifier.weight(1f),
                )
                CategoryTile(
                    name = "Cards",
                    count = "${stats.cards} items",
                    icon = AegisIcon.CreditCard,
                    accent = colors.purple,
                    onClick = { onOpenCategory(ItemType.Card) },
                    modifier = Modifier.weight(1f),
                )
                CategoryTile(
                    name = "Notes",
                    count = "${stats.notes} items",
                    icon = AegisIcon.FileText,
                    accent = colors.green,
                    onClick = { onOpenCategory(ItemType.Note) },
                    modifier = Modifier.weight(1f),
                )
                CategoryTile(
                    name = "Identities",
                    count = "${stats.identities} items",
                    icon = AegisIcon.User,
                    accent = colors.amber,
                    onClick = { onOpenCategory(ItemType.Identity) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Recently used.
            if (recentLogins.isNotEmpty()) {
                SectionHeader(
                    title = "Recently used",
                    actionLabel = "See all",
                    onAction = { onOpenCategory(ItemType.Login) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentLogins.forEach { login ->
                        ItemRow(
                            title = login.title,
                            subtitle = login.username.ifBlank { login.domain },
                            badgeLetter = login.title.take(1),
                            badgeColor = badgeColorFor(login.title),
                            onClick = { onOpenLogin(login.id) },
                        )
                    }
                }
            }

            // Needs attention.
            if (attentionRows.isNotEmpty()) {
                SectionHeader(
                    title = "Needs attention",
                    actionLabel = "Fix all",
                    onAction = { onOpenCategory(ItemType.Login) },
                    actionColor = colors.amber,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    attentionRows.take(3).forEach { row ->
                        ItemRow(
                            title = row.title,
                            subtitle = row.subtitle,
                            badgeLetter = row.badgeLetter,
                            badgeColor = row.badgeColor,
                            onClick = { onOpenLogin(row.loginId) },
                        )
                    }
                }
            }

            if (stats.total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AegisText(
                        text = "Add your first item with the + button",
                        style = AegisType.footnote,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** A row in the "Needs attention" list on L4. */
data class AttentionRow(
    val loginId: String,
    val badgeLetter: String,
    val badgeColor: androidx.compose.ui.graphics.Color,
    val title: String,
    val subtitle: String,
)

/**
 * Builds the attention list exactly like the mockups: reused passwords first
 * ("Summer21! reused" / "5 accounts share it"), then untouched accounts
 * ("Unchanged for 3 years" / "chase.com · amorgan_92").
 */
internal fun buildAttentionRows(logins: List<LoginItem>): List<AttentionRow> {
    val rows = mutableListOf<AttentionRow>()
    val groups = logins.groupBy { it.password }

    groups.entries
        .filter { it.value.size > 1 }
        .sortedByDescending { it.value.size }
        .forEach { (password, members) ->
            val first = members.first()
            rows += AttentionRow(
                loginId = first.id,
                badgeLetter = members.size.toString(),
                badgeColor = androidx.compose.ui.graphics.Color(0xFFFF9500),
                title = "$password reused",
                subtitle = "${members.size} accounts share it",
            )
        }

    val now = System.currentTimeMillis()
    val stale = logins
        .filter { now - it.lastModified > 2L * 365 * 24 * 60 * 60 * 1000 }
        .sortedBy { it.lastModified }
        .take(2)
    stale.forEach { login ->
        val years = ((now - login.lastModified) / (365L * 24 * 60 * 60 * 1000)).coerceAtLeast(1)
        rows += AttentionRow(
            loginId = login.id,
            badgeLetter = login.title.take(1),
            badgeColor = androidx.compose.ui.graphics.Color(0xFFFF3B30),
            title = "Unchanged for $years years",
            subtitle = "${login.domain} · ${login.username}",
        )
    }

    return rows
}

