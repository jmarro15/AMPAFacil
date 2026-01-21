// File: app/src/main/java/com/ampafacil/navigation/AppNavGraph.kt
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
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = { navController.navigate(Routes.AMPA_CODE) }
            )
        }
        composable(Routes.AMPA_CODE) {
            AmpaCodeScreen(
                onCodeAccepted = { navController.navigate(Routes.HOME) }
            )
        }
        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}
