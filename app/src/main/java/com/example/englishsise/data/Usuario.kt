package com.example.englishsise.data

/** Perfiles de acceso del sistema (Tabla 4 del ERS). [clave] es el valor guardado en Firestore. */
enum class Perfil(val clave: String) {
    ALUMNO_UTCJ("alumno_utcj"),
    ALUMNO_EXTERNO("alumno_externo"),
    DOCENTE("docente"),
    ADMINISTRADOR("administrador");

    companion object {
        fun desdeClave(clave: String?): Perfil? = entries.firstOrNull { it.clave == clave }
    }
}

/** Documento `usuarios/{uid}` de Firestore; el id del documento es el uid de Firebase Auth. */
data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val perfil: Perfil? = null,
    val matricula: String = "",
    val correo: String = "",
    val carrera: String = "",
    val telefono: String = "",
    val activo: Boolean = true,
) {
    val nombreCompleto: String get() = "$nombre $apellidos".trim()
}
