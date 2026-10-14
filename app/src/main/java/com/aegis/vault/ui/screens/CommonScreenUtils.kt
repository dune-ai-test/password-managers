package com.aegis.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.ui.theme.AegisTheme
import java.util.concurrent.TimeUnit

/** Deterministic badge color for a vault item, iOS-style app-icon palette. */
fun badgeColorFor(seed: String): androidx.compose.ui.graphics.Color {
    val palette = listOf(
        0xFF007AFF, 0xFF34C759, 0xFFFF9500, 0xFFAF52DE, 0xFFFF3B30, 0xFF5E5CE6, 0xFF117ACA,
    )
    val hash = seed.lowercase().hashCode().let { if (it == Int.MIN_VALUE) 0 else it }
    return androidx.compose.ui.graphics.Color(palette[(hash and 0x7fffffff) % palette.size])
}

/** Accent color per item type, matching the L4 category tiles. */
fun accentForType(type: ItemType): androidx.compose.ui.graphics.Color = when (type) {
    ItemType.Login -> androidx.compose.ui.graphics.Color(0xFF007AFF)
    ItemType.Card -> androidx.compose.ui.graphics.Color(0xFFAF52DE)
    ItemType.Note -> androidx.compose.ui.graphics.Color(0xFF34C759)
    ItemType.Identity -> androidx.compose.ui.graphics.Color(0xFFFF9500)
}

/** Copies [text] to the system clipboard, returning false when unavailable. */
fun copyToClipboard(context: Context, label: String, text: String): Boolean = try {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText(label, text)) != null
} catch (e: Exception) {
    false
}

/** Shares plain text with the system share sheet. */
fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

/** Shares a file by content:// URI. */
fun shareFile(context: Context, uri: Uri, mimeType: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

/** "2 weeks ago" style relative time, matching the mockup copy. */
fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0) return "never"
    val delta = (now - timestamp).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        days < 30 -> if (days == 1L) "1 day ago" else "$days days ago"
        days < 365 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}

/** The masked password string used in list/detail values (••• with wide spacing). */
fun maskedPassword(length: Int = 12): String = "•".repeat(length)

/** Formats a file size like the L4 storage strip. */
fun formatStorageSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

/** Opens the system autofill settings so the user can enable Aegis. */
fun autofillSettingsIntent(context: Context): Intent? = try {
    Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
        data = Uri.parse("package:${context.packageName}")
    }
} catch (e: Exception) {
    null
}
