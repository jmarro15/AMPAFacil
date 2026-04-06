// File: app/src/main/java/com/ampafacil/app/data/AmpaContextResolver.kt

package com.ampafacil.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class AmpaAppearanceResult(
    val ampaCode: String = "",
    val logoUrl: String = "",
    val schoolName: String = ""
)

fun resolveAmpaAppearanceForCurrentContext(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onResult: (AmpaAppearanceResult) -> Unit
) {
    // // Primero intentamos obtener el AMPA desde local (SharedPreferences)
    val prefs = context.getSharedPreferences("ampafacil_auth", Context.MODE_PRIVATE)
    val localAmpaCode = prefs.getString("last_ampa_code", null)

    if (!localAmpaCode.isNullOrBlank()) {
        loadAmpaFromFirestore(localAmpaCode, db, onResult)
        return
    }

    // // Si no hay en local, intentamos con usuario logado
    val user = auth.currentUser
    if (user != null) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                val ampaCode = userDoc.getString("activeAmpaCode") ?: ""

                if (ampaCode.isNotBlank()) {
                    // Guardamos en local para futuras veces
                    prefs.edit().putString("last_ampa_code", ampaCode).apply()
                    loadAmpaFromFirestore(ampaCode, db, onResult)
                } else {
                    onResult(AmpaAppearanceResult())
                }
            }
            .addOnFailureListener {
                onResult(AmpaAppearanceResult())
            }
    } else {
        onResult(AmpaAppearanceResult())
    }
}

private fun loadAmpaFromFirestore(
    ampaCode: String,
    db: FirebaseFirestore,
    onResult: (AmpaAppearanceResult) -> Unit
) {
    db.collection("ampas").document(ampaCode).get()
        .addOnSuccessListener { ampaDoc ->
            val themeConfig = ampaDoc.get("themeConfig") as? Map<String, Any>

            val logoUrl = themeConfig?.get("logoUrl")?.toString()?.trim().orEmpty()
            val schoolName = ampaDoc.getString("schoolName") ?: ""

            onResult(
                AmpaAppearanceResult(
                    ampaCode = ampaCode,
                    logoUrl = logoUrl,
                    schoolName = schoolName
                )
            )
        }
        .addOnFailureListener {
            onResult(AmpaAppearanceResult())
        }
}