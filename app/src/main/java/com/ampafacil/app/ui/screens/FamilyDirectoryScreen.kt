// File: app/src/main/java/com/ampafacil/app/ui/screens/FamilyDirectoryScreen.kt
package com.ampafacil.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.ampafacil.app.data.FamilyDirectoryDemoSeeder

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

    // Aquí filtramos manteniendo la tarjeta por familia,
    // pero obligando a que curso y clase coincidan en el mismo hijo.
    val filteredFamilies = remember(
        uiState.families,
        surnameFilter,
        nameFilter,
        phoneFilter,
        courseFilter,
        classFilter
    ) {
        uiState.families.filter { family ->
            val tutorSurnameMatches = surnameFilter.isBlank() ||
                    family.tutorSurname.contains(surnameFilter, ignoreCase = true)

            val tutorNameMatches = nameFilter.isBlank() ||
                    family.tutorName.contains(nameFilter, ignoreCase = true)

            val phoneMatches = phoneFilter.isBlank() ||
                    family.phone.contains(phoneFilter, ignoreCase = true)

            // Aquí comprobamos si hay filtros dirigidos al alumnado.
            val childFiltersActive =
                surnameFilter.isNotBlank() ||
                        nameFilter.isNotBlank() ||
                        courseFilter.isNotBlank() ||
                        classFilter.isNotBlank()

            // Aquí pedimos que el mismo hijo cumpla a la vez
            // los filtros de nombre/apellido/curso/clase que estén informados.
            val matchingChildren = family.children.filter { child ->
                val childSurnameMatches = surnameFilter.isBlank() ||
                        child.apellidos.contains(surnameFilter, ignoreCase = true)

                val childNameMatches = nameFilter.isBlank() ||
                        child.nombre.contains(nameFilter, ignoreCase = true)

                val childCourseMatches = courseFilter.isBlank() ||
                        child.curso.contains(courseFilter, ignoreCase = true)

                val childClassMatches = classFilter.isBlank() ||
                        child.clase.contains(classFilter, ignoreCase = true)

                childSurnameMatches &&
                        childNameMatches &&
                        childCourseMatches &&
                        childClassMatches
            }

            // Aquí decidimos cuándo mostrar la familia:
            // - si el tutor coincide y no hay filtros de hijos, mostramos la familia
            // - si hay filtros de hijos, exigimos que haya al menos un hijo coincidente
            val familyMatchesByTutor = tutorSurnameMatches && tutorNameMatches && phoneMatches
            val familyMatchesByChild = matchingChildren.isNotEmpty() && phoneMatches

            if (childFiltersActive) {
                familyMatchesByChild
            } else {
                familyMatchesByTutor
            }
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
                // Aquí colocamos los filtros principales del buscador.
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
                            // Aquí devolvemos el buscador a su estado inicial.
                            surnameFilter = ""
                            nameFilter = ""
                            phoneFilter = ""
                            courseFilter = ""
                            classFilter = ""
                        }
                    ) {
                        Text("Limpiar filtros")
                    }

                    Button(
                        onClick = {
                            // Aquí cargamos 20 familias demo en el AMPA activo para probar el buscador.
                            FamilyDirectoryDemoSeeder.seed20DemoFamiliesInActiveAmpa(
                                onResult = { result ->
                                    when (result) {
                                        is FamilyDirectoryDemoSeeder.SeedResult.Success -> {
                                            Toast.makeText(
                                                context,
                                                result.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            vm.loadFamilies()
                                        }

                                        is FamilyDirectoryDemoSeeder.SeedResult.Error -> {
                                            Toast.makeText(
                                                context,
                                                result.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    ) {
                        Text("Cargar familias de ejemplo")
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
                                        .clickable(enabled = phone.isNotBlank()) {
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

                                    // Aquí comprobamos si este hijo concreto cumple los filtros activos.
                                    val isMatch =
                                        (surnameFilter.isBlank() || child.apellidos.contains(surnameFilter, true)) &&
                                                (nameFilter.isBlank() || child.nombre.contains(nameFilter, true)) &&
                                                (courseFilter.isBlank() || child.curso.contains(courseFilter, true)) &&
                                                (classFilter.isBlank() || child.clase.contains(classFilter, true))

                                    Text(
                                        text = "• ${child.nombre} ${child.apellidos} · ${child.ciclo} · ${child.curso} ${child.clase}",

                                        // Si coincide, lo ponemos en negrita
                                        style = if (isMatch)
                                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        else
                                            MaterialTheme.typography.bodyMedium,

                                        // Y además le damos un color más visible
                                        color = if (isMatch)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
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