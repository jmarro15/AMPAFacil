// File: app/src/main/java/com/ampafacil/app/data/AmpaMember.kt
package com.ampafacil.app.data

import com.google.firebase.Timestamp

data class AmpaMember(
    // Aquí guardamos el uid para localizar al miembro correcto dentro del sistema.
    val uid: String = "",
    // Aquí guardamos el código del AMPA para saber a qué asociación pertenece.
    val ampaCode: String = "",
    // Aquí dejamos una clave interna de apoyo, útil para administración pero no para exportaciones.
    val memberKey: String = "",
    val role: String = "FAMILY",
    val firstName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val phone: String = "",
    val email: String = "",
    // Aquí añadimos un correo secundario para poder enviar comunicaciones al otro tutor si hace falta.
    val secondaryEmail: String = "",
    val dni: String = "",
    // Aquí enseñamos un pequeño resumen de hijos sin tener que entrar en más detalle.
    val childrenCount: Int = 0,
    val childrenSummary: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)