// File: app/src/main/java/com/ampafacil/app/navigation/AppNavGraph.kt
// // Este archivo es el “mapa” de navegación: qué pantallas existen y cómo se pasa de una a otra.
package com.ampafacil.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ampafacil.app.ui.screens.AmpaCodeScreen
import com.ampafacil.app.ui.screens.AuthScreen
import com.ampafacil.app.ui.screens.CreateAmpaScreen
import com.ampafacil.app.ui.screens.HomeScreen
import com.ampafacil.app.ui.screens.FamilyChildrenScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.AMPA_CODE) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AMPA_CODE) {
            AmpaCodeScreen(
                onCodeAccepted = { navController.navigate(Routes.HOME) },
                onCreateAmpa = { navController.navigate(Routes.CREATE_AMPA) }
            )
        }

        composable(Routes.FAMILY_CHILDREN) {
            FamilyChildrenScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.navigate(Routes.HOME) }
            )
        }

        composable(Routes.CREATE_AMPA) {
            CreateAmpaScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AMPA_CODE) { inclusive = true }
                    }
                }
            )
            // // Aquí conectamos la pantalla de creación de AMPA y limpiamos la vuelta atrás cuando terminamos.
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    /* Aquí volvemos a Auth y limpiamos el backstack para que “Atrás” no nos devuelva a Home. */
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}