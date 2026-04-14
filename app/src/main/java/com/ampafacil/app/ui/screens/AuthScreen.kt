// File: app/src/main/java/com/ampafacil/app/ui/screens/AuthScreen.kt

package com.ampafacil.app.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    // Aquí guardamos lo que va escribiendo el usuario.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Aquí controlamos si enseñamos o no la contraseña.
    var passwordVisible by remember { mutableStateOf(false) }

    // Aquí evitamos pulsaciones repetidas mientras Firebase responde.
    var isLoading by remember { mutableStateOf(false) }

    // Aquí ocultamos sugerencias cuando el usuario ya ha elegido una.
    var hideSuggestions by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    fun register() {
        val cleanEmail = email.trim()

        // Aquí comprobamos que no falten datos.
        if (cleanEmail.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Completa email y contraseña", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true
        Log.d("AuthScreen", "Intentando registrar usuario con email: $cleanEmail")

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    Log.d("AuthScreen", "Usuario creado correctamente en Firebase Auth")

                    // Aquí comprobamos que Firebase nos devuelva el usuario recién creado.
                    if (user == null) {
                        isLoading = false
                        Log.e("AuthScreen", "Usuario creado pero auth.currentUser ha venido null")

                        Toast.makeText(
                            context,
                            "Usuario creado, pero no se pudo obtener la cuenta actual.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    Log.d(
                        "AuthScreen",
                        "Vamos a enviar correo de verificación a: ${user.email}"
                    )

                    // Aquí enviamos el correo de verificación y esperamos el resultado real.
                    user.sendEmailVerification()
                        .addOnCompleteListener { verifyTask ->
                            isLoading = false

                            if (verifyTask.isSuccessful) {
                                Log.d(
                                    "AuthScreen",
                                    "Correo de verificación enviado correctamente a ${user.email}"
                                )

                                Toast.makeText(
                                    context,
                                    "Registro correcto. Revisa tu correo y verifica la cuenta.",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Aquí cerramos sesión solo después de confirmar que el correo salió.
                                auth.signOut()
                            } else {
                                Log.e(
                                    "AuthScreen",
                                    "Error al enviar correo de verificación",
                                    verifyTask.exception
                                )

                                Toast.makeText(
                                    context,
                                    "No se pudo enviar el correo de verificación: ${verifyTask.exception?.message ?: "Error desconocido"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    isLoading = false

                    Log.e(
                        "AuthScreen",
                        "Error al registrar usuario",
                        task.exception
                    )

                    Toast.makeText(
                        context,
                        task.exception?.message ?: "Error al registrarse",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun login() {
        val cleanEmail = email.trim()

        // Aquí comprobamos que no falten datos.
        if (cleanEmail.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Completa email y contraseña", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true
        Log.d("AuthScreen", "Intentando login con email: $cleanEmail")

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    Log.d(
                        "AuthScreen",
                        "Login correcto. Usuario actual: ${user?.email}, verificado: ${user?.isEmailVerified}"
                    )

                    if (user != null && user.isEmailVerified) {
                        // Aquí dejamos entrar solo si el correo ya está verificado.
                        onAuthSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            "Debes verificar tu correo antes de entrar.",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.d("AuthScreen", "Login bloqueado porque el email no está verificado")

                        // Aquí cerramos sesión si todavía no está verificado.
                        auth.signOut()
                    }
                } else {
                    Log.e(
                        "AuthScreen",
                        "Error al iniciar sesión",
                        task.exception
                    )

                    Toast.makeText(
                        context,
                        task.exception?.message ?: "Error al iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun resendVerification() {
        // Aquí reintentamos el envío solo si hay un usuario activo y no está verificado.
        val user = auth.currentUser

        if (user != null && !user.isEmailVerified) {
            Log.d("AuthScreen", "Reintentando envío de verificación a ${user.email}")

            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(
                            "AuthScreen",
                            "Correo de verificación reenviado correctamente a ${user.email}"
                        )

                        Toast.makeText(
                            context,
                            "Te he reenviado el correo de verificación.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Log.e(
                            "AuthScreen",
                            "Error al reenviar correo de verificación",
                            task.exception
                        )

                        Toast.makeText(
                            context,
                            "No se pudo reenviar el correo: ${task.exception?.message ?: "Error desconocido"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Log.d(
                "AuthScreen",
                "No se puede reenviar verificación porque no hay usuario activo o ya está verificado"
            )

            Toast.makeText(
                context,
                "Primero haz login y, si falta verificación, lo reenvío.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Aquí preparamos dominios sugeridos para escribir el email más rápido en móvil.
    val domainOptions = listOf("gmail.com", "hotmail.com")

    val atIndex = email.indexOf('@')
    val hasAtSymbol = atIndex >= 0

    val localPart = if (hasAtSymbol) email.substring(0, atIndex) else ""

    val typedDomain = if (hasAtSymbol && atIndex < email.length - 1) {
        email.substring(atIndex + 1)
    } else {
        ""
    }

    // Aquí enseñamos sugerencias solo cuando tiene sentido.
    val shouldShowEmailSuggestions =
        hasAtSymbol &&
                localPart.isNotBlank() &&
                !typedDomain.contains(" ") &&
                !hideSuggestions &&
                !typedDomain.contains(".")

    val filteredDomains = domainOptions.filter { domain ->
        typedDomain.isBlank() || domain.startsWith(typedDomain, ignoreCase = true)
    }

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
                // Aquí mantenemos el campo limpio y reactivamos sugerencias al seguir escribiendo.
                email = newValue.replace("\n", "")
                hideSuggestions = false
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (shouldShowEmailSuggestions) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDomains) { domain ->
                    SuggestionChip(
                        onClick = {
                            email = "$localPart@$domain"

                            // Aquí ocultamos sugerencias después de elegir una.
                            hideSuggestions = true
                        },
                        label = { Text(domain) }
                    )
                }

                item {
                    SuggestionChip(
                        onClick = {
                            // Aquí dejamos que el usuario complete el dominio a mano.
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
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        // Aquí alternamos entre ver y ocultar la contraseña.
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { register() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Cargando..." else "Si desea registrarse por primera vez, introduzca su correo electrónico y su nueva contraseña; después, confirme el registro en su email")
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