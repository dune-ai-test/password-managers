package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.ActionRow
import com.aegis.vault.ui.components.ConfirmDialog
import com.aegis.vault.ui.components.DetailRow
import com.aegis.vault.ui.components.HairlineDivider
import com.aegis.vault.ui.components.Pill
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L8 — card detail with the visual credit card, its detail rows and actions.
 */
@Composable
fun CardDetailScreen(
    locator: ServiceLocator,
    cardId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    val card = remember(version, cardId) { repo.card(cardId) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }

    if (card == null) {
        LaunchedEffect(cardId) { onBack() }
        return
    }

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopNavBar(
                    title = card.title,
                    onBack = onBack,
                    backContentDescription = "Back",
                    trailingLabel = "Edit",
                    trailingAction = onEdit,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // The visual credit card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF117ACA), Color(0xFF0A3D6B)),
                                ),
                            )
                            .padding(20.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color(0x14FFFFFF)),
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AegisText(
                                    text = card.bankName.ifBlank { card.title },
                                    style = AegisType.bodyStrong.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                                Pill(
                                    text = card.network.ifBlank { "CARD" },
                                    background = Color(0x33FFFFFF),
                                    contentColor = Color.White,
                                    textStyle = AegisType.captionStrong,
                                )
                            }
                            AegisText(
                                text = card.number.chunked(4).joinToString(" "),
                                style = AegisType.navTitle.copy(fontSize = 20.sp, letterSpacing = 2.sp),
                                color = Color.White,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    AegisText(
                                        text = "CARDHOLDER",
                                        style = AegisType.mini,
                                        color = Color(0x99FFFFFF),
                                    )
                                    AegisText(
                                        text = card.cardholder,
                                        style = AegisType.captionStrong,
                                        color = Color.White,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    AegisText(
                                        text = "EXPIRES",
                                        style = AegisType.mini,
                                        color = Color(0x99FFFFFF),
                                    )
                                    AegisText(
                                        text = card.expiry,
                                        style = AegisType.captionStrong,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }

                    copiedLabel?.let { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.greenTint)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AegisIconGraphic(
                                icon = AegisIcon.Check,
                                contentDescription = null,
                                tint = colors.green,
                                size = 14.dp,
                            )
                            AegisText(label, style = AegisType.caption, color = colors.textPrimary)
                        }
                    }

                    // Detail rows.
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                        Column {
                            DetailRow(
                                label = "Cardholder",
                                value = card.cardholder,
                                onClick = {
                                    if (copyToClipboard(context, "Cardholder", card.cardholder)) {
                                        copiedLabel = "Cardholder copied"
                                    }
                                },
                                trailingIcon = AegisIcon.Copy,
                            )
                            HairlineDivider()
                            DetailRow(
                                label = "Number",
                                value = card.number,
                                onClick = {
                                    if (copyToClipboard(context, "Card number", card.number)) {
                                        copiedLabel = "Card number copied"
                                    }
                                },
                                trailingIcon = AegisIcon.Copy,
                            )
                            HairlineDivider()
                            DetailRow(label = "Expires", value = card.expiry)
                            HairlineDivider()
                            DetailRow(label = "Security code", value = card.securityCode.ifBlank { "•••" })
                            HairlineDivider()
                            DetailRow(
                                label = "Billing address",
                                value = card.billingAddress.ifBlank { "Not set" },
                            )
                        }
                    }

                    // Actions.
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                        Column {
                            ActionRow(
                                icon = AegisIcon.Copy,
                                label = "Copy card number",
                                onClick = {
                                    if (copyToClipboard(context, "Card number", card.number)) {
                                        copiedLabel = "Card number copied"
                                    }
                                },
                            )
                            ActionRow(
                                icon = AegisIcon.Share2,
                                label = "Share via file",
                                onClick = {
                                    shareText(
                                        context = context,
                                        subject = card.title,
                                        text = "${card.title}\n${card.number}\n${card.cardholder}\n${card.expiry}",
                                    )
                                },
                            )
                            ActionRow(
                                icon = AegisIcon.Trash2,
                                label = "Move to trash",
                                tint = colors.red,
                                destructive = true,
                                onClick = { confirmTrash = true },
                            )
                        }
                    }
                }
            }

            if (confirmTrash) {
                ConfirmDialog(
                    title = "Move to trash?",
                    message = "The card stays in the encrypted vault until you delete it.",
                    confirmLabel = "Move to trash",
                    destructive = true,
                    onConfirm = {
                        repo.deleteCard(card.id)
                        confirmTrash = false
                        onBack()
                    },
                    onDismiss = { confirmTrash = false },
                )
            }
        }
    }
}

/**
 * L9 — secure note: title, meta, body, tags, and the note actions.
 */
@Composable
fun NoteDetailScreen(
    locator: ServiceLocator,
    noteId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    val note = remember(version, noteId) { repo.note(noteId) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }

    if (note == null) {
        LaunchedEffect(noteId) { onBack() }
        return
    }

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopNavBar(
                    title = note.title,
                    onBack = onBack,
                    backContentDescription = "Back",
                    trailingLabel = "Edit",
                    trailingAction = onEdit,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AegisText(note.title, style = AegisType.titleCompact)
                            AegisText(
                                text = "Edited ${relativeTime(note.lastModified)} · saved locally",
                                style = AegisType.caption,
                                color = colors.textSecondary,
                            )
                            Box(Modifier.size(8.dp))
                            AegisText(
                                text = note.body,
                                style = AegisType.subhead.copy(lineHeight = 22.sp),
                                color = colors.textPrimary,
                            )
                            if (note.tags.isNotEmpty()) {
                                Box(Modifier.size(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    note.tags.forEach { tag ->
                                        Pill(text = tag)
                                    }
                                }
                            }
                        }
                    }

                    copiedLabel?.let { label ->
                        AegisText(
                            text = label,
                            style = AegisType.caption,
                            color = colors.green,
                        )
                    }

                    // Actions.
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                        Column {
                            ActionRow(
                                icon = AegisIcon.Copy,
                                label = "Copy note",
                                onClick = {
                                    if (copyToClipboard(context, "Note", "${note.title}\n${note.body}")) {
                                        copiedLabel = "Note copied"
                                    }
                                },
                            )
                            ActionRow(
                                icon = AegisIcon.Share2,
                                label = "Export to file",
                                onClick = {
                                    shareText(
                                        context = context,
                                        subject = note.title,
                                        text = note.body,
                                    )
                                },
                            )
                            ActionRow(
                                icon = AegisIcon.Trash2,
                                label = "Move to trash",
                                tint = colors.red,
                                destructive = true,
                                onClick = { confirmTrash = true },
                            )
                        }
                    }
                }
            }

            if (confirmTrash) {
                ConfirmDialog(
                    title = "Move to trash?",
                    message = "The note stays in the encrypted vault until you delete it.",
                    confirmLabel = "Move to trash",
                    destructive = true,
                    onConfirm = {
                        repo.deleteNote(note.id)
                        confirmTrash = false
                        onBack()
                    },
                    onDismiss = { confirmTrash = false },
                )
            }
        }
    }
}
