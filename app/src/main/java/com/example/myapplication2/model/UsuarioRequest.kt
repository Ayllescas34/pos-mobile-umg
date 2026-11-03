package com.example.myapplication2.model

import com.google.gson.annotations.SerializedName

data class UsuarioRequest(
    val nombre: String,
    val usuario: String,

    // En la interfaz se llama "Contraseña"; el backend espera password_hash
    // y la convierte a bcrypt en el primer inicio de sesión.
    @SerializedName("password_hash")
    val password: String,

    val rol: String,

    @SerializedName("id_sucursal")
    val idSucursal: Int?
) {
    // Evita que la contraseña aparezca si el objeto llega a imprimirse.
    override fun toString(): String =
        "UsuarioRequest(nombre=$nombre, usuario=$usuario, rol=$rol, idSucursal=$idSucursal)"
}
