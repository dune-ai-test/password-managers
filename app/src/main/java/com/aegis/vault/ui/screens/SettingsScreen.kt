package com.aegis.vault.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.security.MasterPasswordManager
import com.aegis.vault.ui.auth.rememberBiometricPromptHost
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.icons.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.ConfirmDialog
import com.aegis.vault.ui.components.LetterBadge
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SecondaryButton
import com.aegis.vault.ui.components.SettingsRow
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * L12 — Settings. Rows are wired to real behavior where the app supports it:
 * auto-lock, biometric unlock, master password change, autofill deep-link,
 * import/export, backup timestamp, demo-data cleanup, and lock now.
 */
@Composable
fun SettingsScreen(
    locator: ServiceLocator,
    onLockNow: () -> Unit,
    onAbout: () -> Unit,
    onHelp: () -> Unit,
    onChangeMasterPassword: () -> Unit,
    onAutoLock: () -> Unit,
    onTwoFactorInfo: () -> Unit,
    onSharedItemsInfo: () -> Unit,
    onStorageInfo: () -> Unit,
    onImportExport: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository
    val master = locator.masterPasswordManager

    var version by remember { mutableIntStateOf(repo.dataVersion.value) }
    LaunchedEffect(Unit) { repo.dataVersion.collect { version = it } }

    var biometricEnabled by remember { mutableStateOf(master.biometricUnlockEnabled) }
    var autolockLabel by remember { mutableStateOf(MasterPasswordManager.optionForAutoLockSeconds(master.autoLockSeconds)) }
    var backupLabel by remember {
        mutableStateOf(
            master.lastBackupAtMillis
                ?.let { formatRelativeShort(it) }
                ?: "Never",
        )
    }
    var showDeleteDemo by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val promptHost = rememberBiometricPromptHost()

    val dbSize = remember(version) { formatStorageSize(repo.databaseSizeBytes()) }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AegisText("Settings", style = AegisType.title)

            // Profile card.
            AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LetterBadge(
                        letter = "A",
                        color = colors.blue,
                        size = 40.dp,
                    )
                    Box(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        AegisText("Alex Morgan", style = AegisType.bodyStrong)
                        Box(Modifier.size(2.dp))
                        AegisText(
                            text = "Local profile · no account needed",
                            style = AegisType.footnote,
                            color = colors.textSecondary,
                        )
                    }
                    AegisIconGraphic(
                        icon = AegisIcon.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        size = 14.dp,
                    )
                }
            }

            // Group 1 — security.
            AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                Column {
                    SettingsRow(
                        icon = AegisIcon.Lock,
                        accent = colors.blue,
                        label = "Auto-lock",
                        value = autolockLabel,
                        onClick = onAutoLock,
                    )
                    SettingsRow(
                        icon = AegisIcon.ScanFace,
                        accent = colors.blue,
                        label = "Fingerprint unlock",
                        toggleState = biometricEnabled,
                        onToggle = { want ->
                            if (want) {
                                val passphrase = locator.sessionHolder.passphrase.value
                                if (passphrase == null) {
                                    message = "Unlock the vault first"
                                    return@SettingsRow
                                }
                                val cipher = try {
                                    com.aegis.vault.data.security.BiometricGate.prepareWrapCipher()
                                } catch (e: Exception) {
                                    message = "Could not enable fingerprint unlock"
                                    return@SettingsRow
                                }
                                promptHost.show(
                                    cipher = cipher,
                                    title = "Enable fingerprint unlock",
                                    subtitle = "Confirm it's you to finish setup",
                                    negativeButton = "Cancel",
                                    onSuccess = { result ->
                                        try {
                                            val wrapped = com.aegis.vault.data.security.BiometricGate.wrap(
                                                locator.sessionHolder.passphrase.value ?: passphrase,
                                                result,
                                            )
                                            master.enableBiometricUnlock(wrapped)
                                            biometricEnabled = true
                                        } catch (e: Exception) {
                                            message = "Could not enable fingerprint unlock"
                                        }
                                    },
                                    onError = { /* user cancelled */ },
                                )
                            } else {
                                master.disableBiometricUnlock()
                                biometricEnabled = false
                            }
                        },
                    )
                    SettingsRow(
                        icon = AegisIcon.KeyRound,
                        accent = colors.blue,
                        label = "Master password",
                        onClick = onChangeMasterPassword,
                    )
                    SettingsRow(
                        icon = AegisIcon.ShieldCheck,
                        accent = colors.green,
                        label = "Two-factor auth",
                        value = "On",
                        onClick = onTwoFactorInfo,
                    )
                }
            }

            // Group 2 — data.
            AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                Column {
                    SettingsRow(
                        icon = AegisIcon.Settings,
                        accent = colors.purple,
                        label = "Autofill",
                        value = "On",
                        onClick = {
                            autofillSettingsIntent(context)?.let { context.startActivity(it) }
                                ?: run {
                                    message = "Open Settings → Security → Autofill to enable Aegis"
                                }
                        },
                    )
                    SettingsRow(
                        icon = AegisIcon.Share2,
                        accent = colors.blue,
                        label = "Import & export",
                        onClick = onImportExport,
                    )
                    SettingsRow(
                        icon = AegisIcon.HardDrive,
                        accent = colors.green,
                        label = "Backup",
                        value = backupLabel,
                        onClick = onImportExport,
                    )
                    SettingsRow(
                        icon = AegisIcon.User,
                        accent = colors.blue,
                        label = "Shared items",
                        onClick = onSharedItemsInfo,
                    )
                    SettingsRow(
                        icon = AegisIcon.FileText,
                        accent = colors.amber,
                        label = "Storage",
                        value = "This device · $dbSize",
                        onClick = onStorageInfo,
                    )
                }
            }

            // Group 3 — app.
            AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                Column {
                    SettingsRow(
                        icon = AegisIcon.HardDrive,
                        accent = colors.red,
                        label = "Lock now",
                        onClick = onLockNow,
                    )
                    SettingsRow(
                        icon = AegisIcon.Bell,
                        accent = colors.red,
                        label = "Help & support",
                        onClick = onHelp,
                    )
                    SettingsRow(
                        icon = AegisIcon.Shield,
                        accent = colors.blue,
                        label = "About Aegis",
                        onClick = onAbout,
                    )
                }
            }

            // Group 4 — destructive / maintenance.
            AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                Column {
                    SettingsRow(
                        icon = AegisIcon.Trash2,
                        accent = colors.red,
                        label = "Delete sample data",
                        onClick = { showDeleteDemo = true },
                    )
                }
            }

            message?.let { text ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.tintFor(colors.amber))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    AegisText(text, style = AegisType.caption, color = colors.textPrimary)
                }
            }

            AegisText(
                text = "Aegis · Version ${MasterPasswordManager.DEFAULT_VERSION}",
                style = AegisType.caption,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showDeleteDemo) {
        ConfirmDialog(
            title = "Delete sample data?",
            message = "Removes every demo logins, card, note and identity from the vault. Your own items stay.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                repo.deleteDemoData()
                showDeleteDemo = false
            },
            onDismiss = { showDeleteDemo = false },
        )
    }
}

