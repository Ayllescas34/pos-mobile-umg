package com.example.myapplication2.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.example.myapplication2.R
import com.example.myapplication2.model.Categoria
import com.example.myapplication2.model.Producto
import com.example.myapplication2.model.ProductoRequest
import com.example.myapplication2.network.RetrofitClient

class ProductoActivity : BaseApiActivity() {

    private lateinit var txtBuscar: EditText
    private lateinit var txtSeleccion: TextView
    private lateinit var txtCodigo: EditText
    private lateinit var txtNombre: EditText
    private lateinit var txtDescripcion: EditText
    private lateinit var txtPrecio: EditText
    private lateinit var txtCosto: EditText
    private lateinit var txtStock: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var listaProductos: LinearLayout

    private lateinit var btnBuscar: Button
    private lateinit var btnNuevo: Button
    private lateinit var btnGuardar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnListar: Button

    // Caché en memoria de lo descargado desde la API
    private val productos = mutableListOf<Producto>()
    private val categorias = mutableListOf<Categoria>()
    private var filtro = ""
    private var seleccionado: Producto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_producto)) return

        txtBuscar = findViewById(R.id.txtBuscar)
        txtSeleccion = findViewById(R.id.txtSeleccion)
        txtCodigo = findViewById(R.id.txtCodigo)
        txtNombre = findViewById(R.id.txtNombre)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        txtPrecio = findViewById(R.id.txtPrecio)
        txtCosto = findViewById(R.id.txtCosto)
        txtStock = findViewById(R.id.txtStock)
        spCategoria = findViewById(R.id.spCategoria)
        listaProductos = findViewById(R.id.listaProductos)

        btnBuscar = findViewById(R.id.btnBuscar)
        btnNuevo = findViewById(R.id.btnNuevo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnListar = findViewById(R.id.btnListar)

        btnBuscar.setOnClickListener { buscarProducto() }
        btnNuevo.setOnClickListener { limpiarFormulario() }
        btnGuardar.setOnClickListener { guardarProducto() }
        btnActualizar.setOnClickListener { actualizarProducto() }
        btnEliminar.setOnClickListener { eliminarProducto() }
        btnListar.setOnClickListener {
            filtro = ""
            txtBuscar.text.clear()
            cargarDatosDesdeApi()
        }

        configurarSpinner(spCategoria, listOf(Opcion(null, "Sin categoría")))
        limpiarFormulario()
        cargarDatosDesdeApi()
    }

    override fun botonesAccion(): List<Button> =
        listOf(btnBuscar, btnNuevo, btnGuardar, btnActualizar, btnEliminar, btnListar)

    override fun refrescarBotones() {
        super.refrescarBotones()
        btnGuardar.isEnabled = seleccionado == null
        btnActualizar.isEnabled = seleccionado != null
        btnEliminar.isEnabled = seleccionado != null
    }

    /** Carga categorías (para el Spinner) y productos. */
    private fun cargarDatosDesdeApi(mensajeFinal: String? = null) {
        ejecutar("Cargando productos desde la API...", "consultar los productos") {
            val respuestaCategorias = RetrofitClient.api.obtenerCategorias()
            val respuestaProductos = RetrofitClient.api.obtenerProductos()

            categorias.clear()
            categorias.addAll(respuestaCategorias.data.sortedBy { it.nombre.lowercase() })
            productos.clear()
            productos.addAll(respuestaProductos.data)

            val idCategoriaActual = idSeleccionado(spCategoria)
            configurarSpinner(
                spCategoria,
                listOf(Opcion(null, "Sin categoría")) +
                        categorias.map { Opcion(it.id, it.nombre) },
                idCategoriaActual
            )

            seleccionado = seleccionado?.let { actual -> productos.find { it.id == actual.id } }
            if (seleccionado == null) mostrarFormularioNuevo()

            mostrarListado()
            if (mensajeFinal != null) {
                mostrarMensaje(mensajeFinal)
            } else {
                mostrarInfo("Productos registrados: ${productos.size}")
            }
        }
    }

    private fun coincide(producto: Producto, texto: String): Boolean {
        return producto.nombre.contains(texto, ignoreCase = true) ||
                producto.codigoBarras.orEmpty().equals(texto, ignoreCase = true) ||
                producto.id.toString() == texto
    }

    private fun mostrarListado() {
        val visibles = if (filtro.isBlank()) productos else productos.filter { coincide(it, filtro) }

        llenarLista(
            listaProductos,
            visibles,
            esSeleccionado = { it.id == seleccionado?.id },
            texto = { producto ->
                "#${producto.id}  ${producto.nombre}\n" +
                        "Precio: ${dinero(producto.precioVenta)}   Stock: ${producto.stock}\n" +
                        "Categoría: ${producto.categoria ?: "Sin categoría"}" +
                        (producto.codigoBarras?.takeIf { it.isNotBlank() }?.let { "   Código: $it" } ?: "")
            },
            alSeleccionar = { seleccionarProducto(it) }
        )
    }

    private fun buscarProducto() {
        filtro = txtBuscar.text.toString().trim()
        mostrarListado()

        if (filtro.isEmpty()) {
            mostrarInfo("Mostrando todos los productos: ${productos.size}")
            return
        }

        val encontrados = productos.filter { coincide(it, filtro) }

        when (encontrados.size) {
            0 -> {
                // Evita que quede seleccionado un registro que ya no se ve
                mostrarFormularioNuevo()
                refrescarBotones()
                mostrarMensaje("No se encontraron productos con \"$filtro\".", true)
            }
            1 -> seleccionarProducto(encontrados.first())
            else -> mostrarInfo("Se encontraron ${encontrados.size} productos. Toca uno para editarlo.")
        }
    }

    private fun seleccionarProducto(producto: Producto) {
        seleccionado = producto
        txtSeleccion.text = "Editando producto ID ${producto.id}"
        txtCodigo.setText(producto.codigoBarras.orEmpty())
        txtNombre.setText(producto.nombre)
        txtDescripcion.setText(producto.descripcion.orEmpty())
        txtPrecio.setText(String.format(java.util.Locale.US, "%.2f", producto.precioVenta))
        txtCosto.setText(String.format(java.util.Locale.US, "%.2f", producto.costo))
        txtStock.setText(producto.stock.toString())
        seleccionarEnSpinner(spCategoria, producto.idCategoria)
        mostrarListado()
        refrescarBotones()
        mostrarInfo("Producto seleccionado: ${producto.nombre}")
    }

    private fun mostrarFormularioNuevo() {
        seleccionado = null
        txtSeleccion.text = "Nuevo producto"
        txtCodigo.text.clear()
        txtNombre.text.clear()
        txtDescripcion.text.clear()
        txtPrecio.text.clear()
        txtCosto.text.clear()
        txtStock.setText("0")
        seleccionarEnSpinner(spCategoria, null)
    }

    private fun limpiarFormulario() {
        mostrarFormularioNuevo()
        if (::listaProductos.isInitialized) mostrarListado()
        refrescarBotones()
        mostrarInfo("Formulario listo para un nuevo producto.")
    }

    private fun leerFormulario(idActual: Int?): ProductoRequest? {
        val codigo = txtCodigo.text.toString().trim()
        val nombre = txtNombre.text.toString().trim()
        val descripcion = txtDescripcion.text.toString().trim()
        val precio = txtPrecio.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val costo = txtCosto.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val textoStock = txtStock.text.toString().trim()
        val stock = if (textoStock.isEmpty()) 0 else textoStock.toIntOrNull()

        if (nombre.length < 2) {
            mostrarMensaje("Ingresa un nombre válido (mínimo 2 caracteres).", true)
            return null
        }

        if (precio == null || precio < 0) {
            mostrarMensaje("El precio de venta debe ser un número mayor o igual a 0.", true)
            return null
        }

        if (costo == null || costo < 0) {
            mostrarMensaje("El costo debe ser un número mayor o igual a 0.", true)
            return null
        }

        if (stock == null || stock < 0) {
            mostrarMensaje("El stock debe ser un número entero mayor o igual a 0.", true)
            return null
        }

        // El código de barras es único en la base de datos
        if (codigo.isNotEmpty() &&
            productos.any { it.id != idActual && it.codigoBarras.equals(codigo, ignoreCase = true) }
        ) {
            mostrarMensaje("Ya existe otro producto con el código de barras $codigo.", true)
            return null
        }

        return ProductoRequest(
            codigoBarras = codigo.ifEmpty { null },
            nombre = nombre,
            descripcion = descripcion.ifEmpty { null },
            precioVenta = precio,
            costo = costo,
            stock = stock,
            idCategoria = idSeleccionado(spCategoria)
        )
    }

    private fun guardarProducto() {
        val datos = leerFormulario(null) ?: return

        ejecutar("Guardando producto...", "guardar el producto") {
            verificar(RetrofitClient.api.guardarProducto(datos), "guardar el producto")
            mostrarFormularioNuevo()
            cargarDatosDesdeApi("Producto guardado correctamente.")
        }
    }

    private fun actualizarProducto() {
        val producto = seleccionado ?: run {
            mostrarMensaje("Selecciona un producto del listado.", true)
            return
        }
        val datos = leerFormulario(producto.id) ?: return

        ejecutar("Actualizando producto...", "actualizar el producto") {
            verificar(
                RetrofitClient.api.actualizarProducto(producto.id, datos),
                "actualizar el producto"
            )
            cargarDatosDesdeApi("Producto actualizado correctamente.")
        }
    }

    private fun eliminarProducto() {
        val producto = seleccionado ?: run {
            mostrarMensaje("Selecciona un producto del listado.", true)
            return
        }

        confirmar(
            "Confirmar eliminación",
            "¿Deseas eliminar el producto \"${producto.nombre}\" (ID ${producto.id})?\n\n" +
                    "No se podrá eliminar si ya forma parte de alguna venta.",
            "Eliminar"
        ) {
            ejecutar("Eliminando producto...", "eliminar el producto") {
                verificar(
                    RetrofitClient.api.eliminarProducto(producto.id),
                    "eliminar el producto"
                )
                mostrarFormularioNuevo()
                cargarDatosDesdeApi("Producto eliminado correctamente.")
            }
        }
    }
}
