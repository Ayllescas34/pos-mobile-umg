package com.example.myapplication2.ui

import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication2.R
import com.example.myapplication2.session.SessionManager
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/**
 * Opción de un Spinner: muestra un texto amigable y guarda el ID interno.
 * Un id null representa "sin selección" (el backend acepta null).
 */
data class Opcion(val id: Int?, val texto: String) {
    override fun toString(): String = texto
}

/** Error devuelto por la API con su mensaje ya legible para el usuario. */
class ApiException(val codigo: Int, mensaje: String) : Exception(mensaje)

/**
 * Comportamiento común de las pantallas que consumen la API:
 * insets, verificación de sesión, progreso, manejo de errores,
 * confirmaciones, listas seleccionables y Spinners.
 */
abstract class BaseApiActivity : AppCompatActivity() {

    protected lateinit var progreso: ProgressBar
    protected lateinit var txtEstado: TextView

    private var solicitudesActivas = 0

    protected val cargando: Boolean
        get() = solicitudesActivas > 0

    /** Botones que se deshabilitan mientras hay una solicitud en curso. */
    protected abstract fun botonesAccion(): List<Button>

    /** Habilita cada botón según la selección actual (se llama al terminar cada solicitud). */
    protected open fun refrescarBotones() {
        botonesAccion().forEach { it.isEnabled = true }
    }

    /**
     * Prepara la pantalla. Devuelve false si no hay sesión
     * (en ese caso ya se redirigió al login).
     */
    protected fun prepararPantalla(layout: Int): Boolean {
        enableEdgeToEdge()
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!SessionManager.haySesion()) {
            irALogin()
            return false
        }

        progreso = findViewById(R.id.progreso)
        txtEstado = findViewById(R.id.txtEstado)
        return true
    }

    protected fun irALogin() {
        SessionManager.cerrarSesion()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    protected fun mostrarMensaje(texto: String, esError: Boolean = false) {
        txtEstado.text = texto
        txtEstado.setTextColor(
            if (esError) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
        )
    }

    protected fun mostrarInfo(texto: String) {
        txtEstado.text = texto
        txtEstado.setTextColor(Color.parseColor("#455A64"))
    }

    /**
     * Ejecuta una solicitud mostrando progreso, deshabilitando los botones
     * y traduciendo cualquier error a un mensaje entendible.
     */
    protected fun ejecutar(
        mensajeCarga: String,
        accion: String,
        bloque: suspend () -> Unit
    ) {
        solicitudesActivas++
        progreso.visibility = View.VISIBLE
        botonesAccion().forEach { it.isEnabled = false }
        mostrarInfo(mensajeCarga)

        lifecycleScope.launch {
            try {
                bloque()

            } catch (error: ApiException) {
                if (error.codigo == 401) {
                    sesionExpirada()
                } else {
                    mostrarMensaje(error.message ?: "No se pudo $accion.", true)
                }

            } catch (error: HttpException) {
                if (error.code() == 401) {
                    sesionExpirada()
                } else {
                    val mensaje = leerMensaje(error.response()?.errorBody())
                    mostrarMensaje(
                        mensaje ?: mensajePorCodigo(error.code(), accion),
                        true
                    )
                }

            } catch (error: IOException) {
                mostrarMensaje(
                    "No se pudo conectar con la API.\n" +
                            "Verifica que el servidor esté encendido.",
                    true
                )

            } catch (error: Exception) {
                mostrarMensaje("Ocurrió un error inesperado al $accion.", true)

            } finally {
                solicitudesActivas--
                if (solicitudesActivas == 0) {
                    progreso.visibility = View.GONE
                    refrescarBotones()
                }
            }
        }
    }

    /**
     * Revisa una respuesta de escritura. Si falló, lanza ApiException con el
     * mensaje del servidor; si tuvo éxito, devuelve el JSON recibido.
     */
    protected fun verificar(respuesta: Response<ResponseBody>, accion: String): JSONObject {
        if (!respuesta.isSuccessful) {
            val mensaje = leerMensaje(respuesta.errorBody())
                ?: mensajePorCodigo(respuesta.code(), accion)
            throw ApiException(respuesta.code(), mensaje)
        }

        val cuerpo = respuesta.body()?.string().orEmpty()
        return try {
            JSONObject(cuerpo)
        } catch (error: Exception) {
            JSONObject()
        }
    }

    private fun leerMensaje(cuerpo: ResponseBody?): String? {
        return try {
            val texto = cuerpo?.string() ?: return null
            JSONObject(texto).optString("mensaje").takeIf { it.isNotBlank() }
        } catch (error: Exception) {
            null
        }
    }

    private fun mensajePorCodigo(codigo: Int, accion: String): String {
        return when (codigo) {
            400 -> "Los datos enviados no son válidos."
            403 -> "No tienes permiso para $accion."
            404 -> "El registro ya no existe. Recarga el listado."
            in 500..599 -> "El servidor no pudo $accion."
            else -> "No se pudo $accion (código $codigo)."
        }
    }

    private fun sesionExpirada() {
        AlertDialog.Builder(this)
            .setTitle("Sesión expirada")
            .setMessage("Tu sesión no es válida o expiró. Inicia sesión nuevamente.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ -> irALogin() }
            .show()
    }

    protected fun confirmar(
        titulo: String,
        mensaje: String,
        textoAceptar: String,
        alAceptar: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(textoAceptar) { _, _ -> alAceptar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Muestra una lista seleccionable dentro del contenedor (compatible con ScrollView). */
    protected fun <T> llenarLista(
        contenedor: LinearLayout,
        elementos: List<T>,
        esSeleccionado: (T) -> Boolean,
        texto: (T) -> String,
        alSeleccionar: (T) -> Unit
    ) {
        contenedor.removeAllViews()

        if (elementos.isEmpty()) {
            contenedor.addView(TextView(this).apply {
                setText("No hay registros para mostrar.")
                setPadding(dp(8), dp(12), dp(8), dp(12))
            })
            return
        }

        val fondoSeleccion = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, fondoSeleccion, true)

        elementos.forEach { elemento ->
            val fila = TextView(this).apply {
                setText(texto(elemento))
                textSize = 15f
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isClickable = true
                isFocusable = true

                if (esSeleccionado(elemento)) {
                    setBackgroundColor(Color.parseColor("#33607D8B"))
                } else {
                    setBackgroundResource(fondoSeleccion.resourceId)
                }

                setOnClickListener { alSeleccionar(elemento) }
            }

            contenedor.addView(fila)
            contenedor.addView(View(this).apply {
                setBackgroundColor(Color.parseColor("#22000000"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                )
            })
        }
    }

    protected fun configurarSpinner(spinner: Spinner, opciones: List<Opcion>, idSeleccionado: Int? = null) {
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, opciones)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adaptador
        seleccionarEnSpinner(spinner, idSeleccionado)
    }

    protected fun seleccionarEnSpinner(spinner: Spinner, id: Int?) {
        val adaptador = spinner.adapter ?: return
        for (i in 0 until adaptador.count) {
            if ((adaptador.getItem(i) as Opcion).id == id) {
                spinner.setSelection(i)
                return
            }
        }
        if (adaptador.count > 0) spinner.setSelection(0)
    }

    protected fun idSeleccionado(spinner: Spinner): Int? {
        return (spinner.selectedItem as? Opcion)?.id
    }

    protected fun dinero(valor: Double): String {
        return String.format(Locale.US, "Q %.2f", valor)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}
