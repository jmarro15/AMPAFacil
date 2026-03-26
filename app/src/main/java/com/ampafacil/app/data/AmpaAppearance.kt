// File: app/src/main/java/com/ampafacil/app/data/AmpaAppearance.kt
package com.ampafacil.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/*
 * Aquí definimos cómo guardamos y leemos la apariencia visual de cada AMPA.
 * Está hecho con campos sencillos para que sea fácil defenderlo en el proyecto DAM.
 */
data class AmpaAppearance(
    val primaryColor: String = "#1565C0",
    val secondaryColor: String = "#2E7D32",
    val backgroundColor: String = "#F7F9FC",
    val borderThickness: String = BorderThickness.MEDIUM.value,
    val fontStyle: String = FontStyleOption.DEFAULT.value,
    val logoUrl: String = "",
    val schoolName: String = ""
)

enum class BorderThickness(val value: String, val dp: Int) {
    THIN("THIN", 1),
    MEDIUM("MEDIUM", 2),
    THICK("THICK", 4)
}

enum class FontStyleOption(val value: String) {
    DEFAULT("DEFAULT"),
    ROUNDED("ROUNDED"),
    SERIF("SERIF")
}

fun borderThicknessFrom(value: String?): BorderThickness {
    return BorderThickness.entries.firstOrNull { it.value == value } ?: BorderThickness.MEDIUM
}

fun fontStyleFrom(value: String?): FontStyleOption {
    return FontStyleOption.entries.firstOrNull { it.value == value } ?: FontStyleOption.DEFAULT
}

fun AmpaAppearance.toMap(): Map<String, Any> {
    return mapOf(
        "primaryColor" to primaryColor,
        "secondaryColor" to secondaryColor,
        "backgroundColor" to backgroundColor,
        "borderThickness" to borderThickness,
        "fontStyle" to fontStyle,
        "logoUrl" to logoUrl,
        "schoolName" to schoolName
    )
}

fun ampaAppearanceFromMap(map: Map<String, Any>?): AmpaAppearance {
    if (map == null) return AmpaAppearance()

    return AmpaAppearance(
        primaryColor = map["primaryColor"]?.toString() ?: "#1565C0",
        secondaryColor = map["secondaryColor"]?.toString() ?: "#2E7D32",
        backgroundColor = map["backgroundColor"]?.toString() ?: "#F7F9FC",
        borderThickness = map["borderThickness"]?.toString() ?: BorderThickness.MEDIUM.value,
        fontStyle = map["fontStyle"]?.toString() ?: FontStyleOption.DEFAULT.value,
        logoUrl = map["logoUrl"]?.toString() ?: "",
        schoolName = map["schoolName"]?.toString() ?: ""
    )
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

fun colorToHex(color: Color): String {
    return String.format("#%06X", 0xFFFFFF and color.toArgb())
}
