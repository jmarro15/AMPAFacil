//Boardinvitesrepository
package com.ampafacil.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/* Repositorio pequeño para encapsular operaciones de roleInvites.
   Mantiene la V1 simple: persistencia clara y acciones básicas de gestión. */
class BoardInvitesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun loadInvites(
        ampaCode: String,
        onSuccess: (List<BoardRoleInvite>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("ampas").document(ampaCode)
            .collection("roleInvites")
            .get()
            .addOnSuccessListener { snapshot ->
                val byRole = snapshot.documents
                    .map { boardRoleInviteFromDoc(it) }
                    .associateBy { it.role }
                    .toMutableMap()

                // Si falta algún rol, devolvemos uno "virtual" vacante para que la UI sea consistente.
                BoardRoles.all.forEach { role ->
                    if (!byRole.containsKey(role)) {
                        byRole[role] = BoardRoleInvite(role = role, status = BoardInviteStatus.VACANT)
                    }
                }

                onSuccess(BoardRoles.all.mapNotNull { byRole[it] })
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo cargar la gestión de directiva.")
            }
    }

    fun saveInvite(
        ampaCode: String,
        role: String,
        emailInput: String,
        actorUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = emailInput.trim().lowercase()
        val ref = db.collection("ampas").document(ampaCode)
            .collection("roleInvites").document(role)

        ref.get()
            .addOnSuccessListener { existing ->
                val now = Timestamp.now()
                val exists = existing.exists()
                val oldSentCount = (existing.getLong("sentCount") ?: 0L).toInt()
                val createdByUid = existing.getString("createdByUid")?.takeIf { it.isNotBlank() } ?: actorUid

                val status = if (email.isBlank()) BoardInviteStatus.VACANT else BoardInviteStatus.PENDING
                val data = hashMapOf<String, Any?>(
                    "role" to role,
                    "email" to email,
                    "status" to status,
                    "sentCount" to oldSentCount,
                    "updatedAt" to now,
                    "updatedByUid" to actorUid,
                    // Si volvemos a PENDING tras editar, limpiamos memberUid para evitar inconsistencias.
                    "memberUid" to if (status == BoardInviteStatus.PENDING) null else existing.getString("memberUid")
                )

                if (!exists) {
                    data["createdAt"] = now
                    data["createdByUid"] = createdByUid
                    data["lastSentAt"] = null
                }

                ref.set(data, SetOptions.merge())
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "No se pudo guardar la invitación.") }
            }
            .addOnFailureListener { e -> onError(e.message ?: "No se pudo leer la invitación actual.") }
    }

    fun saveInitialInvites(
        ampaCode: String,
        invitesByRole: Map<String, String>,
        actorUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = db.batch()
        val now = Timestamp.now()

        invitesByRole.forEach { (role, rawEmail) ->
            val email = rawEmail.trim().lowercase()
            val status = if (email.isBlank()) BoardInviteStatus.VACANT else BoardInviteStatus.PENDING
            val ref = db.collection("ampas").document(ampaCode)
                .collection("roleInvites").document(role)

            val data = hashMapOf<String, Any?>(
                "role" to role,
                "email" to email,
                "status" to status,
                "updatedAt" to now,
                "updatedByUid" to actorUid,
                "createdAt" to now,
                "createdByUid" to actorUid,
                // En V1 dejamos contador a 0: aún no hay envío real de email integrado.
                "sentCount" to 0,
                "lastSentAt" to null,
                "memberUid" to null
            )
            batch.set(ref, data, SetOptions.merge())
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "No se pudieron guardar las invitaciones iniciales.") }
    }

    fun markVacant(
        ampaCode: String,
        role: String,
        actorUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateStatus(
            ampaCode = ampaCode,
            role = role,
            actorUid = actorUid,
            status = BoardInviteStatus.VACANT,
            email = "",
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun revokeInvite(
        ampaCode: String,
        role: String,
        actorUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateStatus(
            ampaCode = ampaCode,
            role = role,
            actorUid = actorUid,
            status = BoardInviteStatus.REVOKED,
            email = null,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun resendInvite(
        ampaCode: String,
        role: String,
        actorUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = db.collection("ampas").document(ampaCode)
            .collection("roleInvites").document(role)

        ref.update(
            mapOf(
                "status" to BoardInviteStatus.PENDING,
                "updatedAt" to Timestamp.now(),
                "updatedByUid" to actorUid,
                "lastSentAt" to Timestamp.now(),
                "sentCount" to FieldValue.increment(1)
            )
        )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "No se pudo reenviar la invitación.") }
    }

    private fun updateStatus(
        ampaCode: String,
        role: String,
        actorUid: String,
        status: String,
        email: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = db.collection("ampas").document(ampaCode)
            .collection("roleInvites").document(role)

        val data = hashMapOf<String, Any?>(
            "role" to role,
            "status" to status,
            "updatedAt" to Timestamp.now(),
            "updatedByUid" to actorUid,
            "memberUid" to null
        )
        if (email != null) data["email"] = email

        ref.set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "No se pudo actualizar el estado del cargo.") }
    }
}
