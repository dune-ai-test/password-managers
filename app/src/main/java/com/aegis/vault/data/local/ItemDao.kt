package com.aegis.vault.data.local

import com.aegis.vault.data.model.CardItem
import com.aegis.vault.data.model.IdentityItem
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.data.model.NoteItem
import com.aegis.vault.data.model.PasswordHistoryEntry
import com.aegis.vault.data.model.SearchResult

/**
 * All vault reads and writes. Every method runs on the caller's dispatcher — the
 * repository guarantees they are called from [kotlinx.coroutines.Dispatchers.IO].
 *
 * Passwords and card numbers are stored as plaintext INSIDE the SQLCipher
 * database; the file itself is AES-256 encrypted, so the plaintext never exists
 * on disk alone.
 */
class ItemDao(private val db: VaultDatabase) {

    private val now = { System.currentTimeMillis() }

    // ---------------------------------------------------------------- logins

    fun allLogins(): List<LoginItem> = queryItems(TABLE_LOGIN) { c -> c.toLogin() }

    fun login(id: String): LoginItem? =
        queryOne(TABLE_LOGIN, "id = ?", arrayOf(id)) { c -> c.toLogin() }

    fun recentLogins(limit: Int): List<LoginItem> =
        db.rawQuery(
            "SELECT * FROM $TABLE_LOGIN WHERE trashed_at IS NULL ORDER BY last_modified DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { it.toItems { c -> c.toLogin() } }

    fun insertLogin(item: LoginItem, historyPassword: String? = null): LoginItem {
        val row = VaultDatabase.ContentRow()
            .put("id", item.id)
            .put("title", item.title)
            .put("username", item.username)
            .put("password", item.password)
            .put("website", item.website)
            .put("notes", item.notes)
            .put("totp_secret", item.totpSecret)
            .put("is_demo", if (item.isDemo) 1 else 0)
            .put("created_at", if (item.createdAt != 0L) item.createdAt else now())
            .put("last_modified", now())
        db.insert(TABLE_LOGIN, row)
        if (historyPassword != null) addPasswordHistory(item.id, historyPassword)
        return item.copy(createdAt = if (item.createdAt != 0L) item.createdAt else now(), lastModified = now())
    }

    fun updateLogin(item: LoginItem, previousPassword: String? = null): LoginItem {
        val row = VaultDatabase.ContentRow()
            .put("title", item.title)
            .put("username", item.username)
            .put("password", item.password)
            .put("website", item.website)
            .put("notes", item.notes)
            .put("totp_secret", item.totpSecret)
            .put("last_modified", now())
        db.update(TABLE_LOGIN, row, "id = ?", arrayOf(item.id))
        if (previousPassword != null && previousPassword != item.password) {
            addPasswordHistory(item.id, previousPassword)
        }
        return item.copy(lastModified = now())
    }

    fun trashLogin(id: String) {
        db.update(TABLE_LOGIN, VaultDatabase.ContentRow().put("trashed_at", now()), "id = ?", arrayOf(id))
    }

    fun addPasswordHistory(loginId: String, password: String) {
        val row = VaultDatabase.ContentRow()
            .put("login_id", loginId)
            .put("password", password)
            .put("changed_at", now())
        db.insert(TABLE_PASSWORD_HISTORY, row)
    }

    fun passwordHistory(loginId: String): List<PasswordHistoryEntry> =
        db.rawQuery(
            "SELECT * FROM $TABLE_PASSWORD_HISTORY WHERE login_id = ? ORDER BY changed_at DESC LIMIT 8",
            arrayOf(loginId),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        PasswordHistoryEntry(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            password = c.getString(c.getColumnIndexOrThrow("password")),
                            changedAt = c.getLong(c.getColumnIndexOrThrow("changed_at")),
                        ),
                    )
                }
            }
        }

    // ----------------------------------------------------------------- cards

    fun allCards(): List<CardItem> = queryItems(TABLE_CARD) { c -> c.toCard() }

    fun card(id: String): CardItem? =
        queryOne(TABLE_CARD, "id = ?", arrayOf(id)) { c -> c.toCard() }

    fun insertCard(item: CardItem): CardItem {
        val ts = now()
        val row = VaultDatabase.ContentRow()
            .put("id", item.id)
            .put("title", item.title)
            .put("bank_name", item.bankName)
            .put("network", item.network)
            .put("cardholder", item.cardholder)
            .put("number", item.number)
            .put("expiry", item.expiry)
            .put("security_code", item.securityCode)
            .put("billing_address", item.billingAddress)
            .put("notes", item.notes)
            .put("is_demo", if (item.isDemo) 1 else 0)
            .put("created_at", if (item.createdAt != 0L) item.createdAt else ts)
            .put("last_modified", ts)
        db.insert(TABLE_CARD, row)
        return item.copy(createdAt = if (item.createdAt != 0L) item.createdAt else ts, lastModified = ts)
    }

    fun updateCard(item: CardItem): CardItem {
        val row = VaultDatabase.ContentRow()
            .put("title", item.title)
            .put("bank_name", item.bankName)
            .put("network", item.network)
            .put("cardholder", item.cardholder)
            .put("number", item.number)
            .put("expiry", item.expiry)
            .put("security_code", item.securityCode)
            .put("billing_address", item.billingAddress)
            .put("notes", item.notes)
            .put("last_modified", now())
        db.update(TABLE_CARD, row, "id = ?", arrayOf(item.id))
        return item.copy(lastModified = now())
    }

    fun trashCard(id: String) {
        db.update(TABLE_CARD, VaultDatabase.ContentRow().put("trashed_at", now()), "id = ?", arrayOf(id))
    }

    // ----------------------------------------------------------------- notes

    fun allNotes(): List<NoteItem> = queryItems(TABLE_NOTE) { c -> c.toNote() }

    fun note(id: String): NoteItem? =
        queryOne(TABLE_NOTE, "id = ?", arrayOf(id)) { c -> c.toNote() }

    fun insertNote(item: NoteItem): NoteItem {
        val ts = now()
        val row = VaultDatabase.ContentRow()
            .put("id", item.id)
            .put("title", item.title)
            .put("body", item.body)
            .put("tags", item.tags.joinToString(","))
            .put("is_demo", if (item.isDemo) 1 else 0)
            .put("created_at", if (item.createdAt != 0L) item.createdAt else ts)
            .put("last_modified", ts)
        db.insert(TABLE_NOTE, row)
        return item.copy(createdAt = if (item.createdAt != 0L) item.createdAt else ts, lastModified = ts)
    }

    fun updateNote(item: NoteItem): NoteItem {
        val row = VaultDatabase.ContentRow()
            .put("title", item.title)
            .put("body", item.body)
            .put("tags", item.tags.joinToString(","))
            .put("last_modified", now())
        db.update(TABLE_NOTE, row, "id = ?", arrayOf(item.id))
        return item.copy(lastModified = now())
    }

    fun trashNote(id: String) {
        db.update(TABLE_NOTE, VaultDatabase.ContentRow().put("trashed_at", now()), "id = ?", arrayOf(id))
    }

    // ------------------------------------------------------------ identities

    fun allIdentities(): List<IdentityItem> = queryItems(TABLE_IDENTITY) { c -> c.toIdentity() }

    fun identity(id: String): IdentityItem? =
        queryOne(TABLE_IDENTITY, "id = ?", arrayOf(id)) { c -> c.toIdentity() }

    fun insertIdentity(item: IdentityItem): IdentityItem {
        val ts = now()
        val row = VaultDatabase.ContentRow()
            .put("id", item.id)
            .put("title", item.title)
            .put("first_name", item.firstName)
            .put("last_name", item.lastName)
            .put("email", item.email)
            .put("phone", item.phone)
            .put("address", item.address)
            .put("notes", item.notes)
            .put("is_demo", if (item.isDemo) 1 else 0)
            .put("created_at", if (item.createdAt != 0L) item.createdAt else ts)
            .put("last_modified", ts)
        db.insert(TABLE_IDENTITY, row)
        return item.copy(createdAt = if (item.createdAt != 0L) item.createdAt else ts, lastModified = ts)
    }

    fun updateIdentity(item: IdentityItem): IdentityItem {
        val row = VaultDatabase.ContentRow()
            .put("title", item.title)
            .put("first_name", item.firstName)
            .put("last_name", item.lastName)
            .put("email", item.email)
            .put("phone", item.phone)
            .put("address", item.address)
            .put("notes", item.notes)
            .put("last_modified", now())
        db.update(TABLE_IDENTITY, row, "id = ?", arrayOf(item.id))
        return item.copy(lastModified = now())
    }

    fun trashIdentity(id: String) {
        db.update(TABLE_IDENTITY, VaultDatabase.ContentRow().put("trashed_at", now()), "id = ?", arrayOf(id))
    }

    // ---------------------------------------------------------------- search

    /**
     * Full-text-ish search across titles, usernames, websites, note bodies and
     * card holders/masked numbers — backs the L13 search screen.
     */
    fun search(query: String, types: Set<ItemType> = ItemType.entries.toSet()): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val q = "%${query.trim().lowercase()}%"
        // "wifi" should also match "Wi-Fi": compare with separators stripped on both sides.
        val nq = "%${query.trim().lowercase().filter { it.isLetterOrDigit() }}%"
        val results = mutableListOf<SearchResult>()
        if (ItemType.Login in types) {
            db.rawQuery(
                """
                SELECT * FROM $TABLE_LOGIN WHERE trashed_at IS NULL
                AND (
                    lower(title) LIKE ? OR replace(lower(title),'-','') LIKE ?
                    OR lower(username) LIKE ? OR lower(website) LIKE ?
                    OR replace(lower(website),'.','') LIKE ? OR lower(notes) LIKE ?
                )
                ORDER BY last_modified DESC LIMIT 30
                """.trimIndent(),
                arrayOf(q, nq, q, q, nq, q),
            ).use { c ->
                while (c.moveToNext()) {
                    val item = c.toLogin()
                    results += SearchResult(item.id, ItemType.Login, item.title, item.username.ifBlank { item.domain }, item.title)
                }
            }
        }
        if (ItemType.Card in types) {
            db.rawQuery(
                """
                SELECT * FROM $TABLE_CARD WHERE trashed_at IS NULL
                AND (lower(title) LIKE ? OR lower(cardholder) LIKE ? OR lower(number) LIKE ?
                     OR replace(lower(title),'-','') LIKE ?)
                ORDER BY last_modified DESC LIMIT 30
                """.trimIndent(),
                arrayOf(q, q, q, nq),
            ).use { c ->
                while (c.moveToNext()) {
                    val item = c.toCard()
                    results += SearchResult(item.id, ItemType.Card, item.title, "card · ${item.maskedNumber}", item.title)
                }
            }
        }
        if (ItemType.Note in types) {
            db.rawQuery(
                """
                SELECT * FROM $TABLE_NOTE WHERE trashed_at IS NULL
                AND (lower(title) LIKE ? OR lower(body) LIKE ?
                     OR replace(lower(title),'-','') LIKE ? OR replace(lower(body),'-','') LIKE ?)
                ORDER BY last_modified DESC LIMIT 30
                """.trimIndent(),
                arrayOf(q, q, nq, nq),
            ).use { c ->
                while (c.moveToNext()) {
                    val item = c.toNote()
                    results += SearchResult(item.id, ItemType.Note, item.title, "secure note", item.title)
                }
            }
        }
        if (ItemType.Identity in types) {
            db.rawQuery(
                """
                SELECT * FROM $TABLE_IDENTITY WHERE trashed_at IS NULL
                AND (lower(title) LIKE ? OR lower(first_name) LIKE ? OR lower(last_name) LIKE ? OR lower(email) LIKE ?
                     OR replace(lower(title),'-','') LIKE ?)
                ORDER BY last_modified DESC LIMIT 30
                """.trimIndent(),
                arrayOf(q, q, q, q, nq),
            ).use { c ->
                while (c.moveToNext()) {
                    val item = c.toIdentity()
                    results += SearchResult(item.id, ItemType.Identity, item.title, item.email, item.title)
                }
            }
        }
        return results
    }

    // ----------------------------------------------------------------- misc

    fun isVaultEmpty(): Boolean {
        queryCount(TABLE_LOGIN).let { if (it > 0) return false }
        queryCount(TABLE_CARD).let { if (it > 0) return false }
        queryCount(TABLE_NOTE).let { if (it > 0) return false }
        queryCount(TABLE_IDENTITY).let { if (it > 0) return false }
        return true
    }

    fun deleteAllDemoItems() {
        db.delete(TABLE_LOGIN, "is_demo = 1", emptyArray())
        db.delete(TABLE_CARD, "is_demo = 1", emptyArray())
        db.delete(TABLE_NOTE, "is_demo = 1", emptyArray())
        db.delete(TABLE_IDENTITY, "is_demo = 1", emptyArray())
        db.exec("DELETE FROM $TABLE_PASSWORD_HISTORY")
    }

    fun deleteEverything() {
        db.exec("DELETE FROM $TABLE_LOGIN")
        db.exec("DELETE FROM $TABLE_CARD")
        db.exec("DELETE FROM $TABLE_NOTE")
        db.exec("DELETE FROM $TABLE_IDENTITY")
        db.exec("DELETE FROM $TABLE_PASSWORD_HISTORY")
    }

    private fun queryCount(table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    private fun <T> queryItems(table: String, mapper: (android.database.Cursor) -> T): List<T> =
        db.rawQuery("SELECT * FROM $table WHERE trashed_at IS NULL ORDER BY title ASC", null)
            .use { it.toItems(mapper) }

    private fun <T> queryOne(table: String, where: String, args: Array<String>, mapper: (android.database.Cursor) -> T): T? =
        db.rawQuery("SELECT * FROM $table WHERE $where LIMIT 1", args)
            .use { c -> if (c.moveToFirst()) mapper(c) else null }

    private fun <T> android.database.Cursor.toItems(mapper: (android.database.Cursor) -> T): List<T> =
        buildList {
            while (moveToNext()) add(mapper(this@toItems))
        }

    // ------------------------------------------------------- row mapping

    private fun android.database.Cursor.str(col: String): String =
        getString(getColumnIndexOrThrow(col)) ?: ""

    private fun android.database.Cursor.lng(col: String): Long =
        getLong(getColumnIndexOrThrow(col))

    private fun android.database.Cursor.trashedAt(): Long? =
        if (isNull(getColumnIndexOrThrow("trashed_at"))) null else getLong(getColumnIndexOrThrow("trashed_at"))

    private fun android.database.Cursor.toLogin() = LoginItem(
        id = str("id"),
        title = str("title"),
        username = str("username"),
        password = str("password"),
        website = str("website"),
        notes = str("notes"),
        totpSecret = str("totp_secret"),
        isDemo = getInt(getColumnIndexOrThrow("is_demo")) == 1,
        createdAt = lng("created_at"),
        lastModified = lng("last_modified"),
        trashedAt = trashedAt(),
    )

    private fun android.database.Cursor.toCard() = CardItem(
        id = str("id"),
        title = str("title"),
        bankName = str("bank_name"),
        network = str("network"),
        cardholder = str("cardholder"),
        number = str("number"),
        expiry = str("expiry"),
        securityCode = str("security_code"),
        billingAddress = str("billing_address"),
        notes = str("notes"),
        isDemo = getInt(getColumnIndexOrThrow("is_demo")) == 1,
        createdAt = lng("created_at"),
        lastModified = lng("last_modified"),
        trashedAt = trashedAt(),
    )

    private fun android.database.Cursor.toNote() = NoteItem(
        id = str("id"),
        title = str("title"),
        body = str("body"),
        tags = str("tags").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        isDemo = getInt(getColumnIndexOrThrow("is_demo")) == 1,
        createdAt = lng("created_at"),
        lastModified = lng("last_modified"),
        trashedAt = trashedAt(),
    )

    private fun android.database.Cursor.toIdentity() = IdentityItem(
        id = str("id"),
        title = str("title"),
        firstName = str("first_name"),
        lastName = str("last_name"),
        email = str("email"),
        phone = str("phone"),
        address = str("address"),
        notes = str("notes"),
        isDemo = getInt(getColumnIndexOrThrow("is_demo")) == 1,
        createdAt = lng("created_at"),
        lastModified = lng("last_modified"),
        trashedAt = trashedAt(),
    )

    companion object {
        const val TABLE_LOGIN = "login_items"
        const val TABLE_CARD = "card_items"
        const val TABLE_NOTE = "note_items"
        const val TABLE_IDENTITY = "identity_items"
        const val TABLE_PASSWORD_HISTORY = "login_password_history"
    }
}
