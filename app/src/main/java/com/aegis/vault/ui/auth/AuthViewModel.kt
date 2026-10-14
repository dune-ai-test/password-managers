package com.aegis.vault.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.crypto.VaultCrypto
import com.aegis.vault.data.crypto.toHexString
import com.aegis.vault.data.security.BiometricGate
import com.aegis.vault.data.security.MasterPasswordManager
import com.aegis.vault.data.security.SecurityScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

/** Everything the onboarding / create / unlock screens need. */
class AuthViewModel(private val locator: ServiceLocator) : ViewModel() {

    private val master: MasterPasswordManager = locator.masterPasswordManager
    private val scanner: SecurityScanner = locator.securityScanner

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Non-null while a BiometricPrompt should be shown to unlock the vault. */
    private val _biometricUnlockCipher = MutableStateFlow<Cipher?>(null)
    val biometricUnlockCipher: StateFlow<Cipher?> = _biometricUnlockCipher.asStateFlow()

    val hasMasterPassword: Boolean get() = master.hasMasterPassword
    val biometricUnlockConfigured: Boolean get() = master.biometricUnlockEnabled
    val autoLockOptions = MasterPasswordManager.AUTOLOCK_OPTIONS

    init {
        _uiState.update { it.copy(biometricConfigured = master.biometricUnlockEnabled) }
    }

    // ------------------------------------------------------------ UI state

    fun onPasswordChanged(value: String) {
        val level = scanner.strengthLevel(value)
        val (length, cases, numberOrSymbol) = scanner.requirementStatus(value)
        _uiState.update {
            it.copy(
                password = value,
                strengthLevel = level,
                strengthLabel = strengthLabel(level),
                meetsLength = length,
                meetsCases = cases,
                meetsNumberOrSymbol = numberOrSymbol,
                error = null,
            )
        }
    }

    fun setBiometricToggle(value: Boolean) {
        _uiState.update { it.copy(enableBiometric = value) }
    }

