package com.example.englishsise.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/** Errores de acceso con un mensaje en español que indica cómo corregirlo (RNF-13). */
sealed class ErrorAcceso(val mensaje: String) {
    object CredencialesInvalidas : ErrorAcceso("Usuario o contraseña incorrectos. Verifica tus datos e inténtalo de nuevo.")
    object DemasiadosIntentos : ErrorAcceso("Demasiados intentos fallidos. Espera unos minutos o restablece tu contraseña.")
    object SinConexion : ErrorAcceso("Sin conexión a internet. Revisa tu red e inténtalo de nuevo.")
    object SinRegistro : ErrorAcceso("Tu cuenta no tiene datos registrados en EnglishSISE. Acude al área de Inglés.")
    object CuentaInactiva : ErrorAcceso("Tu cuenta está inactiva. Acude al área de Inglés para reactivarla.")
    class PerfilDistinto(perfilReal: String) : ErrorAcceso("Esta cuenta pertenece al perfil $perfilReal. Regresa y elige ese tipo de ingreso.")
    class Desconocido(detalle: String?) : ErrorAcceso("No se pudo completar la operación. ${detalle.orEmpty()}".trim())
}

/**
 * Acceso a Firebase Auth y a la colección `usuarios` de Firestore.
 * La interfaz llama a este repositorio y nunca a Firebase directamente (RNF-17).
 */
class UsuarioRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val uidActual: String? get() = auth.currentUser?.uid

    /**
     * Inicia sesión (RF-02) y comprueba que la cuenta pertenezca al [perfil] elegido en la pantalla 1.
     * [identificador] puede ser el correo o la matrícula / número de empleado.
     */
    fun iniciarSesion(
        identificador: String,
        contrasena: String,
        perfil: Perfil,
        alTerminar: (Usuario?, ErrorAcceso?) -> Unit,
    ) {
        auth.signInWithEmailAndPassword(correoDesde(identificador), contrasena)
            .addOnSuccessListener { resultado ->
                obtenerUsuario(resultado.user!!.uid) { usuario, error ->
                    val errorFinal = error ?: when {
                        usuario!!.perfil != perfil -> ErrorAcceso.PerfilDistinto(usuario.perfil?.let(::nombrePerfil) ?: "desconocido")
                        !usuario.activo -> ErrorAcceso.CuentaInactiva
                        else -> null
                    }
                    if (errorFinal == null) {
                        alTerminar(usuario, null)
                    } else {
                        auth.signOut()
                        alTerminar(null, errorFinal)
                    }
                }
            }
            .addOnFailureListener { e -> alTerminar(null, traducir(e)) }
    }

    /** Lee el documento `usuarios/{uid}`. */
    fun obtenerUsuario(uid: String, alTerminar: (Usuario?, ErrorAcceso?) -> Unit) {
        db.collection(COLECCION_USUARIOS).document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) alTerminar(doc.aUsuario(), null) else alTerminar(null, ErrorAcceso.SinRegistro)
            }
            .addOnFailureListener { e -> alTerminar(null, traducir(e)) }
    }

    /** Envía el enlace para restablecer la contraseña al correo registrado (RF-03). */
    fun restablecerContrasena(identificador: String, alTerminar: (ErrorAcceso?) -> Unit) {
        auth.sendPasswordResetEmail(correoDesde(identificador))
            .addOnSuccessListener { alTerminar(null) }
            .addOnFailureListener { e -> alTerminar(traducir(e)) }
    }

    fun cerrarSesion() = auth.signOut()

    private fun correoDesde(identificador: String): String {
        val limpio = identificador.trim().lowercase()
        return if ("@" in limpio) limpio else "$limpio@$DOMINIO_CUENTAS"
    }

    private fun traducir(e: Exception): ErrorAcceso = when {
        e is FirebaseAuthException && e.errorCode in CODIGOS_CREDENCIALES -> ErrorAcceso.CredencialesInvalidas
        e is FirebaseAuthException && e.errorCode == "ERROR_TOO_MANY_REQUESTS" -> ErrorAcceso.DemasiadosIntentos
        e is com.google.firebase.FirebaseNetworkException -> ErrorAcceso.SinConexion
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorAcceso.SinConexion
        else -> ErrorAcceso.Desconocido(e.localizedMessage)
    }

    private fun DocumentSnapshot.aUsuario() = Usuario(
        uid = id,
        nombre = getString("nombre").orEmpty(),
        apellidos = getString("apellidos").orEmpty(),
        perfil = Perfil.desdeClave(getString("perfil")),
        matricula = getString("matricula").orEmpty(),
        correo = getString("correo").orEmpty(),
        carrera = getString("carrera").orEmpty(),
        telefono = getString("telefono").orEmpty(),
        activo = getBoolean("activo") ?: true,
    )

    companion object {
        const val COLECCION_USUARIOS = "usuarios"

        /**
         * Dominio con el que se forma el correo de acceso cuando el usuario escribe su matrícula
         * o número de empleado (p. ej. 2103456 → 2103456@utcj.edu.mx).
         */
        const val DOMINIO_CUENTAS = "utcj.edu.mx"

        private val CODIGOS_CREDENCIALES = setOf(
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_EMAIL", "ERROR_USER_DISABLED",
        )

        fun nombrePerfil(perfil: Perfil) = when (perfil) {
            Perfil.ALUMNO_UTCJ -> "Alumno UTCJ"
            Perfil.ALUMNO_EXTERNO -> "Alumno Externo"
            Perfil.DOCENTE -> "Docente"
            Perfil.ADMINISTRADOR -> "Administrador"
        }
    }
}
