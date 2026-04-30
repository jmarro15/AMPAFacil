// File: app/src/main/java/com/ampafacil/app/ui/screens/AmpaCodeScreen.kt
package com.ampafacil.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.AmpaAppearance
import com.ampafacil.app.data.FontStyleOption
import com.ampafacil.app.data.Roles
import com.ampafacil.app.data.ampaAppearanceFromMap
import com.ampafacil.app.data.borderThicknessFrom
import com.ampafacil.app.data.fontStyleFrom
import com.ampafacil.app.data.parseHexColor
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.foundation.BorderStroke

import androidx.compose.ui.text.font.FontWeight



@Composable
fun AmpaCodeScreen(
    onCodeAccepted: () -> Unit,
    onCreateAmpa: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    var selectedRole by remember { mutableStateOf(Roles.FAMILY) }
    var appearance by remember { mutableStateOf(AmpaAppearance()) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val uid = auth.currentUser?.uid

    /*
     * Aquí cargamos datos del perfil para no pedirlos otra vez.
     * Si además el usuario ya tiene activeAmpaCode/ampaCode, también cargamos la apariencia de ese AMPA.
     */
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    if (nombre.isBlank()) nombre = doc.getString("nombre") ?: ""
                    if (apellidos.isBlank()) apellidos = doc.getString("apellidos") ?: ""
                    if (telefono.isBlank()) telefono = doc.getString("telefono") ?: ""

                    val activeCode = doc.getString("activeAmpaCode")?.trim()
                        ?: doc.getString("ampaCode")?.trim()

                    if (!activeCode.isNullOrBlank()) {
                        db.collection("ampas").document(activeCode).get()
                            .addOnSuccessListener { ampaDoc ->
                                val schoolName = ampaDoc.getString("schoolName") ?: ""
                                val loaded =
                                    ampaAppearanceFromMap(ampaDoc.get("themeConfig") as? Map<String, Any>)
                                appearance = loaded.copy(
                                    schoolName = if (loaded.schoolName.isBlank()) schoolName else loaded.schoolName
                                )
                            }
                    }
                }
            }
    }

    val primaryColor = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val secondaryColor = parseHexColor(appearance.secondaryColor, Color(0xFF2E7D32))
    val backgroundColor = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val borderThickness = borderThicknessFrom(appearance.borderThickness)
    val borderWidth = (borderThickness.dp).dp
    val fontStyle = fontStyleFrom(appearance.fontStyle)

    // Aquí usamos el mismo gris oscuro en todos los estados del texto,
    // para que se vea bien mientras escribimos y también al salir del campo.
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF424242),
        unfocusedTextColor = Color(0xFF424242),
        focusedLabelColor = Color(0xFF616161),
        unfocusedLabelColor = Color(0xFF9E9E9E),
        focusedPlaceholderColor = Color(0xFF9E9E9E),
        unfocusedPlaceholderColor = Color(0xFFBDBDBD),
        cursorColor = primaryColor,
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color.Gray
    )

    val fontFamily = when (fontStyle) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
        FontStyleOption.MODERN -> FontFamily.SansSerif
        FontStyleOption.FRIENDLY -> FontFamily.SansSerif
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = primaryColor,
        contentColor = Color.White
    )

    fun validateAndJoin() {
        /* Aquí validamos código y datos mínimos del miembro antes de unirnos. */
        val clean = code.trim()

        // Debemos colocar un texto advirtiendo de que el código debe ser el que le ha proporcionado el AMPA.
        if (clean.length != 6 || clean.any { !it.isDigit() }) {
            Toast.makeText(context, "El código tiene que tener 6 números.", Toast.LENGTH_LONG)
                .show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(
                context,
                "No hay sesión iniciada. Hay que entrar antes.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val email = (user.email ?: "").trim()
        if (email.isBlank()) {
            Toast.makeText(
                context,
                "Este usuario no tiene email. Para unirse necesitamos email/password.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (nombre.trim().isBlank() || apellidos.trim().isBlank()) {
            Toast.makeText(
                context,
                "Necesitamos nombre y apellidos para identificar al miembro.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (selectedRole != Roles.FAMILY && selectedRole != Roles.TUTOR) {
            Toast.makeText(context, "Rol no válido.", Toast.LENGTH_LONG).show()
            return
        }

        isLoading = true

        val uidReal = user.uid
        val ampaRef = db.collection("ampas").document(clean)
        val memberRef = ampaRef.collection("members").document(uidReal)
        val userRef = db.collection("users").document(uidReal)

        /* Aquí comprobamos si el AMPA existe antes de escribir nada. */
        ampaRef.get()
            .addOnSuccessListener { ampaDoc ->
                if (!ampaDoc.exists()) {
                    isLoading = false
                    Toast.makeText(context, "Ese código no existe.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                /* Aquí comprobamos si ya existía membership para no tocar role/status. */
                memberRef.get()
                    .addOnSuccessListener { memberDoc ->
                        val now = Timestamp.now()

                        /* Aquí actualizamos el perfil del usuario. */
                        val userUpdate = hashMapOf(
                            "email" to email,
                            "nombre" to nombre.trim(),
                            "apellidos" to apellidos.trim(),
                            "telefono" to telefono.trim(),
                            "activeAmpaCode" to clean,
                            "ampaCode" to clean,
                            "updatedAt" to now
                        )

                        if (memberDoc.exists()) {
                            val existingRole = memberDoc.getString("role") ?: ""
                            val existingStatus = memberDoc.getString("status") ?: ""
                            val existingEmail = memberDoc.getString("email") ?: email

                            val memberPatch = hashMapOf(
                                "email" to existingEmail,
                                "role" to existingRole,
                                "status" to existingStatus,
                                "nombre" to nombre.trim(),
                                "apellidos" to apellidos.trim(),
                                "telefono" to telefono.trim(),
                                "updatedAt" to now
                            )

                            val batch = db.batch()
                            batch.set(userRef, userUpdate, SetOptions.merge())
                            batch.set(memberRef, memberPatch, SetOptions.merge())

                            batch.commit()
                                .addOnSuccessListener {
                                    // Aquí guardamos en local el último AMPA válido para poder reutilizarlo
                                    // en Splash y en Auth sin depender siempre del login.
                                    val prefs = context.getSharedPreferences(
                                        "ampafacil_auth",
                                        Context.MODE_PRIVATE
                                    )
                                    prefs.edit().putString("last_ampa_code", clean).apply()

                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Datos actualizados ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onCodeAccepted()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Error actualizando datos: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            return@addOnSuccessListener
                        }

                        /* Aquí creamos membership como FAMILY/TUTOR en estado PENDING. */
                        val memberData = hashMapOf(
                            "email" to email,
                            "nombre" to nombre.trim(),
                            "apellidos" to apellidos.trim(),
                            "telefono" to telefono.trim(),
                            "role" to selectedRole,
                            "status" to "PENDING",
                            "createdAt" to now,
                            "updatedAt" to now
                        )

                        val batch = db.batch()
                        batch.set(memberRef, memberData)
                        batch.set(userRef, userUpdate, SetOptions.merge())

                        batch.commit()
                            .addOnSuccessListener {
                                // Aquí guardamos en local el último AMPA válido para que la app pueda
                                // recuperar logo y apariencia también cuando ya no hay sesión activa.
                                val prefs = context.getSharedPreferences(
                                    "ampafacil_auth",
                                    Context.MODE_PRIVATE
                                )
                                prefs.edit().putString("last_ampa_code", clean).apply()

                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Unión realizada ✅ (pendiente de aprobación)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onCodeAccepted()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Error uniéndonos al AMPA: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Error comprobando membresía: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(
                    context,
                    "Error consultando Firestore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Código AMPA",
            style = MaterialTheme.typography.headlineSmall,
            color = primaryColor,
            fontFamily = fontFamily
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, secondaryColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Introduce el código de 6 dígitos que te ha facilitado tu AMPA",
                    color = primaryColor,
                    fontFamily = fontFamily
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { new -> code = new.filter { it.isDigit() }.take(6) },
                    label = { Text("Código AMPA (6 dígitos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Datos del padre/madre o tutor/a",
                    color = primaryColor,
                    fontFamily = fontFamily
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )



                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(15) },
                    label = { Text("Teléfono (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(Modifier.height(12.dp))



                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { validateAndJoin() },
                    enabled = !isLoading && code.length == 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = buttonColors
                ) {
                    Text(
                        text = if (isLoading) "Uniéndonos..." else "Unirse",
                        fontFamily = fontFamily
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { onCreateAmpa() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = 2.dp,
                color = primaryColor
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = primaryColor.copy(alpha = 0.08f),
                contentColor = primaryColor
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mostramos claramente que esta opción sirve para crear un AMPA desde cero.
                Text(
                    text = "CREAR UNA AMPA NUEVA",
                    color = primaryColor,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Aclaramos que esta opción no es para familias, sino para miembros de la directiva.
                Text(
                    text = "Solo presidente, secretario, vicepresidente",
                    color = primaryColor.copy(alpha = 0.80f),
                    fontFamily = fontFamily,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}