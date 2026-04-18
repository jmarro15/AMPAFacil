// File: app/src/main/java/com/ampafacil/app/ui/screens/FamilyDirectoryScreen.kt
package com.ampafacil.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDirectoryScreen(
    onBack: () -> Unit,
    vm: FamilyDirectoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = vm.uiState

    var surnameFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var phoneFilter by remember { mutableStateOf("") }
    var courseFilter by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("") }

    // Mantenemos los filtros en memoria para conservar el comportamiento actual de la pantalla.
    val filteredFamilies = remember(
        uiState.families,
        surnameFilter,
        nameFilter,
        phoneFilter,
        courseFilter,
        classFilter
    ) {
        uiState.families.filter { family ->
            val surnameMatches = surnameFilter.isBlank() ||
                    family.tutorSurname.contains(surnameFilter, ignoreCase = true) ||
                    family.children.any { child -> child.apellidos.contains(surnameFilter, ignoreCase = true) }

            val nameMatches = nameFilter.isBlank() ||
                    family.tutorName.contains(nameFilter, ignoreCase = true) ||
                    family.children.any { child -> child.nombre.contains(nameFilter, ignoreCase = true) }

            val phoneMatches = phoneFilter.isBlank() ||
                    family.phone.contains(phoneFilter, ignoreCase = true)

            val courseMatches = courseFilter.isBlank() ||
                    family.children.any { child -> child.curso.contains(courseFilter, ignoreCase = true) }

            val classMatches = classFilter.isBlank() ||
                    family.children.any { child -> child.clase.contains(classFilter, ignoreCase = true) }

            surnameMatches && nameMatches && phoneMatches && courseMatches && classMatches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar familias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Zona de filtros sencilla para buscar por tutor o por alumnado.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = surnameFilter,
                        onValueChange = { surnameFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Apellido") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nameFilter,
                        onValueChange = { nameFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneFilter,
                        onValueChange = { phoneFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teléfono") },
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = courseFilter,
                            onValueChange = { courseFilter = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Curso") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = classFilter,
                            onValueChange = { classFilter = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Clase") },
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            // Con este botón se vuelve al estado inicial de forma rápida.
                            surnameFilter = ""
                            nameFilter = ""
                            phoneFilter = ""
                            courseFilter = ""
                            classFilter = ""
                        }
                    ) {
                        Text("Limpiar filtros")
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Cargando familias…")
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { vm.loadFamilies() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                filteredFamilies.isEmpty() -> {
                    item {
                        val noFilters = surnameFilter.isBlank() &&
                                nameFilter.isBlank() &&
                                phoneFilter.isBlank() &&
                                courseFilter.isBlank() &&
                                classFilter.isBlank()

                        val message = if (noFilters) {
                            "No hay familias con hijos para mostrar en este AMPA."
                        } else {
                            "No se han encontrado familias con esos filtros."
                        }

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    items(filteredFamilies) { family ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Tutor: ${family.tutorName} ${family.tutorSurname}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (family.email.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Email: ${family.email}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                val phone = family.phone.trim()
                                Row(
                                    modifier = Modifier
                                        // Si no hay teléfono, evitamos marcar la fila como acción disponible.
                                        .clickable(enabled = phone.isNotBlank()) {
                                            // Usamos ACTION_DIAL para evitar permisos de llamada directa.
                                            val dialIntent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:$phone")
                                            )
                                            context.startActivity(dialIntent)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Teléfono: ${family.phone.ifBlank { "No disponible" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (phone.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Phone,
                                            contentDescription = "Llamar",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Hijos asociados (${family.childrenCount}):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                family.children.forEach { child ->
                                    Text(
                                        text = "• ${child.nombre} ${child.apellidos} · ${child.ciclo} · ${child.curso} ${child.clase}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
