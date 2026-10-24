package com.example.myapplication2.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.R
import com.example.myapplication2.model.Cliente
import androidx.lifecycle.lifecycleScope
import com.example.myapplication2.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.example.myapplication2.model.ClienteRequest
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import com.example.myapplication2.session.SessionManager

class MainActivity : AppCompatActivity() {

    // Propiededas
    private lateinit var nombre: EditText
    private lateinit var nit: EditText
    private lateinit var direccion: EditText
    private lateinit var telefono: EditText
    private lateinit var correo: EditText
    private lateinit var resultado: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnListar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnCerrarSesion: Button

    private val clientes = mutableListOf<Cliente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /*CRUD*/
        nombre = findViewById(R.id.txtNombre)
        nit = findViewById(R.id.txtNit)
        direccion = findViewById(R.id.txtDireccion)
        telefono = findViewById(R.id.txtTelefono)
        correo = findViewById(R.id.txtCorreo)
        resultado = findViewById(R.id.txtResultado)

        btnGuardar = findViewById(R.id.btnGuardar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnLimpiar = findViewById(R.id.btnLimpiar)
        btnListar = findViewById(R.id.btnListar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        //inicializaClientes()
        /*resultado.movementMethod =
            android.text.method.ScrollingMovementMethod()*/
        cargarClientesDesdeApi()

        btnGuardar.setOnClickListener {
            guardarCliente()
        }
        btnBuscar.setOnClickListener {
            buscarCliente()
        }
        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }
        btnListar.setOnClickListener {
            listarClientes()
        }
        btnActualizar.setOnClickListener {
            actualizarCliente()
        }
        btnEliminar.setOnClickListener {
            eliminarCliente()
        }
        btnCerrarSesion.setOnClickListener {
            confirmarCierreSesion()
        }
    }

    /* Funciones para CRUD*/
    private fun inicializaClientes() {
        if (clientes.isNotEmpty()) return

        clientes.add(
            Cliente(
                id = 1,
                nombre = "Ana López",
                nit = "1234567-8",
                telefono = "55551234",
                correo = "ana@ejemplo.com"
            )
        )

        clientes.add(
            Cliente(
                id = 2,
                nombre = "Carlos Pérez",
                nit = "9876543-2",
                telefono = "55556789",
                correo = "carlos@ejemplo.com"
            )
        )

        clientes.add(
            Cliente(
                id = 3,
                nombre = "María García",
                nit = "4567891-0",
                telefono = "55559876",
                correo = "maria@ejemplo.com"
            )
        )

    }

    private fun cargarClientesDesdeApi(){

        resultado.text = "Cargando clientes desde la API..."

        lifecycleScope.launch{
            try{
                val respuesta = RetrofitClient.api.obtenerClientes()

                clientes.clear()
                clientes.addAll(respuesta.data)


                listarClientes()

            } catch (error: HttpException) {
                resultado.text =
                    when (error.code()) {
                        401 -> "La sesión no es válida o expiró."
                        403 -> "No tienes permiso para consultar clientes."
                        else -> "Error del servidor: ${error.code()}"
                    }

            } catch (error: IOException) {
                resultado.text =
                    "No se pudo conectar con la API.\n" +
                            "Verifica que el servidor esté encendido."

            } catch (error: Exception) {
                resultado.text =
                    "Ocurrió un error al cargar los clientes:\n${error.message}"
            }

        }
    }

    /*private fun guardarCliente() {
        val nombreIngresado = nombre.text.toString().trim()
        val nitIngresado = nit.text.toString().trim()
        val telefonoIngresado = telefono.text.toString().trim()
        val correoIngresado = correo.text.toString().trim()

        if (nombreIngresado.length < 2) {
            resultado.text = "Ingresa un nombre valido."
        } else if (nitIngresado.isEmpty()) {
            resultado.text = "Ingresa el Nit."
        } else if (telefonoIngresado.isEmpty()) {
            resultado.text = "Ingresa el telefono."
        } else if (!correoIngresado.contains("@")) {
            resultado.text = "Ingresa un email valido."
        } else {
            val cliente = Cliente(
                id = siguienteId,
                nombre = nombreIngresado,
                nit = nitIngresado,
                telefono = telefonoIngresado,
                correo = correoIngresado
            )
            clientes.add(cliente)
            siguienteId++
            resultado.text =
                "Cliente capturado correctamente\n\n" +
                        "Nombre: $nombreIngresado\n" +
                        "NIT: $nitIngresado\n" +
                        "Teléfono: $telefonoIngresado\n" +
                        "Correo: $correoIngresado"
        }
    }*/

