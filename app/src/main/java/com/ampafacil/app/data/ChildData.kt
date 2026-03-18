// File: app/src/main/java/com/ampafacil/app/data/ChildData.kt
package com.ampafacil.app.data

data class ChildData(
    val nombre: String = "",
    val apellidos: String = "",
    val ciclo: String = "PRIMARIA",
    val curso: String = "1P",
    val clase: String = "A",
    val alergico: Boolean = false,
    val alergiasDetalle: String = ""
)