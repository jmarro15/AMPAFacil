// File: app/src/main/java/com/ampafacil/app/data/Announcement.kt
package com.ampafacil.app.data

import com.google.firebase.Timestamp

// Aquí representamos un comunicado interno tal y como se guarda en Firestore.
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val attachmentUrl: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val targetScope: String = "ALL",
    val createdAt: Timestamp? = null
)
