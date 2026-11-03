package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

// La API no devuelve password_hash en las consultas.
data class Usuario(
    @SerializedName("id_usuario")
    val id: Int,

    val nombre: String,
    val usuario: String,
    val rol: String,

    @SerializedName("id_sucursal")
    val idSucursal: Int?
)
