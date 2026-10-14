package com.aegis.vault.data.repo

import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.local.ItemDao
import com.aegis.vault.data.model.CardItem
import com.aegis.vault.data.model.IdentityItem
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.data.model.LoginItem
import com.aegis.vault.data.model.NoteItem
import com.aegis.vault.data.model.SearchResult
import com.aegis.vault.data.model.VaultStats
import com.aegis.vault.data.security.SecurityScanner
import com.aegis.vault.data.seed.SeedData
import com.aegis.vault.data.security.SecurityReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single door into the vault for the UI layer.
 *
 * There is no Room flow plumbing: mutations bump [dataVersion], and every screen
 * that lists vault data collects that counter and re-reads. For a vault-sized
 * dataset this is simpler and always consistent with the database.
 */
class VaultRepository(private val locator: ServiceLocator) {

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    private val dao: ItemDao get() = locator.dao()

    fun bump() {
        _dataVersion.value = _dataVersion.value + 1
    }

    // --------------------------------------------------------------- reads

    fun logins(): List<LoginItem> = dao.allLogins()

    fun cards(): List<CardItem> = dao.allCards()

    fun notes(): List<NoteItem> = dao.allNotes()

    fun identities(): List<IdentityItem> = dao.allIdentities()

    fun stats(): VaultStats = VaultStats(
        logins = cardinal(logins()),
        cards = cardinal(cards()),
        notes = cardinal(notes()),
        identities = cardinal(identities()),
    )

    fun recentLogins(limit: Int = 5): List<LoginItem> = dao.recentLogins(limit)

    fun securityReport(): SecurityReport = locator.securityScanner.scan(logins())

    fun search(query: String, types: Set<ItemType>): List<SearchResult> = dao.search(query, types)

    fun login(id: String): LoginItem? = dao.login(id)

    fun card(id: String): CardItem? = dao.card(id)

    fun note(id: String): NoteItem? = dao.note(id)

    fun identity(id: String): IdentityItem? = dao.identity(id)

    fun passwordHistory(loginId: String) = dao.passwordHistory(loginId)

    // -------------------------------------------------------------- writes

    fun saveLogin(item: LoginItem, originalPassword: String? = null): LoginItem {
        val saved = if (originalPassword != null) {
            dao.updateLogin(item, previousPassword = originalPassword)
        } else {
            dao.insertLogin(item)
        }
        bump()
        return saved
    }

    fun deleteLogin(id: String) {
        dao.trashLogin(id)
        bump()
    }

    fun saveCard(item: CardItem): CardItem {
        val existing = dao.card(item.id)
        val saved = if (existing == null) dao.insertCard(item) else dao.updateCard(item)
        bump()
        return saved
    }

    fun deleteCard(id: String) {
        dao.trashCard(id)
        bump()
    }

    fun saveNote(item: NoteItem): NoteItem {
        val existing = dao.note(item.id)
        val saved = if (existing == null) dao.insertNote(item) else dao.updateNote(item)
        bump()
        return saved
    }

    fun deleteNote(id: String) {
        dao.trashNote(id)
        bump()
    }

    fun saveIdentity(item: IdentityItem): IdentityItem {
        val existing = dao.identity(item.id)
        val saved = if (existing == null) dao.insertIdentity(item) else dao.updateIdentity(item)
        bump()
        return saved
    }

    fun deleteIdentity(id: String) {
        dao.trashIdentity(id)
        bump()
    }

    // ------------------------------------------------------------ seeding

    /** Seeds the demo vault on first launch. Returns true when data was written. */
    fun seedIfEmpty(): Boolean {
        if (!dao.isVaultEmpty()) return false
        SeedData.logins.forEach { dao.insertLogin(it, historyPassword = null) }
        SeedData.cards.forEach { dao.insertCard(it) }
        SeedData.notes.forEach { dao.insertNote(it) }
        SeedData.identities.forEach { dao.insertIdentity(it) }
        bump()
        return true
    }

    fun deleteDemoData() {
        dao.deleteAllDemoItems()
        bump()
    }

    fun deleteAllItems() {
        dao.deleteEverything()
        bump()
    }

    fun databaseSizeBytes(): Long {
        val context = locator.appContext
        val file = context.getDatabasePath("aegis.db")
        return if (file.exists()) file.length() else 0L
    }

    private fun cardinal(list: List<*>) = list.size
}
