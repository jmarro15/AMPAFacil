package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.ampafacil.app.data.BoardRoles
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialBoardInvitesScreen(
    ampaCode: String,
    creatorRole: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    repository: BoardInvitesRepository = BoardInvitesRepository()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid.orEmpty()

    val remainingRoles = remember(creatorRole) { BoardRoles.remainingRoles(creatorRole) }
    val emailsByRole = remember(remainingRoles) {
        mutableStateMapOf<String, String>().apply {
            remainingRoles.forEach { put(it, "") }
        }
    }

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invitaciones iniciales") },
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "AMPA creada correctamente. Ahora puedes invitar por email a los dos cargos de directiva que faltan. Si prefieres, puedes hacerlo más tarde desde Gestión de directiva."
            )

            Spacer(modifier = Modifier.height(16.dp))

            remainingRoles.forEach { role ->
                Text(text = BoardRoles.label(role))
                OutlinedTextField(
                    value = emailsByRole[role].orEmpty(),
                    onValueChange = { emailsByRole[role] = it.trim() },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (emailsByRole[role].isNullOrBlank()) {
                        "Estado inicial: ${BoardInviteStatus.label(BoardInviteStatus.VACANT)}"
                    } else {
                        "Estado inicial: ${BoardInviteStatus.label(BoardInviteStatus.PENDING)}"
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (uid.isBlank()) {
                        Toast.makeText(context, "Sesión inválida. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isSaving = true
                    repository.saveInitialInvites(
                        ampaCode = ampaCode,
                        invitesByRole = remainingRoles.associateWith { emailsByRole[it].orEmpty() },
                        actorUid = uid,
                        onSuccess = {
                            isSaving = false
                            // V1: aún no enviamos correo real. Dejamos persistencia completa y contador preparado.
                            Toast.makeText(
                                context,
                                "Invitaciones guardadas. Podrás reenviarlas desde Gestión de directiva.",
                                Toast.LENGTH_LONG
                            ).show()
                            onDone()
                        },
                        onError = { msg ->
                            isSaving = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar invitaciones")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Saltar por ahora")
            }
        }
    }
}
