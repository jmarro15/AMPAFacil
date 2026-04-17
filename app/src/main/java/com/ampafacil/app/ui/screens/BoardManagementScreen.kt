// File: app/src/main/java/com/ampafacil/app/ui/screens/BoardManagementScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.BoardInviteStatus
import com.ampafacil.app.data.BoardInvitesRepository
import com.ampafacil.app.data.BoardRoleInvite
import com.ampafacil.app.data.BoardRoles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Aquí guardamos la información real de cada miembro de directiva.
// Nos sirve para saber cuándo un cargo está ocupado de verdad.
private data class BoardMemberInfo(
    val uid: String,
    val role: String,
    val nombre: String,
    val apellidos: String,
    val email: String
) {
    // Aquí montamos un nombre legible para enseñar en pantalla.
    fun fullName(): String {
        val composed = listOf(nombre, apellidos)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return if (composed.isBlank()) uid else composed
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardManagementScreen(
    onBack: () -> Unit,
    repository: BoardInvitesRepository = BoardInvitesRepository()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid.orEmpty()

    var ampaCode by remember { mutableStateOf("") }
    var invitesByRole by remember { mutableStateOf<Map<String, BoardRoleInvite>>(emptyMap()) }
    var membersByRole by remember { mutableStateOf<Map<String, BoardMemberInfo>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Aquí guardamos lo que se escribe en cada caja de email.
    val emailsInEdition = remember { mutableStateMapOf<String, String>() }

    fun refresh() {
        if (ampaCode.isBlank()) return

        isLoading = true

        // Primero leemos los miembros reales de directiva.
        // Si un cargo está aquí, para nosotros ese cargo está ocupado.
        db.collection("ampas").document(ampaCode)
            .collection("members")
            .whereIn("role", BoardRoles.all)
            .get()
            .addOnSuccessListener { membersSnapshot ->
                val loadedMembers = membersSnapshot.documents
                    .mapNotNull { doc ->
                        val role = (doc.getString("role") ?: "").uppercase()

                        if (!BoardRoles.all.contains(role)) return@mapNotNull null

                        BoardMemberInfo(
                            uid = doc.id,
                            role = role,
                            nombre = doc.getString("nombre").orEmpty(),
                            apellidos = doc.getString("apellidos").orEmpty(),
                            email = doc.getString("email").orEmpty()
                        )
                    }
                    .associateBy { it.role }

                // Después leemos las invitaciones guardadas.
                // Esto nos sirve para cargos pendientes, vacantes o revocados.
                repository.loadInvites(
                    ampaCode = ampaCode,
                    onSuccess = { invites ->
                        val inviteMap = invites.associateBy { it.role }

                        membersByRole = loadedMembers
                        invitesByRole = inviteMap

                        BoardRoles.all.forEach { role ->
                            val member = loadedMembers[role]
                            val invite = inviteMap[role]

                            emailsInEdition[role] = when {
                                member != null -> member.email
                                invite != null -> invite.email
                                else -> ""
                            }
                        }

                        isLoading = false
                    },
                    onError = { msg ->
                        isLoading = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(
                    context,
                    "No se pudieron cargar los miembros de directiva.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    LaunchedEffect(Unit) {
        if (uid.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        // Aquí leemos el AMPA activo del usuario para saber sobre qué AMPA trabajar.
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                ampaCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim().orEmpty()

                if (ampaCode.isBlank()) {
                    isLoading = false
                    Toast.makeText(context, "No hay AMPA activa para gestionar.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                refresh()
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "No se pudo leer tu contexto de AMPA.", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de directiva") },
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
            verticalArrangement = Arrangement.Top
        ) {
            if (isLoading) {
                Text("Cargando cargos de directiva...")
                return@Column
            }

            if (ampaCode.isBlank()) {
                Text("No hay AMPA activa.")
                return@Column
            }

            BoardRoles.all.forEach { role ->
                val member = membersByRole[role]
                val invite = invitesByRole[role] ?: BoardRoleInvite(role = role)
                val isOccupied = member != null

                // Aquí decidimos el estado interno real del bloque.
                val inferredInternalStatus = when {
                    isOccupied -> BoardInviteStatus.ACCEPTED
                    invite.status == BoardInviteStatus.REVOKED -> BoardInviteStatus.REVOKED
                    emailsInEdition[role].orEmpty().isNotBlank() -> BoardInviteStatus.PENDING
                    else -> BoardInviteStatus.VACANT
                }

                val visibleStatus = BoardInviteStatus.label(
                    status = inferredInternalStatus,
                    isOccupied = isOccupied
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(BoardRoles.label(role))
                        Text("Estado: $visibleStatus")

                        if (member != null) {
                            Text("Miembro: ${member.fullName()}")

                            if (member.email.isNotBlank()) {
                                Text("Email miembro: ${member.email}")
                            }

                            Text(
                                "Este cargo está ocupado por un miembro real. " +
                                        "Las invitaciones solo se usan cuando el cargo queda libre."
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = emailsInEdition[role].orEmpty(),
                            onValueChange = { emailsInEdition[role] = it.trim() },
                            label = { Text("Email asociado") },
                            singleLine = true,
                            enabled = !isOccupied,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                repository.saveInvite(
                                    ampaCode = ampaCode,
                                    role = role,
                                    emailInput = emailsInEdition[role].orEmpty(),
                                    actorUid = uid,
                                    onSuccess = {
                                        Toast.makeText(context, "Invitación actualizada.", Toast.LENGTH_SHORT).show()
                                        refresh()
                                    },
                                    onError = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isOccupied,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar o actualizar invitación")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    repository.markVacant(
                                        ampaCode = ampaCode,
                                        role = role,
                                        actorUid = uid,
                                        onSuccess = {
                                            Toast.makeText(context, "Cargo marcado como vacante.", Toast.LENGTH_SHORT).show()
                                            emailsInEdition[role] = ""
                                            refresh()
                                        },
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isOccupied,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Marcar vacante")
                            }

                            Button(
                                onClick = {
                                    repository.revokeInvite(
                                        ampaCode = ampaCode,
                                        role = role,
                                        actorUid = uid,
                                        onSuccess = {
                                            Toast.makeText(context, "Invitación revocada.", Toast.LENGTH_SHORT).show()
                                            refresh()
                                        },
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isOccupied,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Revocar")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                repository.resendInvite(
                                    ampaCode = ampaCode,
                                    role = role,
                                    actorUid = uid,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Reenvío registrado. El envío real de email llegará en una fase posterior.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        refresh()
                                    },
                                    onError = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isOccupied && emailsInEdition[role].orEmpty().isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reenviar invitación")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}