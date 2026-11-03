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
import com.example.myapplication2.model.Cliente
import com.example.myapplication2.model.DetalleVenta
import com.example.myapplication2.model.DetalleVentaItem
import com.example.myapplication2.model.DetalleVentaRequest
import com.example.myapplication2.model.Producto
import com.example.myapplication2.model.Sucursal
import com.example.myapplication2.model.Usuario
import com.example.myapplication2.model.Venta
import com.example.myapplication2.model.VentaRequest
import com.example.myapplication2.network.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Registro de ventas con carrito, consulta de ventas y su detalle.
 * La venta nueva se envía completa a POST /api/ventas (transaccional);
 * total y subtotales los calcula el servidor.
 */
class VentaActivity : BaseApiActivity() {

    companion object {
        // Valores del ENUM ventas.metodo_pago en la base de datos
        private val METODOS_PAGO = listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA")
    }

    /** Línea del carrito antes de registrar la venta. */
    private data class LineaCarrito(val producto: Producto, val cantidad: Int, val precio: Double) {
        val subtotal: Double get() = cantidad * precio
    }

    private lateinit var spCliente: Spinner
    private lateinit var spVendedor: Spinner
    private lateinit var spCajero: Spinner
    private lateinit var spSucursal: Spinner
    private lateinit var spMetodoPago: Spinner
    private lateinit var spProducto: Spinner
    private lateinit var txtInfoProducto: TextView
    private lateinit var txtCantidad: EditText
    private lateinit var txtPrecio: EditText
    private lateinit var listaCarrito: LinearLayout
    private lateinit var txtTotal: TextView
    private lateinit var listaVentas: LinearLayout
    private lateinit var panelDetalle: LinearLayout
    private lateinit var txtDetalleVenta: TextView
    private lateinit var listaDetalles: LinearLayout

    private lateinit var btnAgregar: Button
    private lateinit var btnNuevaVenta: Button
    private lateinit var btnRegistrar: Button
    private lateinit var btnListarVentas: Button
    private lateinit var btnAgregarAVenta: Button
    private lateinit var btnAnularVenta: Button
    private lateinit var btnCerrarDetalle: Button

    // Caché en memoria de lo descargado desde la API
    private val clientes = mutableListOf<Cliente>()
    private val usuarios = mutableListOf<Usuario>()
    private val sucursales = mutableListOf<Sucursal>()
    private val productos = mutableListOf<Producto>()
    private val ventas = mutableListOf<Venta>()

