// File: app/src/main/java/com/ampafacil/app/data/BoardRoleInvite.kt
package com.ampafacil.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/* Aquí representamos una invitación guardada para un cargo de directiva.
   Cada rol tiene su propio documento dentro de roleInvites. */
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

/* Aquí transformamos el documento de Firestore en un objeto Kotlin fácil de usar. */
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