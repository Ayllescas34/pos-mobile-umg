package com.example.myapplication2.network

import com.example.myapplication2.model.ClienteRequest
import com.example.myapplication2.model.ClientesResponse
import com.example.myapplication2.model.LoginRequest
import com.example.myapplication2.model.LoginResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body datos: LoginRequest
    ): LoginResponse

    @GET("api/clientes")
    suspend fun obtenerClientes(): ClientesResponse

    @POST("api/clientes")
    suspend fun guardarCliente(
        @Body cliente: ClienteRequest
    ): Response<ResponseBody>

    @PUT("api/clientes/{id}")
    suspend fun actualizarCliente(
        @Path("id") idCliente: Int,
        @Body cliente: ClienteRequest
    ): Response<ResponseBody>

    @DELETE("api/clientes/{id}")
    suspend fun eliminarCliente(
        @Path("id") idCliente: Int
    ): Response<ResponseBody>

}