    private val carrito = mutableListOf<LineaCarrito>()
    private var ventaSeleccionada: Venta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prepararPantalla(R.layout.activity_venta)) return

        spCliente = findViewById(R.id.spCliente)
        spVendedor = findViewById(R.id.spVendedor)
        spCajero = findViewById(R.id.spCajero)
        spSucursal = findViewById(R.id.spSucursal)
        spMetodoPago = findViewById(R.id.spMetodoPago)
        spProducto = findViewById(R.id.spProducto)
        txtInfoProducto = findViewById(R.id.txtInfoProducto)
        txtCantidad = findViewById(R.id.txtCantidad)
        txtPrecio = findViewById(R.id.txtPrecio)
        listaCarrito = findViewById(R.id.listaCarrito)
        txtTotal = findViewById(R.id.txtTotal)
        listaVentas = findViewById(R.id.listaVentas)
        panelDetalle = findViewById(R.id.panelDetalle)
        txtDetalleVenta = findViewById(R.id.txtDetalleVenta)
        listaDetalles = findViewById(R.id.listaDetalles)

        btnAgregar = findViewById(R.id.btnAgregar)
        btnNuevaVenta = findViewById(R.id.btnNuevaVenta)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnListarVentas = findViewById(R.id.btnListarVentas)
        btnAgregarAVenta = findViewById(R.id.btnAgregarAVenta)
        btnAnularVenta = findViewById(R.id.btnAnularVenta)
        btnCerrarDetalle = findViewById(R.id.btnCerrarDetalle)

        btnAgregar.setOnClickListener { agregarAlCarrito() }
        btnNuevaVenta.setOnClickListener { confirmarNuevaVenta() }
        btnRegistrar.setOnClickListener { registrarVenta() }
        btnListarVentas.setOnClickListener { cargarDatosDesdeApi() }
        btnAgregarAVenta.setOnClickListener { agregarDetalleAVenta() }
        btnAnularVenta.setOnClickListener { anularVenta() }
        btnCerrarDetalle.setOnClickListener { cerrarDetalle() }

        configurarSpinner(spMetodoPago, METODOS_PAGO.mapIndexed { i, metodo -> Opcion(i, metodo) })

        spProducto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mostrarProductoSeleccionado()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        txtCantidad.setText("1")
        mostrarCarrito()
        cerrarDetalle()
        cargarDatosDesdeApi()
    }

    override fun botonesAccion(): List<Button> = listOf(
        btnAgregar, btnNuevaVenta, btnRegistrar, btnListarVentas,
        btnAgregarAVenta, btnAnularVenta, btnCerrarDetalle
    )

    override fun refrescarBotones() {
        super.refrescarBotones()
        val hayProducto = productoSeleccionado() != null
        btnAgregar.isEnabled = hayProducto
        btnRegistrar.isEnabled = carrito.isNotEmpty()
        btnAgregarAVenta.isEnabled = hayProducto && ventaSeleccionada != null
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    /** Descarga catálogos (clientes, usuarios, sucursales, productos) y ventas. */
    private fun cargarDatosDesdeApi() {
        ejecutar("Cargando datos de ventas desde la API...", "consultar las ventas") {
            descargarDatos()
            mostrarInfo("Ventas registradas: ${ventas.size}")
        }
    }

    private suspend fun descargarDatos() {
        val api = RetrofitClient.api
        val respuestaClientes = api.obtenerClientes()
        val respuestaUsuarios = api.obtenerUsuarios()
        val respuestaSucursales = api.obtenerSucursales()
        val respuestaProductos = api.obtenerProductos()
        val respuestaVentas = api.obtenerVentas()

        clientes.clear(); clientes.addAll(respuestaClientes.data)
        usuarios.clear(); usuarios.addAll(respuestaUsuarios.data)
        sucursales.clear(); sucursales.addAll(respuestaSucursales.data)
        productos.clear(); productos.addAll(respuestaProductos.data)
        ventas.clear(); ventas.addAll(respuestaVentas.data)

        actualizarSpinners()
        actualizarCarritoConStock()
        mostrarVentas()
    }

    private fun actualizarSpinners() {
        configurarSpinner(
            spCliente,
            listOf(Opcion(null, "Sin cliente")) +
                    clientes.map { Opcion(it.id, "${it.nombre} (NIT ${it.nit})") },
            idSeleccionado(spCliente)
        )

        // Vendedores y cajeros son usuarios filtrados por rol
        configurarSpinner(
            spVendedor,
            listOf(Opcion(null, "Sin vendedor")) +
                    usuarios.filter { it.rol.equals("VENDEDOR", ignoreCase = true) }
                        .map { Opcion(it.id, it.nombre) },
            idSeleccionado(spVendedor)
        )

        configurarSpinner(
            spCajero,
            listOf(Opcion(null, "Sin cajero")) +
                    usuarios.filter { it.rol.equals("CAJERO", ignoreCase = true) }
                        .map { Opcion(it.id, it.nombre) },
            idSeleccionado(spCajero)
        )

        configurarSpinner(
            spSucursal,
            listOf(Opcion(null, "Sin sucursal")) + sucursales.map { Opcion(it.id, it.nombre) },
            idSeleccionado(spSucursal) ?: sesionIdSucursal()
        )

        // Solo productos con existencias
        val idProductoActual = idSeleccionado(spProducto)
        val disponibles = productos.filter { it.stock > 0 }.sortedBy { it.nombre.lowercase() }

        configurarSpinner(
            spProducto,
            if (disponibles.isEmpty()) {
                listOf(Opcion(null, "No hay productos con stock"))
            } else {
                disponibles.map {
                    Opcion(it.id, "${it.nombre} — ${dinero(it.precioVenta)} (stock ${it.stock})")
                }
            },
            idProductoActual
        )
        mostrarProductoSeleccionado()
    }

    private fun sesionIdSucursal(): Int? =
        com.example.myapplication2.session.SessionManager.obtenerUsuario()?.idSucursal

    private fun productoSeleccionado(): Producto? {
        val id = if (::spProducto.isInitialized) idSeleccionado(spProducto) else null
        return productos.find { it.id == id }
    }

    private fun mostrarProductoSeleccionado() {
        val producto = productoSeleccionado()

        if (producto == null) {
            txtInfoProducto.text = "Selecciona un producto."
            txtPrecio.text.clear()
        } else {
            // El precio unitario inicia con el precio de venta del producto
            txtPrecio.setText(String.format(Locale.US, "%.2f", producto.precioVenta))
            val enCarrito = cantidadEnCarrito(producto.id)
            val disponible = producto.stock - enCarrito
            txtInfoProducto.text = if (disponible >= 0) {
                "Stock disponible: $disponible (en carrito: $enCarrito)"
            } else {
                "El carrito tiene $enCarrito y solo hay ${producto.stock} en stock. " +
                        "Quita unidades del carrito."
            }
        }

        if (!cargando) refrescarBotones()
    }

    // ------------------------------------------------------------------
    // Carrito
    // ------------------------------------------------------------------

    private fun cantidadEnCarrito(idProducto: Int): Int =
        carrito.filter { it.producto.id == idProducto }.sumOf { it.cantidad }

    /** Lee producto, cantidad y precio del formulario validando stock. */
    private fun leerLinea(stockReservado: Int): LineaCarrito? {
        val producto = productoSeleccionado() ?: run {
            mostrarMensaje("Selecciona un producto con stock.", true)
            return null
        }

        val cantidad = txtCantidad.text.toString().trim().toIntOrNull()
        val precio = txtPrecio.text.toString().trim().replace(',', '.').toDoubleOrNull()

        if (cantidad == null || cantidad <= 0) {
            mostrarMensaje("La cantidad debe ser un número entero mayor que 0.", true)
            return null
        }

        if (precio == null || precio < 0) {
            mostrarMensaje("El precio unitario debe ser un número mayor o igual a 0.", true)
            return null
        }

        val disponible = producto.stock - stockReservado
        if (cantidad > disponible) {
            mostrarMensaje(
                "Stock insuficiente para ${producto.nombre}. Disponible: $disponible.",
                true
            )
            return null
        }

        return LineaCarrito(producto, cantidad, precio)
    }

    private fun agregarAlCarrito() {
        val producto = productoSeleccionado()
        val linea = leerLinea(producto?.let { cantidadEnCarrito(it.id) } ?: 0) ?: return

        // Si ya existe el mismo producto con el mismo precio, se suma la cantidad
        val indice = carrito.indexOfFirst {
            it.producto.id == linea.producto.id && it.precio == linea.precio
        }

        if (indice >= 0) {
            carrito[indice] = carrito[indice].copy(cantidad = carrito[indice].cantidad + linea.cantidad)
        } else {
            carrito.add(linea)
        }

        txtCantidad.setText("1")
        mostrarCarrito()
        mostrarProductoSeleccionado()
        mostrarMensaje("Agregado: ${linea.cantidad} x ${linea.producto.nombre}")
    }

    private fun mostrarCarrito() {
        llenarLista(
            listaCarrito,
            carrito,
            esSeleccionado = { false },
            texto = { linea ->
                "${linea.producto.nombre}\n" +
                        "${linea.cantidad} x ${dinero(linea.precio)} = ${dinero(linea.subtotal)}"
            },
            alSeleccionar = { linea ->
                confirmar(
                    "Quitar del carrito",
                    "¿Quitar ${linea.cantidad} x ${linea.producto.nombre} del carrito?",
                    "Quitar"
                ) {
                    carrito.remove(linea)
                    mostrarCarrito()
                    mostrarProductoSeleccionado()
                }
            }
        )

        val total = carrito.sumOf { it.subtotal }
        txtTotal.text = "Artículos: ${carrito.sumOf { it.cantidad }}   " +
                "Total estimado: ${dinero(total)}"
        if (!cargando) refrescarBotones()
    }

    /** Tras recargar productos, actualiza el stock de las líneas del carrito. */
    private fun actualizarCarritoConStock() {
        val actualizadas = carrito.mapNotNull { linea ->
            productos.find { it.id == linea.producto.id }?.let { linea.copy(producto = it) }
        }
        carrito.clear()
        carrito.addAll(actualizadas)
        mostrarCarrito()
    }

    private fun confirmarNuevaVenta() {
        if (carrito.isEmpty()) {
            limpiarVenta()
            return
        }

        confirmar(
            "Nueva venta",
            "Se vaciará el carrito actual (${carrito.size} producto(s)). ¿Continuar?",
            "Vaciar"
        ) { limpiarVenta() }
    }

    private fun limpiarVenta() {
        carrito.clear()
        seleccionarEnSpinner(spCliente, null)
        seleccionarEnSpinner(spVendedor, null)
        seleccionarEnSpinner(spCajero, null)
        seleccionarEnSpinner(spSucursal, sesionIdSucursal())
        spMetodoPago.setSelection(0)
        txtCantidad.setText("1")
        mostrarCarrito()
        mostrarProductoSeleccionado()
        mostrarInfo("Carrito vacío. Lista para una nueva venta.")
    }

    // ------------------------------------------------------------------
    // Registrar venta
    // ------------------------------------------------------------------

    private fun registrarVenta() {
        if (carrito.isEmpty()) {
            mostrarMensaje("Agrega al menos un producto al carrito.", true)
            return
        }

        // Validación final de stock por producto (el servidor también lo valida)
        carrito.groupBy { it.producto.id }.forEach { (_, lineas) ->
            val producto = lineas.first().producto
            val cantidad = lineas.sumOf { it.cantidad }
            if (cantidad > producto.stock) {
                mostrarMensaje(
                    "Stock insuficiente para ${producto.nombre}: " +
                            "pides $cantidad y hay ${producto.stock}.",
                    true
                )
                return
            }
        }

        val metodo = (spMetodoPago.selectedItem as Opcion).texto
        val venta = VentaRequest(
            metodoPago = metodo,
            idCliente = idSeleccionado(spCliente),
            idVendedor = idSeleccionado(spVendedor),
            idCajero = idSeleccionado(spCajero),
            idSucursal = idSeleccionado(spSucursal),
            detalles = carrito.map {
                DetalleVentaItem(
                    idProducto = it.producto.id,
                    cantidad = it.cantidad,
                    precioUnitario = it.precio
                )
            }
        )

        val total = carrito.sumOf { it.subtotal }

        confirmar(
            "Registrar venta",
            "Productos: ${carrito.sumOf { it.cantidad }}\n" +
                    "Método de pago: $metodo\n" +
                    "Total estimado: ${dinero(total)}\n\n¿Registrar la venta?",
            "Registrar"
        ) {
            ejecutar("Registrando venta...", "registrar la venta") {
                val json = try {
                    verificar(RetrofitClient.api.guardarVenta(venta), "registrar la venta")
                } catch (error: ApiException) {
                    // El stock pudo cambiar en el servidor: se recarga antes de mostrar el error
                    descargarDatos()
                    throw error
                }
                val datos = json.optJSONObject("data")
                val idVenta = datos?.optInt("id_venta")
                val totalServidor = datos?.optDouble("total", total) ?: total

                carrito.clear()
                txtCantidad.setText("1")
                mostrarCarrito()

                // Recarga productos para reflejar el nuevo stock
                descargarDatos()
                mostrarMensaje("Venta #${idVenta ?: "?"} registrada. Total: ${dinero(totalServidor)}")
            }
        }
    }

    // ------------------------------------------------------------------
    // Ventas registradas y detalle
    // ------------------------------------------------------------------

    private fun mostrarVentas() {
        llenarLista(
            listaVentas,
            ventas,
            esSeleccionado = { it.id == ventaSeleccionada?.id },
            texto = { venta ->
                "Venta #${venta.id}  ${formatearFecha(venta.fechaHora)}\n" +
                        "Total: ${dinero(venta.total)}   ${venta.metodoPago.orEmpty()}   ${venta.estado.orEmpty()}\n" +
                        "Cliente: ${venta.cliente ?: "Sin cliente"}\n" +
                        "Sucursal: ${venta.sucursal ?: "Sin sucursal"}"
            },
            alSeleccionar = { cargarDetalleVenta(it.id) }
        )
    }

    private fun cargarDetalleVenta(idVenta: Int) {
        ejecutar("Consultando venta #$idVenta...", "consultar la venta") {
            descargarDetalle(idVenta)
            mostrarInfo("Detalle de la venta #$idVenta cargado (desplázate hacia abajo).")
        }
    }

    private suspend fun descargarDetalle(idVenta: Int) {
        val venta = RetrofitClient.api.obtenerVenta(idVenta).data
        ventaSeleccionada = venta
        mostrarDetalle(venta)
        mostrarVentas()
    }

    private fun mostrarDetalle(venta: Venta) {
        panelDetalle.visibility = View.VISIBLE
        val detalles = venta.detalles.orEmpty()

        txtDetalleVenta.text =
            "Venta #${venta.id}\n" +
                    "Fecha: ${formatearFecha(venta.fechaHora)}\n" +
                    "Cliente: ${venta.cliente ?: "Sin cliente"}\n" +
                    "Vendedor: ${venta.vendedor ?: "Sin vendedor"}\n" +
                    "Cajero: ${venta.cajero ?: "Sin cajero"}\n" +
                    "Sucursal: ${venta.sucursal ?: "Sin sucursal"}\n" +
                    "Método de pago: ${venta.metodoPago.orEmpty()}   Estado: ${venta.estado.orEmpty()}\n" +
                    "Total: ${dinero(venta.total)}   Líneas: ${detalles.size}"

        llenarLista(
            listaDetalles,
            detalles,
            esSeleccionado = { false },
            texto = { detalle ->
                "${detalle.producto ?: "Producto ${detalle.idProducto}"}\n" +
                        "${detalle.cantidad} x ${dinero(detalle.precioUnitario)} = ${dinero(detalle.subtotal)}"
            },
            alSeleccionar = { eliminarDetalle(venta, it) }
        )

        btnAgregarAVenta.text = "Agregar producto seleccionado a la venta #${venta.id}"
    }

    private fun cerrarDetalle() {
        ventaSeleccionada = null
        panelDetalle.visibility = View.GONE
        if (::listaVentas.isInitialized) mostrarVentas()
        if (!cargando) refrescarBotones()
    }

    /** POST /api/detalles-ventas: descuenta stock y suma al total de la venta. */
    private fun agregarDetalleAVenta() {
        val venta = ventaSeleccionada ?: run {
            mostrarMensaje("Selecciona una venta registrada.", true)
            return
        }

        val producto = productoSeleccionado()
        val linea = leerLinea(producto?.let { cantidadEnCarrito(it.id) } ?: 0) ?: return

        confirmar(
            "Agregar a la venta #${venta.id}",
            "Se agregará ${linea.cantidad} x ${linea.producto.nombre} " +
                    "(${dinero(linea.subtotal)}) a la venta registrada y se descontará el stock.",
            "Agregar"
        ) {
            ejecutar("Agregando producto a la venta...", "agregar el producto a la venta") {
                verificar(
                    RetrofitClient.api.agregarDetalleVenta(
                        DetalleVentaRequest(
                            idVenta = venta.id,
                            idProducto = linea.producto.id,
                            cantidad = linea.cantidad,
                            precioUnitario = linea.precio
                        )
                    ),
                    "agregar el producto a la venta"
                )
                txtCantidad.setText("1")
                descargarDatos()
                descargarDetalle(venta.id)
                mostrarMensaje("Producto agregado a la venta #${venta.id}.")
            }
        }
    }

    /** DELETE /api/detalles-ventas/{id}: devuelve stock y reduce el total. */
    private fun eliminarDetalle(venta: Venta, detalle: DetalleVenta) {
        confirmar(
            "Eliminar línea de la venta",
            "¿Eliminar ${detalle.cantidad} x ${detalle.producto ?: "producto"} " +
                    "de la venta #${venta.id}?\n\nEl stock se devolverá al inventario " +
                    "y el total de la venta se reducirá en ${dinero(detalle.subtotal)}.",
            "Eliminar"
        ) {
            ejecutar("Eliminando línea...", "eliminar la línea de la venta") {
                verificar(
                    RetrofitClient.api.eliminarDetalleVenta(detalle.id),
                    "eliminar la línea de la venta"
                )
                descargarDatos()
                descargarDetalle(venta.id)
                mostrarMensaje("Línea eliminada y stock devuelto.")
            }
        }
    }

    /**
     * El backend borra la venta y sus detalles en cascada SIN devolver stock.
     * Para no perder inventario, se eliminan primero los detalles uno por uno
     * (DELETE /api/detalles-ventas/{id} sí devuelve stock) y al final la venta.
     */
    private fun anularVenta() {
        val venta = ventaSeleccionada ?: run {
            mostrarMensaje("Selecciona una venta registrada.", true)
            return
        }

        confirmar(
            "⚠ Anular venta #${venta.id}",
            "Esta acción es IRREVERSIBLE.\n\n" +
                    "Se eliminarán las ${venta.detalles.orEmpty().size} línea(s) de la venta " +
                    "(devolviendo su stock) y después la venta completa por ${dinero(venta.total)}.\n\n" +
                    "Hazlo solo si la venta se registró por error.",
            "Anular venta"
        ) {
            ejecutar("Anulando venta #${venta.id}...", "anular la venta") {
                val api = RetrofitClient.api
                // Se consulta de nuevo para trabajar con el detalle más reciente
                val detalles = api.obtenerVenta(venta.id).data.detalles.orEmpty()

                detalles.forEachIndexed { indice, detalle ->
                    try {
                        verificar(api.eliminarDetalleVenta(detalle.id), "devolver el stock")
                    } catch (error: ApiException) {
                        throw ApiException(
                            error.codigo,
                            "La anulación se detuvo en la línea ${indice + 1} de ${detalles.size}: " +
                                    "${error.message}\nLas líneas anteriores ya devolvieron su stock."
                        )
                    }
                }

                verificar(api.eliminarVenta(venta.id), "eliminar la venta")
                cerrarDetalle()
                descargarDatos()
                mostrarMensaje(
                    "Venta #${venta.id} anulada. Se devolvió el stock de ${detalles.size} línea(s)."
                )
            }
        }
    }

    private fun formatearFecha(fecha: String?): String {
        if (fecha.isNullOrBlank()) return ""
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val salida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            salida.format(entrada.parse(fecha)!!)
        } catch (error: Exception) {
            fecha
        }
    }
}
