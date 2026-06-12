package com.batallagroup.myco.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Identity : Screen("identity")
    object Chats : Screen("chats")
    object Chat : Screen("chat/{contactId}") {
        fun createRoute(contactId: String) = "chat/$contactId"
    }
    object Contacts : Screen("contacts")
    object Network : Screen("network")
    object Settings : Screen("settings")
    object QrDisplay : Screen("qr_display")
    object QrScanner : Screen("qr_scanner")
}
