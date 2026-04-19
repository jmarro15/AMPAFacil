// File: app/src/main/java/com/ampafacil/app/ui/screens/CreateAnnouncementScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.Roles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementScreen(
    ampaCode: String,
    currentUserRole: String,
    onBack: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var message by remember { mutableStateOf(TextFieldValue("")) }
    var attachmentUrl by remember { mutableStateOf(TextFieldValue("")) }
    var isPublishing by remember { mutableStateOf(false) }

    val normalizedRole = currentUserRole.uppercase()
    val canPublish = normalizedRole == Roles.PRESIDENT ||
            normalizedRole == Roles.SECRETARY ||
            normalizedRole == Roles.VICEPRESIDENT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear comunicado") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!canPublish) {
                // Aquí frenamos la creación si el rol no pertenece a la directiva autorizada.
                Text("Acceso no permitido para crear comunicados.")
                return@Column
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asunto") },
                singleLine = true
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mensaje") },
                minLines = 5
            )

            OutlinedTextField(
                value = attachmentUrl,
                onValueChange = { attachmentUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enlace adjunto opcional") },
                singleLine = true
            )

            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    val cleanTitle = title.text.trim()
                    val cleanMessage = message.text.trim()
                    val cleanAttachmentUrl = attachmentUrl.text.trim()

                    if (cleanTitle.isBlank()) {
                        Toast.makeText(context, "El asunto es obligatorio.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (cleanMessage.isBlank()) {
                        Toast.makeText(context, "El mensaje es obligatorio.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (ampaCode.isBlank()) {
                        Toast.makeText(context, "No hay AMPA activa para publicar.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (currentUser == null) {
                        Toast.makeText(context, "Necesitamos sesión iniciada para publicar.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isPublishing = true

                    // Aquí creamos el comunicado en la colección interna del AMPA.
                    db.collection("ampas")
                        .document(ampaCode)
                        .collection("announcements")
                        .add(
                            mapOf(
                                "title" to cleanTitle,
                                "message" to cleanMessage,
                                "attachmentUrl" to cleanAttachmentUrl,
                                "createdByUid" to currentUser.uid,
                                "createdByName" to (currentUser.displayName?.takeIf { it.isNotBlank() }
                                    ?: currentUser.email.orEmpty()),
                                "targetScope" to "ALL",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            isPublishing = false
                            Toast.makeText(context, "Comunicado publicado.", Toast.LENGTH_LONG).show()
                            onPublished()
                        }
                        .addOnFailureListener {
                            isPublishing = false
                            Toast.makeText(
                                context,
                                "No se pudo publicar el comunicado.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                },
                enabled = !isPublishing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPublishing) "Publicando..." else "Publicar comunicado")
            }
        }
    }
}
