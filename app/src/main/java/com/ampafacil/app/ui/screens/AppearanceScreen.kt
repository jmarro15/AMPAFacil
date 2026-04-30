// File: app/src/main/java/com/ampafacil/app/ui/screens/AppearanceScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ampafacil.app.data.AmpaAppearance
import com.ampafacil.app.data.BorderThickness
import com.ampafacil.app.data.FontStyleOption
import com.ampafacil.app.data.Roles
import com.ampafacil.app.data.ampaAppearanceFromMap
import com.ampafacil.app.data.ampaTextFieldColors
import com.ampafacil.app.data.borderThicknessFrom
import com.ampafacil.app.data.colorToHex
import com.ampafacil.app.data.fontStyleFrom
import com.ampafacil.app.data.parseHexColor
import com.ampafacil.app.data.toMap
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    var loading by remember { mutableStateOf(true) }
    var isDirector by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var ampaCode by remember { mutableStateOf("") }
    var noActiveAmpa by remember { mutableStateOf(false) }

    var appearance by remember { mutableStateOf(AmpaAppearance()) }

    val primaryOptions = listOf(Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFFE65100))
    val secondaryOptions = listOf(Color(0xFF2E7D32), Color(0xFF0288D1), Color(0xFFC2185B), Color(0xFF6D4C41))
    val backgroundOptions = listOf(Color(0xFFF7F9FC), Color(0xFFFFF8E1), Color(0xFFF3E5F5), Color(0xFFE8F5E9))

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user == null) {
            loading = false
            return@LaunchedEffect
        }

        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val activeCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (activeCode.isNullOrBlank()) {
                    noActiveAmpa = true
                    loading = false
                    return@addOnSuccessListener
                }

                ampaCode = activeCode

                db.collection("ampas").document(activeCode)
                    .collection("members").document(uid).get()
                    .addOnSuccessListener { memberDoc ->
                        if (!memberDoc.exists()) {
                            noActiveAmpa = true
                            loading = false
                            return@addOnSuccessListener
                        }

                        val role = (memberDoc.getString("role") ?: "").uppercase()
                        isDirector = role == Roles.PRESIDENT || role == Roles.SECRETARY || role == Roles.VICEPRESIDENT

                        db.collection("ampas").document(activeCode).get()
                            .addOnSuccessListener { ampaDoc ->
                                val schoolName = ampaDoc.getString("schoolName") ?: ""
                                val loaded = ampaAppearanceFromMap(ampaDoc.get("themeConfig") as? Map<String, Any>)
                                appearance = loaded.copy(
                                    schoolName = if (loaded.schoolName.isBlank()) schoolName else loaded.schoolName
                                )
                                loading = false
                            }
                            .addOnFailureListener {
                                noActiveAmpa = true
                                loading = false
                            }
                    }
                    .addOnFailureListener {
                        noActiveAmpa = true
                        loading = false
                    }
            }
            .addOnFailureListener {
                noActiveAmpa = true
                loading = false
            }
    }

    val primaryColor = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val secondaryColor = parseHexColor(appearance.secondaryColor, Color(0xFF2E7D32))
    val backgroundColor = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val border = borderThicknessFrom(appearance.borderThickness)
    val borderWidth = (border.dp).dp
    val font = fontStyleFrom(appearance.fontStyle)

    val fontFamily = when (font) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    fun saveAppearance() {
        if (!isDirector) {
            Toast.makeText(context, "Solo la directiva puede editar la apariencia.", Toast.LENGTH_LONG).show()
            return
        }
        if (ampaCode.isBlank()) {
            Toast.makeText(context, "No hemos encontrado tu AMPA activo.", Toast.LENGTH_LONG).show()
            return
        }

        isSaving = true

        db.collection("ampas").document(ampaCode)
            .set(
                mapOf(
                    "themeConfig" to appearance.toMap(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                isSaving = false
                Toast.makeText(context, "Apariencia guardada ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error guardando apariencia: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia del AMPA") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Atrás") }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Cargando apariencia…")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(backgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (noActiveAmpa) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("No hemos encontrado un AMPA activo", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Revisa tu acceso o vuelve a introducir el código del AMPA.")
                    }
                }
                return@Column
            }

            if (!isDirector) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Acceso solo para directiva", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Tu usuario no tiene permisos de edición en esta pantalla.")
                    }
                }
            }

            /* Cabecera con logo + nombre del cole. */
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(borderWidth, primaryColor, RoundedCornerShape(14.dp)),
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
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Logo pendiente", color = primaryColor, fontFamily = fontFamily)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = appearance.schoolName.ifBlank { "Colegio sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = fontFamily
                    )
                }
            }

            OutlinedTextField(
                value = appearance.logoUrl,
                onValueChange = { appearance = appearance.copy(logoUrl = it) },
                label = { Text("URL del logo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = ampaTextFieldColors(appearance),
                enabled = isDirector && !isSaving
            )

            HorizontalDivider()

            Text("Color principal")
            ColorOptionsRow(primaryOptions, primaryColor, isDirector && !isSaving) { selected ->
                appearance = appearance.copy(primaryColor = colorToHex(selected))
            }

            Text("Color secundario")
            ColorOptionsRow(secondaryOptions, secondaryColor, isDirector && !isSaving) { selected ->
                appearance = appearance.copy(secondaryColor = colorToHex(selected))
            }

            Text("Color de fondo")
            ColorOptionsRow(backgroundOptions, backgroundColor, isDirector && !isSaving) { selected ->
                appearance = appearance.copy(backgroundColor = colorToHex(selected))
            }

            Text("Grosor de borde")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BorderThickness.entries.forEach { option ->
                    FilterChip(
                        selected = border == option,
                        onClick = { appearance = appearance.copy(borderThickness = option.value) },
                        label = { Text("${option.value} (${option.dp}dp)") },
                        enabled = isDirector && !isSaving
                    )
                }
            }

            Text("Tipografía")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontStyleOption.entries.forEach { option ->
                    FilterChip(
                        selected = font == option,
                        onClick = { appearance = appearance.copy(fontStyle = option.value) },
                        label = { Text(option.value) },
                        enabled = isDirector && !isSaving
                    )
                }
            }

            HorizontalDivider()
            Text("Vista previa", style = MaterialTheme.typography.titleMedium)

            PreviewCard(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                backgroundColor = backgroundColor,
                borderDp = border.dp,
                fontFamily = fontFamily
            )

            Button(
                onClick = { saveAppearance() },
                modifier = Modifier.fillMaxWidth(),
                colors = ampaTextFieldColors(appearance),
                enabled = isDirector && !isSaving
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar apariencia")
            }
        }
    }
}

@Composable
private fun ColorOptionsRow(
    options: List<Color>,
    selected: Color,
    enabled: Boolean,
    onSelect: (Color) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(if (isSelected) 3.dp else 1.dp, Color.Black, RoundedCornerShape(8.dp))
            ) {
                TextButton(
                    onClick = { onSelect(color) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxSize()
                ) { Text("") }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    borderDp: Int,
    fontFamily: FontFamily
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderDp.dp, secondaryColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Cabecera de ejemplo",
                color = primaryColor,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = fontFamily
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("Tarjeta de ejemplo", fontFamily = fontFamily)
                    Text("Así se verían colores, bordes y tipografía.", fontFamily = fontFamily)
                }
            }

            Button(
                onClick = {},
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Botón de ejemplo", fontFamily = fontFamily)
            }
        }
    }
}
