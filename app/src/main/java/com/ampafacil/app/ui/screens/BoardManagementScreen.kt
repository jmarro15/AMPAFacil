//BoardManagementScreen
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
    var invites by remember { mutableStateOf<List<BoardRoleInvite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val emailsInEdition = remember { mutableStateMapOf<String, String>() }
    val memberNames = remember { mutableStateMapOf<String, String>() }

    fun refresh() {
        if (ampaCode.isBlank()) return
        isLoading = true
        repository.loadInvites(
            ampaCode = ampaCode,
            onSuccess = { list ->
                invites = list
                list.forEach { invite ->
                    emailsInEdition[invite.role] = invite.email
                }

                // Si el cargo ya está ocupado (memberUid), intentamos mostrar nombre legible del miembro.
                list.forEach { invite ->
                    val memberUid = invite.memberUid ?: return@forEach
                    db.collection("ampas").document(ampaCode)
                        .collection("members").document(memberUid)
                        .get()
                        .addOnSuccessListener { memberDoc ->
                            val nombre = memberDoc.getString("nombre").orEmpty()
                            val apellidos = memberDoc.getString("apellidos").orEmpty()
                            val fullName = listOf(nombre, apellidos).filter { it.isNotBlank() }.joinToString(" ")
                            memberNames[invite.role] = if (fullName.isBlank()) memberUid else fullName
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

    LaunchedEffect(Unit) {
        if (uid.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        // Leemos el AMPA activo desde users para reutilizar el contexto ya existente del proyecto.
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

            // TODO Fase 2: además de memberUid, resolver por email para detectar invitaciones pendientes tras login.
            BoardRoles.all.forEach { role ->
                val invite = invites.firstOrNull { it.role == role }
                    ?: BoardRoleInvite(role = role, status = BoardInviteStatus.VACANT)
                val editableEmail = emailsInEdition[role].orEmpty()

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(BoardRoles.label(role))
                        Text("Estado: ${BoardInviteStatus.label(invite.status)}")
                        if (invite.memberUid != null) {
                            val memberLabel = memberNames[role] ?: invite.memberUid
                            Text("Miembro asignado: $memberLabel")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editableEmail,
                            onValueChange = { emailsInEdition[role] = it.trim() },
                            label = { Text("Email asociado") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                repository.saveInvite(
                                    ampaCode = ampaCode,
                                    role = role,
                                    emailInput = editableEmail,
                                    actorUid = uid,
                                    onSuccess = {
                                        Toast.makeText(context, "Invitación actualizada.", Toast.LENGTH_SHORT).show()
                                        refresh()
                                    },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar o actualizar invitación")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                    )
                                },
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
                                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                    )
                                },
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
                                        // V1: simulamos el reenvío actualizando contador y fecha de último envío.
                                        Toast.makeText(
                                            context,
                                            "Reenvío registrado. El envío real de email se integrará en una fase posterior.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        refresh()
                                    },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                )
                            },
                            enabled = editableEmail.isNotBlank(),
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
