package com.familyguard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familyguard.app.ServiceLocator
import com.familyguard.app.ui.disclosure.DisclosureScreen
import com.familyguard.app.ui.pairing.PairingScreen
import com.familyguard.app.ui.permissions.PermissionWizardScreen
import com.familyguard.app.ui.status.StatusHomeScreen
import com.familyguard.app.work.WorkScheduler

/**
 * The app's single navigation graph. There is intentionally no back-stack juggling beyond
 * what's needed for this linear flow (disclosure -> pairing -> permission wizard -> status
 * home), plus the two special transitions: status home -> pairing on unpair/session-expiry,
 * and status home -> permission wizard -> status home for "review permissions".
 */
@Composable
fun FamilyGuardNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val secureStorage = ServiceLocator.secureStorage

    val disclosureAccepted by secureStorage.disclosureAccepted.collectAsStateWithLifecycle()
    val pairingState by secureStorage.pairingState.collectAsStateWithLifecycle()
    val wizardCompleted by secureStorage.permissionWizardCompleted.collectAsStateWithLifecycle()

    var showUnpairedBanner by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ServiceLocator.sessionManager.sessionExpiredEvents.collect {
            showUnpairedBanner = true
            navController.navigate(Screen.Pairing.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = remember {
        when {
            !disclosureAccepted -> Screen.Disclosure.route
            !pairingState.isPaired -> Screen.Pairing.route
            !wizardCompleted -> Screen.PermissionWizard.route
            else -> Screen.StatusHome.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Disclosure.route) {
            DisclosureScreen(
                onAccepted = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(Screen.Disclosure.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Pairing.route) {
            PairingScreen(
                showUnpairedBanner = showUnpairedBanner,
                onPaired = {
                    showUnpairedBanner = false
                    WorkScheduler.scheduleBackgroundWorkIfPaired(context)
                    navController.navigate(Screen.PermissionWizard.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PermissionWizard.route) {
            PermissionWizardScreen(
                onFinished = {
                    // Reset to a single StatusHome instance whether this was the initial
                    // setup wizard or a later "review permissions" revisit.
                    navController.navigate(Screen.StatusHome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StatusHome.route) {
            StatusHomeScreen(
                onManagePermissions = {
                    navController.navigate(Screen.PermissionWizard.route) {
                        launchSingleTop = true
                    }
                },
                onUnpaired = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
