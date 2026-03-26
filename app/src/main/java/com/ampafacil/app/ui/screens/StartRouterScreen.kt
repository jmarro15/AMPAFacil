// File: app/src/main/java/com/ampafacil/app/ui/screens/StartRouterScreen.kt
package com.ampafacil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ampafacil.app.data.Roles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

/*
 * Esta pantalla decide adónde ir al arrancar.
 * Lo hace leyendo sesión, AMPA activa, rol y estado de hijos.
 */
@Composable
fun StartRouterScreen(
    onGoAuth: () -> Unit,
    onGoAmpaCode: () -> Unit,
    onGoFamilyChildren: () -> Unit,
    onGoHome: () -> Unit,
    onGoAmpaSplash: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val routed = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (routed.value) return@LaunchedEffect

        val user = auth.currentUser
        if (user == null) {
            routed.value = true
            onGoAuth()
            return@LaunchedEffect
        }

        user.reload().addOnCompleteListener {
            val refreshed = auth.currentUser
            if (refreshed == null || !refreshed.isEmailVerified) {
                routed.value = true
                onGoAuth()
                return@addOnCompleteListener
            }

            val uid = refreshed.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val activeAmpaCode = userDoc.getString("activeAmpaCode")?.trim()
                        ?: userDoc.getString("ampaCode")?.trim()

                    if (activeAmpaCode.isNullOrBlank()) {
                        routed.value = true
                        onGoAmpaCode()
                        return@addOnSuccessListener
                    }

                    db.collection("ampas").document(activeAmpaCode)
                        .collection("members").document(uid).get()
                        .addOnSuccessListener { memberDoc ->
                            if (!memberDoc.exists()) {
                                routed.value = true
                                onGoAmpaCode()
                                return@addOnSuccessListener
                            }

                            val role = (memberDoc.getString("role") ?: "").uppercase()
                            val childrenCount = (memberDoc.getLong("childrenCount") ?: 0L).toInt()

                            val isFamilyLike = role == Roles.FAMILY || role == Roles.TUTOR
                            if (isFamilyLike && childrenCount <= 0) {
                                routed.value = true
                                onGoFamilyChildren()
                                return@addOnSuccessListener
                            }

                            val prefs = context.getSharedPreferences("ampafacil_start", 0)
                            val today = LocalDate.now().toString()
                            val key = "splash_day_${uid}_$activeAmpaCode"
                            val lastDay = prefs.getString(key, null)

                            if (lastDay == today) {
                                routed.value = true
                                onGoHome()
                            } else {
                                prefs.edit().putString(key, today).apply()
                                routed.value = true
                                onGoAmpaSplash()
                            }
                        }
                        .addOnFailureListener {
                            routed.value = true
                            onGoAmpaCode()
                        }
                }
                .addOnFailureListener {
                    routed.value = true
                    onGoAmpaCode()
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Preparando tu AMPA…", style = MaterialTheme.typography.bodyMedium)
    }
}
