package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class Producto(
    @SerializedName("id_producto")
    val id: Int,

    @SerializedName("codigo_barras")
    val codigoBarras: String?,

    val nombre: String,
    val descripcion: String?,

    @SerializedName("precio_venta")
    val precioVenta: Double,

    val costo: Double,
    val stock: Int,

    @SerializedName("id_categoria")
    val idCategoria: Int?,

    // Nombre de la categoría unido por la API (solo en consultas)
    val categoria: String?
)
