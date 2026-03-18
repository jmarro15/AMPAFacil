// File: app/src/main/java/com/ampafacil/app/ui/screens/FamilyChildrenScreen.kt
package com.ampafacil.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.SchoolCatalog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyChildrenScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val user = auth.currentUser
    val uid = user?.uid

    var isLoadingAmpaCode by remember { mutableStateOf(true) }
    var ampaCode by remember { mutableStateOf<String?>(null) }

    var isSaving by remember { mutableStateOf(false) }

    // Aquí controlamos cuántos hijos se están editando.
    var childrenCount by remember { mutableStateOf(1) }

    // Aquí guardamos el estado del formulario para cada hijo.
    var children by remember { mutableStateOf(List(childrenCount) { ChildFormState() }) }

    // Aquí cargamos el AMPA activo desde users/{uid}.
    LaunchedEffect(uid) {
        if (uid.isNullOrBlank()) {
            isLoadingAmpaCode = false
            ampaCode = null
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val active = doc.getString("activeAmpaCode")?.trim()
                val fallback = doc.getString("ampaCode")?.trim()
                ampaCode = when {
                    !active.isNullOrBlank() -> active
                    !fallback.isNullOrBlank() -> fallback
                    else -> null
                }
                isLoadingAmpaCode = false
            }
            .addOnFailureListener {
                ampaCode = null
                isLoadingAmpaCode = false
            }
    }

    // Aquí aseguramos que la lista tenga el tamaño correcto cuando cambia el número de hijos.
    fun resizeChildrenList(newCount: Int) {
        val safe = newCount.coerceIn(1, 6)
        childrenCount = safe

        val current = children
        children = if (current.size == safe) {
            current
        } else if (current.size < safe) {
            current + List(safe - current.size) { ChildFormState() }
        } else {
            current.take(safe)
        }
    }

    fun updateChild(index: Int, newState: ChildFormState) {
        // Aquí forzamos recomposición reemplazando el objeto completo.
        children = children.toMutableList().also { it[index] = newState }
    }

    fun validateForm(): String? {
        if (uid.isNullOrBlank()) return "No hay sesión iniciada."
        val code = ampaCode
        if (code.isNullOrBlank() || code.length != 6) return "No tenemos un AMPA activo. Volvemos atrás y reintentamos."

        children.forEachIndexed { i, c ->
            val pos = i + 1

            if (c.nombre.trim().isBlank()) return "Falta el nombre del hijo $pos."
            if (c.apellidos.trim().isBlank()) return "Faltan los apellidos del hijo $pos."

            if (!SchoolCatalog.ciclos.contains(c.ciclo)) return "Ciclo inválido en hijo $pos."
            if (!SchoolCatalog.cursosPorCiclo(c.ciclo).contains(c.curso)) return "Curso inválido en hijo $pos."
            if (!SchoolCatalog.clases.contains(c.clase)) return "Clase inválida en hijo $pos."

            if (c.alergico && c.alergiasDetalle.trim().isBlank()) {
                return "Falta indicar a qué es alérgico el hijo $pos."
            }
        }
        return null
    }

    fun saveAll() {
        val error = validateForm()
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            return
        }
        if (isSaving) return

        val uidReal = uid!!
        val code = ampaCode!!

        isSaving = true

        val memberRef = db.collection("ampas").document(code)
            .collection("members").document(uidReal)

        // Aquí leemos el contador anterior para poder borrar hijos sobrantes si se reduce el número.
        memberRef.get()
            .addOnSuccessListener { memberDoc ->
                val oldCount = (memberDoc.getLong("childrenCount") ?: 0L).toInt()
                val now = Timestamp.now()

                // Aquí actualizamos el resumen del miembro (contador y fecha de actualización).
                val memberUpdate = hashMapOf(
                    "childrenCount" to childrenCount,
                    "childrenUpdatedAt" to now,
                    "updatedAt" to now
                )

                val batch = db.batch()
                batch.set(memberRef, memberUpdate, SetOptions.merge())

                // Aquí guardamos cada hijo en su documento estable child_1, child_2...
                children.take(childrenCount).forEachIndexed { index, child ->
                    val childId = "child_${index + 1}"
                    val childRef = memberRef.collection("children").document(childId)

                    val childData = hashMapOf<String, Any>(
                        "nombre" to child.nombre.trim(),
                        "apellidos" to child.apellidos.trim(),
                        "ciclo" to child.ciclo,
                        "curso" to child.curso,
                        "clase" to child.clase,
                        "alergico" to child.alergico,
                        "alergiasDetalle" to (if (child.alergico) child.alergiasDetalle.trim() else ""),
                        // Aquí metemos campos para filtros globales con collectionGroup.
                        "ampaCode" to code,
                        "memberUid" to uidReal,
                        "updatedAt" to now,
                        "createdAt" to now
                    )

                    batch.set(childRef, childData, SetOptions.merge())
                }

                // Aquí borramos los hijos sobrantes si antes había más y ahora menos.
                if (oldCount > childrenCount) {
                    for (k in (childrenCount + 1)..oldCount) {
                        val childRef = memberRef.collection("children").document("child_$k")
                        batch.delete(childRef)
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        isSaving = false
                        Toast.makeText(context, "Datos de hijos guardados ✅", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                    .addOnFailureListener { e ->
                        isSaving = false
                        Toast.makeText(context, "Error guardando hijos: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error leyendo miembro: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos de los hijos") },
                navigationIcon = {
                    TextButton(onClick = { onBack() }) {
                        Text("←")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onBack() },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Volver") }

                    Button(
                        onClick = { saveAll() },
                        enabled = !isSaving && !isLoadingAmpaCode,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Guardando…")
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uid == null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { Text("No hay sesión iniciada.") }
            }

            isLoadingAmpaCode -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text("Cargando AMPA activo…")
                }
            }

            ampaCode.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No encontramos un AMPA activo en el perfil.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onBack() }) { Text("Volver") }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Registramos los datos escolares con valores controlados para poder filtrar bien en el futuro.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Número de hijos (1..6)
                    ControlledDropdown(
                        label = "Número de hijos",
                        options = (1..6).map { it.toString() },
                        selected = childrenCount.toString(),
                        onSelected = { resizeChildrenList(it.toInt()) },
                        optionLabel = { it }
                    )

                    children.take(childrenCount).forEachIndexed { index, child ->
                        ChildCard(
                            index = index,
                            state = child,
                            onChange = { updateChild(index, it) }
                        )
                    }

                    Spacer(Modifier.height(70.dp))
                }
            }
        }
    }
}

