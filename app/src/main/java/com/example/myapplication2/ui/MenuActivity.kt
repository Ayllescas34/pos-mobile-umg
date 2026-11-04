package com.example.myapplication2.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.myapplication2.R
import com.example.myapplication2.network.RetrofitClient
import com.example.myapplication2.session.SessionManager

class MenuActivity : BaseApiActivity() {

    companion object {
        private const val STOCK_BAJO = 10
    }

    private lateinit var lblVentas: TextView
    private lateinit var lblIngresos: TextView
    private lateinit var lblProductos: TextView
    private lateinit var lblProductosAyuda: TextView
    private lateinit var lblClientes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_menu)) return

        lblVentas = findViewById(R.id.lblVentas)
        lblIngresos = findViewById(R.id.lblIngresos)
        lblProductos = findViewById(R.id.lblProductos)
        lblProductosAyuda = findViewById(R.id.lblProductosAyuda)
        lblClientes = findViewById(R.id.lblClientes)

        val usuario = SessionManager.obtenerUsuario()

        findViewById<TextView>(R.id.txtBienvenida).text =
            if (usuario == null) "Bienvenido" else "Hola, ${usuario.nombre}"

        findViewById<TextView>(R.id.txtUsuarioMenu).text = usuario?.rol.orEmpty()

        abrirAlPulsar(R.id.btnMenuClientes, MainActivity::class.java)
        abrirAlPulsar(R.id.btnMenuCategorias, CategoriaActivity::class.java)
        abrirAlPulsar(R.id.btnMenuProductos, ProductoActivity::class.java)
        abrirAlPulsar(R.id.btnMenuSucursales, SucursalActivity::class.java)
        abrirAlPulsar(R.id.btnMenuUsuarios, UsuarioActivity::class.java)
        abrirAlPulsar(R.id.btnMenuVentas, VentaActivity::class.java)

        findViewById<View>(R.id.btnMenuCerrarSesion).setOnClickListener {
            confirmarCierreSesion()
        }
    }

    // El resumen se actualiza cada vez que se regresa al inicio
    override fun onResume() {
        super.onResume()
        if (SessionManager.haySesion()) cargarResumen()
    }

    override fun botonesAccion(): List<Button> = emptyList()

    private fun cargarResumen() {
        ejecutar("Actualizando resumen...", "consultar el resumen") {
            val api = RetrofitClient.api
            val ventas = api.obtenerVentas().data
            val productos = api.obtenerProductos().data
            val clientes = api.obtenerClientes().data
            val stockBajo = productos.count { it.stock <= STOCK_BAJO }

            lblVentas.text = ventas.size.toString()
            lblIngresos.text = dinero(ventas.sumOf { it.total })
            lblProductos.text = productos.size.toString()
            lblClientes.text = clientes.size.toString()
            lblProductosAyuda.text =
                if (stockBajo == 0) "stock en buen nivel" else "$stockBajo con poco stock"

            mostrarInfo("Resumen actualizado.")
        }
    }

    private fun abrirAlPulsar(idBoton: Int, pantalla: Class<*>) {
        findViewById<View>(idBoton).setOnClickListener {
            startActivity(Intent(this, pantalla))
        }
    }

    private fun confirmarCierreSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar la sesión actual?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                irALogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
