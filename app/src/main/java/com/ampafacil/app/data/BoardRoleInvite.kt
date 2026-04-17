package com.ampafacil.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/* Modelo de Firestore para ampas/{ampaCode}/roleInvites/{role}.
   Guardamos un documento persistente por cargo para que la invitación sea estable y editable. */
data class BoardRoleInvite(
    val role: String = "",
    val email: String = "",
    val status: String = BoardInviteStatus.VACANT,
    val sentCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastSentAt: Timestamp? = null,
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val memberUid: String? = null
)

fun boardRoleInviteFromDoc(doc: DocumentSnapshot): BoardRoleInvite {
    return BoardRoleInvite(
        role = doc.getString("role") ?: doc.id,
        email = doc.getString("email") ?: "",
        status = doc.getString("status") ?: BoardInviteStatus.VACANT,
        sentCount = (doc.getLong("sentCount") ?: 0L).toInt(),
        createdAt = doc.getTimestamp("createdAt"),
        updatedAt = doc.getTimestamp("updatedAt"),
        lastSentAt = doc.getTimestamp("lastSentAt"),
        createdByUid = doc.getString("createdByUid") ?: "",
        updatedByUid = doc.getString("updatedByUid") ?: "",
        memberUid = doc.getString("memberUid")
    )
}
