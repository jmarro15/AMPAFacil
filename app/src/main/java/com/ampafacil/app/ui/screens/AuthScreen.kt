// AuthScreen.kt
// // Yo uso esta pantalla para REGISTRAR y HACER LOGIN con Firebase usando email + contraseña.
// // También obligo a verificar el correo antes de dejar entrar a la app.

package com.ampafacil.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    // // Yo guardo aquí lo que escribe el usuario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // // Yo controlo si estoy “cargando” para no dejar pulsar 20 veces
    var isLoading by remember { mutableStateOf(false) }

    // // Estado del logo (solo se muestra si existe URL y la carga va bien)
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var logoLoadFailed by remember { mutableStateOf(false) }

    // // Yo uso esto para enseñar mensajitos rápidos en pantalla
    val context = LocalContext.current

    // // Yo obtengo el “motor” de Firebase que gestiona login/registro
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val prefs = remember {
        context.getSharedPreferences("ampafacil_auth", Context.MODE_PRIVATE)
    }

    fun loadLogoForAmpa(ampaCode: String) {
        db.collection("ampas").document(ampaCode).get()
            .addOnSuccessListener { ampaDoc ->
                val themeConfig = ampaDoc.get("themeConfig") as? Map<*, *>
                val loadedLogo = themeConfig?.get("logoUrl")?.toString()?.trim().orEmpty()

                logoUrl = loadedLogo.takeIf { it.isNotBlank() }
                logoLoadFailed = false
            }
            .addOnFailureListener {
                logoUrl = null
                logoLoadFailed = false
            }
    }

    fun resolveLogoForAuthScreen() {
        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            val localAmpaCode = prefs.getString("last_ampa_code", null)?.trim()
            if (!localAmpaCode.isNullOrBlank()) {
                loadLogoForAmpa(localAmpaCode)
            } else {
                logoUrl = null
                logoLoadFailed = false
            }
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val activeCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (activeCode.isNullOrBlank()) {
                    logoUrl = null
                    logoLoadFailed = false
                    return@addOnSuccessListener
                }

                prefs.edit().putString("last_ampa_code", activeCode).apply()
                loadLogoForAmpa(activeCode)
            }
            .addOnFailureListener {
                logoUrl = null
                logoLoadFailed = false
            }
    }

    LaunchedEffect(Unit) {
        resolveLogoForAuthScreen()
    }

    fun register() {
        // // Yo hago una validación simple antes de llamar a Firebase
        val e = email.trim()
        if (e.isEmpty() || password.length < 6) {
            Toast.makeText(context, "Pon un email válido y una contraseña de 6+ caracteres.", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true

        // // Yo creo el usuario en Firebase con email/contraseña
        auth.createUserWithEmailAndPassword(e, password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    // // Yo mando el email de verificación al registrarse
                    auth.currentUser?.sendEmailVerification()

                    Toast.makeText(
                        context,
                        "Te he enviado un correo de verificación. Revisa tu email y verifica la cuenta.",
                        Toast.LENGTH_LONG
                    ).show()

                    // // Yo cierro sesión para que no entre hasta verificar
                    auth.signOut()
                } else {
                    Toast.makeText(
                        context,
                        task.exception?.message ?: "Error al registrar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun login() {
        val e = email.trim()
        if (e.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Escribe email y contraseña.", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true

        // // Yo inicio sesión con email/contraseña
        auth.signInWithEmailAndPassword(e, password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // // Yo obligo a que el correo esté verificado antes de entrar
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(context, "Login OK ✅", Toast.LENGTH_SHORT).show()
                        onAuthSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            "Verifica tu correo antes de entrar (mira tu bandeja).",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                    }
                } else {
                    Toast.makeText(
                        context,
                        task.exception?.message ?: "Error al iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun resendVerification() {
        // // Yo reenvío el correo de verificación si el usuario ya existe y está logueado
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            user.sendEmailVerification()
            Toast.makeText(context, "Te he reenviado el correo de verificación.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Primero haz login y si falta verificación, lo reenvío.", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------- UI ----------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (!logoUrl.isNullOrBlank() && !logoLoadFailed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "Logo AMPA",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(width = 140.dp, height = 84.dp),
                contentScale = ContentScale.Fit,
                onError = { logoLoadFailed = true }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AMPAFácil - Accede a tu AMPA.", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { register() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Cargando..." else "Registrarme")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { login() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Cargando..." else "Entrar")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { resendVerification() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reenviar verificación")
            }
        }
    }
}