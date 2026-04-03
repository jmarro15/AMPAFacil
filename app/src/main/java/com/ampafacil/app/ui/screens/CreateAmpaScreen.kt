// File: app/src/main/java/com/ampafacil/app/ui/screens/CreateAmpaScreen.kt
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun CreateAmpaScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: CreateAmpaViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val screenScroll = rememberScrollState()

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var appearance by remember { mutableStateOf(AmpaAppearance()) }

    /* Aquí definimos provincia y prefijo para guardar el dato completo. */
    data class Province(val name: String, val prefix: String)

    val provinces = remember {
        listOf(
            Province("Araba/Álava", "01"),
            Province("Albacete", "02"),
            Province("Alicante/Alacant", "03"),
            Province("Almería", "04"),
            Province("Ávila", "05"),
            Province("Badajoz", "06"),
            Province("Illes Balears", "07"),
            Province("Barcelona", "08"),
            Province("Burgos", "09"),
            Province("Cáceres", "10"),
            Province("Cádiz", "11"),
            Province("Castellón/Castelló", "12"),
            Province("Ciudad Real", "13"),
            Province("Córdoba", "14"),
            Province("A Coruña", "15"),
            Province("Cuenca", "16"),
            Province("Girona", "17"),
            Province("Granada", "18"),
            Province("Guadalajara", "19"),
            Province("Gipuzkoa", "20"),
            Province("Huelva", "21"),
            Province("Huesca", "22"),
            Province("Jaén", "23"),
            Province("León", "24"),
            Province("Lleida", "25"),
            Province("La Rioja", "26"),
            Province("Lugo", "27"),
            Province("Madrid", "28"),
            Province("Málaga", "29"),
            Province("Murcia", "30"),
            Province("Navarra", "31"),
            Province("Ourense", "32"),
            Province("Asturias", "33"),
            Province("Palencia", "34"),
            Province("Las Palmas", "35"),
            Province("Pontevedra", "36"),
            Province("Salamanca", "37"),
            Province("Santa Cruz de Tenerife", "38"),
            Province("Cantabria", "39"),
            Province("Segovia", "40"),
            Province("Sevilla", "41"),
            Province("Soria", "42"),
            Province("Tarragona", "43"),
            Province("Teruel", "44"),
            Province("Toledo", "45"),
            Province("Valencia/València", "46"),
            Province("Valladolid", "47"),
            Province("Bizkaia", "48"),
            Province("Zamora", "49"),
            Province("Zaragoza", "50"),
            Province("Ceuta", "51"),
            Province("Melilla", "52")
        )
    }

    var provinceExpanded by remember { mutableStateOf(false) }
    var selectedProvince by remember { mutableStateOf<Province?>(null) }

    var localidad by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }

    var role by remember { mutableStateOf(Roles.PRESIDENT) }

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    /* Aquí cargamos apariencia del AMPA activo si existe en el usuario. */
    val uid = auth.currentUser?.uid
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val activeCode = userDoc.getString("activeAmpaCode")?.trim()
                    ?: userDoc.getString("ampaCode")?.trim()

                if (!activeCode.isNullOrBlank()) {
                    db.collection("ampas").document(activeCode).get()
                        .addOnSuccessListener { ampaDoc ->
                            val school = ampaDoc.getString("schoolName") ?: ""
                            val loaded = ampaAppearanceFromMap(ampaDoc.get("themeConfig") as? Map<String, Any>)
                            appearance = loaded.copy(
                                schoolName = if (loaded.schoolName.isBlank()) school else loaded.schoolName
                            )
                        }
                }
            }
    }

    val backgroundColor = parseHexColor(appearance.backgroundColor, Color(0xFFF7F9FC))
    val primaryColor = parseHexColor(appearance.primaryColor, Color(0xFF1565C0))
    val secondaryColor = parseHexColor(appearance.secondaryColor, Color(0xFF2E7D32))
    val borderThickness = borderThicknessFrom(appearance.borderThickness)
    val borderWidth = (borderThickness.dp).dp
    val fontStyle = fontStyleFrom(appearance.fontStyle)

    val fontFamily = when (fontStyle) {
        FontStyleOption.DEFAULT -> FontFamily.Default
        FontStyleOption.ROUNDED -> FontFamily.SansSerif
        FontStyleOption.SERIF -> FontFamily.Serif
    }

    val buttonColors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White)

    val isFormValid = remember(
        selectedProvince, localidad, schoolName, schoolCode, role, nombre, apellidos, telefono
    ) {
        val okProvince = selectedProvince != null
        val okSchoolCode = schoolCode.length == 8 && schoolCode.all { it.isLetterOrDigit() }
        okProvince &&
                localidad.isNotBlank() &&
                schoolName.isNotBlank() &&
                okSchoolCode &&
                role.isNotBlank() &&
                nombre.isNotBlank() &&
                apellidos.isNotBlank() &&
                telefono.isNotBlank()
    }

    /* Aquí mostramos los errores del ViewModel de forma simple. */
    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val createdCode = vm.createdAmpaCode
    if (createdCode != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("AMPA creado ✅", color = primaryColor, fontFamily = fontFamily) },
            text = {
                Column {
                    Text("Código AMPA:", fontFamily = fontFamily)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = createdCode,
                        style = MaterialTheme.typography.headlineMedium,
                        color = primaryColor,
                        fontFamily = fontFamily
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Más adelante se podrá subir un PDF con instrucciones y personalizar aún más la app.",
                        fontFamily = fontFamily
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(createdCode))
                        Toast.makeText(context, "Código copiado ✅", Toast.LENGTH_SHORT).show()
                        onDone()
                    },
                    colors = buttonColors
                ) {
                    Text("Copiar y continuar", fontFamily = fontFamily)
                }
            },
            containerColor = backgroundColor,
            tonalElevation = 2.dp
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crear AMPA (Directiva)",
                        color = primaryColor,
                        fontFamily = fontFamily
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Atrás", color = primaryColor, fontFamily = fontFamily)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .background(backgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Datos obligatorios del AMPA",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontFamily = fontFamily
            )
            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(borderWidth, secondaryColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(12.dp)
                ) {
                    /* Aquí abrimos un menú simple de provincias para evitar problemas de compatibilidad. */
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { provinceExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedProvince?.let { "${it.name} (${it.prefix})" } ?: "Seleccionar provincia *",
                                color = primaryColor,
                                fontFamily = fontFamily
                            )
                        }

                        DropdownMenu(
                            expanded = provinceExpanded,
                            onDismissRequest = { provinceExpanded = false }
                        ) {
                            provinces.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.prefix})") },
                                    onClick = {
                                        selectedProvince = p
                                        provinceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = selectedProvince?.prefix ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Prefijo provincia (auto)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = localidad,
                        onValueChange = { localidad = it },
                        label = { Text("Localidad *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("Nombre del colegio *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = schoolCode,
                        onValueChange = { new ->
                            schoolCode = new.filter { it.isLetterOrDigit() }.uppercase().take(8)
                        },
                        label = { Text("Código del centro (8 alfanuméricos) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = secondaryColor)
            Spacer(Modifier.height(18.dp))

            Text(
                text = "Datos obligatorios de la directiva (creador)",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontFamily = fontFamily
            )
            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(borderWidth, secondaryColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rol *:", color = primaryColor, fontFamily = fontFamily)
                        Spacer(Modifier.size(8.dp))

                        FilterChip(
                            selected = role == Roles.PRESIDENT,
                            onClick = { role = Roles.PRESIDENT },
                            label = { Text("PRESIDENTE", fontFamily = fontFamily) }
                        )

                        Spacer(Modifier.size(8.dp))

                        FilterChip(
                            selected = role == Roles.VICEPRESIDENT,
                            onClick = { role = Roles.VICEPRESIDENT },
                            label = { Text("VICE", fontFamily = fontFamily) }
                        )

                        Spacer(Modifier.size(8.dp))

                        FilterChip(
                            selected = role == Roles.SECRETARY,
                            onClick = { role = Roles.SECRETARY },
                            label = { Text("SECRETARIO", fontFamily = fontFamily) }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apellidos,
                        onValueChange = { apellidos = it },
                        label = { Text("Apellidos *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(15) },
                        label = { Text("Teléfono *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val p = selectedProvince!!
                    vm.createAmpa(
                        provincePrefix = p.prefix,
                        provincia = p.name,
                        localidad = localidad,
                        schoolName = schoolName,
                        schoolCode8Chars = schoolCode,
                        role = role,
                        nombre = nombre,
                        apellidos = apellidos,
                        telefono = telefono
                    )
                },
                enabled = isFormValid && !vm.isCreating,
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors
            ) {
                Text(if (vm.isCreating) "Creando..." else "Crear AMPA", fontFamily = fontFamily)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Los campos con * son obligatorios. El PDF y la personalización se completan después.",
                style = MaterialTheme.typography.bodySmall,
                color = primaryColor,
                fontFamily = fontFamily
            )
        }
    }
}