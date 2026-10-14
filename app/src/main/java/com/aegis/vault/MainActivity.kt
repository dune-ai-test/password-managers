package com.aegis.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.aegis.vault.ui.AegisNavHost
import com.aegis.vault.ui.theme.AegisTheme

/**
 * The single activity. Extends [FragmentActivity] because Android's
 * BiometricPrompt requires a FragmentActivity host — the unlock flow depends on it.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AegisTheme {
                AegisNavHost()
            }
        }
    }
}
