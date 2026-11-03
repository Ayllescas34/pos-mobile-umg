package com.example.myapplication2.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication2.R
import com.example.myapplication2.model.LoginRequest
import com.example.myapplication2.network.RetrofitClient
import com.example.myapplication2.session.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var txtUsuario: EditText
    private lateinit var txtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar
    private lateinit var txtMensajeLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtUsuario = findViewById(R.id.txtUsuario)
        txtPassword = findViewById(R.id.txtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)
        txtMensajeLogin = findViewById(R.id.txtMensajeLogin)

        btnLogin.setOnClickListener {
            iniciarSesion()
        }
    }

    private fun iniciarSesion() {
        val usuario = txtUsuario.text.toString().trim()
        val password = txtPassword.text.toString()

        if (usuario.isEmpty() || password.isEmpty()) {
            txtMensajeLogin.text = "Ingresa tu usuario y contraseña."
            return
        }

        cambiarEstadoCarga(true)
        txtMensajeLogin.text = ""

        lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.api.login(
                    LoginRequest(
                        usuario = usuario,
                        password = password
                    )
                )

                SessionManager.iniciarSesion(respuesta.token)
                SessionManager.guardarUsuario(respuesta.usuario)

                val intent = Intent(
                    this@LoginActivity,
                    MenuActivity::class.java
                )

                startActivity(intent)
                finish()

            } catch (error: HttpException) {
                txtMensajeLogin.text =
                    when (error.code()) {
                        401 -> "Usuario o contraseña incorrectos."
                        403 -> "No tienes autorización."
                        else -> "Error del servidor: ${error.code()}"
                    }

            } catch (error: IOException) {
                txtMensajeLogin.text =
                    "No se pudo conectar con la API. Verifica que el servidor esté encendido."

            } catch (error: Exception) {
                txtMensajeLogin.text =
                    "Ocurrió un error: ${error.message}"

            } finally {
                cambiarEstadoCarga(false)
            }
        }
    }

    private fun cambiarEstadoCarga(cargando: Boolean) {
        progressLogin.visibility =
            if (cargando) View.VISIBLE else View.GONE

        btnLogin.isEnabled = !cargando
        txtUsuario.isEnabled = !cargando
        txtPassword.isEnabled = !cargando
    }
}