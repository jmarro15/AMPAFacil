package com.ampafacil.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ampafacil.app.R
import com.ampafacil.app.data.AmpaAppearance
import com.ampafacil.app.data.FontStyleOption
import com.ampafacil.app.data.ampaAppearanceFromMap
import com.ampafacil.app.data.borderThicknessFrom
import com.ampafacil.app.data.fontStyleFrom
import com.ampafacil.app.data.parseHexColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import com.ampafacil.app.data.BorderThickness

/*
 * Pantallazo del AMPA: se ve al abrir la app (solo una vez al día).
 * Dura unos segundos y luego pasa a Home.
 */
@Composable
fun AmpaSplashScreen(
    onDone: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var appearance by remember { mutableStateOf(AmpaAppearance()) }
    var loading by remember { mutableStateOf(true) }
    var ampaName by remember { mutableStateOf("") }
    var schoolType by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            loading = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val ampaCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (ampaCode.isNullOrBlank()) {
                    loading = false
                    return@addOnSuccessListener
                }

                db.collection("ampas").document(ampaCode).get()
                    .addOnSuccessListener { ampaDoc ->
                        val baseSchoolName = ampaDoc.getString("schoolName") ?: "AMPA"
                        ampaName = ampaDoc.getString("ampaName")?.trim().orEmpty()
                        schoolType = ampaDoc.getString("schoolType")?.trim().orEmpty()
                        schoolName = baseSchoolName.trim()
                        val loaded = ampaAppearanceFromMap(ampaDoc.get("themeConfig") as? Map<String, Any>)
                        appearance = loaded.copy(
                            schoolName = if (loaded.schoolName.isBlank()) baseSchoolName else loaded.schoolName
                        )
                        loading = false
                    }
                    .addOnFailureListener {
                        loading = false
                    }
            }
            .addOnFailureListener {
                loading = false
            }
    }

    LaunchedEffect(loading) {
        if (!loading) {
            delay(9000)
            onDone()
        }
    }

    val primary = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val background = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val borderWidth = when (borderThicknessFrom(appearance.borderThickness)) {
        BorderThickness.THIN -> 1.dp
        BorderThickness.MEDIUM -> 2.dp
        BorderThickness.THICK -> 3.dp
    }
    val fontFamily = when (fontStyleFrom(appearance.fontStyle)) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    // Preparamos los textos con fallback suave para que la splash no se rompa con datos antiguos.
    val displayAmpaName = ampaName.trim()
    val displaySchoolType = schoolType.trim()
    val displaySchoolName = schoolName.ifBlank { appearance.schoolName }.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empujamos el contenido principal hacia la mitad superior para darle protagonismo.
        Spacer(modifier = Modifier.weight(0.7f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(borderWidth, primary, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (appearance.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = appearance.logoUrl,
                        contentDescription = "Logo del AMPA",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Si aún no hay logo, mostramos un placeholder   claro.
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "AQUÍ LOGO DE SU AMPA",
                            color = primary,
                            fontFamily = fontFamily,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (displayAmpaName.isNotBlank()) {
                Text(
                    text = "AMPA $displayAmpaName",
                    style = MaterialTheme.typography.titleMedium,
                    color = primary,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
            }

            if (displaySchoolType.isNotBlank()) {
                Text(
                    text = displaySchoolType,
                    style = MaterialTheme.typography.titleSmall,
                    color = primary,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
            }

            if (displaySchoolName.isNotBlank()) {
                Text(
                    text = displaySchoolName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = primary,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center
                )
            } else if (displayAmpaName.isBlank() && displaySchoolType.isBlank()) {
                Text(
                    text = "AMPA",
                    style = MaterialTheme.typography.headlineSmall,
                    color = primary,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center
                )
            }

            if (loading) {
                Spacer(Modifier.height(22.dp))
                CircularProgressIndicator(color = primary)
            }
        }

        Spacer(modifier = Modifier.weight(1.1f))

        // En la parte inferior dejamos el logo de AMPAFácil más discreto y con margen.
        Image(
            painter = painterResource(id = R.drawable.logo_ampafacil),
            contentDescription = "Logo de AMPAFácil",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}