/* Aquí definimos el estado de un hijo, usando Strings controlados por SchoolCatalog. */
private data class ChildFormState(
    val nombre: String = "",
    val apellidos: String = "",
    val ciclo: String = "PRIMARIA",
    val curso: String = "1P",
    val clase: String = "A",
    val alergico: Boolean = false,
    val alergiasDetalle: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildCard(
    index: Int,
    state: ChildFormState,
    onChange: (ChildFormState) -> Unit
) {
    val alergiasInfo =
        "Esta información solo es a modo informativo y contable para que en los distintos eventos organizados por el AMPA podamos tener alternativas para su hijo."

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Hijo ${index + 1}", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.nombre,
                onValueChange = { onChange(state.copy(nombre = it)) },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.apellidos,
                onValueChange = { onChange(state.copy(apellidos = it)) },
                label = { Text("Apellidos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Ciclo
            ControlledDropdown(
                label = "Ciclo",
                options = SchoolCatalog.ciclos,
                selected = state.ciclo,
                onSelected = { newCiclo ->
                    // Aquí, al cambiar ciclo, ajustamos curso a uno válido.
                    val cursos = SchoolCatalog.cursosPorCiclo(newCiclo)
                    val newCurso = if (cursos.contains(state.curso)) state.curso else (cursos.firstOrNull() ?: "")
                    onChange(state.copy(ciclo = newCiclo, curso = newCurso))
                },
                optionLabel = { it }
            )

            // Curso (depende del ciclo)
            val cursosDisponibles = SchoolCatalog.cursosPorCiclo(state.ciclo)
            ControlledDropdown(
                label = "Curso",
                options = cursosDisponibles,
                selected = state.curso,
                onSelected = { onChange(state.copy(curso = it)) },
                optionLabel = { SchoolCatalog.labelCurso(it) }
            )

            // Clase
            ControlledDropdown(
                label = "Clase",
                options = SchoolCatalog.clases,
                selected = state.clase,
                onSelected = { onChange(state.copy(clase = it)) },
                optionLabel = { it }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Switch(
                    checked = state.alergico,
                    onCheckedChange = { onChange(state.copy(alergico = it)) }
                )
                Text("¿Alérgico?")
            }

            if (state.alergico) {
                OutlinedTextField(
                    value = state.alergiasDetalle,
                    onValueChange = { onChange(state.copy(alergiasDetalle = it)) },
                    label = { Text("¿A qué es alérgico?") },
                    supportingText = { Text(alergiasInfo) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlledDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    optionLabel: (String) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(optionLabel(opt)) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}