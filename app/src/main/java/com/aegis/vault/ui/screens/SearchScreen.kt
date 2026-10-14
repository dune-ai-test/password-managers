package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.SearchResult
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.EmptyState
import com.aegis.vault.ui.components.FilterChip
import com.aegis.vault.ui.components.ItemRow
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.TextLink
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L13 — search over the whole vault. Chips narrow by item type; the suggestion
 * card offers the queries from the mockups when the query is empty.
 */
@Composable
fun SearchScreen(
    locator: ServiceLocator,
    onOpenLogin: (String) -> Unit,
    onOpenCard: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val repo = locator.vaultRepository

    var query by remember { mutableStateOf("") }
    var chips by remember { mutableStateOf(ItemType.entries.toSet()) }
    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    val results: List<SearchResult> = remember(query, chips, version) {
        repo.search(query, chips)
    }
    val suggestions = remember(version) {
        listOf(
            "work email",
            "passport",
            "wifi",
        ).filter { it.contains(query.trim(), ignoreCase = true) || query.isBlank() }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search field.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AegisIconGraphic(
                        icon = AegisIcon.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        size = 18.dp,
                    )
                    Box(Modifier.size(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            AegisText(
                                text = "Search passwords, cards, notes",
                                style = AegisType.body,
                                color = colors.textTertiary,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .focusable(),
                            textStyle = AegisType.body.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.blue),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(Modifier.size(8.dp))
                        AegisIconGraphic(
                            icon = AegisIcon.X,
                            contentDescription = "Clear",
                            tint = colors.textSecondary,
                            size = 16.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { query = "" }
                                .padding(4.dp),
                        )
                    }
                }
                Box(Modifier.size(10.dp))
                TextLink(text = "Cancel", onClick = onBack, style = AegisType.subhead)
            }

            if (query.isNotBlank()) {
                AegisText(
                    text = "${results.size} results for “$query”",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )

                // Type chips.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        label = "All",
                        selected = chips.size == ItemType.entries.size,
                        onClick = { chips = ItemType.entries.toSet() },
                    )
                    FilterChip(
                        label = "Logins",
                        selected = chips == setOf(ItemType.Login),
                        onClick = { chips = setOf(ItemType.Login) },
                    )
                    FilterChip(
                        label = "Notes",
                        selected = chips == setOf(ItemType.Note),
                        onClick = { chips = setOf(ItemType.Note) },
                    )
                    FilterChip(
                        label = "Cards",
                        selected = chips == setOf(ItemType.Card),
                        onClick = { chips = setOf(ItemType.Card) },
                    )
                }

                if (results.isEmpty()) {
                    EmptyState(
                        icon = AegisIcon.Search,
                        accent = colors.blue,
                        title = "No matches",
                        text = "Nothing in the vault matches “$query”. Try a shorter query or a different type.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        results.forEach { result ->
                            ItemRow(
                                title = result.title,
                                subtitle = result.subtitle,
                                badgeLetter = result.badgeLetter,
                                badgeColor = badgeColorFor(result.title),
                                onClick = {
                                    when (result.itemType) {
                                        ItemType.Login, ItemType.Identity -> onOpenLogin(result.id)
                                        ItemType.Card -> onOpenCard(result.id)
                                        ItemType.Note -> onOpenNote(result.id)
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                // Suggestions.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    AegisText(
                        text = "Try searching for",
                        style = AegisType.bodyStrong,
                        color = colors.textPrimary,
                    )
                    Box(Modifier.size(6.dp))
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { query = suggestion }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AegisIconGraphic(
                                icon = AegisIcon.Search,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                size = 15.dp,
                            )
                            Box(Modifier.size(10.dp))
                            AegisText(
                                text = suggestion,
                                style = AegisType.body,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
