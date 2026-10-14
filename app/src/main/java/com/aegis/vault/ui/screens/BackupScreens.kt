package com.aegis.vault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aegis.vault.core.BackupManager
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.ui.components.AegisCard
import com.aegis.vault.ui.components.AegisIconGraphic
import com.aegis.vault.ui.components.AegisText
import com.aegis.vault.ui.components.PrimaryButton
import com.aegis.vault.ui.components.ScreenBackground
import com.aegis.vault.ui.components.SecondaryButton
import com.aegis.vault.ui.components.TopNavBar
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.theme.AegisTheme
import com.aegis.vault.ui.theme.AegisType

/**
 * Import & export — the real encrypted backup flow. Exports write a
 * PBKDF2 + AES-GCM encrypted `.aegis` file through the app's FileProvider and
 * hand it to the system share sheet; imports read a file you pick and restore it
 * into the open vault. Nothing is uploaded anywhere.
 */
@Composable
fun ExportImportScreen(
    locator: ServiceLocator,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val repo = locator.vaultRepository
    val master = locator.masterPasswordManager
    val backupManager = remember { BackupManager(context) }

    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val snapshot = backupManager.buildSnapshot(repo)
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val blob = com.aegis.vault.data.crypto.VaultCrypto.encryptWithPassword(
                    snapshot.toByteArray(Charsets.UTF_8),
                    exportPassword.toCharArray(),
                )
                stream.write(blob)
            } ?: error("Destination unavailable")
            master.lastBackupAtMillis = System.currentTimeMillis()
            status = "Backup written"
            error = null
        } catch (e: Exception) {
            error = "Could not write the backup"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        status = "File picked — enter the backup password and tap Restore"
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "Import & export", onBack = onBack, backContentDescription = "Back")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                status?.let {
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp, color = colors.greenTint) {
                        AegisText(it, style = AegisType.footnote, color = colors.textPrimary)
                    }
                }
                error?.let {
                    AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp, color = colors.redTint) {
                        AegisText(it, style = AegisType.footnote, color = colors.red)
                    }
                }

                // Export.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AegisText("Export", style = AegisType.bodyStrong)
                        AegisText(
                            text = "Choose a backup password; the vault is encrypted with it before it is " +
                                "written anywhere. Without it the file is unreadable.",
                            style = AegisType.footnote,
                            color = colors.textSecondary,
                        )
                        BackupPasswordField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = "Backup password",
                        )
                        SecondaryButton(
                            text = "Choose destination…",
                            onClick = {
                                if (exportPassword.length < 8) {
                                    error = "Use at least 8 characters"
                                } else {
                                    exportLauncher.launch("aegis-backup.aegis")
                                }
                            },
                        )
                    }
                }

                // Import.
                AegisCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AegisText("Import", style = AegisType.bodyStrong)
                        AegisText(
                            text = "Pick an encrypted .aegis backup and enter its backup password. Importing " +
                                "replaces everything currently in the vault.",
                            style = AegisType.footnote,
                            color = colors.textSecondary,
                        )
                        BackupPasswordField(
                            value = importPassword,
                            onValueChange = { importPassword = it },
                            label = "Backup password",
                        )
                        SecondaryButton(
                            text = "Pick file…",
                            onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                        )
                        pendingImportUri?.let { uri ->
                            PrimaryButton(
                                text = "Restore into vault",
                                onClick = {
                                    val json = backupManager.readEncryptedBackup(uri, importPassword)
                                    if (json == null) {
                                        error = "Wrong backup password or corrupt file"
                                    } else if (backupManager.restoreSnapshot(json, repo)) {
                                        status = "Vault restored"
                                        error = null
                                        pendingImportUri = null
                                    } else {
                                        error = "That file is not an Aegis backup"
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Change master password: verifies the old one, re-keys the database, stores the new salt. */
@Composable
fun ChangeMasterPasswordScreen(
    locator: ServiceLocator,
    onBack: () -> Unit,
) {
    val colors = AegisTheme.colors
    val context = LocalContext.current
    val master = locator.masterPasswordManager

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopNavBar(title = "Master password", onBack = onBack, backContentDescription = "Back")
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AegisText(
                    text = "Change the master password. The vault is re-keyed in place — nothing is re-uploaded, " +
                        "and the old password stops working immediately.",
                    style = AegisType.footnote,
                    color = colors.textSecondary,
                )

                BackupPasswordField("Current password", currentPassword) { currentPassword = it }
                BackupPasswordField("New password", newPassword) { newPassword = it }
                BackupPasswordField("Confirm new password", confirmPassword) { confirmPassword = it }

                error?.let {
                    AegisText(it, style = AegisType.footnote, color = colors.red)
                }
                if (done) {
                    AegisText(
                        "Master password updated.",
                        style = AegisType.footnote,
                        color = colors.green,
                    )
                }

                PrimaryButton(
                    text = "Update master password",
                    onClick = {
                        when {
                            newPassword.length < 12 -> error = "Use at least 12 characters"
                            newPassword != confirmPassword -> error = "Confirmation does not match"
                            master.unlockWithPassword(currentPassword) == null ->
                                error = "Current password is wrong"
                            else -> {
                                val db = locator.openDatabase
                                if (db == null) {
                                    error = "Vault is locked — unlock it first"
                                } else {
                                    try {
                                        // 1. store the new salt + verifier, deriving the new passphrase
                                        // 2. re-key the live SQLCipher database with that passphrase
                                        // 3. swap the in-memory session passphrase (no reopen needed)
                                        val newPassphrase = master.changeMasterPassword(newPassword)
                                        db.readable.changePassword(
                                            com.aegis.vault.data.crypto.VaultCrypto
                                                .toHexString(newPassphrase)
                                                .toByteArray(),
                                        )
                                        locator.sessionHolder.unlock(newPassphrase)
                                        // The biometric wrap encrypts the passphrase, so the old
                                        // wrapped copy is stale after a change — drop it.
                                        master.disableBiometricUnlock()
                                        error = null
                                        done = true
                                    } catch (e: Exception) {
                                        error = "Could not change the master password"
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BackupPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = AegisTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        AegisText(label, style = AegisType.caption, color = colors.textSecondary)
        Box(Modifier.size(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = AegisType.body,
            cursorBrush = SolidColor(colors.blue),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (value.isEmpty()) {
                        AegisText("••••••••", style = AegisType.body, color = colors.textTertiary)
                    }
                    inner()
                }
            },
        )
    }
}
