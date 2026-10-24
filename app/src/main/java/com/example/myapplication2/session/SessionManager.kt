package com.example.myapplication2.session

object SessionManager {
    private var token: String? = null

    fun iniciarSesion(tokenRecibido: String){
        token = tokenRecibido
    }

    fun obtenerToken(): String? {
        return token
    }

    fun cerrarSesion(){
        token = null
    }

    fun haySesion(): Boolean {
        return !token.isNullOrBlank()
    }
}