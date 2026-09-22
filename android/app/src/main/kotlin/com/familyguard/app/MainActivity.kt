package com.familyguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.familyguard.app.ui.navigation.FamilyGuardNavHost
import com.familyguard.app.ui.theme.FamilyGuardTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            FamilyGuardTheme {
                FamilyGuardNavHost()

                val pairingState by ServiceLocator.secureStorage.pairingState.collectAsStateWithLifecycle()
                LaunchedEffect(pairingState.isPaired) {
                    if (pairingState.isPaired) {
                        registerFcmTokenIfNeeded()
                    }
                }
            }
        }
    }

    /**
     * FCM here is used purely as a push-delivery transport for the "refresh now" data message
     * — there is no Firebase Auth and nothing else is ever read from or written to Firebase.
     */
    private fun registerFcmTokenIfNeeded() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch {
                ServiceLocator.deviceRepository.registerFcmToken(token)
            }
        }
    }
}
