package com.ampafacil.app.data

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DefaultPrimaryColor = Color(0xFF1565C0)
private val DefaultSecondaryColor = Color(0xFF2E7D32)
private val DefaultBackgroundColor = Color(0xFFF7F9FC)

private fun blendColors(start: Color, end: Color, fraction: Float): Color {
    val safeFraction = fraction.coerceIn(0f, 1f)

    return Color(
        red = start.red + (end.red - start.red) * safeFraction,
        green = start.green + (end.green - start.green) * safeFraction,
        blue = start.blue + (end.blue - start.blue) * safeFraction,
        alpha = 1f
    )
}

/*
 * Sistema global de diseño de AMPAFácil.
 *
 * MaterialTheme sigue siendo la base de Compose, pero estas funciones convierten
 * AmpaAppearance en colores seguros y reutilizables para las pantallas de un AMPA.
 * Así evitamos repetir parseos y decisiones visuales en cada pantalla.
 */

fun ampaPrimaryColor(appearance: AmpaAppearance): Color {
    // Si Firestore trae un color vacío o mal formado, mantenemos un azul legible.
    return parseHexColor(appearance.primaryColor, DefaultPrimaryColor)
}

fun ampaSecondaryColor(appearance: AmpaAppearance): Color {
    // El secundario también tiene fallback para que bordes y acciones alternativas no fallen.
    return parseHexColor(appearance.secondaryColor, DefaultSecondaryColor)
}

fun ampaBackgroundColor(appearance: AmpaAppearance): Color {
    // El fondo por defecto es claro para asegurar una base limpia y fácil de leer.
    return parseHexColor(appearance.backgroundColor, DefaultBackgroundColor)
}

fun textColorFor(backgroundColor: Color): Color {
    /*
     * Elegimos automáticamente el texto con más contraste.
     * Sobre fondos claros suele ganar el negro y sobre fondos oscuros suele ganar el blanco.
     */
    val luminance = backgroundColor.luminance()
    val contrastWithBlack = (luminance + 0.05f) / 0.05f
    val contrastWithWhite = 1.05f / (luminance + 0.05f)

    return if (contrastWithBlack >= contrastWithWhite) Color.Black else Color.White
}

fun ampaTextColor(appearance: AmpaAppearance): Color {
    // El color de texto principal se calcula desde el fondo real del AMPA.
    return textColorFor(ampaBackgroundColor(appearance))
}

@Composable
fun primaryButtonColorsFrom(appearance: AmpaAppearance): ButtonColors {
    val primary = ampaPrimaryColor(appearance)

    return ButtonDefaults.buttonColors(
        containerColor = primary,
        contentColor = textColorFor(primary),
        disabledContainerColor = primary.copy(alpha = 0.35f),
        disabledContentColor = textColorFor(primary).copy(alpha = 0.65f)
    )
}

@Composable
fun secondaryButtonColorsFrom(appearance: AmpaAppearance): ButtonColors {
    val secondary = ampaSecondaryColor(appearance)

    return ButtonDefaults.buttonColors(
        containerColor = secondary,
        contentColor = textColorFor(secondary),
        disabledContainerColor = secondary.copy(alpha = 0.35f),
        disabledContentColor = textColorFor(secondary).copy(alpha = 0.65f)
    )
}

@Composable
fun ampaCardColorsFrom(appearance: AmpaAppearance): CardColors {
    val background = ampaBackgroundColor(appearance)
    val cardBackground = if (textColorFor(background) == Color.White) {
        // En temas oscuros aclaramos ligeramente la card sin usar transparencias difíciles de leer.
        blendColors(background, Color.White, 0.12f)
    } else {
        // En temas claros usamos blanco puro para mantener una lectura cómoda.
        Color.White
    }

    return CardDefaults.cardColors(
        containerColor = cardBackground,
        contentColor = textColorFor(cardBackground),
        disabledContainerColor = cardBackground.copy(alpha = 0.45f),
        disabledContentColor = textColorFor(cardBackground).copy(alpha = 0.65f)
    )
}

fun ampaCardBorderColor(appearance: AmpaAppearance): Color {
    // El borde de las cards usa el secundario del AMPA con transparencia moderada.
    return ampaSecondaryColor(appearance).copy(alpha = 0.35f)
}
