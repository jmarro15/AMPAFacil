// File: app/src/main/java/com/ampafacil/app/ui/screens/FamilyDirectoryScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ChildRecord(
    val name: String,
    val surname: String,
    val course: String,
    val groupClass: String
)

private data class FamilyRecord(
    val tutorName: String,
    val tutorSurname: String,
    val phone: String,
    val children: List<ChildRecord>
)

// Datos locales de ejemplo para esta primera versión sin Firestore.
private val sampleFamilies = listOf(
    FamilyRecord(
        tutorName = "Laura",
        tutorSurname = "Martín López",
        phone = "600123456",
        children = listOf(
            ChildRecord("Hugo", "Martín Pérez", "3º Primaria", "A"),
            ChildRecord("Inés", "Martín Pérez", "1º Primaria", "B")
        )
    ),
    FamilyRecord(
        tutorName = "David",
        tutorSurname = "Sánchez Ruiz",
        phone = "611987654",
        children = listOf(
            ChildRecord("Clara", "Sánchez Gil", "5º Primaria", "C")
        )
    ),
    FamilyRecord(
        tutorName = "Marta",
        tutorSurname = "García Moreno",
        phone = "622555321",
        children = listOf(
            ChildRecord("Álvaro", "García Torres", "2º Primaria", "A"),
            ChildRecord("Nora", "García Torres", "Infantil 5", "B")
        )
    ),
    FamilyRecord(
        tutorName = "Carlos",
        tutorSurname = "Pérez Navarro",
        phone = "633444222",
        children = listOf(
            ChildRecord("Lucía", "Pérez Ortega", "4º Primaria", "B")
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDirectoryScreen(
    onBack: () -> Unit
) {
    var surnameFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var phoneFilter by remember { mutableStateOf("") }
    var courseFilter by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("") }

    // La búsqueda se recalcula al escribir para que la pantalla responda al momento.
    val filteredFamilies = remember(
        surnameFilter,
        nameFilter,
        phoneFilter,
        courseFilter,
        classFilter
    ) {
        sampleFamilies.filter { family ->
            val surnameMatches = surnameFilter.isBlank() ||
                    family.tutorSurname.contains(surnameFilter, ignoreCase = true) ||
                    family.children.any { child -> child.surname.contains(surnameFilter, ignoreCase = true) }

            val nameMatches = nameFilter.isBlank() ||
                    family.tutorName.contains(nameFilter, ignoreCase = true) ||
                    family.children.any { child -> child.name.contains(nameFilter, ignoreCase = true) }

            val phoneMatches = phoneFilter.isBlank() ||
                    family.phone.contains(phoneFilter, ignoreCase = true)

            val courseMatches = courseFilter.isBlank() ||
                    family.children.any { child -> child.course.contains(courseFilter, ignoreCase = true) }

            val classMatches = classFilter.isBlank() ||
                    family.children.any { child -> child.groupClass.contains(classFilter, ignoreCase = true) }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Limpiar filtros")
                    }
                }
            }

            if (filteredFamilies.isEmpty()) {
                item {
                    Text(
                        text = "No se han encontrado familias con esos filtros.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
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
                                text = "Tutor/a: ${family.tutorName} ${family.tutorSurname}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Teléfono: ${family.phone}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Hijos/as:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            family.children.forEach { child ->
                                Text(
                                    text = "• ${child.name} ${child.surname} · ${child.course} ${child.groupClass}",
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