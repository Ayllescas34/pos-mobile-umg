package com.example.myapplication2.session

import com.example.myapplication2.model.UsuarioResponse

object SessionManager {
    private var token: String? = null
    private var usuario: UsuarioResponse? = null

    fun iniciarSesion(tokenRecibido: String){
        token = tokenRecibido
    }

    fun guardarUsuario(usuarioRecibido: UsuarioResponse) {
        usuario = usuarioRecibido
    }

    fun obtenerToken(): String? {
        return token
    }

    fun obtenerUsuario(): UsuarioResponse? {
        return usuario
    }

    fun cerrarSesion(){
        token = null
        usuario = null
    }

    fun haySesion(): Boolean {
        return !token.isNullOrBlank()
    }
}
