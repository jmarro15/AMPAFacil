// File: app/src/main/java/com/ampafacil/app/data/BoardInviteStatus.kt
package com.ampafacil.app.data

/* Aquí centralizamos los estados internos de los cargos de directiva.
   Además añadimos una ayuda para enseñar textos más naturales en pantalla. */
object BoardInviteStatus {
    const val VACANT = "VACANT"
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REVOKED = "REVOKED"

    /* Aquí traducimos el estado interno a un texto más claro para la interfaz.
       Si el cargo está ocupado por una persona real, mostramos "Ocupado". */
    fun label(status: String, isOccupied: Boolean = false): String {
        if (isOccupied) return "Ocupado"

        return when (status) {
            VACANT -> "Vacante"
            PENDING -> "Pendiente"
            ACCEPTED -> "Aceptado"
            REVOKED -> "Revocada"
            else -> status
        }
    }
}