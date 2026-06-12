package com.batallagroup.myco.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.batallagroup.myco.presentation.chat.ChatScreen
import com.batallagroup.myco.presentation.chats.ChatsScreen
import com.batallagroup.myco.presentation.contacts.ContactsScreen
import com.batallagroup.myco.presentation.identity.IdentityScreen
import com.batallagroup.myco.presentation.network.NetworkScreen
import com.batallagroup.myco.presentation.onboarding.OnboardingScreen
import com.batallagroup.myco.presentation.qr.QrDisplayScreen
import com.batallagroup.myco.presentation.qr.QrScannerScreen
import com.batallagroup.myco.presentation.settings.SettingsScreen
import com.batallagroup.myco.presentation.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) },
                onNavigateToChats = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Identity.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Identity.route) {
            IdentityScreen(
                onContinue = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Identity.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chats.route) {
            ChatsScreen(
                onOpenChat = { contactId ->
                    navController.navigate(Screen.Chat.createRoute(contactId))
                },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStack ->
            val contactId = backStack.arguments?.getString("contactId") ?: return@composable
            ChatScreen(
                contactId = contactId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Contacts.route) {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenChat = { contactId ->
                    navController.navigate(Screen.Chat.createRoute(contactId)) {
                        popUpTo(Screen.Contacts.route) { inclusive = true }
                    }
                },
                onScanQr = { navController.navigate(Screen.QrScanner.route) },
                onShowQr = { navController.navigate(Screen.QrDisplay.route) }
            )
        }

        composable(Screen.Network.route) {
            NetworkScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowQr = { navController.navigate(Screen.QrDisplay.route) }
            )
        }

        composable(Screen.QrDisplay.route) {
            QrDisplayScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
