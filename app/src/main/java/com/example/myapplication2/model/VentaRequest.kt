package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class VentaRequest(
    @SerializedName("metodo_pago")
    val metodoPago: String,

    @SerializedName("id_cliente")
    val idCliente: Int?,

    @SerializedName("id_vendedor")
    val idVendedor: Int?,

    @SerializedName("id_cajero")
    val idCajero: Int?,

    @SerializedName("id_sucursal")
    val idSucursal: Int?,

    val detalles: List<DetalleVentaItem>
)

// Línea del carrito enviada al crear la venta (el servidor calcula subtotal y total)
data class DetalleVentaItem(
    @SerializedName("id_producto")
    val idProducto: Int,

    val cantidad: Int,

    @SerializedName("precio_unitario")
    val precioUnitario: Double
)
