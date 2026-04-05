// File: app/src/main/java/com/ampafacil/app/ui/screens/AmpaSplashScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ampafacil.app.data.AmpaAppearance
import com.ampafacil.app.data.FontStyleOption
import com.ampafacil.app.data.ampaAppearanceFromMap
import com.ampafacil.app.data.borderThicknessFrom
import com.ampafacil.app.data.fontStyleFrom
import com.ampafacil.app.data.parseHexColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

/*
 * Pantallazo del AMPA: se ve al abrir la app (solo una vez al día).
 * Dura 3 segundos y luego pasa a Home.
 */
@Composable
fun AmpaSplashScreen(
    onDone: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var appearance by remember { mutableStateOf(AmpaAppearance()) }
    var loading by remember { mutableStateOf(true) }

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
            delay(3000)
            onDone()
        }
    }

    val primary = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val background = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val borderWidth = Dp(borderThicknessFrom(appearance.borderThickness).toFloat())
    val fontFamily = when (fontStyleFrom(appearance.fontStyle)) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(borderWidth, primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (appearance.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = appearance.logoUrl,
                    contentDescription = "Logo del AMPA",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Logo pendiente", color = primary, fontFamily = fontFamily)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = appearance.schoolName.ifBlank { "AMPA" },
            style = MaterialTheme.typography.headlineSmall,
            color = primary,
            fontFamily = fontFamily
        )

        Spacer(Modifier.height(8.dp))
        Text("Bienvenido/a", color = primary, fontFamily = fontFamily)

        if (loading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = primary)
        }
    }
}