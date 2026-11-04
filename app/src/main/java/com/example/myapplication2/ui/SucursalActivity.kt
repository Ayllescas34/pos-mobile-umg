package com.example.myapplication2.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication2.R
import com.example.myapplication2.model.Sucursal
import com.example.myapplication2.model.SucursalRequest
import com.example.myapplication2.network.RetrofitClient

class SucursalActivity : BaseApiActivity() {

    private lateinit var txtBuscar: EditText
    private lateinit var txtSeleccion: TextView
    private lateinit var txtNombre: EditText
    private lateinit var txtDireccion: EditText
    private lateinit var txtTelefono: EditText
    private lateinit var listaSucursales: LinearLayout

    private lateinit var btnNuevo: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnListar: Button

    // Caché en memoria de lo descargado desde la API
    private val sucursales = mutableListOf<Sucursal>()
    private var filtro = ""
    private var seleccionada: Sucursal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_sucursal, "Sucursales", "Administra los puntos de venta.")) return

        txtBuscar = findViewById(R.id.txtBuscar)
        txtSeleccion = findViewById(R.id.txtSeleccion)
        txtNombre = findViewById(R.id.txtNombre)
        txtDireccion = findViewById(R.id.txtDireccion)
        txtTelefono = findViewById(R.id.txtTelefono)
        listaSucursales = findViewById(R.id.listaSucursales)

        btnNuevo = findViewById(R.id.btnNuevo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnListar = findViewById(R.id.btnListar)

        buscarAlEscribir(txtBuscar) { buscarSucursal() }
        btnNuevo.setOnClickListener { limpiarFormulario() }
        btnGuardar.setOnClickListener { guardarSucursal() }
        btnActualizar.setOnClickListener { actualizarSucursal() }
        btnEliminar.setOnClickListener { eliminarSucursal() }
        btnListar.setOnClickListener {
            filtro = ""
            txtBuscar.text.clear()
            cargarSucursalesDesdeApi()
        }

        limpiarFormulario()
        cargarSucursalesDesdeApi()
    }

    override fun botonesAccion(): List<Button> =
        listOf(btnNuevo, btnGuardar, btnActualizar, btnEliminar, btnListar)

    override fun refrescarBotones() {
        super.refrescarBotones()
        btnGuardar.isEnabled = seleccionada == null
        btnActualizar.isEnabled = seleccionada != null
        btnEliminar.isEnabled = seleccionada != null
    }

    private fun cargarSucursalesDesdeApi(mensajeFinal: String? = null) {
        ejecutar("Cargando sucursales desde la API...", "consultar las sucursales") {
            val respuesta = RetrofitClient.api.obtenerSucursales()

            sucursales.clear()
            sucursales.addAll(respuesta.data)

            seleccionada = seleccionada?.let { actual -> sucursales.find { it.id == actual.id } }
            if (seleccionada == null) mostrarFormularioNuevo()

            mostrarListado()
            if (mensajeFinal != null) {
                mostrarMensaje(mensajeFinal)
            } else {
                mostrarInfo("Sucursales registradas: ${sucursales.size}")
            }
        }
    }

    private fun coincide(sucursal: Sucursal, texto: String): Boolean {
        return sucursal.nombre.contains(texto, ignoreCase = true) ||
                sucursal.id.toString() == texto
    }

    private fun mostrarListado() {
        val visibles = if (filtro.isBlank()) sucursales else sucursales.filter { coincide(it, filtro) }

        llenarLista(
            listaSucursales,
            visibles,
            esSeleccionado = { it.id == seleccionada?.id },
            texto = { sucursal ->
                "#${sucursal.id}  ${sucursal.nombre}\n" +
                        "Dirección: ${sucursal.direccion?.takeIf { it.isNotBlank() } ?: "—"}\n" +
                        "Teléfono: ${sucursal.telefono?.takeIf { it.isNotBlank() } ?: "—"}"
            },
            alSeleccionar = { seleccionarSucursal(it) }
        )
    }

    private fun buscarSucursal() {
        filtro = txtBuscar.text.toString().trim()
        mostrarListado()

        if (filtro.isEmpty()) {
            mostrarInfo("Mostrando todas las sucursales: ${sucursales.size}")
            return
        }

        val encontradas = sucursales.filter { coincide(it, filtro) }

        // Evita que quede seleccionado un registro que ya no se ve
        seleccionada?.let { actual ->
            if (actual !in encontradas) {
                mostrarFormularioNuevo()
                refrescarBotones()
            }
        }

        when (encontradas.size) {
            0 -> mostrarMensaje("No se encontraron sucursales con \"$filtro\".", true)
            else -> mostrarInfo("Resultados: ${encontradas.size}. Toca uno para editarlo.")
        }
    }

    private fun seleccionarSucursal(sucursal: Sucursal) {
        seleccionada = sucursal
        txtSeleccion.text = "Editando sucursal ID ${sucursal.id}"
        txtNombre.setText(sucursal.nombre)
        txtDireccion.setText(sucursal.direccion.orEmpty())
        txtTelefono.setText(sucursal.telefono.orEmpty())
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Sucursal seleccionada: ${sucursal.nombre}")
    }

    private fun mostrarFormularioNuevo() {
        seleccionada = null
        txtSeleccion.text = "Nueva sucursal"
        txtNombre.text.clear()
        txtDireccion.text.clear()
        txtTelefono.text.clear()
    }

    private fun limpiarFormulario() {
        mostrarFormularioNuevo()
        if (::listaSucursales.isInitialized) mostrarListado()
        refrescarBotones()
        mostrarInfo("Formulario listo para una nueva sucursal.")
    }

    private fun leerFormulario(): SucursalRequest? {
        val nombre = txtNombre.text.toString().trim()
        val direccion = txtDireccion.text.toString().trim()
        val telefono = txtTelefono.text.toString().trim()

        if (nombre.length < 2) {
            mostrarMensaje("Ingresa un nombre válido (mínimo 2 caracteres).", true)
            return null
        }

        if (telefono.length > 20) {
            mostrarMensaje("El teléfono no puede tener más de 20 caracteres.", true)
            return null
        }

        return SucursalRequest(
            nombre = nombre,
            direccion = direccion.ifEmpty { null },
            telefono = telefono.ifEmpty { null }
        )
    }

    private fun guardarSucursal() {
        val datos = leerFormulario() ?: return

        ejecutar("Guardando sucursal...", "guardar la sucursal") {
            verificar(RetrofitClient.api.guardarSucursal(datos), "guardar la sucursal")
            mostrarFormularioNuevo()
            cargarSucursalesDesdeApi("Sucursal guardada correctamente.")
        }
    }

    private fun actualizarSucursal() {
        val sucursal = seleccionada ?: run {
            mostrarMensaje("Selecciona una sucursal del listado.", true)
            return
        }
        val datos = leerFormulario() ?: return

        ejecutar("Actualizando sucursal...", "actualizar la sucursal") {
            verificar(
                RetrofitClient.api.actualizarSucursal(sucursal.id, datos),
                "actualizar la sucursal"
            )
            cargarSucursalesDesdeApi("Sucursal actualizada correctamente.")
        }
    }

    private fun eliminarSucursal() {
        val sucursal = seleccionada ?: run {
            mostrarMensaje("Selecciona una sucursal del listado.", true)
            return
        }

        // Los usuarios quedan sin sucursal (ON DELETE SET NULL); las ventas impiden borrarla.
        ejecutar("Revisando usuarios relacionados...", "revisar los usuarios") {
            val usuarios = RetrofitClient.api.obtenerUsuarios().data
                .count { it.idSucursal == sucursal.id }

            val aviso = if (usuarios > 0) {
                "\n\nAtención: $usuarios usuario(s) pertenecen a esta sucursal " +
                        "y quedarán sin sucursal."
            } else {
                ""
            }

            mostrarInfo("Confirma o cancela la eliminación.")
            confirmar(
                "Confirmar eliminación",
                "¿Deseas eliminar la sucursal \"${sucursal.nombre}\" (ID ${sucursal.id})?$aviso\n\n" +
                        "No se podrá eliminar si tiene ventas registradas.",
                "Eliminar"
            ) { enviarEliminacion(sucursal) }
        }
    }

    private fun enviarEliminacion(sucursal: Sucursal) {
        ejecutar("Eliminando sucursal...", "eliminar la sucursal") {
            verificar(
                RetrofitClient.api.eliminarSucursal(sucursal.id),
                "eliminar la sucursal"
            )
            mostrarFormularioNuevo()
            cargarSucursalesDesdeApi("Sucursal eliminada correctamente.")
        }
    }
}
