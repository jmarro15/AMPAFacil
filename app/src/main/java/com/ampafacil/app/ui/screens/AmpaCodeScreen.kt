package com.ampafacil.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmpaCodeScreen(
    onCodeAccepted: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Código AMPA") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Introducción de código del AMPA para padres/madres y junta directiva",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                label = { Text("Código AMPA (6 dígitos)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onCodeAccepted,
                enabled = code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Validar código (placeholder)")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* Paso 3: Crear AMPA */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear un AMPA nuevo (solo directiva)")
            }
        }
    }
}