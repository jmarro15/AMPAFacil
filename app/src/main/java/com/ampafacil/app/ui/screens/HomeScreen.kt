// File: app/src/main/java/com/ampafacil/app/ui/screens/HomeScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    /* Aquí cogemos FirebaseAuth para poder cerrar sesión desde Home. */
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = { TopAppBar(title = { Text("AMPAFácil") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Home (placeholder)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Siguiente paso: Firebase Auth real + Firestore base.")

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    /* Aquí cerramos la sesión actual para poder entrar con otro correo y probar el alta como FAMILY/TUTOR. */
                    auth.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}