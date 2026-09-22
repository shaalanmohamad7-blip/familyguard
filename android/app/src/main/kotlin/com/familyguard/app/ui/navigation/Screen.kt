package com.familyguard.app.ui.navigation

sealed class Screen(val route: String) {
    data object Disclosure : Screen("disclosure")
    data object Pairing : Screen("pairing")
    data object PermissionWizard : Screen("permission_wizard")
    data object StatusHome : Screen("status_home")
}
