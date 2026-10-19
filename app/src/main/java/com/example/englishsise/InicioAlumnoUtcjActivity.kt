package com.example.englishsise

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Pantalla 2 · Inicio del Alumno UTCJ (Tabla 4 del ERS).
 * Módulos: Inicio, Mi perfil, Mi credencial, Cursos disponibles, Pago de libros y Notificaciones.
 * Prototipo visual: los módulos todavía no están implementados.
 */
class InicioAlumnoUtcjActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio_alumno_utcj)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        mapOf(
            R.id.cardCursos to R.string.modulo_cursos_disponibles,
            R.id.cardPerfil to R.string.modulo_mi_perfil,
            R.id.cardCredencial to R.string.modulo_mi_credencial,
            R.id.cardPagoLibros to R.string.modulo_pago_libros,
            R.id.cardNotificaciones to R.string.modulo_notificaciones,
        ).forEach { (id, modulo) ->
            findViewById<View>(id).setOnClickListener { mostrarProximamente(modulo) }
        }

        findViewById<View>(R.id.btnMenu).setOnClickListener {
            mostrarProximamente(R.string.menu)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != R.id.nav_inicio) {
                Toast.makeText(this, getString(R.string.proximamente, item.title), Toast.LENGTH_SHORT).show()
            }
            false // Se mantiene "Inicio" seleccionado mientras no existan las demás pantallas.
        }
    }

    private fun mostrarProximamente(@StringRes modulo: Int) {
        Toast.makeText(this, getString(R.string.proximamente, getString(modulo)), Toast.LENGTH_SHORT).show()
    }
}
