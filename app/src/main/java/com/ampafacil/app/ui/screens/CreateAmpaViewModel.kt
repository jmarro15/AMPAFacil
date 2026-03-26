// File: app/src/main/java/com/ampafacil/app/ui/screens/CreateAmpaViewModel.kt
package com.ampafacil.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ampafacil.app.data.Roles
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.random.Random

class CreateAmpaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var isCreating by mutableStateOf(false)
        private set

    var createdAmpaCode by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun createAmpa(
        provincePrefix: String,
        provincia: String,
        localidad: String,
        schoolName: String,
        schoolCode8Chars: String,
        role: String,
        nombre: String,
        apellidos: String,
        telefono: String
    ) {
        if (isCreating) return

        val user = auth.currentUser
        if (user == null) {
            errorMessage = "No hay usuario autenticado. Hay que iniciar sesión antes de crear un AMPA."
            return
        }

        val uid = user.uid
        val email = (user.email ?: "").trim()

        isCreating = true
        errorMessage = null
        createdAmpaCode = null

        /* Aquí normalizamos el rol para no depender de lo que venga de la UI.
           Así evitamos que se cuele un "PRESI" y dejamos todo listo para reglas futuras. */
        val normalizedRole = normalizeRole(role)

        /* Aquí generamos un código de 6 dígitos y comprobamos que no exista en ampas/{code}.
           Es una comprobación sencilla para evitar colisiones. */
        generateUniqueAmpaCode(
            onOk = { ampaCode ->
                val now = Timestamp.now()

                val ampaRef = db.collection("ampas").document(ampaCode)
                val memberRef = ampaRef.collection("members").document(uid)
                val userRef = db.collection("users").document(uid)

                // Colección auxiliar para encontrar el AMPA por colegio (como ya tenemos en Firestore).
                val ampaBySchoolRef = db.collection("ampaBySchool").document(schoolCode8Chars)

                /* Aquí preparamos los datos del documento principal del AMPA. */
                val ampaData = hashMapOf(
                    "active" to true,
                    "code" to ampaCode,
                    "createdAt" to now,
                    "createdByUid" to uid,
                    "localidad" to localidad.trim(),
                    "provincia" to provincia.trim(),
                    "provincePrefix" to provincePrefix.trim(),
                    "schoolCode" to schoolCode8Chars.trim(),
                    "schoolName" to schoolName.trim(),
                    "themeConfig" to hashMapOf(
                        "primaryColor" to "#1565C0",
                        "secondaryColor" to "#2E7D32",
                        "backgroundColor" to "#F7F9FC",
                        "borderThickness" to "MEDIUM",
                        "fontStyle" to "DEFAULT",
                        "logoUrl" to "",
                        "schoolName" to schoolName.trim()
                    )
                )

                /* Aquí guardamos al creador dentro de members/{uid} con rol ya normalizado. */
                val memberData = hashMapOf(
                    "nombre" to nombre.trim(),
                    "apellidos" to apellidos.trim(),
                    "email" to email,
                    "telefono" to telefono.trim(),
                    "role" to normalizedRole,
                    "createdAt" to now
                )

                /* Aquí guardamos el perfil mínimo en users/{uid}.
                   Esto nos sirve para pantallas futuras ("Hola Sergio") sin depender de displayName. */
                val userProfileData = hashMapOf(
                    "nombre" to nombre.trim(),
                    "apellidos" to apellidos.trim(),
                    "email" to email,
                    "activeAmpaCode" to ampaCode,
                    "updatedAt" to now,

                    // Aquí mantenemos también el campo antiguo por compatibilidad si ya lo usábamos en pantallas previas.
                    "ampaCode" to ampaCode
                )

                /* Aquí dejamos un mapa rápido de colegio -> ampaCode.
                   Nos facilita validar si un cole ya tiene AMPA creado. */
                val ampaBySchoolData = hashMapOf(
                    "schoolCode" to schoolCode8Chars.trim(),
                    "ampaCode" to ampaCode,
                    "createdAt" to now
                )

                /* Aquí hacemos un batch para que todo se escriba en bloque.
                   Si algo falla, no dejamos medias tintas en la base de datos. */
                val batch = db.batch()
                batch.set(ampaRef, ampaData)
                batch.set(memberRef, memberData)
                batch.set(userRef, userProfileData, SetOptions.merge())
                batch.set(ampaBySchoolRef, ampaBySchoolData, SetOptions.merge())

                batch.commit()
                    .addOnSuccessListener {
                        createdAmpaCode = ampaCode
                        isCreating = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error al crear el AMPA: ${e.message}"
                        isCreating = false
                    }
            },
            onFail = { msg ->
                errorMessage = msg
                isCreating = false
            }
        )
    }

    private fun normalizeRole(inputRole: String): String {
        /* Aquí convertimos cualquier forma antigua del rol a nuestras constantes oficiales.
           Si llega algo raro, devolvemos FAMILY como valor seguro para no romper el flujo. */
        return when (inputRole.trim().uppercase()) {
            "PRESI", "PRESIDENT", "PRESIDENTE" -> Roles.PRESIDENT
            "SECRETARY", "SECRETARIO" -> Roles.SECRETARY
            "VICEPRESIDENT", "VICEPRESIDENTE" -> Roles.VICEPRESIDENT
            "FAMILY", "PARENT", "FAMILIA" -> Roles.FAMILY
            "TUTOR" -> Roles.TUTOR
            else -> Roles.FAMILY
        }
    }

    private fun generateUniqueAmpaCode(
        maxTries: Int = 12,
        onOk: (String) -> Unit,
        onFail: (String) -> Unit
    ) {
        /* Aquí intentamos generar un código de 6 dígitos que no exista ya.
           No es criptografía, es solo evitar choques en un MVP. */
        fun attempt(tryNum: Int) {
            if (tryNum > maxTries) {
                onFail("No se pudo generar un código único. Probamos varias veces y seguía colisionando.")
                return
            }

            val candidate = Random.nextInt(100000, 1000000).toString() // 6 dígitos

            db.collection("ampas")
                .document(candidate)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        attempt(tryNum + 1)
                    } else {
                        onOk(candidate)
                    }
                }
                .addOnFailureListener { e ->
                    onFail("Error comprobando el código en Firestore: ${e.message}")
                }
        }

        attempt(1)
    }
}