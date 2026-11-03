package com.example.myapplication2.network

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Emulador: 10.0.2.2 apunta al localhost de la computadora.
    private const val URL_EMULADOR = "http://10.0.2.2:3000/"

    // Teléfono físico por USB: ejecutar antes "adb reverse tcp:3000 tcp:3000".
    private const val URL_DISPOSITIVO_USB = "http://127.0.0.1:3000/"

    val BASE_URL: String =
        if (esEmulador()) URL_EMULADOR else URL_DISPOSITIVO_USB

    // BASIC no imprime los cuerpos, así las contraseñas no llegan a logcat.
    private val logginInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(logginInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)

    private fun esEmulador(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.contains("emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("sdk_gphone") ||
                Build.MODEL.contains("Emulator") ||
                Build.PRODUCT.contains("sdk")
    }

}
