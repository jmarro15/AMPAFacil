// File: app/src/main/java/com/ampafacil/app/data/AmpaAppearance.kt
package com.ampafacil.app.data

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable

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
    val schoolName: String = "",
    val gradientDirection: String = GradientDirection.TOP_TO_BOTTOM.value,
    val buttonShape: String = ButtonShape.ROUNDED_MEDIUM.value,
    val themePreset: String = ThemePreset.CLASICO_AZUL.value
)

enum class BorderThickness(val value: String, val dp: Int) {
    THIN("THIN", 1),
    MEDIUM("MEDIUM", 2),
    THICK("THICK", 4)
}

// Tipografía base para textos de la app.
enum class FontStyleOption(val value: String) {
    DEFAULT("DEFAULT"),
    ROUNDED("ROUNDED"),
    SERIF("SERIF"),
    MODERN("MODERN"),
    FRIENDLY("FRIENDLY")
}

// Dirección del degradado para fondos o superficies futuras.
enum class GradientDirection(val value: String) {
    TOP_TO_BOTTOM("TOP_TO_BOTTOM"),
    LEFT_TO_RIGHT("LEFT_TO_RIGHT"),
    DIAGONAL_TOP_START_TO_BOTTOM_END("DIAGONAL_TOP_START_TO_BOTTOM_END")
}

// Forma base de botones para definir el estilo global.
enum class ButtonShape(val value: String, val cornerRadiusDp: Int) {
    RECTANGLE("RECTANGLE", 0),
    ROUNDED_MEDIUM("ROUNDED_MEDIUM", 12),
    PILL("PILL", 24)
}

// Presets listos para usar sin aplicarlos todavía en pantallas.
enum class ThemePreset(val value: String) {
    CLASICO_AZUL("CLASICO_AZUL"),
    NATURAL_VERDE("NATURAL_VERDE"),
    MORADO_MODERNO("MORADO_MODERNO"),
    ARENA_CALIDO("ARENA_CALIDO")
}

data class ThemePresetDefinition(
    val preset: ThemePreset,
    val appearance: AmpaAppearance
)

val initialThemePresets: List<ThemePresetDefinition> = listOf(
    ThemePresetDefinition(
        preset = ThemePreset.CLASICO_AZUL,
        appearance = AmpaAppearance(
            primaryColor = "#1565C0",
            secondaryColor = "#2E7D32",
            backgroundColor = "#F7F9FC",
            gradientDirection = GradientDirection.TOP_TO_BOTTOM.value,
            buttonShape = ButtonShape.ROUNDED_MEDIUM.value,
            themePreset = ThemePreset.CLASICO_AZUL.value
        )
    ),
    ThemePresetDefinition(
        preset = ThemePreset.NATURAL_VERDE,
        appearance = AmpaAppearance(
            primaryColor = "#2E7D32",
            secondaryColor = "#00695C",
            backgroundColor = "#F3FAF4",
            themePreset = ThemePreset.NATURAL_VERDE.value
        )
    ),
    ThemePresetDefinition(
        preset = ThemePreset.MORADO_MODERNO,
        appearance = AmpaAppearance(
            primaryColor = "#6A1B9A",
            secondaryColor = "#8E24AA",
            backgroundColor = "#F3E5F5",
            themePreset = ThemePreset.MORADO_MODERNO.value
        )
    ),
    ThemePresetDefinition(
        preset = ThemePreset.ARENA_CALIDO,
        appearance = AmpaAppearance(
            primaryColor = "#8D6E63",
            secondaryColor = "#E65100",
            backgroundColor = "#FFF8E1",
            themePreset = ThemePreset.ARENA_CALIDO.value
        )
    )
)

fun borderThicknessFrom(value: String?): BorderThickness {
    return BorderThickness.entries.firstOrNull { it.value == value } ?: BorderThickness.MEDIUM
}

fun fontStyleFrom(value: String?): FontStyleOption {
    return FontStyleOption.entries.firstOrNull { it.value == value } ?: FontStyleOption.DEFAULT
}

fun gradientDirectionFrom(value: String?): GradientDirection {
    return GradientDirection.entries.firstOrNull { it.value == value } ?: GradientDirection.TOP_TO_BOTTOM
}

fun buttonShapeFrom(value: String?): ButtonShape {
    return ButtonShape.entries.firstOrNull { it.value == value } ?: ButtonShape.ROUNDED_MEDIUM
}

fun themePresetFrom(value: String?): ThemePreset {
    return ThemePreset.entries.firstOrNull { it.value == value } ?: ThemePreset.CLASICO_AZUL
}

fun AmpaAppearance.toMap(): Map<String, Any> {
    return mapOf(
        "primaryColor" to primaryColor,
        "secondaryColor" to secondaryColor,
        "backgroundColor" to backgroundColor,
        "borderThickness" to borderThickness,
        "fontStyle" to fontStyle,
        "logoUrl" to logoUrl,
        "schoolName" to schoolName,
        "gradientDirection" to gradientDirection,
        "buttonShape" to buttonShape,
        "themePreset" to themePreset
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
        schoolName = map["schoolName"]?.toString() ?: "",
        gradientDirection = map["gradientDirection"]?.toString() ?: GradientDirection.TOP_TO_BOTTOM.value,
        buttonShape = map["buttonShape"]?.toString() ?: ButtonShape.ROUNDED_MEDIUM.value,
        themePreset = map["themePreset"]?.toString() ?: ThemePreset.CLASICO_AZUL.value
    )
}
@Composable
fun ampaTextFieldColors(appearance: AmpaAppearance): TextFieldColors {
    val primary = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val secondary = parseHexColor(appearance.secondaryColor, Color(0xFF2E7D32))

    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primary,
        focusedLabelColor = primary,
        cursorColor = primary,
        unfocusedBorderColor = secondary.copy(alpha = 0.45f)
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
