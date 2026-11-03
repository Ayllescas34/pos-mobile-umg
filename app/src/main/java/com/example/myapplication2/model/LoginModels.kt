package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val usuario: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioResponse
)

data class UsuarioResponse(
    @SerializedName("id_usuario")
    val id: Int? = null,

    val nombre: String,
    val usuario: String? = null,
    val rol: String? = null,

    @SerializedName("id_sucursal")
    val idSucursal: Int? = null
)
