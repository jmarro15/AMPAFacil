// File: app/src/main/java/com/ampafacil/app/ui/screens/AmpaCodeScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.Roles
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val uid = auth.currentUser?.uid

    /* Aquí, si ya teníamos perfil guardado, lo precargamos para no volver a escribirlo a mano. */
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    if (nombre.isBlank()) nombre = doc.getString("nombre") ?: ""
                    if (apellidos.isBlank()) apellidos = doc.getString("apellidos") ?: ""
                    if (telefono.isBlank()) telefono = doc.getString("telefono") ?: ""
                }
            }
    }

    fun validateAndJoin() {
        /* Aquí validamos código y datos mínimos del miembro antes de unirnos. */
        val clean = code.trim()

        if (clean.length != 6 || clean.any { !it.isDigit() }) {
            Toast.makeText(context, "El código tiene que tener 6 números.", Toast.LENGTH_LONG).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "No hay sesión iniciada. Hay que entrar antes.", Toast.LENGTH_LONG).show()
            return
        }

        val email = (user.email ?: "").trim()
        if (email.isBlank()) {
            Toast.makeText(context, "Este usuario no tiene email. Para unirse necesitamos email/password.", Toast.LENGTH_LONG).show()
            return
        }

        if (nombre.trim().isBlank() || apellidos.trim().isBlank()) {
            Toast.makeText(context, "Necesitamos nombre y apellidos para identificar al miembro.", Toast.LENGTH_LONG).show()
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

                        /* Aquí actualizamos el perfil del usuario (sirve para rellenar en futuras pantallas). */
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
                            /* Aquí ya éramos miembros: rellenamos datos identificativos sin cambiar role/status. */
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
                                    isLoading = false
                                    Toast.makeText(context, "Datos actualizados ✅", Toast.LENGTH_SHORT).show()
                                    onCodeAccepted()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Error actualizando datos: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                            return@addOnSuccessListener
                        }

                        /* Aquí creamos membership como FAMILY/TUTOR en estado PENDING, ya con datos completos. */
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

                        /* Aquí escribimos en batch para que quede todo coherente de golpe. */
                        val batch = db.batch()
                        batch.set(memberRef, memberData)
                        batch.set(userRef, userUpdate, SetOptions.merge())

                        batch.commit()
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Unión realizada ✅ (pendiente de aprobación)", Toast.LENGTH_SHORT).show()
                                onCodeAccepted()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error uniéndonos al AMPA: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Error comprobando membresía: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error consultando Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Código AMPA", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { new -> code = new.filter { it.isDigit() }.take(6) },
            label = { Text("Código AMPA (6 dígitos)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Datos del miembro")
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = apellidos,
            onValueChange = { apellidos = it },
            label = { Text("Apellidos") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(15) },
            label = { Text("Teléfono (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text("Tipo de usuario")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedRole == Roles.FAMILY,
                onClick = { selectedRole = Roles.FAMILY }
            )
            Text("Familia", modifier = Modifier.padding(end = 16.dp))

            RadioButton(
                selected = selectedRole == Roles.TUTOR,
                onClick = { selectedRole = Roles.TUTOR }
            )
            Text("Tutor/a")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { validateAndJoin() },
            enabled = !isLoading && code.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isLoading) "Uniéndonos..." else "Unirse") }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { onCreateAmpa() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Crear un AMPA nuevo (solo directiva)") }
    }
}