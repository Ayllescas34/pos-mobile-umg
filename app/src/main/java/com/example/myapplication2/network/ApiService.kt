package com.example.myapplication2.network

import com.example.myapplication2.model.ClienteRequest
import com.example.myapplication2.model.ClientesResponse
import com.example.myapplication2.model.LoginRequest
import com.example.myapplication2.model.LoginResponse
import com.example.myapplication2.model.CategoriaRequest
import com.example.myapplication2.model.CategoriasResponse
import com.example.myapplication2.model.DetalleVentaRequest
import com.example.myapplication2.model.DetallesVentaResponse
import com.example.myapplication2.model.ProductoRequest
import com.example.myapplication2.model.ProductosResponse
import com.example.myapplication2.model.SucursalRequest
import com.example.myapplication2.model.SucursalesResponse
import com.example.myapplication2.model.UsuarioRequest
import com.example.myapplication2.model.UsuariosResponse
import com.example.myapplication2.model.VentaRequest
import com.example.myapplication2.model.VentaResponse
import com.example.myapplication2.model.VentasResponse
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

    // ==============================
    // CATEGORÍAS
    // ==============================

    @GET("api/categorias")
    suspend fun obtenerCategorias(): CategoriasResponse

    @POST("api/categorias")
    suspend fun guardarCategoria(
        @Body categoria: CategoriaRequest
    ): Response<ResponseBody>

    @PUT("api/categorias/{id}")
    suspend fun actualizarCategoria(
        @Path("id") idCategoria: Int,
        @Body categoria: CategoriaRequest
    ): Response<ResponseBody>

    @DELETE("api/categorias/{id}")
    suspend fun eliminarCategoria(
        @Path("id") idCategoria: Int
    ): Response<ResponseBody>

    // ==============================
    // PRODUCTOS
    // ==============================

    @GET("api/productos")
    suspend fun obtenerProductos(): ProductosResponse

    @POST("api/productos")
    suspend fun guardarProducto(
        @Body producto: ProductoRequest
    ): Response<ResponseBody>

    @PUT("api/productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") idProducto: Int,
        @Body producto: ProductoRequest
    ): Response<ResponseBody>

    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(
        @Path("id") idProducto: Int
    ): Response<ResponseBody>

    // ==============================
    // SUCURSALES
    // ==============================

    @GET("api/sucursales")
    suspend fun obtenerSucursales(): SucursalesResponse

    @POST("api/sucursales")
    suspend fun guardarSucursal(
        @Body sucursal: SucursalRequest
    ): Response<ResponseBody>

    @PUT("api/sucursales/{id}")
    suspend fun actualizarSucursal(
        @Path("id") idSucursal: Int,
        @Body sucursal: SucursalRequest
    ): Response<ResponseBody>

    @DELETE("api/sucursales/{id}")
    suspend fun eliminarSucursal(
        @Path("id") idSucursal: Int
    ): Response<ResponseBody>

    // ==============================
    // USUARIOS (vendedores, cajeros y administradores)
    // ==============================

    @GET("api/usuarios")
    suspend fun obtenerUsuarios(): UsuariosResponse

    @POST("api/usuarios")
    suspend fun guardarUsuario(
        @Body usuario: UsuarioRequest
    ): Response<ResponseBody>

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") idUsuario: Int,
        @Body usuario: UsuarioRequest
    ): Response<ResponseBody>

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(
        @Path("id") idUsuario: Int
    ): Response<ResponseBody>

    // ==============================
    // VENTAS
    // ==============================

    @GET("api/ventas")
    suspend fun obtenerVentas(): VentasResponse

    @GET("api/ventas/{id}")
    suspend fun obtenerVenta(
        @Path("id") idVenta: Int
    ): VentaResponse

    @POST("api/ventas")
    suspend fun guardarVenta(
        @Body venta: VentaRequest
    ): Response<ResponseBody>

    @DELETE("api/ventas/{id}")
    suspend fun eliminarVenta(
        @Path("id") idVenta: Int
    ): Response<ResponseBody>

    // ==============================
    // DETALLES DE VENTA
    // ==============================

    @GET("api/detalles-ventas/venta/{idVenta}")
    suspend fun obtenerDetallesVenta(
        @Path("idVenta") idVenta: Int
    ): DetallesVentaResponse


    @POST("api/detalles-ventas")
    suspend fun agregarDetalleVenta(
        @Body detalle: DetalleVentaRequest
    ): Response<ResponseBody>


    @DELETE("api/detalles-ventas/{id}")
    suspend fun eliminarDetalleVenta(
        @Path("id") idDetalle: Int
    ): Response<ResponseBody>
}
