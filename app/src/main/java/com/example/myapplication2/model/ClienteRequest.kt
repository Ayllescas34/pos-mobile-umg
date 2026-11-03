package com.example.myapplication2.model

data class ClienteRequest(
    val nit: String,
    val nombre: String,
    val direccion: String?,
    val email: String?,
    val telefono: String?
)
