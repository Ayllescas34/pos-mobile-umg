package com.example.myapplication2.model

data class LoginRequest(
    val usuario: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioResponse
)

data class UsuarioResponse(
    val nombre: String
)

