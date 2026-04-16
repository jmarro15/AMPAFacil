// File: app/src/main/java/com/ampafacil/app/data/BoardInviteStatus.kt
package com.ampafacil.app.data

/* Aquí centralizamos los estados internos de cada cargo.
   Además añadimos una función para enseñar un texto más natural en pantalla. */
object BoardInviteStatus {
    const val VACANT = "VACANT"
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REVOKED = "REVOKED"

    /* Aquí convertimos el valor interno en una etiqueta más clara para la interfaz. */
    fun label(status: String): String = when (status) {
        VACANT -> "Vacante"
        PENDING -> "Pendiente"
        ACCEPTED -> "Aceptada"
        REVOKED -> "Revocada"
        else -> status
    }
}