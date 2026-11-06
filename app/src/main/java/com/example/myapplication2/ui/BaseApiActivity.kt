package com.example.myapplication2.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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


data class Opcion(val id: Int?, val texto: String) {
    override fun toString(): String = texto
}


fun AppCompatActivity.aplicarBordes() {
    enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

    val encabezado = findViewById<View?>(R.id.encabezado)
    val margenSuperior = encabezado?.paddingTop ?: 0
    val espacio = (12 * resources.displayMetrics.density).toInt()

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        val barras = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        v.setPadding(barras.left, 0, barras.right, barras.bottom)
        encabezado?.updatePadding(top = margenSuperior + barras.top + espacio)
        insets
    }
}


class ApiException(val codigo: Int, mensaje: String) : Exception(mensaje)


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
    protected fun prepararPantalla(
        layout: Int,
        titulo: String? = null,
        subtitulo: String? = null
    ): Boolean {
        setContentView(layout)
        aplicarBordes()

        findViewById<TextView?>(R.id.txtTituloPantalla)?.text = titulo
        findViewById<TextView?>(R.id.txtSubtituloPantalla)?.text = subtitulo
        findViewById<View?>(R.id.btnAtras)?.setOnClickListener { finish() }

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
            ContextCompat.getColor(
                this,
                if (esError) R.color.pos_peligro else R.color.pos_exito
            )
        )
    }

    protected fun mostrarInfo(texto: String) {
        txtEstado.text = texto
        txtEstado.setTextColor(ContextCompat.getColor(this, R.color.pos_texto_secundario))
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
                    progreso.visibility = View.INVISIBLE
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

    /** Ejecuta la búsqueda conforme se escribe, como en la aplicación de escritorio. */
    protected fun buscarAlEscribir(campo: EditText, alCambiar: () -> Unit) {
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit

            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit

            override fun afterTextChanged(s: Editable?) = alCambiar()
        })
    }

    /** Sube al formulario para ver el registro que se acaba de elegir. */
    private fun volverArriba() {
        findViewById<ScrollView?>(R.id.desplazamiento)?.smoothScrollTo(0, 0)
    }

    /**
     * Muestra una lista seleccionable dentro del contenedor (compatible con ScrollView).
     * La primera línea de cada fila va en negrita; `resaltar` marca en rojo un fragmento.
     */
    protected fun <T> llenarLista(
        contenedor: LinearLayout,
        elementos: List<T>,
        esSeleccionado: (T) -> Boolean,
        texto: (T) -> String,
        resaltar: (T) -> String? = { null },
        alSeleccionar: (T) -> Unit
    ) {
        contenedor.removeAllViews()

        if (elementos.isEmpty()) {
            contenedor.addView(TextView(this).apply {
                setText("No hay registros para mostrar.")
                setTextColor(ContextCompat.getColor(context, R.color.pos_texto_secundario))
                setPadding(dp(4), dp(10), dp(4), dp(10))
            })
            return
        }

        val fondoSeleccion = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, fondoSeleccion, true)

        elementos.forEach { elemento ->
            val fila = TextView(this).apply {
                setText(textoDeFila(texto(elemento), resaltar(elemento)))
                textSize = 14f
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isClickable = true
                isFocusable = true

                if (esSeleccionado(elemento)) {
                    setBackgroundResource(R.color.pos_seleccion)
                } else {
                    setBackgroundResource(fondoSeleccion.resourceId)
                }

                setOnClickListener {
                    alSeleccionar(elemento)
                    volverArriba()
                }
            }

            contenedor.addView(fila)
            contenedor.addView(View(this).apply {
                setBackgroundResource(R.color.pos_separador)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                )
            })
        }
    }

    private fun textoDeFila(texto: String, resaltado: String?): CharSequence {
        val fila = SpannableString(texto)
        val finPrimeraLinea = texto.indexOf('\n').let { if (it == -1) texto.length else it }

        fila.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            finPrimeraLinea,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (finPrimeraLinea < texto.length) {
            fila.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.pos_texto_secundario)),
                finPrimeraLinea,
                texto.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val inicio = resaltado?.let { texto.indexOf(it) } ?: -1
        if (inicio >= 0) {
            val fin = inicio + resaltado!!.length
            fila.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.pos_peligro)),
                inicio,
                fin,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            fila.setSpan(StyleSpan(Typeface.BOLD), inicio, fin, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return fila
    }

    protected fun configurarSpinner(spinner: Spinner, opciones: List<Opcion>, idSeleccionado: Int? = null) {
        val adaptador = ArrayAdapter(this, R.layout.item_spinner, opciones)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
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
