// File: app/src/main/java/com/ampafacil/app/ui/screens/HomeScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.AmpaAppearance
import com.ampafacil.app.data.FontStyleOption
import com.ampafacil.app.data.Roles
import com.ampafacil.app.data.ampaAppearanceFromMap
import com.ampafacil.app.data.borderThicknessFrom
import com.ampafacil.app.data.fontStyleFrom
import com.ampafacil.app.data.parseHexColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAddChild: () -> Unit,
    onOpenPersonalData: () -> Unit,
    onOpenFamilyDirectory: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBoardManagement: () -> Unit,
    onOpenAnnouncements: (String) -> Unit,
    onOpenCreateAnnouncement: (String, String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var role by remember { mutableStateOf("") }
    var ampaCode by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf(AmpaAppearance()) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect

        // Aquí cargamos primero los datos básicos del usuario para saludarlo
        // y para saber a qué AMPA pertenece actualmente.
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                userName = userDoc.getString("nombre") ?: ""
                val resolvedAmpaCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (resolvedAmpaCode.isNullOrBlank()) return@addOnSuccessListener

                ampaCode = resolvedAmpaCode

                // Aquí leemos el rol guardado en members para decidir qué botones
                // debe ver el usuario dentro del menú principal.
                db.collection("ampas").document(resolvedAmpaCode)
                    .collection("members").document(uid).get()
                    .addOnSuccessListener { memberDoc ->
                        role = (memberDoc.getString("role") ?: "").uppercase()
                    }

                // Aquí cargamos la apariencia del AMPA para que Home mantenga
                // colores, estilo de letra y demás personalización.
                db.collection("ampas").document(resolvedAmpaCode).get()
                    .addOnSuccessListener { ampaDoc ->
                        val schoolName = ampaDoc.getString("schoolName") ?: ""
                        val loaded = ampaAppearanceFromMap(
                            ampaDoc.get("themeConfig") as? Map<String, Any>
                        )
                        appearance = loaded.copy(
                            schoolName = if (loaded.schoolName.isBlank()) schoolName else loaded.schoolName
                        )
                    }
            }
    }

    val isDirector =
        role == Roles.PRESIDENT ||
                role == Roles.SECRETARY ||
                role == Roles.VICEPRESIDENT

    val backgroundColor = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val primaryColor = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val secondaryColor = parseHexColor(appearance.secondaryColor, Color(0xFF2E7D32))

    val borderThickness = borderThicknessFrom(appearance.borderThickness)
    val borderWidth = borderThickness.dp.dp

    val fontStyle = fontStyleFrom(appearance.fontStyle)

    val fontFamily = when (fontStyle) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = primaryColor,
        contentColor = Color.White
    )

    val disabledButtonColors = ButtonDefaults.buttonColors(
        disabledContainerColor = Color(0xFF424242),
        disabledContentColor = Color.White
    )

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AMPAFácil",
                        color = primaryColor,
                        fontFamily = fontFamily
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = if (userName.isBlank()) "Menú principal" else "Hola, $userName",
                style = MaterialTheme.typography.titleLarge,
                color = primaryColor,
                fontFamily = fontFamily
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bienvenido/a al menú de tu AMPA.",
                color = primaryColor,
                fontFamily = fontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = borderWidth,
                        color = secondaryColor,
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(12.dp)
                ) {
                    if (ampaCode.isNotBlank()) {
                        Button(
                            onClick = { onOpenAnnouncements(ampaCode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("Comunicados", fontFamily = fontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (isDirector && ampaCode.isNotBlank()) {
                        Button(
                            onClick = { onOpenCreateAnnouncement(ampaCode, role) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("Crear comunicado", fontFamily = fontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (isDirector) {
                        Button(
                            onClick = onOpenFamilyDirectory,
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("Buscar familias", fontFamily = fontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = onOpenPersonalData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors
                    ) {
                        Text("Mis datos personales", fontFamily = fontFamily)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onAddChild,
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors
                    ) {
                        Text("Añadir hijos/as", fontFamily = fontFamily)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDirector) {
                        Button(
                            onClick = onOpenAppearance,
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("Apariencia del AMPA", fontFamily = fontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenBoardManagement,
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("Gestión de directiva", fontFamily = fontFamily)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            // Aquí cerramos sesión y volvemos al flujo inicial.
                            auth.signOut()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors
                    ) {
                        Text("Cerrar sesión", fontFamily = fontFamily)
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Futuras ampliaciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor,
                        fontFamily = fontFamily
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isDirector) {
                        DisabledFutureButton(
                            text = "Colaboradores del AMPA",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DisabledFutureButton(
                            text = "Comunidad de AMPAs",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DisabledFutureButton(
                            text = "Citas / calendario",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )
                    } else {
                        DisabledFutureButton(
                            text = "Colabora con el AMPA",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DisabledFutureButton(
                            text = "Comunidad de AMPAs",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DisabledFutureButton(
                            text = "Citas / calendario",
                            fontFamily = fontFamily,
                            colors = disabledButtonColors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisabledFutureButton(
    text: String,
    fontFamily: FontFamily,
    colors: androidx.compose.material3.ButtonColors
) {
    Button(
        onClick = {
            // De momento no hacemos nada porque esta opción queda preparada
            // para una fase futura de AMPAFácil.
        },
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        colors = colors
    ) {
        Text(text, fontFamily = fontFamily)
    }
}