    fun setShowPassword(value: Boolean) {
        _uiState.update { it.copy(showPassword = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // -------------------------------------------------------------- actions

    /** Creates the vault from a new master password. */
    fun createMasterPassword(password: String) {
        val requirementsMet = listOf(_uiState.value.meetsLength, _uiState.value.meetsCases, _uiState.value.meetsNumberOrSymbol).all { it }
        if (!requirementsMet) {
            _uiState.update { it.copy(error = "Password doesn't meet all requirements yet") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Enter a master password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val passphrase = withContext(Dispatchers.Default) {
                    master.setMasterPassword(password)
                }
                openVault(passphrase)
                if (_uiState.value.enableBiometric) {
                    // Ask for the fingerprint on this screen; navigation waits
                    // until the wrap resolves (see vaultReady).
                    requestBiometricWrap(passphrase)
                } else {
                    _uiState.update { it.copy(vaultReady = true) }
                }
                withContext(Dispatchers.IO) { locator.vaultRepository.seedIfEmpty() }
                _uiState.update { it.copy(isBusy = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, error = e.message ?: "Could not create the vault") }
            }
        }
    }

    /** Unlocks an existing vault with the master password. */
    fun unlockWithPassword(password: String) {
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Enter your master password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val passphrase = withContext(Dispatchers.Default) {
                master.unlockWithPassword(password)
            }
            if (passphrase == null) {
                _uiState.update { it.copy(isBusy = false, error = "Wrong master password") }
                return@launch
            }
            try {
                openVault(passphrase)
                _uiState.update { it.copy(isBusy = false, vaultReady = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, error = "Could not open the vault") }
            }
        }
    }

    /**
     * Biometric unlock. The screen owns the BiometricPrompt (it needs an activity);
     * this function performs the crypto and reports the outcome:
     *  - [BiometricOutcome.NeedPrompt] → prompt with the given cipher, then call
     *    [finishBiometricUnlock] from the prompt's success callback with the
     *    cipher from `result.cryptoObject`
     */
    sealed interface BiometricOutcome {
        data object Ready : BiometricOutcome
        data object NotConfigured : BiometricOutcome
        data class NeedPrompt(val cipher: Cipher) : BiometricOutcome
        data class Failed(val message: String) : BiometricOutcome
    }

    fun beginBiometricUnlock(): BiometricOutcome {
        val wrapped = master.wrappedPassphrase() ?: return BiometricOutcome.NotConfigured
        return try {
            val cipher = BiometricGate.prepareUnwrapCipher(BiometricGate.ivOf(wrapped))
            _biometricUnlockCipher.value = cipher
            BiometricOutcome.NeedPrompt(cipher)
        } catch (e: Exception) {
            BiometricOutcome.Failed("Biometric unlock is unavailable")
        }
    }

    /** Called by the screen after the prompt finished (any outcome). */
    fun onBiometricUnlockDismissed() {
        _biometricUnlockCipher.value = null
    }

    fun onBiometricError(message: String) {
        _biometricUnlockCipher.value = null
        _uiState.update { it.copy(error = message) }
    }

    fun finishBiometricUnlock(cipher: Cipher?) {
        val wrapped = master.wrappedPassphrase() ?: run {
            _biometricUnlockCipher.value = null
            return
        }
        val activeCipher = cipher ?: run {
            _biometricUnlockCipher.value = null
            _uiState.update { it.copy(error = "Biometric unlock failed") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val passphrase = withContext(Dispatchers.Default) {
                    BiometricGate.unwrap(wrapped, activeCipher)
                }
                openVault(passphrase)
                _uiState.update { it.copy(isBusy = false, vaultReady = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, error = "Could not unlock with biometrics") }
            } finally {
                _biometricUnlockCipher.value = null
            }
        }
    }

    // ---------------------------------------------- enabling biometrics

    /**
     * Called after a master password is created while the biometric toggle is on.
     * Prepares the Keystore cipher and stashes the passphrase so
     * [completeBiometricWrap] can store it after the user passes the prompt.
     * The screen collects [biometricWrapCipher] and prompts when it appears.
     */
    fun requestBiometricWrap(passphrase: ByteArray) {
        pendingWrapPassphrase = passphrase
        _uiState.update {
            it.copy(biometricWrapCipher = BiometricGate.prepareWrapCipher())
        }
    }

    fun completeBiometricWrap(cipher: Cipher) {
        val passphrase = pendingWrapPassphrase ?: run {
            _uiState.update { it.copy(biometricWrapCipher = null, vaultReady = true) }
            return
        }
        viewModelScope.launch {
            try {
                val wrapped = withContext(Dispatchers.Default) {
                    BiometricGate.wrap(passphrase, cipher)
                }
                master.enableBiometricUnlock(wrapped)
                _uiState.update { it.copy(biometricWrapCipher = null, biometricConfigured = true, vaultReady = true) }
            } catch (e: Exception) {
                master.disableBiometricUnlock()
                _uiState.update { it.copy(biometricWrapCipher = null, vaultReady = true) }
            } finally {
                pendingWrapPassphrase?.fill(0)
                pendingWrapPassphrase = null
            }
        }
    }

    fun cancelBiometricWrap() {
        _uiState.update { it.copy(biometricWrapCipher = null, vaultReady = true) }
        pendingWrapPassphrase?.fill(0)
        pendingWrapPassphrase = null
    }

    private var pendingWrapPassphrase: ByteArray? = null

    // ---------------------------------------------------------------- misc

    fun lockVault() {
        locator.sessionHolder.lock()
        locator.closeVault()
    }

    fun autoLockSeconds(): Int = master.autoLockSeconds

    private fun openVault(passphrase: ByteArray) {
        locator.sessionHolder.unlock(passphrase)
        locator.openVault(passphrase.toHexString())
    }

    private fun strengthLabel(level: Int) = strengthLabelForLevel(level)

    private fun MutableStateFlow<AuthUiState>.update(block: (AuthUiState) -> AuthUiState) {
        value = block(value)
    }
}

data class AuthUiState(
    val password: String = "",
    val showPassword: Boolean = false,
    val strengthLevel: Int = 0,
    val strengthLabel: String = "Weak",
    val meetsLength: Boolean = false,
    val meetsCases: Boolean = false,
    val meetsNumberOrSymbol: Boolean = false,
    val enableBiometric: Boolean = true,
    val biometricConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null,
    /** Non-null when a BiometricPrompt should be shown to finish enabling unlock. */
    val biometricWrapCipher: Cipher? = null,
    /** Set once the vault is open and every prompt has resolved — navigate here. */
    val vaultReady: Boolean = false,
)

internal fun strengthLabelForLevel(level: Int): String = when (level) {
    0, 1 -> "Weak"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Strong"
    else -> "Excellent"
}
