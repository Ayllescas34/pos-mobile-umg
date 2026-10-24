package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class Cliente(
    @SerializedName("id_cliente")
    val id: Int,

    val nombre: String,
    val nit: String,
    val telefono: String,

    @SerializedName("email")
    val correo: String,

    val direccion: String = ""
)