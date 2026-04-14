// File: app/src/main/java/com/ampafacil/app/data/AmpaMember.kt
package com.ampafacil.app.data

import com.google.firebase.Timestamp

data class AmpaMember(
    // Este identificador ayuda a localizar a la familia correcta dentro del sistema.
    val uid: String = "",
    // Este código sirve para saber a qué AMPA pertenece este perfil.
    val ampaCode: String = "",
    // Esta clave se guarda como referencia interna, pero no está pensada para exportaciones.
    val memberKey: String = "",
    val role: String = "",
    val firstName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val phone: String = "",
    val email: String = "",
    // Este correo secundario permite avisar también al otro tutor cuando haga falta.
    val secondaryEmail: String = "",
    val dni: String = "",
    // Estos dos datos se muestran en lectura para que la familia vea rápido el resumen de hijos.
    val childrenCount: Int = 0,
    val childrenSummary: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)