package com.aegis.vault.data.local

import android.content.Context
import android.database.Cursor
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Opens and owns the encrypted vault database.
 *
 * The database is a single SQLCipher-encrypted file (`aegis.db`) in the app's
 * private storage. The passphrase is never persisted: the session keeps it in
 * memory while the vault is unlocked, and every process start re-derives it from
 * the master password (or unwraps it after biometric auth).
 */
class VaultDatabase(private val db: SQLiteDatabase) {

    val readable: SQLiteDatabase get() = db

    fun close() {
        try {
            db.close()
        } catch (e: Exception) {
            Log.w(TAG, "close failed", e)
        }
    }

    fun exec(sql: String) {
        db.execSQL(sql)
    }

    fun rawQuery(sql: String, args: Array<String>?): Cursor =
        if (args == null) db.rawQuery(sql, null) else db.rawQuery(sql, args)

    fun insert(table: String, values: ContentRow): Long = db.insert(table, null, values.self())

    fun update(table: String, values: ContentRow, where: String, args: Array<String>): Int =
        db.update(table, values.self(), where, args)

    fun delete(table: String, where: String, args: Array<String>): Int =
        db.delete(table, where, args)

    /** Tiny wrapper so callers don't import android ContentValues everywhere. */
    class ContentRow {
        internal val map = LinkedHashMap<String, Any?>()
        fun put(key: String, value: String?) = apply { map[key] = value }
        fun put(key: String, value: Long?) = apply { map[key] = value }
        fun put(key: String, value: Int?) = apply { map[key] = value }
        fun putNull(key: String) = apply { map[key] = null }
        internal fun self(): android.content.ContentValues =
            android.content.ContentValues().apply {
                map.forEach { (key, value) ->
                    when (value) {
                        null -> putNull(key)
                        is String -> put(key, value)
                        is Long -> put(key, value)
                        is Int -> put(key, value.toLong())
                    }
                }
            }
    }

        companion object {
            private const val TAG = "VaultDatabase"
            const val DB_NAME = "aegis.db"
            const val DB_VERSION = 1

            /**
             * Loads the bundled SQLCipher native library. This must run before any
             * `net.zetetic.database.sqlcipher` class is touched. Calling it more
             * than once is harmless (System.loadLibrary is idempotent per loader).
             */
            fun loadNativeLib() = System.loadLibrary("sqlcipher")

            /**
             * Opens (creating on first use) the vault at [passphraseHex]. SQLCipher 4
             * defaults (256k PBKDF2 iterations, 256-bit AES) already meet our
             * security target, so no PRAGMA tuning is needed.
             */
            fun open(context: Context, passphraseHex: String): VaultDatabase {
                loadNativeLib()
                val file: File = context.getDatabasePath(DB_NAME)
                file.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(
                    file,
                    passphraseHex.toByteArray(),
                    /* cursorFactory = */ null,
                    /* errorHandler = */ null,
                )
                createSchema(db)
                return VaultDatabase(db)
            }

        fun existsOnDisk(context: Context): Boolean =
            context.getDatabasePath(DB_NAME).exists()

        private fun createSchema(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS login_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    username TEXT NOT NULL DEFAULT '',
                    password TEXT NOT NULL DEFAULT '',
                    website TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    totp_secret TEXT NOT NULL DEFAULT '',
                    is_demo INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    last_modified INTEGER NOT NULL DEFAULT 0,
                    trashed_at INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS login_password_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login_id TEXT NOT NULL,
                    password TEXT NOT NULL,
                    changed_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS card_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    bank_name TEXT NOT NULL DEFAULT '',
                    network TEXT NOT NULL DEFAULT '',
                    cardholder TEXT NOT NULL DEFAULT '',
                    number TEXT NOT NULL DEFAULT '',
                    expiry TEXT NOT NULL DEFAULT '',
                    security_code TEXT NOT NULL DEFAULT '',
                    billing_address TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    is_demo INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    last_modified INTEGER NOT NULL DEFAULT 0,
                    trashed_at INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS note_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    body TEXT NOT NULL DEFAULT '',
                    tags TEXT NOT NULL DEFAULT '',
                    is_demo INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    last_modified INTEGER NOT NULL DEFAULT 0,
                    trashed_at INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS identity_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    first_name TEXT NOT NULL DEFAULT '',
                    last_name TEXT NOT NULL DEFAULT '',
                    email TEXT NOT NULL DEFAULT '',
                    phone TEXT NOT NULL DEFAULT '',
                    address TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    is_demo INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    last_modified INTEGER NOT NULL DEFAULT 0,
                    trashed_at INTEGER
                )
                """.trimIndent(),
            )
        }
    }
}
