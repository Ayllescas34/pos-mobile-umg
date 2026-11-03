package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class Venta(
    @SerializedName("id_venta")
    val id: Int,

    @SerializedName("fecha_hora")
    val fechaHora: String?,

    val total: Double,

    @SerializedName("metodo_pago")
    val metodoPago: String?,

    val estado: String?,

    @SerializedName("id_cliente")
    val idCliente: Int?,

    @SerializedName("id_vendedor")
    val idVendedor: Int?,

    @SerializedName("id_cajero")
    val idCajero: Int?,

    @SerializedName("id_sucursal")
    val idSucursal: Int?,

    // Nombres unidos por la API
    val cliente: String?,
    val vendedor: String?,
    val cajero: String?,
    val sucursal: String?,

    // Solo en GET /api/ventas/{id}
    val detalles: List<DetalleVenta>? = null
)

data class VentasResponse(
    val data: List<Venta>
)

data class VentaResponse(
    val data: Venta
)
