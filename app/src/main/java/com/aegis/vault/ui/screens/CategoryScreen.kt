package com.aegis.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.EmptyState
import com.aegis.vault.ui.components.FilterChip
import com.aegis.vault.ui.components.ItemRow
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/** Which L5 filter chip is active. */
private enum class LoginFilter(val label: String) {
    All("All"), Weak("Weak"), Reused("Reused"), NoTwoFactor("No 2FA");

    companion object {
        val all: List<LoginFilter> = entries.toList()
    }
}

/**
 * L5 — a category list (Logins/Cards/Notes/Identities) with the mockup's filter
 * chips and sort button, and L14's empty state when the category has no items.
 */
@Composable
fun CategoryScreen(
    locator: ServiceLocator,
    itemType: ItemType,
    onOpenLogin: (String) -> Unit,
    onOpenCard: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenIdentity: (String) -> Unit,
    onNewItem: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val repo = locator.vaultRepository
    val scanner = locator.securityScanner

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    var filter by remember(itemType) { mutableStateOf(LoginFilter.All) }
    var sortByName by remember(itemType) { mutableStateOf(true) }

    val loginItems = remember(version, sortByName) {
        repo.logins().let { list ->
            if (sortByName) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.lastModified }
        }
    }
    val cardItems = remember(version, sortByName) {
        repo.cards().let { list ->
            if (sortByName) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.lastModified }
        }
    }
    val noteItems = remember(version, sortByName) {
        repo.notes().let { list ->
            if (sortByName) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.lastModified }
        }
    }
    val identityItems = remember(version, sortByName) {
        repo.identities().let { list ->
            if (sortByName) list.sortedBy { it.title.lowercase() } else list.sortedByDescending { it.lastModified }
        }
    }

    val filteredLogins = remember(loginItems, filter) {
        when (filter) {
            LoginFilter.All -> loginItems
            LoginFilter.Weak -> loginItems.filter { scanner.isWeak(it.password) }
            LoginFilter.Reused -> loginItems.groupBy { it.password }.filter { it.value.size > 1 }.values.flatten()
            LoginFilter.NoTwoFactor -> loginItems.filter { it.totpSecret.isBlank() }
        }
    }

    val count = when (itemType) {
        ItemType.Login -> filteredLogins.size
        ItemType.Card -> cardItems.size
        ItemType.Note -> noteItems.size
        ItemType.Identity -> identityItems.size
    }
    val sortLabel = if (sortByName) "sorted A–Z" else "sorted by last modified"

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(
                title = itemType.title(),
                onBack = onBack,
                backContentDescription = "Back",
                leadingAction = { sortByName = !sortByName },
                leadingIcon = AegisIcon.SlidersHorizontal,
                leadingContentDescription = "Sort",
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AegisText(
                    text = "$count ${itemType.labelPlural()} · $sortLabel",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )

                if (itemType == ItemType.Login) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoginFilter.all.forEach { chip ->
                            FilterChip(
                                label = chip.label,
                                selected = filter == chip,
                                onClick = { filter = chip },
                            )
                        }
                    }
                }

                when (itemType) {
                    ItemType.Login -> {
                        if (filteredLogins.isEmpty()) {
                            EmptyState(
                                icon = AegisIcon.KeyRound,
                                accent = colors.blue,
                                title = "No logins found",
                                text = "Logins matching this filter will show up here. Change the filter or add a new login.",
                                buttonLabel = "Add login",
                                onButton = onNewItem,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredLogins.forEach { login ->
                                    ItemRow(
                                        title = login.title,
                                        subtitle = loginSubtitle(login),
                                        badgeLetter = login.title.take(1),
                                        badgeColor = badgeColorFor(login.title),
                                        onClick = { onOpenLogin(login.id) },
                                    )
                                }
                            }
                        }
                    }

                    ItemType.Card -> {
                        if (cardItems.isEmpty()) {
                            EmptyState(
                                icon = AegisIcon.CreditCard,
                                accent = colors.purple,
                                title = "No cards saved",
                                text = "Store payment cards encrypted on this device. Add one to get started.",
                                buttonLabel = "Add card",
                                onButton = onNewItem,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                cardItems.forEach { card ->
                                    ItemRow(
                                        title = card.title,
                                        subtitle = "card · ${card.maskedNumber}",
                                        badgeLetter = card.title.take(1),
                                        badgeColor = badgeColorFor(card.title),
                                        onClick = { onOpenCard(card.id) },
                                    )
                                }
                            }
                        }
                    }

                    ItemType.Note -> {
                        if (noteItems.isEmpty()) {
                            EmptyState(
                                icon = AegisIcon.FileText,
                                accent = colors.green,
                                title = "No secure notes yet",
                                text = "Store passport details, software keys and Wi-Fi passwords — encrypted on this device.",
                                buttonLabel = "Create a note",
                                onButton = onNewItem,
                                hint = "Everything stays encrypted on this device",
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                noteItems.forEach { note ->
                                    ItemRow(
                                        title = note.title,
                                        subtitle = if (note.tags.isNotEmpty()) note.tags.joinToString(" · ") else "secure note",
                                        badgeLetter = note.title.take(1),
                                        badgeColor = badgeColorFor(note.title),
                                        onClick = { onOpenNote(note.id) },
                                    )
                                }
                            }
                        }
                    }

                    ItemType.Identity -> {
                        if (identityItems.isEmpty()) {
                            EmptyState(
                                icon = AegisIcon.User,
                                accent = colors.amber,
                                title = "No identities saved",
                                text = "Keep addresses, email and phone numbers you reuse across signups.",
                                buttonLabel = "Add identity",
                                onButton = onNewItem,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                identityItems.forEach { identity ->
                                    ItemRow(
                                        title = identity.title,
                                        subtitle = identity.email.ifBlank { identity.phone },
                                        badgeLetter = identity.title.take(1),
                                        badgeColor = badgeColorFor(identity.title),
                                        onClick = { onOpenIdentity(identity.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Keep bottom spacing for the tab bar.
                Box(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

/** "Login" / "Card" ... as used in screen titles. */
private fun ItemType.title(): String = when (this) {
    ItemType.Login -> "Logins"
    ItemType.Card -> "Cards"
    ItemType.Note -> "Notes"
    ItemType.Identity -> "Identities"
}

private fun ItemType.labelPlural(): String = when (this) {
    ItemType.Login -> "logins"
    ItemType.Card -> "cards"
    ItemType.Note -> "notes"
    ItemType.Identity -> "identities"
}

/** Subtitle for a login row: the username, or domain when there is none. */
private fun loginSubtitle(login: LoginItem): String = login.username.ifBlank { login.domain }

