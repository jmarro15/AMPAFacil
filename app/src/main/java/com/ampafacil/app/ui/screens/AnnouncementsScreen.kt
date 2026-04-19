// File: app/src/main/java/com/ampafacil/app/ui/screens/AnnouncementsScreen.kt
package com.ampafacil.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.Announcement
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    ampaCode: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var isLoading by remember { mutableStateOf(true) }
    var announcements by remember { mutableStateOf(emptyList<Announcement>()) }

    DisposableEffect(ampaCode) {
        if (ampaCode.isBlank()) {
            isLoading = false
            announcements = emptyList()
            onDispose { }
        } else {
            // Aquí escuchamos cambios para que la lista esté viva sin recargar pantalla.
            val registration = db.collection("ampas")
                .document(ampaCode)
                .collection("announcements")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        Toast.makeText(
                            context,
                            "No se pudieron cargar los comunicados.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    announcements = snapshot?.documents.orEmpty().map { doc ->
                        Announcement(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            message = doc.getString("message").orEmpty(),
                            attachmentUrl = doc.getString("attachmentUrl").orEmpty(),
                            createdByUid = doc.getString("createdByUid").orEmpty(),
                            createdByName = doc.getString("createdByName").orEmpty(),
                            targetScope = doc.getString("targetScope").orEmpty(),
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    isLoading = false
                }

            onDispose { registration.remove() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunicados") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Atrás")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            announcements.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Todavía no hay comunicados publicados.")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(announcements, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = item.title)

                                item.createdAt?.let { createdAt ->
                                    Text(text = formatDate(createdAt))
                                }

                                Text(text = item.message)

                                val rawUrl = item.attachmentUrl.trim()
                                if (rawUrl.isNotBlank()) {
                                    Text(text = rawUrl)

                                    Button(
                                        onClick = {
                                            // Aquí normalizamos el enlace para abrirlo fuera de la app.
                                            val normalizedUrl = if (
                                                rawUrl.startsWith("http://", ignoreCase = true) ||
                                                rawUrl.startsWith("https://", ignoreCase = true)
                                            ) rawUrl else "https://$rawUrl"

                                            try {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(normalizedUrl)
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo abrir el adjunto.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Text("Abrir adjunto")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Timestamp): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(timestamp.toDate())
}
