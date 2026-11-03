package com.example.myapplication2.ui

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication2.R
import com.example.myapplication2.model.Cliente
import com.example.myapplication2.model.ClienteRequest
import com.example.myapplication2.network.RetrofitClient

/** Pantalla de Clientes (se conserva el nombre MainActivity). */
class MainActivity : BaseApiActivity() {

    companion object {
        // NIT genérico de "Consumidor Final"; puede repetirse
        private const val NIT_CONSUMIDOR_FINAL = "CF"
    }

    private lateinit var txtBuscar: EditText
    private lateinit var txtSeleccion: TextView
    private lateinit var nombre: EditText
    private lateinit var nit: EditText
    private lateinit var direccion: EditText
    private lateinit var telefono: EditText
    private lateinit var correo: EditText
    private lateinit var listaClientes: LinearLayout

    private lateinit var btnBuscar: Button
    private lateinit var btnNuevo: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnListar: Button

    // Caché en memoria de lo descargado desde la API
    private val clientes = mutableListOf<Cliente>()
    private var filtro = ""
    private var seleccionado: Cliente? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_main)) return

        txtBuscar = findViewById(R.id.txtBuscar)
        txtSeleccion = findViewById(R.id.txtSeleccion)
        nombre = findViewById(R.id.txtNombre)
        nit = findViewById(R.id.txtNit)
        direccion = findViewById(R.id.txtDireccion)
        telefono = findViewById(R.id.txtTelefono)
        correo = findViewById(R.id.txtCorreo)
        listaClientes = findViewById(R.id.listaClientes)

        btnBuscar = findViewById(R.id.btnBuscar)
        btnNuevo = findViewById(R.id.btnNuevo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnListar = findViewById(R.id.btnListar)

        btnBuscar.setOnClickListener { buscarCliente() }
        btnNuevo.setOnClickListener { limpiarFormulario() }
        btnGuardar.setOnClickListener { guardarCliente() }
        btnActualizar.setOnClickListener { actualizarCliente() }
        btnEliminar.setOnClickListener { eliminarCliente() }
        btnListar.setOnClickListener {
            filtro = ""
            txtBuscar.text.clear()
            cargarClientesDesdeApi()
        }

        limpiarFormulario()
        cargarClientesDesdeApi()
    }

    override fun botonesAccion(): List<Button> =
        listOf(btnBuscar, btnNuevo, btnGuardar, btnActualizar, btnEliminar, btnListar)

    override fun refrescarBotones() {
        super.refrescarBotones()
        btnGuardar.isEnabled = seleccionado == null
        btnActualizar.isEnabled = seleccionado != null
        btnEliminar.isEnabled = seleccionado != null
    }

    private fun cargarClientesDesdeApi(mensajeFinal: String? = null) {
        ejecutar("Cargando clientes desde la API...", "consultar los clientes") {
            val respuesta = RetrofitClient.api.obtenerClientes()

            clientes.clear()
            clientes.addAll(respuesta.data)

            seleccionado = seleccionado?.let { actual -> clientes.find { it.id == actual.id } }
            if (seleccionado == null) mostrarFormularioNuevo()

            mostrarListado()
            if (mensajeFinal != null) {
                mostrarMensaje(mensajeFinal)
            } else {
                mostrarInfo("Clientes registrados: ${clientes.size}")
            }
        }
    }

    private fun coincide(cliente: Cliente, texto: String): Boolean {
        return cliente.nombre.contains(texto, ignoreCase = true) ||
                cliente.nit.orEmpty().equals(texto, ignoreCase = true) ||
                cliente.id.toString() == texto
    }

    private fun mostrarListado() {
        val visibles = if (filtro.isBlank()) clientes else clientes.filter { coincide(it, filtro) }

        llenarLista(
            listaClientes,
            visibles,
            esSeleccionado = { it.id == seleccionado?.id },
            texto = { cliente ->
                "#${cliente.id}  ${cliente.nombre}\n" +
                        "NIT: ${cliente.nit?.takeIf { it.isNotBlank() } ?: "—"}" +
                        "   Tel: ${cliente.telefono?.takeIf { it.isNotBlank() } ?: "—"}" +
                        (cliente.correo?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "") +
                        (cliente.direccion?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
            },
            alSeleccionar = { seleccionarCliente(it) }
        )
    }

    private fun buscarCliente() {
        filtro = txtBuscar.text.toString().trim()
        mostrarListado()

        if (filtro.isEmpty()) {
            mostrarInfo("Mostrando todos los clientes: ${clientes.size}")
            return
        }

        val encontrados = clientes.filter { coincide(it, filtro) }

        when (encontrados.size) {
            0 -> {
                // Evita que quede seleccionado un registro que ya no se ve
                mostrarFormularioNuevo()
                refrescarBotones()
                mostrarMensaje("No se encontraron clientes con \"$filtro\".", true)
            }
            1 -> seleccionarCliente(encontrados.first())
            else -> mostrarInfo("Se encontraron ${encontrados.size} clientes. Toca uno para editarlo.")
        }
    }

    private fun seleccionarCliente(cliente: Cliente) {
        seleccionado = cliente
        txtSeleccion.text = "Editando cliente ID ${cliente.id}"
        nombre.setText(cliente.nombre)
        nit.setText(cliente.nit.orEmpty())
        direccion.setText(cliente.direccion.orEmpty())
        telefono.setText(cliente.telefono.orEmpty())
        correo.setText(cliente.correo.orEmpty())
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Cliente seleccionado: ${cliente.nombre}")
    }

    private fun mostrarFormularioNuevo() {
        seleccionado = null
        txtSeleccion.text = "Nuevo cliente"
        nombre.text.clear()
        nit.text.clear()
        direccion.text.clear()
        telefono.text.clear()
        correo.text.clear()
    }

    private fun limpiarFormulario() {
        mostrarFormularioNuevo()
        if (::listaClientes.isInitialized) mostrarListado()
        refrescarBotones()
        mostrarInfo("Formulario listo para un nuevo cliente.")
    }

    private fun leerFormulario(idActual: Int?): ClienteRequest? {
        val nombreIngresado = nombre.text.toString().trim()
        val nitIngresado = nit.text.toString().trim().uppercase()
        val direccionIngresada = direccion.text.toString().trim()
        val telefonoIngresado = telefono.text.toString().trim()
        val correoIngresado = correo.text.toString().trim()

        if (nombreIngresado.length < 2) {
            mostrarMensaje("Ingresa un nombre válido (mínimo 2 caracteres).", true)
            return null
        }

        if (nitIngresado.isEmpty()) {
            mostrarMensaje("Ingresa el NIT o \"CF\" para consumidor final.", true)
            return null
        }

        if (nitIngresado.length > 20) {
            mostrarMensaje("El NIT no puede tener más de 20 caracteres.", true)
            return null
        }

        // El NIT identifica al cliente; solo "CF" puede repetirse
        if (nitIngresado != NIT_CONSUMIDOR_FINAL &&
            clientes.any { it.id != idActual && it.nit.equals(nitIngresado, ignoreCase = true) }
        ) {
            mostrarMensaje("Ya existe otro cliente con el NIT $nitIngresado.", true)
            return null
        }

        if (telefonoIngresado.length > 20) {
            mostrarMensaje("El teléfono no puede tener más de 20 caracteres.", true)
            return null
        }

        if (correoIngresado.isNotEmpty() &&
            !Patterns.EMAIL_ADDRESS.matcher(correoIngresado).matches()
        ) {
            mostrarMensaje("Ingresa un correo válido o déjalo vacío.", true)
            return null
        }

        return ClienteRequest(
            nit = nitIngresado,
            nombre = nombreIngresado,
            direccion = direccionIngresada.ifEmpty { null },
            email = correoIngresado.ifEmpty { null },
            telefono = telefonoIngresado.ifEmpty { null }
        )
    }

    private fun guardarCliente() {
        val datos = leerFormulario(null) ?: return

        ejecutar("Guardando cliente...", "guardar el cliente") {
            verificar(RetrofitClient.api.guardarCliente(datos), "guardar el cliente")
            mostrarFormularioNuevo()
            cargarClientesDesdeApi("Cliente guardado correctamente.")
        }
    }

    private fun actualizarCliente() {
        val cliente = seleccionado ?: run {
            mostrarMensaje("Selecciona un cliente del listado.", true)
            return
        }
        val datos = leerFormulario(cliente.id) ?: return

        ejecutar("Actualizando cliente...", "actualizar el cliente") {
            verificar(
                RetrofitClient.api.actualizarCliente(cliente.id, datos),
                "actualizar el cliente"
            )
            cargarClientesDesdeApi("Cliente actualizado correctamente.")
        }
    }

    private fun eliminarCliente() {
        val cliente = seleccionado ?: run {
            mostrarMensaje("Selecciona un cliente del listado.", true)
            return
        }

        confirmar(
            "Confirmar eliminación",
            "¿Deseas eliminar a ${cliente.nombre} (NIT ${cliente.nit ?: "—"})?\n\n" +
                    "No se podrá eliminar si tiene ventas registradas.",
            "Eliminar"
        ) {
            ejecutar("Eliminando cliente...", "eliminar el cliente") {
                verificar(
                    RetrofitClient.api.eliminarCliente(cliente.id),
                    "eliminar el cliente"
                )
                mostrarFormularioNuevo()
                cargarClientesDesdeApi("Cliente eliminado correctamente.")
            }
        }
    }
}
