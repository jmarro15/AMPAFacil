package com.ampafacil.app.data

/* Estados soportados para cada cargo de directiva.
   Los dejamos centralizados para evitar strings sueltos por pantallas y repositorio. */
object BoardInviteStatus {
    const val VACANT = "VACANT"
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REVOKED = "REVOKED"
}