    private fun guardarCliente() {
        val nombreIngresado = nombre.text.toString().trim()
        val nitIngresado = nit.text.toString().trim()
        val direccionIngresada = direccion.text.toString().trim()
        val telefonoIngresado = telefono.text.toString().trim()
        val correoIngresado = correo.text.toString().trim()

        if (nombreIngresado.length < 2) {
            resultado.text = "Ingresa un nombre válido."
            return
        }

        if (nitIngresado.isEmpty()) {
            resultado.text = "Ingresa el NIT."
            return
        }

        if (direccionIngresada.isEmpty()) {
            resultado.text = "Ingresa la dirección."
            return
        }

        if (telefonoIngresado.isEmpty()) {
            resultado.text = "Ingresa el teléfono."
            return
        }

        if (!correoIngresado.contains("@")) {
            resultado.text = "Ingresa un correo válido."
            return
        }

        val nuevoCliente = ClienteRequest(
            nit = nitIngresado,
            nombre = nombreIngresado,
            direccion = direccionIngresada,
            email = correoIngresado,
            telefono = telefonoIngresado
        )

        btnGuardar.isEnabled = false
        resultado.text = "Guardando cliente..."

        lifecycleScope.launch {
            try {
                val respuesta =
                    RetrofitClient.api.guardarCliente(nuevoCliente)

                if (respuesta.isSuccessful) {
                    limpiarFormulario()
                    resultado.text = "Cliente guardado correctamente."

                    cargarClientesDesdeApi()
                } else {
                    val mensajeServidor =
                        respuesta.errorBody()?.string()

                    resultado.text =
                        "No se pudo guardar.\n" +
                                "Código: ${respuesta.code()}\n" +
                                (mensajeServidor ?: "")
                }

            } catch (error: IOException) {
                resultado.text =
                    "No se pudo conectar con la API."

            } catch (error: Exception) {
                resultado.text =
                    "Ocurrió un error al guardar:\n${error.message}"

            } finally {
                btnGuardar.isEnabled = true
            }
        }
    }

    private fun limpiarFormulario() {
        nombre.text.clear()
        nit.text.clear()
        direccion.text.clear()
        telefono.text.clear()
        correo.text.clear()
        resultado.text = "Aquí aparecerán los resultados"
    }
    /*private fun buscarCliente() {
        val nitBuscado = nit.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text = "Ingresa el NIT que deseas buscar."
            return
        }

        val clienteEncontrado = clientes.find {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (clienteEncontrado != null) {
            resultado.text =
                "Cliente encontrado\n\n" +
                        "ID: ${clienteEncontrado.id}\n" +
                        "Nombre: ${clienteEncontrado.nombre}\n" +
                        "NIT: ${clienteEncontrado.nit}\n" +
                        "Teléfono: ${clienteEncontrado.telefono}\n" +
                        "Correo: ${clienteEncontrado.correo}"
        } else {
            resultado.text = "No se encontró un cliente con ese NIT."
        }
    }*/

    private fun buscarCliente() {
        val nitBuscado = nit.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text =
                "Ingresa el NIT que deseas buscar."
            return
        }

        val clienteEncontrado = clientes.find {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (clienteEncontrado == null) {
            resultado.text =
                "No se encontró un cliente con ese NIT."
            return
        }

        nombre.setText(clienteEncontrado.nombre)
        nit.setText(clienteEncontrado.nit)
        direccion.setText(clienteEncontrado.direccion)
        telefono.setText(clienteEncontrado.telefono)
        correo.setText(clienteEncontrado.correo)

        resultado.text =
            "Cliente encontrado\n\n" +
                    "ID: ${clienteEncontrado.id}\n" +
                    "Nombre: ${clienteEncontrado.nombre}\n" +
                    "NIT: ${clienteEncontrado.nit}\n" +
                    "Dirección: ${clienteEncontrado.direccion}\n" +
                    "Teléfono: ${clienteEncontrado.telefono}\n" +
                    "Correo: ${clienteEncontrado.correo}"
    }

    private fun listarClientes() {
        if (clientes.isEmpty()) {
            resultado.text = "No hay clientes registrados."
            return
        }

        val listado = clientes.joinToString(
            separator = "\n\n"
        ) { cliente ->
            "ID: ${cliente.id}\n" +
                    "Nombre: ${cliente.nombre}\n" +
                    "NIT: ${cliente.nit}\n" +
                    "Dirección: ${cliente.direccion}\n" +
                    "Teléfono: ${cliente.telefono}\n" +
                    "Correo: ${cliente.correo}"
        }

        resultado.text =
            "Clientes registrados: ${clientes.size}\n\n$listado"
    }
    /*private fun actualizarCliente() {
        val nitBuscado = nit.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text = "Ingresa el NIT del cliente que deseas actualizar."
            return
        }

        val indice = clientes.indexOfFirst {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (indice == -1) {
            resultado.text = "No se encontró un cliente con ese NIT."
            return
        }

        val nombreIngresado = nombre.text.toString().trim()
        val telefonoIngresado = telefono.text.toString().trim()
        val correoIngresado = correo.text.toString().trim()

        if (nombreIngresado.length < 2) {
            resultado.text = "Ingresa un nombre válido."
            return
        }

        if (telefonoIngresado.isEmpty()) {
            resultado.text = "Ingresa el teléfono."
            return
        }

        if (!correoIngresado.contains("@")) {
            resultado.text = "Ingresa un correo válido."
            return
        }

        clientes[indice] = clientes[indice].copy(
            nombre = nombreIngresado,
            telefono = telefonoIngresado,
            correo = correoIngresado
        )

        resultado.text = "Cliente actualizado correctamente."
    }*/

