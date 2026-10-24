package com.example.myapplication2.network

import com.example.myapplication2.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val solicitudOriginal = chain.request()
        val token = SessionManager.obtenerToken()

        val nuevaSolicitud = solicitudOriginal.newBuilder()

        if (!token.isNullOrBlank()) {
            nuevaSolicitud.header(
                "Authorization",
                "Bearer $token"
            )
        }

        return chain.proceed((nuevaSolicitud.build()))

    }

}