package com.ampafacil.app.data

// Este modelo representa cada tarjeta de familia dentro de "Buscar familias".
data class FamilyDirectoryItem(
    val memberUid: String = "",
    val tutorName: String = "",
    val tutorSurname: String = "",
    val phone: String = "",
    val email: String = "",
    val childrenCount: Int = 0,
    val children: List<DirectoryChild> = emptyList()
)
