package com.aegis.vault.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The in-memory unlocked session.
 *
 * The SQLCipher passphrase lives here and nowhere else: never written to disk,
 * cleared the moment the vault locks (auto-lock timer, background timer, or an
 * explicit lock). Every screen that shows vault data requires an open session.
 */
class SessionHolder {

    private val _passphrase = MutableStateFlow<ByteArray?>(null)
    val passphrase: StateFlow<ByteArray?> = _passphrase.asStateFlow()

    private val _lockedByTimeout = MutableStateFlow(false)
    val lockedByTimeout: StateFlow<Boolean> = _lockedByTimeout.asStateFlow()

    val isUnlocked: Boolean get() = _passphrase.value != null

    fun unlock(passphrase: ByteArray) {
        _passphrase.value = passphrase
        _lockedByTimeout.value = false
    }

    fun lock(clearByAutoLock: Boolean = false) {
        _passphrase.value?.fill(0)
        _passphrase.value = null
        _lockedByTimeout.value = clearByAutoLock
    }
}
