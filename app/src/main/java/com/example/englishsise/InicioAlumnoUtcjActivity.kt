package com.example.englishsise

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.englishsise.data.ErrorAcceso
import com.example.englishsise.data.Perfil
import com.example.englishsise.data.Usuario
import com.example.englishsise.data.UsuarioRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Pantalla 2 · Inicio del Alumno UTCJ (Tabla 4 del ERS).
 * Muestra los datos del alumno con sesión abierta; los módulos todavía no están implementados.
 */
class InicioAlumnoUtcjActivity : AppCompatActivity() {

    private val repositorio = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = repositorio.uidActual
        if (uid == null) {
            irAlAcceso()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio_alumno_utcj)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        cargarUsuario(uid)

        mapOf(
            R.id.cardCursos to R.string.modulo_cursos_disponibles,
            R.id.cardPerfil to R.string.modulo_mi_perfil,
            R.id.cardCredencial to R.string.modulo_mi_credencial,
            R.id.cardPagoLibros to R.string.modulo_pago_libros,
            R.id.cardNotificaciones to R.string.modulo_notificaciones,
        ).forEach { (id, modulo) ->
            findViewById<View>(id).setOnClickListener { mostrarProximamente(modulo) }
        }

        findViewById<View>(R.id.btnMenu).setOnClickListener { boton -> mostrarMenu(boton) }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> Unit
                R.id.nav_mas -> mostrarMenu(bottomNav.findViewById(R.id.nav_mas))
                else -> Toast.makeText(this, getString(R.string.proximamente, item.title), Toast.LENGTH_SHORT).show()
            }
            false // Se mantiene "Inicio" seleccionado mientras no existan las demás pantallas.
        }
    }

    private fun cargarUsuario(uid: String) {
        repositorio.obtenerUsuario(uid) { usuario, error ->
            when {
                usuario == null -> {
                    Toast.makeText(this, error?.mensaje, Toast.LENGTH_LONG).show()
                    if (error != ErrorAcceso.SinConexion) cerrarSesion()
                }
                // La sesión guardada es de otro perfil o la cuenta fue desactivada.
                usuario.perfil != Perfil.ALUMNO_UTCJ || !usuario.activo -> cerrarSesion()
                else -> mostrarUsuario(usuario)
            }
        }
    }

    private fun mostrarUsuario(usuario: Usuario) {
        findViewById<TextView>(R.id.txtSaludo).text = getString(R.string.hola_nombre, usuario.nombreCompleto)
        val subtitulo = getString(R.string.perfil_alumno_utcj)
        findViewById<TextView>(R.id.txtSubtitulo).text =
            if (usuario.matricula.isEmpty()) subtitulo
            else "$subtitulo · ${getString(R.string.matricula_valor, usuario.matricula)}"
    }

    private fun mostrarMenu(ancla: View) {
        PopupMenu(this, ancla).apply {
            menu.add(R.string.cerrar_sesion)
            setOnMenuItemClickListener { cerrarSesion(); true }
            show()
        }
    }

    private fun cerrarSesion() {
        repositorio.cerrarSesion()
        irAlAcceso()
    }

    private fun irAlAcceso() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun mostrarProximamente(@StringRes modulo: Int) {
        Toast.makeText(this, getString(R.string.proximamente, getString(modulo)), Toast.LENGTH_SHORT).show()
    }
}
