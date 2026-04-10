// File: app/src/main/java/com/ampafacil/app/ui/screens/AuthScreen.kt

// AuthScreen.kt
// // Yo uso esta pantalla para REGISTRAR y HACER LOGIN con Firebase usando email + contraseña.
// // También obligo a verificar el correo antes de dejar entrar a la app.

package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    // // Yo guardo aquí lo que escribe el usuario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // // Yo controlo si estoy “cargando” para no dejar pulsar muchas veces seguidas
    var isLoading by remember { mutableStateOf(false) }
    // // Yo uso esta variable para ocultar las sugerencias cuando el usuario ya ha elegido una opción
    var hideSuggestions by remember { mutableStateOf(false) }

    // // Yo uso esto para enseñar mensajes rápidos en pantalla
    val context = LocalContext.current

    // // Yo obtengo el motor de Firebase que gestiona el registro y el login
    val auth = remember { FirebaseAuth.getInstance() }

    fun register() {
        // // Yo compruebo primero que los campos no estén vacíos
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Completa email y contraseña", Toast.LENGTH_LONG).show()
            return
        }

        // // Yo activo el estado de carga para bloquear pulsaciones repetidas
        isLoading = true

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    // // Yo envío el correo de verificación al registrarse correctamente
                    auth.currentUser?.sendEmailVerification()

                    Toast.makeText(
                        context,
                        "Registro correcto. Revisa tu correo y verifica la cuenta.",
                        Toast.LENGTH_LONG
                    ).show()

                    // // Yo cierro la sesión para obligar a entrar de nuevo ya verificado
                    auth.signOut()
                } else {
                    Toast.makeText(
                        context,
                        task.exception?.message ?: "Error al registrarse",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun login() {
        // // Yo compruebo primero que los campos no estén vacíos
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Completa email y contraseña", Toast.LENGTH_LONG).show()
            return
        }

        // // Yo activo el estado de carga para evitar pulsaciones repetidas
        isLoading = true

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {
                        // // Yo dejo entrar solo si el correo ya está verificado
                        onAuthSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            "Debes verificar tu correo antes de entrar.",
                            Toast.LENGTH_LONG
                        ).show()

                        // // Yo cierro sesión si todavía no está verificado
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
        // // Yo reenvío el correo de verificación si el usuario existe y todavía no ha verificado
        val user = auth.currentUser

        if (user != null && !user.isEmailVerified) {
            user.sendEmailVerification()
            Toast.makeText(
                context,
                "Te he reenviado el correo de verificación.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Primero haz login y, si falta verificación, lo reenvío.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // // Yo preparo dominios sugeridos para que sea más rápido escribir el correo en móvil
    val domainOptions = listOf("gmail.com", "hotmail.com")

    // // Yo detecto si el usuario ya ha escrito la arroba dentro del email
    val atIndex = email.indexOf('@')
    val hasAtSymbol = atIndex >= 0

    // // Yo separo la parte local del correo, que es lo que va antes de la arroba
    val localPart = if (hasAtSymbol) email.substring(0, atIndex) else ""

    // // Yo separo la parte del dominio, que es lo que se va escribiendo después de la arroba
    val typedDomain = if (hasAtSymbol && atIndex < email.length - 1) {
        email.substring(atIndex + 1)
    } else {
        ""
    }

    // // Yo solo enseño sugerencias cuando ya hay algo antes de la arroba
    // // y además no hay espacios raros en la parte del dominio
    val shouldShowEmailSuggestions =
        hasAtSymbol &&
                localPart.isNotBlank() &&
                !typedDomain.contains(" ") &&
                !hideSuggestions &&
    !typedDomain.contains(".")

    // // Yo filtro los dominios para enseñar solo los que encajan con lo escrito.
    // // Si después de la arroba aún no hay nada, enseño todas las opciones.
    val filteredDomains = domainOptions.filter { domain ->
        typedDomain.isBlank() || domain.startsWith(typedDomain, ignoreCase = true)
    }

    // ---------------- UI ----------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AMPAFácil - Acceso",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { newValue ->
                // // Yo actualizo el email tal como lo va escribiendo el usuario
                // // y evito saltos de línea para mantener el campo limpio
                email = newValue.replace("\n", "")
                // // Yo vuelvo a permitir sugerencias si el usuario sigue escribiendo
                hideSuggestions = false
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (shouldShowEmailSuggestions) {
            Spacer(modifier = Modifier.height(8.dp))

            // // Yo muestro chips cómodos de pulsar con el dedo para completar el dominio
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDomains) { domain ->
                    SuggestionChip(
                        onClick = {
                            email = "$localPart@$domain"

                            // // Aquí ocultamos las sugerencias tras elegir
                            hideSuggestions = true
                        },
                        label = { Text(domain) }
                    )
                }

                item {
                    SuggestionChip(
                        onClick = {
                            // // Yo no autocompleto nada y simplemente oculto las sugerencias
                            hideSuggestions = true
                        },
                        label = { Text("Seguir escribiendo") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { register() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Cargando..." else "Registrarme")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { login() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Cargando..." else "Entrar")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { resendVerification() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reenviar verificación")
        }
    }
}