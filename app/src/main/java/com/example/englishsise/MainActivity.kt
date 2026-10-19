package com.example.englishsise

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

/** Pantalla 1 · Acceso inicial (RF-01): el usuario elige su tipo de ingreso. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Prototipo: aún sin inicio de sesión (RF-02); Alumno UTCJ abre directamente su inicio.
        findViewById<MaterialButton>(R.id.btnAlumnoUtcj).setOnClickListener {
            startActivity(Intent(this, InicioAlumnoUtcjActivity::class.java))
        }
        listOf(R.id.btnAlumnoExterno, R.id.btnDocente, R.id.btnAdministrador).forEach { id ->
            findViewById<MaterialButton>(id).setOnClickListener { boton ->
                val perfil = (boton as MaterialButton).text
                Toast.makeText(this, getString(R.string.proximamente, perfil), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
