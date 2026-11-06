package com.example.myapplication2.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.example.myapplication2.R
import com.example.myapplication2.model.Sucursal
import com.example.myapplication2.model.Usuario
import com.example.myapplication2.model.UsuarioRequest
import com.example.myapplication2.network.RetrofitClient
import com.example.myapplication2.session.SessionManager

class UsuarioActivity : BaseApiActivity() {

    companion object {
        // Valores del ENUM usuarios.rol en la base de datos
        private val ROLES_CONOCIDOS = listOf("VENDEDOR", "CAJERO", "ADMINISTRADOR")
        private const val TODOS = "Todos los roles"
    }

    private lateinit var txtBuscar: EditText
    private lateinit var spFiltroRol: Spinner
    private lateinit var txtSeleccion: TextView
    private lateinit var txtNombre: EditText
    private lateinit var txtUsuario: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtAyudaPassword: TextView
    private lateinit var spRol: Spinner
    private lateinit var spSucursal: Spinner
    private lateinit var listaUsuarios: LinearLayout

    private lateinit var btnNuevo: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnListar: Button

    // Caché en memoria de lo descargado desde la API
    private val usuarios = mutableListOf<Usuario>()
    private val sucursales = mutableListOf<Sucursal>()
    private var roles = ROLES_CONOCIDOS
    private var filtro = ""
    private var seleccionado: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_usuario, "Usuarios", "Vendedores, cajeros y administradores.")) return

        txtBuscar = findViewById(R.id.txtBuscar)
        spFiltroRol = findViewById(R.id.spFiltroRol)
        txtSeleccion = findViewById(R.id.txtSeleccion)
        txtNombre = findViewById(R.id.txtNombre)
        txtUsuario = findViewById(R.id.txtUsuario)
        txtPassword = findViewById(R.id.txtPassword)
        txtAyudaPassword = findViewById(R.id.txtAyudaPassword)
        spRol = findViewById(R.id.spRol)
        spSucursal = findViewById(R.id.spSucursal)
        listaUsuarios = findViewById(R.id.listaUsuarios)

        btnNuevo = findViewById(R.id.btnNuevo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnListar = findViewById(R.id.btnListar)

        buscarAlEscribir(txtBuscar) { buscarUsuario() }
        btnNuevo.setOnClickListener { limpiarFormulario() }
        btnGuardar.setOnClickListener { guardarUsuario() }
        btnActualizar.setOnClickListener { actualizarUsuario() }
        btnEliminar.setOnClickListener { eliminarUsuario() }
        btnListar.setOnClickListener {
            filtro = ""
            txtBuscar.text.clear()
            cargarDatosDesdeApi()
        }

        configurarSpinnerRoles()
        configurarSpinner(spSucursal, listOf(Opcion(null, "Sin sucursal")))

        spFiltroRol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Si el filtro oculta al usuario seleccionado, se suelta la selección
                if (seleccionado != null && seleccionado !in usuariosVisibles()) {
                    mostrarFormularioNuevo()
                    refrescarBotones()
                }
                mostrarListado()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        limpiarFormulario()
        cargarDatosDesdeApi()
    }

    override fun botonesAccion(): List<Button> =
        listOf(btnNuevo, btnGuardar, btnActualizar, btnEliminar, btnListar)

    override fun refrescarBotones() {
        super.refrescarBotones()
        btnGuardar.isEnabled = seleccionado == null
        btnActualizar.isEnabled = seleccionado != null
        btnEliminar.isEnabled = seleccionado != null
    }

    private fun configurarSpinnerRoles() {
        val rolActual = spRol.selectedItem as? Opcion
        val filtroActual = spFiltroRol.selectedItem as? Opcion

        configurarSpinner(spRol, roles.mapIndexed { i, rol -> Opcion(i, rol) })
        configurarSpinner(
            spFiltroRol,
            listOf(Opcion(null, TODOS)) + roles.mapIndexed { i, rol -> Opcion(i, rol) }
        )

        rolActual?.let { seleccionarRol(spRol, it.texto) }
        filtroActual?.let { seleccionarRol(spFiltroRol, it.texto) }
    }

    private fun seleccionarRol(spinner: Spinner, rol: String) {
        val adaptador = spinner.adapter ?: return
        for (i in 0 until adaptador.count) {
            if ((adaptador.getItem(i) as Opcion).texto.equals(rol, ignoreCase = true)) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun rolSeleccionado(spinner: Spinner): String? {
        val opcion = spinner.selectedItem as? Opcion ?: return null
        return if (opcion.id == null) null else opcion.texto
    }

    /** Carga sucursales (para el Spinner) y usuarios. */
    private fun cargarDatosDesdeApi(mensajeFinal: String? = null) {
        ejecutar("Cargando usuarios desde la API...", "consultar los usuarios") {
            val respuestaSucursales = RetrofitClient.api.obtenerSucursales()
            val respuestaUsuarios = RetrofitClient.api.obtenerUsuarios()

            sucursales.clear()
            sucursales.addAll(respuestaSucursales.data)
            usuarios.clear()
            usuarios.addAll(respuestaUsuarios.data)

            // Conserva cualquier rol existente en la base aunque no sea de los conocidos
            val nuevosRoles = (ROLES_CONOCIDOS + usuarios.map { it.rol.uppercase() }).distinct()
            if (nuevosRoles != roles) {
                roles = nuevosRoles
                configurarSpinnerRoles()
            }

            val idSucursalActual = idSeleccionado(spSucursal)
            configurarSpinner(
                spSucursal,
                listOf(Opcion(null, "Sin sucursal")) +
                        sucursales.map { Opcion(it.id, it.nombre) },
                idSucursalActual
            )

            seleccionado = seleccionado?.let { actual -> usuarios.find { it.id == actual.id } }
            if (seleccionado == null) mostrarFormularioNuevo()

            mostrarListado()
            if (mensajeFinal != null) {
                mostrarMensaje(mensajeFinal)
            } else {
                mostrarInfo("Usuarios registrados: ${usuarios.size}")
            }
        }
    }

    private fun nombreSucursal(idSucursal: Int?): String {
        if (idSucursal == null) return "Sin sucursal"
        return sucursales.find { it.id == idSucursal }?.nombre ?: "Sucursal $idSucursal"
    }

    private fun coincide(usuario: Usuario, texto: String): Boolean {
        return usuario.nombre.contains(texto, ignoreCase = true) ||
                usuario.usuario.contains(texto, ignoreCase = true) ||
                usuario.id.toString() == texto
    }

    private fun usuariosVisibles(): List<Usuario> {
        val rolFiltro = if (::spFiltroRol.isInitialized) rolSeleccionado(spFiltroRol) else null

        return usuarios.filter { usuario ->
            (rolFiltro == null || usuario.rol.equals(rolFiltro, ignoreCase = true)) &&
                    (filtro.isBlank() || coincide(usuario, filtro))
        }
    }

    private fun mostrarListado() {
        if (!::listaUsuarios.isInitialized) return

        llenarLista(
            listaUsuarios,
            usuariosVisibles(),
            esSeleccionado = { it.id == seleccionado?.id },
            texto = { usuario ->
                "#${usuario.id}  ${usuario.nombre}\n" +
                        "Usuario: ${usuario.usuario}   Rol: ${usuario.rol}\n" +
                        "Sucursal: ${nombreSucursal(usuario.idSucursal)}"
            },
            alSeleccionar = { seleccionarUsuario(it) }
        )
    }

    private fun buscarUsuario() {
        filtro = txtBuscar.text.toString().trim()
        mostrarListado()

        val encontrados = usuariosVisibles()

        if (filtro.isEmpty()) {
            mostrarInfo("Mostrando ${encontrados.size} usuario(s).")
            return
        }

        // Evita que quede seleccionado un registro que ya no se ve
        seleccionado?.let { actual ->
            if (actual !in encontrados) {
                mostrarFormularioNuevo()
                refrescarBotones()
            }
        }

        when (encontrados.size) {
            0 -> mostrarMensaje("No se encontraron usuarios con \"$filtro\".", true)
            else -> mostrarInfo("Resultados: ${encontrados.size}. Toca uno para editarlo.")
        }
    }

    private fun seleccionarUsuario(usuario: Usuario) {
        seleccionado = usuario
        txtSeleccion.text = "Editando usuario ID ${usuario.id}"
        txtNombre.setText(usuario.nombre)
        txtUsuario.setText(usuario.usuario)
        txtPassword.text.clear()
        txtAyudaPassword.text =
            "Para actualizar debes escribir una contraseña: el servidor reemplaza " +
                    "la contraseña actual con la que ingreses."
        seleccionarRol(spRol, usuario.rol)
        seleccionarEnSpinner(spSucursal, usuario.idSucursal)
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Usuario seleccionado: ${usuario.nombre}")
    }

    private fun mostrarFormularioNuevo() {
        seleccionado = null
        txtSeleccion.text = "Nuevo usuario"
        txtNombre.text.clear()
        txtUsuario.text.clear()
        txtPassword.text.clear()
        txtAyudaPassword.text = "La contraseña se guarda cifrada al iniciar sesión por primera vez."
        if (spRol.adapter != null) spRol.setSelection(0)
        seleccionarEnSpinner(spSucursal, null)
    }

    private fun limpiarFormulario() {
        mostrarFormularioNuevo()
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Formulario listo para un nuevo usuario.")
    }

    private fun leerFormulario(idActual: Int?): UsuarioRequest? {
        val nombre = txtNombre.text.toString().trim()
        val usuario = txtUsuario.text.toString().trim()
        val password = txtPassword.text.toString()
        val rol = (spRol.selectedItem as? Opcion)?.texto

        if (nombre.length < 2) {
            mostrarMensaje("Ingresa un nombre válido (mínimo 2 caracteres).", true)
            return null
        }

        if (usuario.length < 3 || usuario.contains(' ')) {
            mostrarMensaje("Ingresa un usuario válido (mínimo 3 caracteres, sin espacios).", true)
            return null
        }

        if (usuarios.any { it.id != idActual && it.usuario.equals(usuario, ignoreCase = true) }) {
            mostrarMensaje("El usuario \"$usuario\" ya está en uso.", true)
            return null
        }

        if (password.length < 4) {
            mostrarMensaje("Ingresa una contraseña de al menos 4 caracteres.", true)
            return null
        }

        if (rol == null) {
            mostrarMensaje("Selecciona un rol.", true)
            return null
        }

        return UsuarioRequest(
            nombre = nombre,
            usuario = usuario,
            password = password,
            rol = rol,
            idSucursal = idSeleccionado(spSucursal)
        )
    }

    private fun guardarUsuario() {
        val datos = leerFormulario(null) ?: return

        ejecutar("Guardando usuario...", "guardar el usuario") {
            verificar(RetrofitClient.api.guardarUsuario(datos), "guardar el usuario")
            mostrarFormularioNuevo()
            cargarDatosDesdeApi("Usuario guardado correctamente.")
        }
    }

    private fun actualizarUsuario() {
        val usuario = seleccionado ?: run {
            mostrarMensaje("Selecciona un usuario del listado.", true)
            return
        }
        val datos = leerFormulario(usuario.id) ?: return

        ejecutar("Actualizando usuario...", "actualizar el usuario") {
            verificar(
                RetrofitClient.api.actualizarUsuario(usuario.id, datos),
                "actualizar el usuario"
            )
            txtPassword.text.clear()
            cargarDatosDesdeApi("Usuario actualizado correctamente.")
        }
    }

    private fun eliminarUsuario() {
        val usuario = seleccionado ?: run {
            mostrarMensaje("Selecciona un usuario del listado.", true)
            return
        }

        if (usuario.id == SessionManager.obtenerUsuario()?.id) {
            mostrarMensaje("No puedes eliminar el usuario con el que iniciaste sesión.", true)
            return
        }

        confirmar(
            "Confirmar eliminación",
            "¿Deseas eliminar al usuario \"${usuario.nombre}\" (${usuario.usuario}, ${usuario.rol})?\n\n" +
                    "No se podrá eliminar si participó en alguna venta.",
            "Eliminar"
        ) {
            ejecutar("Eliminando usuario...", "eliminar el usuario") {
                verificar(
                    RetrofitClient.api.eliminarUsuario(usuario.id),
                    "eliminar el usuario"
                )
                mostrarFormularioNuevo()
                cargarDatosDesdeApi("Usuario eliminado correctamente.")
            }
        }
    }
}