    private fun actualizarCliente() {
        val nitBuscado = nit.text.toString().trim()
        val nombreIngresado = nombre.text.toString().trim()
        val direccionIngresada = direccion.text.toString().trim()
        val telefonoIngresado = telefono.text.toString().trim()
        val correoIngresado = correo.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text =
                "Ingresa el NIT del cliente que deseas actualizar."
            return
        }

        val clienteEncontrado = clientes.find {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (clienteEncontrado == null) {
            resultado.text =
                "No se encontró un cliente con ese NIT."
            return
        }

        if (nombreIngresado.length < 2) {
            resultado.text = "Ingresa un nombre válido."
            return
        }

        if (direccionIngresada.isEmpty()) {
            resultado.text = "Ingresa la dirección."
            return
        }

        if (telefonoIngresado.isEmpty()) {
            resultado.text = "Ingresa el teléfono."
            return
        }

        if (!correoIngresado.contains("@")) {
            resultado.text = "Ingresa un correo válido."
            return
        }

        val datosActualizados = ClienteRequest(
            nit = nitBuscado,
            nombre = nombreIngresado,
            direccion = direccionIngresada,
            email = correoIngresado,
            telefono = telefonoIngresado
        )

        btnActualizar.isEnabled = false
        resultado.text = "Actualizando cliente..."

        lifecycleScope.launch {
            try {
                val respuesta =
                    RetrofitClient.api.actualizarCliente(
                        idCliente = clienteEncontrado.id,
                        cliente = datosActualizados
                    )

                if (respuesta.isSuccessful) {
                    limpiarFormulario()
                    resultado.text =
                        "Cliente actualizado correctamente."

                    cargarClientesDesdeApi()
                } else {
                    val mensajeServidor =
                        respuesta.errorBody()?.string()

                    resultado.text =
                        "No se pudo actualizar.\n" +
                                "Código: ${respuesta.code()}\n" +
                                (mensajeServidor ?: "")
                }

            } catch (error: IOException) {
                resultado.text =
                    "No se pudo conectar con la API."

            } catch (error: Exception) {
                resultado.text =
                    "Ocurrió un error al actualizar:\n${error.message}"

            } finally {
                btnActualizar.isEnabled = true
            }
        }
    }

    /*private fun eliminarCliente() {
        val nitBuscado = nit.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text = "Ingresa el NIT del cliente que deseas eliminar."
            return
        }

        val indice = clientes.indexOfFirst {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (indice == -1) {
            resultado.text = "No se encontró un cliente con ese NIT."
            return
        }

        val clienteEliminado = clientes.removeAt(indice)

        resultado.text =
            "Cliente eliminado correctamente.\n\n" +
                    "Nombre: ${clienteEliminado.nombre}\n" +
                    "NIT: ${clienteEliminado.nit}"
    }*/

    private fun eliminarCliente() {
        val nitBuscado = nit.text.toString().trim()

        if (nitBuscado.isEmpty()) {
            resultado.text =
                "Ingresa el NIT del cliente que deseas eliminar."
            return
        }

        val clienteEncontrado = clientes.find {
            it.nit.equals(nitBuscado, ignoreCase = true)
        }

        if (clienteEncontrado == null) {
            resultado.text =
                "No se encontró un cliente con ese NIT."
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage(
                "¿Deseas eliminar a ${clienteEncontrado.nombre}?\n\n" +
                        "NIT: ${clienteEncontrado.nit}"
            )
            .setPositiveButton("Eliminar") { _, _ ->
                enviarEliminacion(clienteEncontrado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarEliminacion(cliente: Cliente) {
        btnEliminar.isEnabled = false
        resultado.text = "Eliminando cliente..."

        lifecycleScope.launch {
            try {
                val respuesta =
                    RetrofitClient.api.eliminarCliente(cliente.id)

                if (respuesta.isSuccessful) {
                    limpiarFormulario()
                    resultado.text =
                        "Cliente eliminado correctamente."

                    cargarClientesDesdeApi()
                } else {
                    val mensajeServidor =
                        respuesta.errorBody()?.string()

                    resultado.text =
                        "No se pudo eliminar.\n" +
                                "Código: ${respuesta.code()}\n" +
                                (mensajeServidor ?: "")
                }

            } catch (error: IOException) {
                resultado.text =
                    "No se pudo conectar con la API."

            } catch (error: Exception) {
                resultado.text =
                    "Ocurrió un error al eliminar:\n${error.message}"

            } finally {
                btnEliminar.isEnabled = true
            }
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