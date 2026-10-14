package com.aegis.vault.data.model

/** The four item families in the vault, matching the L4 category tiles. */
enum class ItemType {
    Login, Card, Note, Identity;

    companion object {
        fun fromRoute(value: String): ItemType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Login
    }
}

/**
 * A saved login. The password is stored as plaintext INSIDE the SQLCipher database —
 * the whole file is AES-256 encrypted at rest, so plaintext columns are the design.
 */
data class LoginItem(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val website: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val favorite: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L,
    val trashedAt: Long? = null,
) {
    val domain: String get() = website.removePrefix("https://").removePrefix("http://").removeSuffix("/")
}

/** A saved payment card (L8). */
data class CardItem(
    val id: String,
    val title: String,
    val bankName: String = "",
    val network: String = "",
    val cardholder: String = "",
    val number: String = "",
    val expiry: String = "",
    val securityCode: String = "",
    val billingAddress: String = "",
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L,
    val trashedAt: Long? = null,
) {
    val maskedNumber: String
        get() = if (number.length >= 4) "•••• " + number.takeLast(4) else "••••"
}

/** A secure note (L9). */
data class NoteItem(
    val id: String,
    val title: String,
    val body: String = "",
    val tags: List<String> = emptyList(),
    val isDemo: Boolean = false,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L,
    val trashedAt: Long? = null,
)

/** A saved identity (used by the Identity category tile). */
data class IdentityItem(
    val id: String,
    val title: String,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val isDemo: Boolean = false,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L,
    val trashedAt: Long? = null,
)

/** A past password kept in the login's history (L6 "Password history"). */
data class PasswordHistoryEntry(
    val id: Long,
    val password: String,
    val changedAt: Long,
)

/** Counts for the L4 category tiles and storage strip. */
data class VaultStats(
    val logins: Int = 0,
    val cards: Int = 0,
    val notes: Int = 0,
    val identities: Int = 0,
) {
    val total: Int get() = logins + cards + notes + identities
    val categoriesText: String
        get() = listOf(
            ItemType.Login to logins,
            ItemType.Card to cards,
            ItemType.Note to notes,
            ItemType.Identity to identities,
        ).joinToString(" · ") { (type, count) -> "$count ${type.name.lowercase()}s" }
}

/** One result row in the L13 search screen. */
data class SearchResult(
    val id: String,
    val itemType: ItemType,
    val title: String,
    val subtitle: String,
    val badgeLetter: String,
)
