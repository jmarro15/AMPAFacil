package com.ampafacil.app.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FamilyDirectoryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    sealed class LoadResult {
        data class Success(val families: List<FamilyDirectoryItem>) : LoadResult()
        data class Error(val message: String) : LoadResult()
    }

    fun loadFamilies(onResult: (LoadResult) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(LoadResult.Error("No hay sesión iniciada."))
            return
        }

        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                // Mantenemos el mismo criterio práctico que ya usa FamilyChildrenScreen.
                val active = userDoc.getString("activeAmpaCode")?.trim()
                val fallback = userDoc.getString("ampaCode")?.trim()
                val ampaCode = when {
                    !active.isNullOrBlank() -> active
                    !fallback.isNullOrBlank() -> fallback
                    else -> null
                }

                if (ampaCode.isNullOrBlank()) {
                    onResult(LoadResult.Error("No encontramos un AMPA activo para este usuario."))
                    return@addOnSuccessListener
                }

                loadMembersWithChildren(
                    ampaCode = ampaCode,
                    onResult = onResult
                )
            }
            .addOnFailureListener { e ->
                onResult(LoadResult.Error("Error leyendo el perfil de usuario: ${e.message}"))
            }
    }

    private fun loadMembersWithChildren(
        ampaCode: String,
        onResult: (LoadResult) -> Unit
    ) {
        val membersRef = db.collection("ampas").document(ampaCode).collection("members")

        membersRef.get()
            .addOnSuccessListener { membersSnapshot ->
                if (membersSnapshot.isEmpty) {
                    onResult(LoadResult.Success(emptyList()))
                    return@addOnSuccessListener
                }

                val tasks = membersSnapshot.documents.map { memberDoc ->
                    memberDoc.reference.collection("children").get()
                        .continueWith { task ->
                            if (!task.isSuccessful) {
                                throw task.exception ?: RuntimeException("No se pudieron leer los hijos.")
                            }

                            val childrenDocs = task.result?.documents.orEmpty()

                            // En este directorio solo mostramos familias con hijos reales cargados.
                            if (childrenDocs.isEmpty()) {
                                return@continueWith null
                            }

                            val children = childrenDocs.map { childDoc ->
                                DirectoryChild(
                                    nombre = childDoc.getString("nombre").orEmpty().trim(),
                                    apellidos = childDoc.getString("apellidos").orEmpty().trim(),
                                    ciclo = childDoc.getString("ciclo").orEmpty().trim(),
                                    curso = childDoc.getString("curso").orEmpty().trim(),
                                    clase = childDoc.getString("clase").orEmpty().trim()
                                )
                            }

                            FamilyDirectoryItem(
                                memberUid = memberDoc.id,
                                tutorName = memberDoc.getString("nombre").orEmpty().trim(),
                                tutorSurname = memberDoc.getString("apellidos").orEmpty().trim(),
                                phone = memberDoc.getString("telefono").orEmpty().trim(),
                                email = memberDoc.getString("email").orEmpty().trim(),
                                // Para evitar desajustes visuales, usamos el número real de hijos cargados.
                                childrenCount = children.size,
                                children = children
                            )
                        }
                }

                if (tasks.isEmpty()) {
                    onResult(LoadResult.Success(emptyList()))
                    return@addOnSuccessListener
                }

                Tasks.whenAllSuccess<Any>(tasks)
                    .addOnSuccessListener { rawResults ->
                        val families = rawResults
                            .mapNotNull { it as? FamilyDirectoryItem }
                            .sortedWith(
                                compareBy<FamilyDirectoryItem> {
                                    it.tutorSurname.lowercase()
                                }.thenBy {
                                    it.tutorName.lowercase()
                                }
                            )

                        onResult(LoadResult.Success(families))
                    }
                    .addOnFailureListener { e ->
                        onResult(LoadResult.Error("Error leyendo familias del directorio: ${e.message}"))
                    }
            }
            .addOnFailureListener { e ->
                onResult(LoadResult.Error("Error leyendo miembros del AMPA: ${e.message}"))
            }
    }
}
