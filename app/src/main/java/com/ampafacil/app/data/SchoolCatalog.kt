// File: app/src/main/java/com/ampafacil/app/data/SchoolCatalog.kt
package com.ampafacil.app.data

object SchoolCatalog {

    /* Aquí centralizamos valores permitidos para que no haya texto libre en Firestore. */
    val ciclos = listOf("INFANTIL", "PRIMARIA", "ESO", "BACHILLER")

    val clases = listOf("A", "B", "C", "D", "E", "F")

    fun cursosPorCiclo(ciclo: String): List<String> {
        /* Aquí devolvemos cursos según el ciclo para que el usuario solo pueda elegir valores válidos. */
        return when (ciclo) {
            "INFANTIL" -> listOf("3A", "4A", "5A")
            "PRIMARIA" -> listOf("1P", "2P", "3P", "4P", "5P", "6P")
            "ESO" -> listOf("1ESO", "2ESO", "3ESO", "4ESO")
            "BACHILLER" -> listOf("1BACH", "2BACH")
            else -> emptyList()
        }
    }

    fun labelCurso(curso: String): String {
        /* Aquí convertimos el código guardado en Firestore a un texto bonito para la pantalla. */
        return when {
            curso.endsWith("A") -> "${curso.dropLast(1)} años"
            curso.endsWith("P") -> "${curso.dropLast(1)}º Primaria"
            curso.endsWith("ESO") -> "${curso.dropLast(3)}º ESO"
            curso.endsWith("BACH") -> "${curso.dropLast(4)}º Bachiller"
            else -> curso
        }
    }
}