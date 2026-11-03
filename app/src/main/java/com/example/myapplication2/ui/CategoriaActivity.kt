package com.example.myapplication2.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication2.R
import com.example.myapplication2.model.Categoria
import com.example.myapplication2.model.CategoriaRequest
import com.example.myapplication2.network.RetrofitClient

class CategoriaActivity : BaseApiActivity() {

    private lateinit var txtBuscar: EditText
    private lateinit var txtSeleccion: TextView
    private lateinit var txtNombre: EditText
    private lateinit var txtDescripcion: EditText
    private lateinit var listaCategorias: LinearLayout

    private lateinit var btnBuscar: Button
    private lateinit var btnNuevo: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnListar: Button

    // Caché en memoria de lo descargado desde la API
    private val categorias = mutableListOf<Categoria>()
    private var filtro = ""
    private var seleccionada: Categoria? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_categoria)) return

        txtBuscar = findViewById(R.id.txtBuscar)
        txtSeleccion = findViewById(R.id.txtSeleccion)
        txtNombre = findViewById(R.id.txtNombre)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        listaCategorias = findViewById(R.id.listaCategorias)

        btnBuscar = findViewById(R.id.btnBuscar)
        btnNuevo = findViewById(R.id.btnNuevo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnListar = findViewById(R.id.btnListar)

        btnBuscar.setOnClickListener { buscarCategoria() }
        btnNuevo.setOnClickListener { limpiarFormulario() }
        btnGuardar.setOnClickListener { guardarCategoria() }
        btnActualizar.setOnClickListener { actualizarCategoria() }
        btnEliminar.setOnClickListener { eliminarCategoria() }
        btnListar.setOnClickListener {
            filtro = ""
            txtBuscar.text.clear()
            cargarCategoriasDesdeApi()
        }

        limpiarFormulario()
        cargarCategoriasDesdeApi()
    }

    override fun botonesAccion(): List<Button> =
        listOf(btnBuscar, btnNuevo, btnGuardar, btnActualizar, btnEliminar, btnListar)

    override fun refrescarBotones() {
        super.refrescarBotones()
        btnGuardar.isEnabled = seleccionada == null
        btnActualizar.isEnabled = seleccionada != null
        btnEliminar.isEnabled = seleccionada != null
    }

    private fun cargarCategoriasDesdeApi(mensajeFinal: String? = null) {
        ejecutar("Cargando categorías desde la API...", "consultar las categorías") {
            val respuesta = RetrofitClient.api.obtenerCategorias()

            categorias.clear()
            categorias.addAll(respuesta.data)

            // Si la categoría seleccionada ya no existe, se limpia el formulario
            seleccionada = seleccionada?.let { actual -> categorias.find { it.id == actual.id } }
            if (seleccionada == null) mostrarFormularioNuevo()

            mostrarListado()
            if (mensajeFinal != null) {
                mostrarMensaje(mensajeFinal)
            } else {
                mostrarInfo("Categorías registradas: ${categorias.size}")
            }
        }
    }

    private fun mostrarListado() {
        val visibles = if (filtro.isBlank()) {
            categorias
        } else {
            categorias.filter {
                it.nombre.contains(filtro, ignoreCase = true) ||
                        it.id.toString() == filtro
            }
        }

        llenarLista(
            listaCategorias,
            visibles,
            esSeleccionado = { it.id == seleccionada?.id },
            texto = { categoria ->
                "#${categoria.id}  ${categoria.nombre}" +
                        (categoria.descripcion?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
            },
            alSeleccionar = { seleccionarCategoria(it) }
        )
    }

    private fun buscarCategoria() {
        filtro = txtBuscar.text.toString().trim()
        mostrarListado()

        if (filtro.isEmpty()) {
            mostrarInfo("Mostrando todas las categorías: ${categorias.size}")
            return
        }

        val encontradas = categorias.filter {
            it.nombre.contains(filtro, ignoreCase = true) || it.id.toString() == filtro
        }

        when (encontradas.size) {
            0 -> {
                // Evita que quede seleccionado un registro que ya no se ve
                mostrarFormularioNuevo()
                refrescarBotones()
                mostrarMensaje("No se encontraron categorías con \"$filtro\".", true)
            }
            1 -> seleccionarCategoria(encontradas.first())
            else -> mostrarInfo("Se encontraron ${encontradas.size} categorías. Toca una para editarla.")
        }
    }

    private fun seleccionarCategoria(categoria: Categoria) {
        seleccionada = categoria
        txtSeleccion.text = "Editando categoría ID ${categoria.id}"
        txtNombre.setText(categoria.nombre)
        txtDescripcion.setText(categoria.descripcion.orEmpty())
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Categoría seleccionada: ${categoria.nombre}")
    }

    private fun mostrarFormularioNuevo() {
        seleccionada = null
        txtSeleccion.text = "Nueva categoría"
        txtNombre.text.clear()
        txtDescripcion.text.clear()
    }

    private fun limpiarFormulario() {
        mostrarFormularioNuevo()
        if (::listaCategorias.isInitialized) mostrarListado()
        refrescarBotones()
        mostrarInfo("Formulario listo para una nueva categoría.")
    }

    private fun leerFormulario(): CategoriaRequest? {
        val nombre = txtNombre.text.toString().trim()
        val descripcion = txtDescripcion.text.toString().trim()

        if (nombre.length < 2) {
            mostrarMensaje("Ingresa un nombre válido (mínimo 2 caracteres).", true)
            return null
        }

        if (nombre.length > 100) {
            mostrarMensaje("El nombre no puede tener más de 100 caracteres.", true)
            return null
        }

        return CategoriaRequest(
            nombre = nombre,
            descripcion = descripcion.ifEmpty { null }
        )
    }

    private fun guardarCategoria() {
        val datos = leerFormulario() ?: return

        if (categorias.any { it.nombre.equals(datos.nombre, ignoreCase = true) }) {
            mostrarMensaje("Ya existe una categoría llamada \"${datos.nombre}\".", true)
            return
        }

        ejecutar("Guardando categoría...", "guardar la categoría") {
            verificar(RetrofitClient.api.guardarCategoria(datos), "guardar la categoría")
            mostrarFormularioNuevo()
            cargarCategoriasDesdeApi("Categoría guardada correctamente.")
        }
    }

    private fun actualizarCategoria() {
        val categoria = seleccionada ?: run {
            mostrarMensaje("Selecciona una categoría del listado.", true)
            return
        }
        val datos = leerFormulario() ?: return

        if (categorias.any { it.id != categoria.id && it.nombre.equals(datos.nombre, ignoreCase = true) }) {
            mostrarMensaje("Ya existe otra categoría llamada \"${datos.nombre}\".", true)
            return
        }

        ejecutar("Actualizando categoría...", "actualizar la categoría") {
            verificar(
                RetrofitClient.api.actualizarCategoria(categoria.id, datos),
                "actualizar la categoría"
            )
            cargarCategoriasDesdeApi("Categoría actualizada correctamente.")
        }
    }

    private fun eliminarCategoria() {
        val categoria = seleccionada ?: run {
            mostrarMensaje("Selecciona una categoría del listado.", true)
            return
        }

        // La base de datos deja los productos sin categoría (ON DELETE SET NULL),
        // por eso se avisa cuántos productos se verían afectados.
        ejecutar("Revisando productos relacionados...", "revisar los productos") {
            val relacionados = RetrofitClient.api.obtenerProductos().data
                .count { it.idCategoria == categoria.id }

            val aviso = if (relacionados > 0) {
                "\n\nAtención: $relacionados producto(s) usan esta categoría " +
                        "y quedarán sin categoría."
            } else {
                ""
            }

            mostrarInfo("Confirma o cancela la eliminación.")
            confirmar(
                "Confirmar eliminación",
                "¿Deseas eliminar la categoría \"${categoria.nombre}\" (ID ${categoria.id})?$aviso",
                "Eliminar"
            ) { enviarEliminacion(categoria) }
        }
    }

    private fun enviarEliminacion(categoria: Categoria) {
        ejecutar("Eliminando categoría...", "eliminar la categoría") {
            verificar(
                RetrofitClient.api.eliminarCategoria(categoria.id),
                "eliminar la categoría"
            )
            mostrarFormularioNuevo()
            cargarCategoriasDesdeApi("Categoría eliminada correctamente.")
        }
    }
}
