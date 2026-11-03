package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class DetalleVenta(
    @SerializedName("id_ventadet")
    val id: Int,

    @SerializedName("id_venta")
    val idVenta: Int?,

    @SerializedName("id_producto")
    val idProducto: Int?,

    val cantidad: Int,

    @SerializedName("precio_unitario")
    val precioUnitario: Double,

    val subtotal: Double,

    // Nombre del producto unido por la API
    val producto: String?
)

data class DetallesVentaResponse(
    val data: List<DetalleVenta>
)

// Agrega un detalle a una venta ya registrada
data class DetalleVentaRequest(
    @SerializedName("id_venta")
    val idVenta: Int,

    @SerializedName("id_producto")
    val idProducto: Int,

    val cantidad: Int,

    @SerializedName("precio_unitario")
    val precioUnitario: Double
)
