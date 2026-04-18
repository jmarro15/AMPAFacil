package com.ampafacil.app.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Utilidad temporal de desarrollo para sembrar familias demo en Firestore.
 *
 * IMPORTANTE:
 * - No se ejecuta automáticamente en ningún flujo de la app.
 * - Debe invocarse manualmente desde una acción de desarrollo controlada.
 */
object FamilyDirectoryDemoSeeder {

    sealed class SeedResult {
        data class Success(val message: String) : SeedResult()
        data class Error(val message: String) : SeedResult()
    }

    fun seed20DemoFamiliesInActiveAmpa(
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        onResult: (SeedResult) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(SeedResult.Error("No hay sesión iniciada."))
            return
        }

        val uid = user.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val active = userDoc.getString("activeAmpaCode")?.trim()
                val fallback = userDoc.getString("ampaCode")?.trim()
                val ampaCode = when {
                    !active.isNullOrBlank() -> active
                    !fallback.isNullOrBlank() -> fallback
                    else -> null
                }

                if (ampaCode.isNullOrBlank()) {
                    onResult(SeedResult.Error("No encontramos un AMPA activo para sembrar datos demo."))
                    return@addOnSuccessListener
                }

                seedIntoAmpa(
                    ampaCode = ampaCode,
                    db = db,
                    onResult = onResult
                )
            }
            .addOnFailureListener { e ->
                onResult(SeedResult.Error("Error leyendo usuario para datos demo: ${e.message}"))
            }
    }

    private fun seedIntoAmpa(
        ampaCode: String,
        db: FirebaseFirestore,
        onResult: (SeedResult) -> Unit
    ) {
        val now = Timestamp.now()
        val familySpecs = buildDemoFamilySpecs()

        val batch = db.batch()
        val membersRef = db.collection("ampas").document(ampaCode).collection("members")

        familySpecs.forEachIndexed { index, spec ->
            val memberUid = "demo_family_${(index + 1).toString().padStart(2, '0')}"
            val memberRef = membersRef.document(memberUid)

            val memberData = hashMapOf(
                "nombre" to spec.tutorName,
                "apellidos" to spec.tutorSurname,
                "telefono" to spec.phone,
                "email" to spec.email,
                "role" to "FAMILY",
                "childrenCount" to spec.children.size,
                "childrenSummary" to spec.children.joinToString(" | ") { child ->
                    "${child.nombre} ${child.apellidos} (${child.curso}${child.clase})"
                },
                "isDemo" to true,
                "updatedAt" to now,
                "createdAt" to now
            )
            batch.set(memberRef, memberData, SetOptions.merge())

            spec.children.forEachIndexed { childIndex, child ->
                val childId = "child_${childIndex + 1}"
                val childRef = memberRef.collection("children").document(childId)

                val childData = hashMapOf<String, Any>(
                    "nombre" to child.nombre,
                    "apellidos" to child.apellidos,
                    "ciclo" to child.ciclo,
                    "curso" to child.curso,
                    "clase" to child.clase,
                    "ampaCode" to ampaCode,
                    "memberUid" to memberUid,
                    "isDemo" to true,
                    "updatedAt" to now,
                    "createdAt" to now
                )

                batch.set(childRef, childData, SetOptions.merge())
            }
        }

        batch.commit()
            .addOnSuccessListener {
                onResult(
                    SeedResult.Success(
                        "Datos demo creados: 20 familias en el AMPA $ampaCode (con marca isDemo=true)."
                    )
                )
            }
            .addOnFailureListener { e ->
                onResult(SeedResult.Error("Error sembrando familias demo: ${e.message}"))
            }
    }

    // Lista fija y entendible para pruebas manuales del buscador.
    private fun buildDemoFamilySpecs(): List<DemoFamilySpec> {
        val tutors = listOf(
            Pair("Laura", "Martín López"),
            Pair("David", "Sánchez Ruiz"),
            Pair("Marta", "García Moreno"),
            Pair("Carlos", "Pérez Navarro"),
            Pair("Elena", "Torres Molina"),
            Pair("Javier", "Ruiz Cordero"),
            Pair("Paula", "Hernández Vega"),
            Pair("Sergio", "Núñez Rivas"),
            Pair("Cristina", "López Romero"),
            Pair("Raúl", "Díaz Prieto"),
            Pair("Irene", "Ortega Sanz"),
            Pair("Marcos", "Gil Paredes"),
            Pair("Noelia", "Vargas León"),
            Pair("Héctor", "Muñoz Vidal"),
            Pair("Ana", "Campos Nieto"),
            Pair("Rubén", "Morales Peña"),
            Pair("Lucía", "Delgado Serra"),
            Pair("Óscar", "Serrano Conde"),
            Pair("Patricia", "Romero Ibáñez"),
            Pair("Álvaro", "Prieto Mena")
        )

        // Exactamente 2 familias con 3 hijos. El resto, 1 o 2.
        val childrenDistribution = listOf(3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1)

        val childNames = listOf(
            "Hugo", "Inés", "Clara", "Álvaro", "Nora", "Lucía", "Mateo", "Vera", "Pablo", "Leire",
            "Martina", "Gael", "Izan", "Carmen", "Adrián", "Aitana", "Mario", "Julia", "Leo", "Emma",
            "Daniel", "Sofía", "Bruno", "Valeria", "Joel", "Alicia", "Enzo", "Olivia", "Diego", "Sara"
        )

        val childSurnames = listOf(
            "Martín Pérez", "Sánchez Gil", "García Torres", "Pérez Ortega", "Torres Molina", "Ruiz Vega",
            "Hernández Prieto", "Núñez Rivas", "López Romero", "Díaz Prieto", "Ortega Sanz", "Gil Paredes"
        )

        val ciclosCursos = listOf(
            Pair("INFANTIL", "3INF"),
            Pair("INFANTIL", "4INF"),
            Pair("INFANTIL", "5INF"),
            Pair("PRIMARIA", "1P"),
            Pair("PRIMARIA", "2P"),
            Pair("PRIMARIA", "3P"),
            Pair("PRIMARIA", "4P"),
            Pair("PRIMARIA", "5P"),
            Pair("PRIMARIA", "6P")
        )

        val clases = listOf("A", "B", "C")

        return tutors.mapIndexed { familyIndex, tutor ->
            val count = childrenDistribution[familyIndex]
            val children = (0 until count).map { childIndex ->
                val globalIndex = familyIndex * 2 + childIndex
                val cicloCurso = ciclosCursos[globalIndex % ciclosCursos.size]

                DemoChildSpec(
                    nombre = childNames[globalIndex % childNames.size],
                    apellidos = childSurnames[(familyIndex + childIndex) % childSurnames.size],
                    ciclo = cicloCurso.first,
                    curso = cicloCurso.second,
                    clase = clases[(familyIndex + childIndex) % clases.size]
                )
            }

            DemoFamilySpec(
                tutorName = tutor.first,
                tutorSurname = tutor.second,
                phone = "6${(10_000_000 + familyIndex).toString().padStart(8, '0')}",
                email = "demo.familia${familyIndex + 1}@ampafacil.test",
                children = children
            )
        }
    }

    private data class DemoFamilySpec(
        val tutorName: String,
        val tutorSurname: String,
        val phone: String,
        val email: String,
        val children: List<DemoChildSpec>
    )

    private data class DemoChildSpec(
        val nombre: String,
        val apellidos: String,
        val ciclo: String,
        val curso: String,
        val clase: String
    )
}
