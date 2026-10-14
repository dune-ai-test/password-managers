package com.aegis.vault.core

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.aegis.vault.data.crypto.VaultCrypto
import com.aegis.vault.data.repo.VaultRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Encrypted backups for Settings → Import & export.
 *
 * The JSON snapshot is AES-256-GCM encrypted (fresh salt + IV, key derived with
 * PBKDF2 from a password the user chooses at export time) and written to
 * cache/exports, where it is shared through the app's FileProvider. The backup
 * never leaves the device unless the user explicitly shares it.
 */
class BackupManager(private val context: Context) {

    fun buildSnapshot(repo: VaultRepository): String {
        val root = JSONObject()
        root.put("format", "aegis-vault-backup")
        root.put("version", 1)
        root.put("createdAt", System.currentTimeMillis())

        val items = JSONObject()

        val logins = JSONArray()
        repo.logins().forEach { login ->
            logins.put(
                JSONObject()
                    .put("id", login.id)
                    .put("title", login.title)
                    .put("username", login.username)
                    .put("password", login.password)
                    .put("website", login.website)
                    .put("notes", login.notes)
                    .put("totpSecret", login.totpSecret)
                    .put("createdAt", login.createdAt)
                    .put("lastModified", login.lastModified),
            )
        }
        items.put("logins", logins)

        val cards = JSONArray()
        repo.cards().forEach { card ->
            cards.put(
                JSONObject()
                    .put("id", card.id)
                    .put("title", card.title)
                    .put("bankName", card.bankName)
                    .put("network", card.network)
                    .put("cardholder", card.cardholder)
                    .put("number", card.number)
                    .put("expiry", card.expiry)
                    .put("securityCode", card.securityCode)
                    .put("billingAddress", card.billingAddress)
                    .put("notes", card.notes)
                    .put("createdAt", card.createdAt)
                    .put("lastModified", card.lastModified),
            )
        }
        items.put("cards", cards)

        val notes = JSONArray()
        repo.notes().forEach { note ->
            notes.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("tags", JSONArray(note.tags))
                    .put("createdAt", note.createdAt)
                    .put("lastModified", note.lastModified),
            )
        }
        items.put("notes", notes)

        val identities = JSONArray()
        repo.identities().forEach { identity ->
            identities.put(
                JSONObject()
                    .put("id", identity.id)
                    .put("title", identity.title)
                    .put("firstName", identity.firstName)
                    .put("lastName", identity.lastName)
                    .put("email", identity.email)
                    .put("phone", identity.phone)
                    .put("address", identity.address)
                    .put("notes", identity.notes)
                    .put("createdAt", identity.createdAt)
                    .put("lastModified", identity.lastModified),
            )
        }
        items.put("identities", identities)

        root.put("items", items)
        return root.toString(2)
    }

    /** Parses a snapshot and writes it into the vault (replace-all import). */
    fun restoreSnapshot(json: String, repo: VaultRepository): Boolean {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return false
        }
        val items = root.optJSONObject("items") ?: return false

        // Parse everything first; only touch the database once the blob is valid.
        val logins = items.optJSONArray("logins")?.let { array ->
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                com.aegis.vault.data.model.LoginItem(
                    id = o.optString("id", VaultCrypto.randomUuid()),
                    title = o.optString("title", "Login"),
                    username = o.optString("username"),
                    password = o.optString("password"),
                    website = o.optString("website"),
                    notes = o.optString("notes"),
                    totpSecret = o.optString("totpSecret"),
                    isDemo = false,
                    createdAt = o.optLong("createdAt"),
                    lastModified = o.optLong("lastModified"),
                )
            }
        } ?: emptyList()

        val cards = items.optJSONArray("cards")?.let { array ->
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                com.aegis.vault.data.model.CardItem(
                    id = o.optString("id", VaultCrypto.randomUuid()),
                    title = o.optString("title", "Card"),
                    bankName = o.optString("bankName"),
                    network = o.optString("network"),
                    cardholder = o.optString("cardholder"),
                    number = o.optString("number"),
                    expiry = o.optString("expiry"),
                    securityCode = o.optString("securityCode"),
                    billingAddress = o.optString("billingAddress"),
                    notes = o.optString("notes"),
                    isDemo = false,
                    createdAt = o.optLong("createdAt"),
                    lastModified = o.optLong("lastModified"),
                )
            }
        } ?: emptyList()

        val noteList = items.optJSONArray("notes")?.let { array ->
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                val tags = o.optJSONArray("tags") ?: JSONArray()
                com.aegis.vault.data.model.NoteItem(
                    id = o.optString("id", VaultCrypto.randomUuid()),
                    title = o.optString("title", "Note"),
                    body = o.optString("body"),
                    tags = List(tags.length()) { t -> tags.getString(t) },
                    isDemo = false,
                    createdAt = o.optLong("createdAt"),
                    lastModified = o.optLong("lastModified"),
                )
            }
        } ?: emptyList()

        val identities = items.optJSONArray("identities")?.let { array ->
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                com.aegis.vault.data.model.IdentityItem(
                    id = o.optString("id", VaultCrypto.randomUuid()),
                    title = o.optString("title", "Identity"),
                    firstName = o.optString("firstName"),
                    lastName = o.optString("lastName"),
                    email = o.optString("email"),
                    phone = o.optString("phone"),
                    address = o.optString("address"),
                    notes = o.optString("notes"),
                    isDemo = false,
                    createdAt = o.optLong("createdAt"),
                    lastModified = o.optLong("lastModified"),
                )
            }
        } ?: emptyList()

        repo.deleteAllItems()
        logins.forEach { repo.saveLogin(it) }
        cards.forEach { repo.saveCard(it) }
        noteList.forEach { repo.saveNote(it) }
        identities.forEach { repo.saveIdentity(it) }
        return true
    }

    /** Writes an encrypted backup to the cache dir and returns a shareable URI. */
    fun writeEncryptedBackup(json: String, password: String): Uri {
        val blob = VaultCrypto.encryptWithPassword(json.toByteArray(Charsets.UTF_8), password.toCharArray())
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "aegis-backup-${System.currentTimeMillis()}.aegis")
        file.writeBytes(blob)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun readEncryptedBackup(uri: Uri, password: String): String? {
        val blob = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return null
        val plain = VaultCrypto.decryptWithPassword(blob, password.toCharArray()) ?: return null
        return String(plain, Charsets.UTF_8)
    }
}
