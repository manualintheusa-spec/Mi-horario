package com.manuel.miturno

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private val FONDO = Color.rgb(243, 244, 246)
    private val TINTA = Color.rgb(21, 23, 26)
    private val GRIS = Color.rgb(106, 112, 122)
    private val ACENTO = Color.rgb(47, 124, 246)

    private lateinit var aj: Ajustes
    private lateinit var col: LinearLayout
    private lateinit var vista: ImageView
    private lateinit var marcoVista: FrameLayout
    private var px = 1f

    private fun dp(v: Float) = (v * px).toInt()
    private fun dp(v: Int) = dp(v.toFloat())

    private fun fondo(color: Int, radio: Float, borde: Int = 0, grosor: Float = 1f) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radio * px
        if (borde != 0) setStroke(dp(grosor), borde)
    }

    private fun conToque(d: Drawable) = RippleDrawable(ColorStateList.valueOf(Color.argb(40, 0, 0, 0)), d, null)

    private fun lp(arriba: Int = 0, ancho: Int = ViewGroup.LayoutParams.MATCH_PARENT) =
        LinearLayout.LayoutParams(ancho, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(arriba) }

    private fun txt(s: String, size: Float, color: Int = TINTA, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setLineSpacing(0f, 1.15f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        px = resources.displayMetrics.density
        aj = Ajustes.cargar(this)

        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = FONDO
            window.navigationBarColor = FONDO
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(22), dp(16), dp(40))
        }

        // Encabezado
        col.addView(txt("Mi turno", 30f, bold = true), lp())
        col.addView(txt("Tu horario y con quién trabajas, en tu pantalla de inicio.", 15f, GRIS), lp(2))

        // Vista previa
        vista = ImageView(this).apply { adjustViewBounds = true }
        marcoVista = FrameLayout(this).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(vista, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        col.addView(marcoVista, lp(18))
        val fondos = listOf(0xFF2F4A33.toInt(), 0xFF1F3A5F.toInt(), 0xFF3B2A4D.toInt(), 0xFF121316.toInt(),
            0xFFC9D6DF.toInt(), 0xFFF1E4D3.toInt(), 0xFFF4C0D1.toInt())
        val filaFondo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        filaFondo.addView(txt("Fondo de la vista previa", 13f, GRIS))
        col.addView(filaFondo, lp(10))
        col.addView(muestras(fondos, aj.fondoVista, 28) { aj.fondoVista = it; cambio(false) }, lp(6))

        // Horario
        val horario = tarjeta("Tu horario", "Pega el texto que te da Claude. Una línea por turno, por ejemplo L TU 16:00-22:00 o L Ana 15:00-21:00. Días: D L M X J V S (X = miércoles).")
        val campo = EditText(this).apply {
            setText(Datos.texto(this@MainActivity))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setTextColor(TINTA)
            minLines = 9
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(false)
            background = fondo(Color.rgb(247, 248, 250), 14f, Color.rgb(226, 229, 234))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        horario.addView(campo, lp(12))
        val error = txt("", 13f, Color.rgb(200, 40, 40))
        horario.addView(error, lp(6))
        val botones = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val guardar = boton("Guardar", true)
        val pegar = boton("Pegar", false)
        val ejemplo = boton("Ejemplo", false)
        botones.addView(guardar, LinearLayout.LayoutParams(0, dp(46), 1.3f))
        botones.addView(pegar, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
        botones.addView(ejemplo, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
        horario.addView(botones, lp(4))

        guardar.setOnClickListener {
            val v = campo.text.toString()
            val r = Horario.parse(v)
            when {
                v.isBlank() -> error.text = "Pega tu horario primero."
                r.errores.isNotEmpty() -> error.text = "Revisa la línea " + r.errores.joinToString(", ") + ". Ejemplo: L Ana 16:00-21:00"
                r.semana.none { it.yo != null } -> error.text = "Falta tu turno. Usa TU como nombre, por ejemplo: L TU 16:00-22:00"
                else -> {
                    error.text = ""
                    Datos.prefs(this).edit().putString("texto", v).apply()
                    cambio()
                    Toast.makeText(this, "Horario guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pegar.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val s = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
            if (s.isNullOrBlank()) error.text = "No hay texto copiado."
            else { campo.setText(s.trim()); error.text = "Revisa y toca Guardar." }
        }
        ejemplo.setOnClickListener { campo.setText(Horario.EJEMPLO); error.text = "" }

        // Apariencia
        val ap = tarjeta("Apariencia")
        etiqueta(ap, "Letras")
        ap.addView(segmentado(listOf("Automático", "Claras", "Oscuras"), aj.letras) { aj.letras = it; cambio() }, lp(8))
        ap.addView(txt("Automático cambia las letras según tu fondo de pantalla.", 12f, GRIS), lp(6))
        etiqueta(ap, "Color del cristal")
        val tintes = listOf(Color.WHITE, Color.rgb(200, 205, 212), Color.rgb(20, 22, 26),
            Color.rgb(120, 170, 255), Color.rgb(130, 220, 170), Color.rgb(255, 170, 205), Color.rgb(255, 210, 140))
        ap.addView(muestras(tintes, aj.tinte) { aj.tinte = it; cambio() }, lp(8))
        etiqueta(ap, "Transparencia del cristal")
        ap.addView(deslizador(4, 60, aj.opacidad, { "$it%" }) { aj.opacidad = it }, lp(2))
        etiqueta(ap, "Esquinas")
        ap.addView(deslizador(4, 28, aj.radio, { if (it < 10) "Rectas" else if (it < 20) "Suaves" else "Redondas" }) { aj.radio = it }, lp(2))
        etiqueta(ap, "Estilo de letra")
        ap.addView(segmentado(listOf("Moderna", "Compacta", "Clásica"), aj.fuente) { aj.fuente = it; cambio() }, lp(8))
        etiqueta(ap, "Tamaño del texto")
        ap.addView(deslizador(85, 120, aj.escala, { "$it%" }) { aj.escala = it }, lp(2))

        // Colores
        val co = tarjeta("Colores")
        val barras = listOf(Color.rgb(47, 124, 246), Color.rgb(124, 92, 255), Color.rgb(16, 185, 129),
            Color.rgb(245, 130, 32), Color.rgb(236, 72, 153), Color.rgb(20, 22, 26), Color.WHITE)
        etiqueta(co, "Tu barra (lo que ya trabajaste)")
        co.addView(muestras(barras, aj.colorTu) { aj.colorTu = it; cambio() }, lp(8))
        val faltas = listOf(Color.rgb(110, 225, 200), Color.rgb(165, 200, 255), Color.rgb(200, 185, 255),
            Color.rgb(255, 214, 150), Color.rgb(255, 190, 215), Color.rgb(220, 224, 230))
        etiqueta(co, "Lo que te falta")
        co.addView(muestras(faltas, aj.colorFalta) { aj.colorFalta = it; cambio() }, lp(8))
        etiqueta(co, "Verde si trabajas menos de…")
        co.addView(deslizador(2, 16, aj.verdeMenos, { horasMedias(it) }) {
            aj.verdeMenos = it
            if (aj.rojoMas < it) aj.rojoMas = it
        }, lp(2))
        etiqueta(co, "Rojo si trabajas más de…")
        co.addView(deslizador(4, 20, aj.rojoMas, { horasMedias(it) }) {
            aj.rojoMas = maxOf(it, aj.verdeMenos)
        }, lp(2))
        co.addView(txt("En medio de los dos sale naranja.", 12f, GRIS), lp(4))

        // Comportamiento
        val cp = tarjeta("Comportamiento")
        etiqueta(cp, "Modo reloj empieza")
        val opcAntes = listOf(60, 120, 180)
        cp.addView(segmentado(listOf("1 h antes", "2 h antes", "3 h antes"), opcAntes.indexOf(aj.antes).coerceAtLeast(0)) {
            aj.antes = opcAntes[it]; cambio()
        }, lp(8))
        etiqueta(cp, "Formato de hora")
        cp.addView(segmentado(listOf("4:00 pm", "16:00"), if (aj.h24) 1 else 0) { aj.h24 = it == 1; cambio() }, lp(8))
        etiqueta(cp, "La semana empieza en")
        cp.addView(segmentado(listOf("Domingo", "Lunes"), if (aj.lunes) 1 else 0) { aj.lunes = it == 1; cambio() }, lp(8))
        cp.addView(interruptor("Horas debajo de cada día", aj.horasDias) { aj.horasDias = it; cambio() }, lp(14))
        cp.addView(interruptor("Porcentaje en tu barra", aj.porcentaje) { aj.porcentaje = it; cambio() }, lp(4))
        cp.addView(interruptor("Espacio para íconos", aj.iconos) { aj.iconos = it; cambio() }, lp(4))

        // Ayuda
        val ay = tarjeta("Cómo ponerlo")
        ay.addView(txt("1. Mantén presionada tu pantalla de inicio.\n2. Toca Widgets y busca Mi turno.\n3. Arrástralo a tu pantalla; lo puedes estirar.\n\nSi en un Samsung el widget se atrasa: Ajustes → Aplicaciones → Mi turno → Batería → Sin restricciones.", 14f, GRIS), lp(8))

        val restablecer = boton("Restablecer apariencia", false)
        restablecer.setOnClickListener {
            val vista0 = aj.fondoVista
            aj = Ajustes().apply { fondoVista = vista0 }
            aj.guardar(this)
            MiTurnoWidget.actualizarTodo(this)
            recreate()
        }
        col.addView(restablecer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(18) })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(FONDO)
            isFillViewport = true
            addView(col)
        })
        muestra()
    }

    override fun onResume() {
        super.onResume()
        if (::vista.isInitialized) muestra()
    }

    private fun horasMedias(v: Int): String = if (v % 2 == 0) "${v / 2} h" else "${v / 2}.5 h"

    /** Guarda, redibuja la vista previa y (si se pide) el widget. */
    private fun cambio(widget: Boolean = true) {
        aj.guardar(this)
        muestra()
        if (widget) MiTurnoWidget.actualizarTodo(this)
    }

    private fun muestra() {
        marcoVista.background = fondo(aj.fondoVista, 22f)
        val oscuro = if (aj.letras == 0) Color.luminance(aj.fondoVista) > 0.5f else aj.letrasOscuras(this)
        val dm = resources.displayMetrics
        val w = dm.widthPixels - dp(56)
        val h = (w * 0.82f).toInt()
        vista.setImageBitmap(
            Dibujo(dm.density, aj, oscuro).dibujar(Datos.semana(this), w, h, Calendar.getInstance(), Datos.esEjemplo(this))
        )
    }

    // ---------- Piezas de la pantalla ----------

    private fun tarjeta(titulo: String, sub: String? = null): LinearLayout {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondo(Color.WHITE, 22f)
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        t.addView(txt(titulo, 19f, bold = true))
        if (sub != null) t.addView(txt(sub, 13f, GRIS), lp(4))
        col.addView(t, lp(16))
        return t
    }

    private fun etiqueta(padre: LinearLayout, s: String) {
        padre.addView(txt(s, 14f, Color.rgb(58, 61, 68), true), lp(16))
    }

    private fun boton(s: String, primario: Boolean) = TextView(this).apply {
        text = s
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(if (primario) Color.WHITE else TINTA)
        background = conToque(if (primario) fondo(TINTA, 14f) else fondo(Color.rgb(238, 240, 243), 14f))
        isClickable = true
        isFocusable = true
    }

    private fun segmentado(opciones: List<String>, sel: Int, al: (Int) -> Unit): LinearLayout {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = fondo(Color.rgb(238, 240, 243), 14f)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        val items = mutableListOf<TextView>()
        fun pinta(i: Int) {
            items.forEachIndexed { k, v ->
                val activo = k == i
                v.background = if (activo) fondo(Color.WHITE, 11f, Color.rgb(226, 229, 234)) else null
                v.setTextColor(if (activo) TINTA else GRIS)
                v.typeface = Typeface.create("sans-serif-medium", if (activo) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        opciones.forEachIndexed { i, s ->
            val v = TextView(this).apply {
                text = s
                textSize = 14f
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener { pinta(i); al(i) }
            }
            items.add(v)
            fila.addView(v, LinearLayout.LayoutParams(0, dp(40), 1f))
        }
        pinta(sel)
        return fila
    }

    private fun muestras(colores: List<Int>, sel: Int, tam: Int = 34, al: (Int) -> Unit): HorizontalScrollView {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val vistas = mutableListOf<View>()
        fun pinta(elegido: Int) {
            vistas.forEachIndexed { k, v ->
                val c = colores[k]
                val activo = c == elegido
                v.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    if (activo) setStroke(dp(3), if (Color.luminance(c) > 0.85f) ACENTO else TINTA)
                    else setStroke(dp(1), Color.rgb(214, 217, 222))
                }
                v.scaleX = if (activo) 1.08f else 1f
                v.scaleY = if (activo) 1.08f else 1f
            }
        }
        colores.forEachIndexed { k, c ->
            val v = View(this).apply {
                isClickable = true
                contentDescription = "Color " + (k + 1)
                setOnClickListener { pinta(c); al(c) }
            }
            vistas.add(v)
            fila.addView(v, LinearLayout.LayoutParams(dp(tam), dp(tam)).apply { rightMargin = dp(12) })
        }
        pinta(sel)
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(2), dp(4), dp(2), dp(4))
            clipToPadding = false
            addView(fila)
        }
    }

    private fun deslizador(min: Int, max: Int, valor: Int, etiqueta: (Int) -> String, al: (Int) -> Unit): LinearLayout {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val valorTxt = txt(etiqueta(valor), 14f, GRIS).apply { gravity = Gravity.END }
        val barra = SeekBar(this).apply {
            this.min = min
            this.max = max
            progress = valor.coerceIn(min, max)
            progressTintList = ColorStateList.valueOf(TINTA)
            thumbTintList = ColorStateList.valueOf(TINTA)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, delUsuario: Boolean) {
                    if (!delUsuario) return
                    al(p)
                    valorTxt.text = etiqueta(p)
                    aj.guardar(this@MainActivity)
                    muestra()
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) { cambio() }
            })
        }
        fila.addView(barra, LinearLayout.LayoutParams(0, dp(44), 1f))
        fila.addView(valorTxt, LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT))
        return fila
    }

    @Suppress("UseSwitchCompatOrMaterialCode")
    private fun interruptor(s: String, valor: Boolean, al: (Boolean) -> Unit) = Switch(this).apply {
        text = s
        textSize = 15f
        setTextColor(TINTA)
        isChecked = valor
        minHeight = dp(44)
        thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.WHITE, Color.rgb(250, 250, 250))
        )
        trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ACENTO, Color.rgb(200, 204, 210))
        )
        setOnCheckedChangeListener { _, v -> al(v) }
    }
}
