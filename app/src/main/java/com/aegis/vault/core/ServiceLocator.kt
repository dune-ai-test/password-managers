package com.aegis.vault.core

import android.content.Context
import com.aegis.vault.data.local.ItemDao
import com.aegis.vault.data.local.VaultDatabase
import com.aegis.vault.data.repo.VaultRepository
import com.aegis.vault.data.security.MasterPasswordManager
import com.aegis.vault.data.security.SecurityScanner

/**
 * Tiny manual service locator. The app has exactly one graph and no modules, so
 * this beats a DI framework here: no annotation processors, fast CI builds, and
 * one obvious place where singletons are wired.
 */
class ServiceLocator(context: Context) {

    internal val appContext = context.applicationContext

    val sessionHolder: SessionHolder = SessionHolder()
    val masterPasswordManager: MasterPasswordManager = MasterPasswordManager(appContext)
    val securityScanner: SecurityScanner = SecurityScanner()

    @Volatile
    private var database: VaultDatabase? = null

    /** The open database, or null when locked. */
    val openDatabase: VaultDatabase? get() = database

    fun openVault(passphraseHex: String): VaultDatabase {
        synchronized(this) {
            database?.close()
            database = VaultDatabase.open(appContext, passphraseHex)
            return database!!
        }
    }

    fun closeVault() {
        synchronized(this) {
            database?.close()
            database = null
        }
    }

    fun dao(): ItemDao {
        val db = database ?: error("Vault is locked — open it with a valid passphrase first")
        return ItemDao(db)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepository(this)
    }

    companion object {
        @Volatile
        private var instance: ServiceLocator? = null

        fun from(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
    }
}
