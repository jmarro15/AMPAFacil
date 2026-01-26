// File: app/src/main/java/com/ampafacil/app/navigation/AppNavGraph.kt
// // Este archivo es el “mapa” de navegación: qué pantallas existen y cómo se pasa de una a otra.
package com.ampafacil.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ampafacil.app.ui.screens.AmpaCodeScreen
import com.ampafacil.app.ui.screens.AuthScreen
import com.ampafacil.app.ui.screens.HomeScreen

@Composable
fun AppNavGraph() {
    // // Controlador que maneja los saltos entre pantallas.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    // // Tras acceder bien, se pasa a la pantalla del código y se evita volver atrás a Auth.
                    navController.navigate(Routes.AMPA_CODE) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AMPA_CODE) {
            AmpaCodeScreen(
                onCodeAccepted = {
                    // // Si el código es correcto, se entra a Home y se evita volver atrás a la pantalla del código.
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AMPA_CODE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            // // Pantalla principal.
            HomeScreen()
        }
    }
}
