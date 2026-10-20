package com.example.englishsise

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.englishsise.data.Perfil
import com.example.englishsise.data.UsuarioRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Inicio de sesión del perfil elegido en la pantalla 1 (RF-02) y restablecimiento de contraseña (RF-03). */
class LoginActivity : AppCompatActivity() {

    private val repositorio = UsuarioRepository()
    private lateinit var perfil: Perfil

    private lateinit var tilUsuario: TextInputLayout
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var edtUsuario: TextInputEditText
    private lateinit var edtContrasena: TextInputEditText
    private lateinit var txtError: TextView
    private lateinit var btnIniciarSesion: MaterialButton
    private lateinit var btnOlvidaste: MaterialButton
    private lateinit var progreso: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        perfil = Perfil.valueOf(intent.getStringExtra(EXTRA_PERFIL) ?: Perfil.ALUMNO_UTCJ.name)

        tilUsuario = findViewById(R.id.tilUsuario)
        tilContrasena = findViewById(R.id.tilContrasena)
        edtUsuario = findViewById(R.id.edtUsuario)
        edtContrasena = findViewById(R.id.edtContrasena)
        txtError = findViewById(R.id.txtError)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnOlvidaste = findViewById(R.id.btnOlvidaste)
        progreso = findViewById(R.id.progreso)

        aplicarEstiloDePerfil()

        findViewById<View>(R.id.btnRegresar).setOnClickListener { finish() }
        btnIniciarSesion.setOnClickListener { iniciarSesion() }
        edtContrasena.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) iniciarSesion()
            accion == EditorInfo.IME_ACTION_DONE
        }
        btnOlvidaste.setOnClickListener { restablecerContrasena() }
    }

    private fun aplicarEstiloDePerfil() {
        val estilo = EstiloPerfil.de(perfil)
        val color = ContextCompat.getColor(this, estilo.color)
        findViewById<ImageView>(R.id.imgPerfil).apply {
            setImageResource(estilo.icono)
            backgroundTintList = ColorStateList.valueOf(color)
        }
        findViewById<TextView>(R.id.txtPerfil).text =
            getString(R.string.ingresar_como, UsuarioRepository.nombrePerfil(perfil))
        tilUsuario.hint = getString(estilo.pista)
        btnIniciarSesion.backgroundTintList = ColorStateList.valueOf(color)
        btnOlvidaste.setTextColor(color)
    }

    private fun iniciarSesion() {
        val usuario = edtUsuario.text?.toString().orEmpty().trim()
        val contrasena = edtContrasena.text?.toString().orEmpty()
        tilUsuario.error = if (usuario.isEmpty()) getString(R.string.error_campo_requerido) else null
        tilContrasena.error = if (contrasena.isEmpty()) getString(R.string.error_campo_requerido) else null
        if (usuario.isEmpty() || contrasena.isEmpty()) return

        mostrarCargando(true)
        repositorio.iniciarSesion(usuario, contrasena, perfil) { usuarioActual, error ->
            mostrarCargando(false)
            if (usuarioActual == null) {
                mostrarError(error?.mensaje)
                return@iniciarSesion
            }
            when (perfil) {
                Perfil.ALUMNO_UTCJ -> {
                    startActivity(
                        Intent(this, InicioAlumnoUtcjActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                else -> {
                    // Las pantallas de inicio de los demás perfiles aún no existen.
                    repositorio.cerrarSesion()
                    val nombre = getString(R.string.modulo_inicio) + " " + UsuarioRepository.nombrePerfil(perfil)
                    Toast.makeText(this, getString(R.string.proximamente, nombre), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restablecerContrasena() {
        val usuario = edtUsuario.text?.toString().orEmpty().trim()
        if (usuario.isEmpty()) {
            tilUsuario.error = getString(R.string.error_usuario_para_restablecer)
            return
        }
        tilUsuario.error = null
        mostrarCargando(true)
        repositorio.restablecerContrasena(usuario) { error ->
            mostrarCargando(false)
            if (error != null) {
                mostrarError(error.mensaje)
            } else {
                txtError.visibility = View.GONE
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.olvidaste_contrasena)
                    .setMessage(R.string.enlace_enviado)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        progreso.visibility = if (cargando) View.VISIBLE else View.INVISIBLE
        btnIniciarSesion.isEnabled = !cargando
        btnOlvidaste.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String?) {
        txtError.text = mensaje
        txtError.visibility = if (mensaje.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    /** Color, icono y pista del campo de usuario de cada perfil (Anexo C del ERS). */
    private enum class EstiloPerfil(
        @ColorRes val color: Int,
        @DrawableRes val icono: Int,
        @StringRes val pista: Int,
    ) {
        ALUMNO_UTCJ(R.color.perfil_alumno_utcj, R.drawable.ic_school, R.string.hint_matricula_o_correo),
        ALUMNO_EXTERNO(R.color.perfil_alumno_externo, R.drawable.ic_person, R.string.hint_correo),
        DOCENTE(R.color.perfil_docente, R.drawable.ic_docente, R.string.hint_empleado_o_correo),
        ADMINISTRADOR(R.color.perfil_administrador, R.drawable.ic_settings, R.string.hint_empleado_o_correo);

        companion object {
            fun de(perfil: Perfil) = valueOf(perfil.name)
        }
    }

    companion object {
        private const val EXTRA_PERFIL = "perfil"

        fun intent(context: Context, perfil: Perfil) =
            Intent(context, LoginActivity::class.java).putExtra(EXTRA_PERFIL, perfil.name)
    }
}
