// File: app/src/main/java/com/ampafacil/app/ui/screens/FamilyChildrenScreen.kt
package com.ampafacil.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.SchoolCatalog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private const val MIN_CHILDREN = 1
private const val MAX_CHILDREN = 6
private const val MAX_PROOF_SIZE_BYTES = 10L * 1024L * 1024L

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

    val proofMimeTypes = listOf("application/pdf", "image/jpeg", "image/png")

    var isLoadingAmpaCode by remember { mutableStateOf(true) }
    var ampaCode by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Aquí guardamos el justificante por familia para dejar preparada la futura subida a Storage.
    var paymentProofUri by remember { mutableStateOf<String?>(null) }
    var paymentProofMimeType by remember { mutableStateOf<String?>(null) }
    var paymentProofFileName by remember { mutableStateOf<String?>(null) }
    var paymentProofSizeBytes by remember { mutableStateOf<Long?>(null) }

    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val mimeType = context.contentResolver.getType(uri)
        if (mimeType !in proofMimeTypes) {
            val msg = "Formato no válido. Adjuntamos PDF, JPG o PNG."
            errorMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        val sizeBytes = readFileSizeBytes(context, uri)
        if (sizeBytes != null && sizeBytes > MAX_PROOF_SIZE_BYTES) {
            val msg = "El justificante supera 10 MB. Adjuntamos un archivo más ligero."
            errorMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Aquí seguimos aunque este dispositivo no permita persistir permisos.
        }

        paymentProofUri = uri.toString()
        paymentProofMimeType = mimeType
        paymentProofFileName = readDisplayName(context, uri)
        paymentProofSizeBytes = sizeBytes
        errorMessage = null
    }

    // Aquí controlamos cuántos hijos se están editando.
    var childrenCount by remember { mutableStateOf(MIN_CHILDREN) }

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

    // Aquí ajustamos la lista al número de hijos para no perder el orden del formulario.
    fun resizeChildrenList(newCount: Int) {
        val safe = newCount.coerceIn(MIN_CHILDREN, MAX_CHILDREN)
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
        if (code.isNullOrBlank() || code.length != 6) {
            return "No tenemos un AMPA activo válido. Volvemos atrás y reintentamos."
        }

        children.forEachIndexed { i, c ->
            val pos = i + 1

            if (c.nombre.trim().isBlank()) return "Falta el nombre del hijo $pos."
            if (c.apellidos.trim().isBlank()) return "Faltan los apellidos del hijo $pos."

            if (!SchoolCatalog.ciclos.contains(c.ciclo)) return "Ciclo inválido en hijo $pos."
            if (!SchoolCatalog.cursosPorCiclo(c.ciclo).contains(c.curso)) return "Curso inválido en hijo $pos."
            if (!SchoolCatalog.clases.contains(c.clase)) return "Clase inválida en hijo $pos."

            if (c.alergico && c.alergiasDetalle.trim().length < 3) {
                return "Necesitamos más detalle de alergias en el hijo $pos (mínimo 3 letras)."
            }
        }
        return null
    }

    fun saveAll() {
        val error = validateForm()
        if (error != null) {
            errorMessage = error
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            return
        }
        if (isSaving) return

        val uidReal = uid!!
        val code = ampaCode!!

        isSaving = true
        errorMessage = null

        val memberRef = db.collection("ampas").document(code)
            .collection("members").document(uidReal)

        // Aquí leemos los hijos actuales para no pisar createdAt por suposición de contador.
        memberRef.get()
            .addOnSuccessListener { memberDoc ->
                val oldCount = (memberDoc.getLong("childrenCount") ?: 0L).toInt().coerceAtLeast(0)
                val now = Timestamp.now()
                val childrenCollectionRef = memberRef.collection("children")

                val existingProofPath = memberDoc.getString("paymentProofStoragePath")
                val existingProofUrl = memberDoc.getString("paymentProofDownloadUrl")
                val hasStoredProof = !existingProofPath.isNullOrBlank() || !existingProofUrl.isNullOrBlank()
                val hasLocalProof = !paymentProofUri.isNullOrBlank()

                if (!hasStoredProof && !hasLocalProof) {
                    isSaving = false
                    errorMessage = "Necesitamos adjuntar justificante de pago anual (PDF, JPG o PNG)."
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Aquí actualizamos el resumen del miembro (contador y fecha de actualización).
                val memberUpdate = hashMapOf(
                    "childrenCount" to childrenCount,
                    "childrenUpdatedAt" to now,
                    "updatedAt" to now,
                    // Aquí dejamos metadatos para la futura subida a Storage.
                    "paymentProofLocalUri" to (paymentProofUri ?: ""),
                    "paymentProofMimeType" to (paymentProofMimeType ?: ""),
                    "paymentProofFileName" to (paymentProofFileName ?: ""),
                    "paymentProofSizeBytes" to (paymentProofSizeBytes ?: 0L),
                    "paymentProofPendingUpload" to hasLocalProof,
                    "paymentProofUpdatedAt" to now
                )

                childrenCollectionRef.get()
                    .addOnSuccessListener { childrenSnapshot ->
                        val existingChildIds = childrenSnapshot.documents.map { it.id }.toSet()
                        val batch = db.batch()
                        batch.set(memberRef, memberUpdate, SetOptions.merge())

                        // Aquí guardamos cada hijo en su documento estable child_1, child_2...
                        children.take(childrenCount).forEachIndexed { index, child ->
                            val childNumber = index + 1
                            val childId = "child_$childNumber"
                            val childRef = childrenCollectionRef.document(childId)

                            val nombre = child.nombre.trim()
                            val apellidos = child.apellidos.trim()
                            val alergiasDetalle = if (child.alergico) child.alergiasDetalle.trim() else ""

                            val childData = hashMapOf<String, Any>(
                                "nombre" to nombre,
                                "apellidos" to apellidos,
                                "nombreCompleto" to "$nombre $apellidos".trim(),
                                "ciclo" to child.ciclo,
                                "curso" to child.curso,
                                "cursoLabel" to SchoolCatalog.labelCurso(child.curso),
                                "clase" to child.clase,
                                "alergico" to child.alergico,
                                "tieneAlergias" to child.alergico,
                                "alergiasDetalle" to alergiasDetalle,
                                // Aquí metemos campos para filtros globales con collectionGroup.
                                "ampaCode" to code,
                                "memberUid" to uidReal,
                                "updatedAt" to now
                            )

                            // Aquí protegemos createdAt revisando existencia real del documento.
                            if (!existingChildIds.contains(childId)) {
                                childData["createdAt"] = FieldValue.serverTimestamp()
                            }

                            batch.set(childRef, childData, SetOptions.merge())
                        }

                        // Aquí borramos los hijos sobrantes si antes había más y ahora menos.
                        if (oldCount > childrenCount) {
                            for (k in (childrenCount + 1)..oldCount) {
                                val childRef = childrenCollectionRef.document("child_$k")
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
                                errorMessage = "No pudimos guardar los hijos. ${e.message ?: "Reintentamos en unos segundos."}"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        isSaving = false
                        errorMessage = "No pudimos revisar los hijos actuales. ${e.message ?: "Reintentamos en unos segundos."}"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                isSaving = false
                errorMessage = "No pudimos leer los datos actuales. ${e.message ?: "Reintentamos en unos segundos."}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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
                CenterMessage(
                    modifier = Modifier.padding(padding),
                    text = "No hay sesión iniciada."
                )
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

                    errorMessage?.let { msg ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    ControlledDropdown(
                        label = "Número de hijos",
                        options = (MIN_CHILDREN..MAX_CHILDREN).map { it.toString() },
                        selected = childrenCount.toString(),
                        onSelected = { selected ->
                            resizeChildrenList(selected.toInt())
                            errorMessage = null
                        },
                        optionLabel = { it }
                    )

                    children.take(childrenCount).forEachIndexed { index, child ->
                        ChildCard(
                            index = index,
                            state = child,
                            onChange = {
                                updateChild(index, it)
                                errorMessage = null
                            }
                        )
                    }

                    // Aquí dejamos el justificante al final de la pantalla, como acordamos.
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Justificante anual de pago", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Adjuntamos un único justificante por familia (PDF, JPG o PNG; máximo 10 MB).",
                                style = MaterialTheme.typography.bodySmall
                            )

                            OutlinedButton(
                                onClick = { proofPickerLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                                enabled = !isSaving
                            ) {
                                Text(if (paymentProofUri.isNullOrBlank()) "Adjuntar justificante" else "Cambiar justificante")
                            }

                            if (!paymentProofUri.isNullOrBlank()) {
                                Text(
                                    text = "Archivo seleccionado: ${paymentProofFileName ?: "sin nombre"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                OutlinedButton(
                                    onClick = {
                                        paymentProofUri = null
                                        paymentProofMimeType = null
                                        paymentProofFileName = null
                                        paymentProofSizeBytes = null
                                    },
                                    enabled = !isSaving
                                ) {
                                    Text("Quitar justificante")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(70.dp))
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(
    modifier: Modifier = Modifier,
    text: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text)
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

            HorizontalDivider()

            ControlledDropdown(
                label = "Ciclo",
                options = SchoolCatalog.ciclos,
                selected = state.ciclo,
                onSelected = { newCiclo ->
                    // Aquí, al cambiar ciclo, ajustamos curso a uno válido para evitar estados imposibles.
                    val cursos = SchoolCatalog.cursosPorCiclo(newCiclo)
                    val newCurso = if (cursos.contains(state.curso)) state.curso else (cursos.firstOrNull() ?: "")
                    onChange(state.copy(ciclo = newCiclo, curso = newCurso))
                },
                optionLabel = { it }
            )

            val cursosDisponibles = SchoolCatalog.cursosPorCiclo(state.ciclo)
            ControlledDropdown(
                label = "Curso",
                options = cursosDisponibles,
                selected = state.curso,
                onSelected = { onChange(state.copy(curso = it)) },
                optionLabel = { SchoolCatalog.labelCurso(it) }
            )

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
                    onCheckedChange = { checked ->
                        // Aquí limpiamos detalle si no hay alergia para guardar datos consistentes.
                        onChange(state.copy(alergico = checked, alergiasDetalle = if (checked) state.alergiasDetalle else ""))
                    }
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
    val safeSelected = options.firstOrNull { it == selected } ?: options.firstOrNull().orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionLabel(safeSelected),
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

private fun readDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) cursor.getString(index) else null
    }
}

private fun readFileSizeBytes(context: Context, uri: Uri): Long? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
    }
}