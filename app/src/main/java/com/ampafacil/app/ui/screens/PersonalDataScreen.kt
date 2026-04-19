// File: app/src/main/java/com/ampafacil/app/ui/screens/PersonalDataScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDataScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val uid = auth.currentUser?.uid

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var ampaCode by remember { mutableStateOf("") }
    var memberKey by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var createdAt by remember { mutableStateOf<Timestamp?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName1 by remember { mutableStateOf("") }
    var lastName2 by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var secondaryEmail by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }

    var childrenCount by remember { mutableStateOf(0) }
    var childrenSummary by remember { mutableStateOf("") }

    // Aquí cargamos primero el usuario, luego el member y después los hijos reales.
    LaunchedEffect(uid) {
        if (uid.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val active = userDoc.getString("activeAmpaCode")?.trim().orEmpty()
                val fallback = userDoc.getString("ampaCode")?.trim().orEmpty()
                ampaCode = if (active.isNotBlank()) active else fallback

                if (ampaCode.isBlank()) {
                    isLoading = false
                    return@addOnSuccessListener
                }

                val memberRef = db.collection("ampas").document(ampaCode)
                    .collection("members").document(uid)

                memberRef.get()
                    .addOnSuccessListener { memberDoc ->
                        // Aquí leemos manualmente para adaptarnos a los distintos nombres de campo
                        // que ya existen en Firestore sin depender de toObject().
                        memberKey = memberDoc.getString("memberKey")?.ifBlank { uid } ?: uid
                        role = memberDoc.getString("role").orEmpty()
                        notes = memberDoc.getString("notes").orEmpty()
                        createdAt = memberDoc.getTimestamp("createdAt")

                        firstName = memberDoc.getString("firstName")
                            ?: memberDoc.getString("nombre")
                                    ?: ""

                        lastName1 = memberDoc.getString("lastName1")
                            ?: memberDoc.getString("apellidos")
                                    ?: ""

                        lastName2 = memberDoc.getString("lastName2").orEmpty()

                        phone = memberDoc.getString("phone")
                            ?: memberDoc.getString("telefono")
                                    ?: ""

                        email = memberDoc.getString("email").orEmpty()
                        secondaryEmail = memberDoc.getString("secondaryEmail").orEmpty()
                        dni = memberDoc.getString("dni").orEmpty()

                        // Aquí traemos los hijos reales desde la subcolección para que el resumen
                        // siempre coincida con lo que de verdad hay guardado.
                        memberRef.collection("children").get()
                            .addOnSuccessListener { childrenSnapshot ->
                                val children = childrenSnapshot.documents.map { childDoc ->
                                    val nombre = childDoc.getString("nombre").orEmpty().trim()
                                    val apellidos = childDoc.getString("apellidos").orEmpty().trim()
                                    val curso = childDoc.getString("curso").orEmpty().trim()
                                    val clase = childDoc.getString("clase").orEmpty().trim()

                                    if (nombre.isBlank() && apellidos.isBlank()) {
                                        ""
                                    } else {
                                        "$nombre $apellidos · $curso $clase".trim()
                                    }
                                }.filter { it.isNotBlank() }

                                childrenCount = children.size
                                childrenSummary = if (children.isEmpty()) {
                                    ""
                                } else {
                                    children.joinToString("\n")
                                }

                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "No he podido cargar el resumen real de hijos.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener {
                        isLoading = false
                        Toast.makeText(
                            context,
                            "No he podido cargar tus datos personales.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(
                    context,
                    "No he podido leer el perfil de usuario.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun savePersonalData() {
        if (uid.isNullOrBlank()) {
            Toast.makeText(context, "No hay sesión iniciada.", Toast.LENGTH_LONG).show()
            return
        }
        if (ampaCode.isBlank()) {
            Toast.makeText(context, "No hay AMPA activo para guardar datos.", Toast.LENGTH_LONG).show()
            return
        }
        if (isSaving) return

        isSaving = true

        val now = Timestamp.now()
        val safeMemberKey = memberKey.ifBlank { uid }

        // Aquí guardamos en merge para no romper otros datos internos del miembro.
        val payload = hashMapOf<String, Any>(
            "uid" to uid,
            "ampaCode" to ampaCode,
            "memberKey" to safeMemberKey,
            "role" to role,
            "firstName" to firstName.trim(),
            "lastName1" to lastName1.trim(),
            "lastName2" to lastName2.trim(),
            "phone" to phone.trim(),
            "email" to email.trim(),
            "secondaryEmail" to secondaryEmail.trim(),
            "dni" to dni.trim(),
            "childrenCount" to childrenCount,
            "childrenSummary" to childrenSummary,
            "notes" to notes,
            "updatedAt" to now
        )

        // También mantenemos nombres equivalentes para convivir con datos previos del proyecto.
        payload["nombre"] = firstName.trim()
        payload["apellidos"] = listOf(lastName1.trim(), lastName2.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        payload["telefono"] = phone.trim()

        if (createdAt == null) {
            payload["createdAt"] = now
        }

        db.collection("ampas").document(ampaCode)
            .collection("members").document(uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                isSaving = false
                memberKey = safeMemberKey
                if (createdAt == null) {
                    createdAt = now
                }
                Toast.makeText(
                    context,
                    "Datos personales guardados correctamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis datos personales") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        when {
            uid == null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay sesión iniciada.")
                }
            }

            isLoading -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text("Cargando mis datos…")
                }
            }

            ampaCode.isBlank() -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No encuentro un AMPA activo para mostrar este formulario.")
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aquí podemos actualizar los datos para que la comunicación del AMPA sea más fácil.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lastName1,
                        onValueChange = { lastName1 = it },
                        label = { Text("Primer apellido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lastName2,
                        onValueChange = { lastName2 = it },
                        label = { Text("Segundo apellido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo principal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = secondaryEmail,
                        onValueChange = { secondaryEmail = it },
                        label = { Text("Correo secundario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dni,
                        onValueChange = { dni = it },
                        label = { Text("DNI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Este bloque se enseña en solo lectura para no mezclar este formulario con la gestión de hijos.
                    OutlinedTextField(
                        value = childrenCount.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Número de hijos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = childrenSummary,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Resumen familiar") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { savePersonalData() },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardando…")
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}