// File: app/src/main/java/com/ampafacil/app/navigation/AppNavGraph.kt
package com.ampafacil.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ampafacil.app.ui.screens.AmpaCodeScreen
import com.ampafacil.app.ui.screens.AmpaSplashScreen
import com.ampafacil.app.ui.screens.AppearanceScreen
import com.ampafacil.app.ui.screens.AuthScreen
import com.ampafacil.app.ui.screens.BoardManagementScreen
import com.ampafacil.app.ui.screens.CreateAmpaScreen
import com.ampafacil.app.ui.screens.FamilyChildrenScreen
import com.ampafacil.app.ui.screens.FamilyDirectoryScreen
import com.ampafacil.app.ui.screens.HomeScreen
import com.ampafacil.app.ui.screens.InitialBoardInvitesScreen
import com.ampafacil.app.ui.screens.PersonalDataScreen
import com.ampafacil.app.ui.screens.StartRouterScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.START
    ) {
        composable(Routes.START) {
            StartRouterScreen(
                onGoAuth = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoAmpaCode = {
                    navController.navigate(Routes.AMPA_CODE) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoFamilyChildren = {
                    navController.navigate(Routes.FAMILY_CHILDREN) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoAmpaSplash = {
                    navController.navigate(Routes.AMPA_SPLASH) {
                        popUpTo(Routes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.START) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AMPA_CODE) {
            AmpaCodeScreen(
                onCodeAccepted = {
                    navController.navigate(Routes.START) {
                        popUpTo(Routes.AMPA_CODE) { inclusive = true }
                    }
                },
                onCreateAmpa = { navController.navigate(Routes.CREATE_AMPA) }
            )
        }

        composable(Routes.CREATE_AMPA) {
            CreateAmpaScreen(
                onBack = { navController.popBackStack() },
                onAmpaCreated = { ampaCode, creatorRole ->
                    navController.navigate(Routes.initialBoardInvites(ampaCode, creatorRole)) {
                        popUpTo(Routes.CREATE_AMPA) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.INITIAL_BOARD_INVITES,
            arguments = listOf(
                navArgument("ampaCode") { type = NavType.StringType },
                navArgument("creatorRole") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ampaCode = backStackEntry.arguments?.getString("ampaCode").orEmpty()
            val creatorRole = backStackEntry.arguments?.getString("creatorRole").orEmpty()

            InitialBoardInvitesScreen(
                ampaCode = ampaCode,
                creatorRole = creatorRole,
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.INITIAL_BOARD_INVITES) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.FAMILY_CHILDREN) {
            FamilyChildrenScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.FAMILY_CHILDREN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AMPA_SPLASH) {
            AmpaSplashScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AMPA_SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.APPEARANCE) {
            AppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PERSONAL_DATA) {
            PersonalDataScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FAMILY_DIRECTORY) {
            FamilyDirectoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BOARD_MANAGEMENT) {
            BoardManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onAddChild = {
                    navController.navigate(Routes.FAMILY_CHILDREN)
                },
                onOpenPersonalData = {
                    navController.navigate(Routes.PERSONAL_DATA)
                },
                onOpenFamilyDirectory = {
                    navController.navigate(Routes.FAMILY_DIRECTORY)
                },
                onOpenAppearance = {
                    navController.navigate(Routes.APPEARANCE)
                },
                onOpenBoardManagement = {
                    navController.navigate(Routes.BOARD_MANAGEMENT)
                }
            )
        }
    }
}
