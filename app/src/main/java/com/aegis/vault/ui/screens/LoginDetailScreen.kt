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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.ActionRow
import com.aegis.vault.ui.components.ConfirmDialog
import com.aegis.vault.ui.components.DetailRow
import com.aegis.vault.ui.components.HairlineDivider
import com.aegis.vault.ui.components.LetterBadge
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SecondaryButton
import com.aegis.vault.ui.components.SectionHeader
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L6 — login detail: hero, credential rows (with copy actions), the action card,
 * and the password history.
 */
@Composable
fun LoginDetailScreen(
    locator: ServiceLocator,
    loginId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    val login = remember(version, loginId) { repo.login(loginId) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    var confirmTrash by remember { mutableStateOf(false) }
    val history = remember(version, loginId) {
        login?.let { repo.passwordHistory(it.id) } ?: emptyList()
    }

    if (login == null) {
        LaunchedEffect(loginId) { onBack() }
        return
    }

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopNavBar(
                    title = login.title,
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
                // Hero.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LetterBadge(
                        letter = login.title.take(1),
                        color = badgeColorFor(login.title),
                        size = 64.dp,
                        textStyle = AegisType.titleCompact,
                    )
                    AegisText(login.title, style = AegisType.titleCompact)
                    AegisText(
                        text = login.domain,
                        style = AegisType.footnote,
                        color = colors.textSecondary,
                    )
                }

                // Credentials.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column {
                        DetailRow(
                            label = "Username",
                            value = login.username,
                            onClick = {
                                if (copyToClipboard(context, "Username", login.username)) {
                                    copiedLabel = "Username copied"
                                }
                            },
                            trailingIcon = AegisIcon.Copy,
                        )
                        HairlineDivider()
                        DetailRow(
                            label = "Password",
                            value = maskedPassword(),
                            onClick = {
                                if (copyToClipboard(context, "Password", login.password)) {
                                    copiedLabel = "Password copied"
                                }
                            },
                            trailingIcon = AegisIcon.Copy,
                        )
                        HairlineDivider()
                        DetailRow(
                            label = "Website",
                            value = login.domain,
                            trailingIcon = AegisIcon.Globe,
                        )
                        HairlineDivider()
                        DetailRow(
                            label = "Two-factor",
                            value = if (login.totpSecret.isNotBlank()) "Authenticator app" else "Not set up",
                        )
                        HairlineDivider()
                        DetailRow(
                            label = "Last changed",
                            value = relativeTime(login.lastModified),
                        )
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

                // Actions.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    Column {
                        ActionRow(
                            icon = AegisIcon.Copy,
                            label = "Copy password",
                            onClick = {
                                if (copyToClipboard(context, "Password", login.password)) {
                                    copiedLabel = "Password copied"
                                }
                            },
                        )
                        ActionRow(
                            icon = AegisIcon.Share2,
                            label = "Share login",
                            onClick = {
                                shareText(
                                    context = context,
                                    subject = login.title,
                                    text = "${login.title}\n${login.username}\n${login.website}",
                                )
                            },
                        )
                        ActionRow(
                            icon = AegisIcon.KeyRound,
                            label = "Add a passkey",
                            onClick = { copiedLabel = "Passkeys need the site's support first" },
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

                // Password history.
                if (history.isNotEmpty()) {
                    SectionHeader(title = "Password history")
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                        Column {
                            history.take(2).forEachIndexed { index, entry ->
                                if (index > 0) HairlineDivider()
                                DetailRow(
                                    label = if (index == 0) "Previous" else "Older",
                                    value = maskedPassword(10),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (confirmTrash) {
            ConfirmDialog(
                title = "Move to trash?",
                message = "The login stays in the encrypted vault until you delete it.",
                confirmLabel = "Move to trash",
                destructive = true,
                onConfirm = {
                    repo.deleteLogin(login.id)
                    confirmTrash = false
                    onBack()
                },
                onDismiss = { confirmTrash = false },
            )
        }
        }
    }
}
