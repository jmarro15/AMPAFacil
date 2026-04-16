package com.ampafacil.app.data

/* Este helper define los tres cargos de directiva "duros" del AMPA.
   Nos sirve para construir la UI y para validar que siempre se gestionen los 3 roles. */
object BoardRoles {
    val all = listOf(
        Roles.PRESIDENT,
        Roles.VICEPRESIDENT,
        Roles.SECRETARY
    )

    fun remainingRoles(creatorRole: String): List<String> {
        val normalized = creatorRole.trim().uppercase()
        return all.filterNot { it == normalized }
    }

    fun label(role: String): String {
        return when (role) {
            Roles.PRESIDENT -> "Presidencia"
            Roles.VICEPRESIDENT -> "Vicepresidencia"
            Roles.SECRETARY -> "Secretaría"
            else -> role
        }
    }
}
