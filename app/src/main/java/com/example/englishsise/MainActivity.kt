package com.example.englishsise

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.englishsise.data.Perfil
import com.example.englishsise.data.UsuarioRepository
import com.google.android.material.button.MaterialButton

/** Pantalla 1 · Acceso inicial (RF-01): el usuario elige su tipo de ingreso. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Con una sesión abierta se entra directo al inicio; ahí se valida el perfil.
        if (UsuarioRepository().uidActual != null) {
            startActivity(Intent(this, InicioAlumnoUtcjActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mapOf(
            R.id.btnAlumnoUtcj to Perfil.ALUMNO_UTCJ,
            R.id.btnAlumnoExterno to Perfil.ALUMNO_EXTERNO,
            R.id.btnDocente to Perfil.DOCENTE,
            R.id.btnAdministrador to Perfil.ADMINISTRADOR,
        ).forEach { (id, perfil) ->
            findViewById<MaterialButton>(id).setOnClickListener {
                startActivity(LoginActivity.intent(this, perfil))
            }
        }
    }
}
