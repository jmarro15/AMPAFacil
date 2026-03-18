// File: app/src/main/java/com/ampafacil/app/ui/screens/CreateAmpaScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ampafacil.app.data.Roles

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
    val provinceMenuScroll = rememberScrollState()

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

    /* Aquí guardamos el rol como constante “buena” (Roles.*),
       y no como abreviatura tipo PRESI/VICE/SECRET. */
    var role by remember { mutableStateOf(Roles.PRESIDENT) }

    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

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
    // // Aquí calculamos si está todo completo para habilitar el botón de crear.

    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val createdCode = vm.createdAmpaCode
    if (createdCode != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("AMPA creado ✅") },
            text = {
                Column {
                    Text("Código AMPA:")
                    Spacer(Modifier.height(6.dp))
                    Text(createdCode, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Más adelante se podrá subir un PDF con instrucciones de afiliación y personalizar el tema.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(createdCode))
                        Toast.makeText(context, "Código copiado ✅", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                ) { Text("Copiar y continuar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear AMPA (Directiva)") },
                navigationIcon = { TextButton(onClick = { onBack() }) { Text("Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Datos obligatorios del AMPA", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            // ---- Provincia (solo ancla + menú dentro del ExposedDropdownMenuBox) ----
            ExposedDropdownMenuBox(
                expanded = provinceExpanded,
                onExpandedChange = { provinceExpanded = !provinceExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProvince?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provincia *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = provinceExpanded,
                    onDismissRequest = { provinceExpanded = false }
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(provinceMenuScroll)
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
            }
            // // Aquí cerramos el Box justo aquí para que el resto del formulario no quede “atrapado” dentro del desplegable.

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

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(18.dp))

            Text("Datos obligatorios de la directiva (creador)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rol *: ")
                Spacer(Modifier.width(8.dp))

                /* Aquí elegimos el rol usando Roles.* para guardar siempre el mismo valor. */
                FilterChip(
                    selected = role == Roles.PRESIDENT,
                    onClick = { role = Roles.PRESIDENT },
                    label = { Text("PRESIDENTE") }
                )

                Spacer(Modifier.width(8.dp))

                FilterChip(
                    selected = role == Roles.VICEPRESIDENT,
                    onClick = { role = Roles.VICEPRESIDENT },
                    label = { Text("VICE") }
                )

                Spacer(Modifier.width(8.dp))

                FilterChip(
                    selected = role == Roles.SECRETARY,
                    onClick = { role = Roles.SECRETARY },
                    label = { Text("SECRETARIO") }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vm.isCreating) "Creando..." else "Crear AMPA")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Los campos con * son obligatorios. El PDF y la personalización se completan después.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}