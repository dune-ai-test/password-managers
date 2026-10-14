package com.aegis.vault

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aegis.vault.core.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AegisApplication owns the service locator and the auto-lock timer.
 *
 * Auto-lock strategy: when the app goes to background we start a timer for the
 * configured delay; if the app comes back first the timer is cancelled, otherwise
 * the session is wiped and the database is closed. "Immediately" locks as soon as
 * the app leaves the foreground.
 */
class AegisApplication : Application() {

    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator.from(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AutoLockObserver(locator))
    }
}

private class AutoLockObserver(private val locator: ServiceLocator) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lockJob: kotlinx.coroutines.Job? = null

    override fun onStart(owner: LifecycleOwner) {
        lockJob?.cancel()
        lockJob = null
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!locator.sessionHolder.isUnlocked) return
        val seconds = locator.masterPasswordManager.autoLockSeconds
        if (seconds <= 0) {
            lockNow()
            return
        }
        lockJob?.cancel()
        lockJob = scope.launch {
            delay(seconds * 1000L)
            lockNow()
        }
    }

    private fun lockNow() {
        locator.sessionHolder.lock(clearByAutoLock = true)
        locator.closeVault()
    }
}
