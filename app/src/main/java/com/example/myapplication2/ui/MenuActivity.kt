package com.example.myapplication2.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.R
import com.example.myapplication2.session.SessionManager

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!SessionManager.haySesion()) {
            cerrarSesion()
            return
        }

        val usuario = SessionManager.obtenerUsuario()
        findViewById<TextView>(R.id.txtBienvenida).text =
            if (usuario == null) {
                "Bienvenido"
            } else {
                "Bienvenido, ${usuario.nombre}" +
                        (usuario.rol?.let { "\nRol: $it" } ?: "")
            }

        abrirAlPulsar(R.id.btnMenuClientes, MainActivity::class.java)
        abrirAlPulsar(R.id.btnMenuCategorias, CategoriaActivity::class.java)
        abrirAlPulsar(R.id.btnMenuProductos, ProductoActivity::class.java)
        abrirAlPulsar(R.id.btnMenuSucursales, SucursalActivity::class.java)
        abrirAlPulsar(R.id.btnMenuUsuarios, UsuarioActivity::class.java)
        abrirAlPulsar(R.id.btnMenuVentas, VentaActivity::class.java)

        findViewById<Button>(R.id.btnMenuCerrarSesion).setOnClickListener {
            confirmarCierreSesion()
        }
    }

    private fun abrirAlPulsar(idBoton: Int, pantalla: Class<*>) {
        findViewById<Button>(idBoton).setOnClickListener {
            startActivity(Intent(this, pantalla))
        }
    }

    private fun confirmarCierreSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar la sesión actual?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        SessionManager.cerrarSesion()

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}
