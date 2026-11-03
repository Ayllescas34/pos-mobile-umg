package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class Sucursal(
    @SerializedName("id_sucursal")
    val id: Int,

    val nombre: String,
    val direccion: String?,
    val telefono: String?
)