/** Auto-lock picker (destination of the Settings → Auto-lock row). */
@Composable
fun AutoLockScreen(
    locator: ServiceLocator,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val master = locator.masterPasswordManager
    var current by remember { mutableStateOf(master.autoLockSeconds) }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "Auto-lock", onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    Column {
                        MasterPasswordManager.AUTOLOCK_OPTIONS.forEach { option ->
                            val seconds = MasterPasswordManager.autoLockSecondsForOption(option)
                            val selected = seconds == current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) colors.tintFor(colors.blue) else Color.Transparent)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                    ) { current = seconds }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AegisText(
                                    text = option,
                                    style = AegisType.body,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    AegisIconGraphic(
                                        icon = AegisIcon.Check,
                                        contentDescription = null,
                                        tint = colors.blue,
                                        size = 16.dp,
                                    )
                                }
                            }
                        }
                    }
                }
                SecondaryButton(
                    text = "Save",
                    onClick = {
                        master.autoLockSeconds = current
                        onBack()
                    },
                )
            }
        }
    }
}

/** About screen with the version from Settings. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = AegisTheme.colors
    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "About Aegis", onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(24.dp))
                com.aegis.vault.ui.components.AegisLogo(size = 88.dp)
                AegisText("Aegis", style = AegisType.title)
                AegisText(
                    text = "Version ${MasterPasswordManager.DEFAULT_VERSION}",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )
                AegisText(
                    text = "Aegis keeps every credential in a single SQLCipher-encrypted database on this " +
                        "device. There is no account, no sync and no network permission — the app cannot " +
                        "phone home even if it wanted to. Backups are encrypted files you create and " +
                        "share yourself.",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                AegisText(
                    text = "Made for people who read the fine print.",
                    style = AegisType.caption,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

/** Help & support facts. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val colors = AegisTheme.colors
    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "Help & support", onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HelpCard(
                    title = "How do I unlock with a fingerprint?",
                    body = "Create a master password with the toggle on, then confirm your fingerprint when asked. " +
                        "The passphrase is then kept in the device keystore and needs your biometric every time.",
                )
                HelpCard(
                    title = "I forgot my master password",
                    body = "Aegis cannot reset it — that is what makes a local vault safe. If you created an " +
                        "encrypted backup in Import & export, you can start fresh and import it with that password.",
                )
                HelpCard(
                    title = "What does the Security tab measure?",
                    body = "Weak passwords (short, too simple, or known), passwords shared across logins, " +
                        "logins without two-factor, and domains in the bundled breach catalog.",
                )
                HelpCard(
                    title = "Can I move to another phone?",
                    body = "Export an encrypted backup from Import & export, then import it on the new device " +
                        "with the backup password you chose.",
                )
            }
        }
    }
}

/** Generic info sheet (two-factor, shared items, storage). */
@Composable
fun InfoSheetScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = title, onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                    AegisText(body, style = AegisType.subhead, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun HelpCard(title: String, body: String) {
    val colors = AegisTheme.colors
    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AegisText(title, style = AegisType.bodyStrong)
            AegisText(body, style = AegisType.footnote, color = colors.textSecondary)
        }
    }
}

/** Compact relative time used for the Backup row ("2 min ago"). */
internal fun formatRelativeShort(timestamp: Long): String = relativeTime(timestamp)

