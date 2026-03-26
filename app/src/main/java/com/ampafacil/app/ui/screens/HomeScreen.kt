// File: app/src/main/java/com/ampafacil/app/ui/screens/HomeScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.Roles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAddChild: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var role by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                userName = userDoc.getString("nombre") ?: ""
                val ampaCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (ampaCode.isNullOrBlank()) return@addOnSuccessListener

                db.collection("ampas").document(ampaCode)
                    .collection("members").document(uid).get()
                    .addOnSuccessListener { memberDoc ->
                        role = (memberDoc.getString("role") ?: "").uppercase()
                    }
            }
    }

    val isDirector = role == Roles.PRESIDENT || role == Roles.SECRETARY || role == Roles.VICEPRESIDENT

    Scaffold(
        topBar = { TopAppBar(title = { Text("AMPAFácil") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = if (userName.isBlank()) "Menú principal" else "Hola, $userName",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text("Bienvenido/a al menú de tu AMPA.")

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAddChild,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Añadir hijo o hija")
            }

            Spacer(Modifier.height(10.dp))

            if (isDirector) {
                Button(
                    onClick = onOpenAppearance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apariencia del AMPA")
                }

                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
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
