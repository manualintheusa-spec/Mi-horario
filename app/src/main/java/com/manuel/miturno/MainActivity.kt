package com.manuel.miturno

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var vista: ImageView
    private var px = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        px = resources.displayMetrics.density
        fun dp(v: Int) = (v * px).toInt()
        val prefs = Datos.prefs(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(36))
        }
        fun agrega(v: android.view.View, arriba: Int = 12) {
            col.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(arriba) })
        }

        agrega(TextView(this).apply { text = "Mi turno"; textSize = 26f; setTypeface(typeface, Typeface.BOLD) }, 0)
        agrega(TextView(this).apply { text = "Así se ve tu widget ahora:"; textSize = 14f })

        vista = ImageView(this).apply {
            adjustViewBounds = true
            background = GradientDrawable().apply { setColor(Color.rgb(47, 74, 51)); cornerRadius = 18 * px }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        agrega(vista, 8)

        agrega(TextView(this).apply {
            text = "Para ponerlo en tu pantalla: mantén presionada la pantalla de inicio → Widgets → Mi turno."
            textSize = 14f
        })

        agrega(TextView(this).apply { text = "Tu horario"; textSize = 20f; setTypeface(typeface, Typeface.BOLD) }, 24)
        agrega(TextView(this).apply {
            text = "Pega aquí el texto que te da Claude. Una línea por turno, por ejemplo:\n" +
                "L TU 16:00-22:00\nL Ana 15:00-21:00\n" +
                "Días: D L M X J V S (X = miércoles). Cierre: CIERRE 21:00 o por día: V CIERRE 22:00"
            textSize = 14f
        }, 6)

        val campo = EditText(this).apply {
            setText(Datos.texto(this@MainActivity))
            typeface = Typeface.MONOSPACE
            textSize = 15f
            minLines = 10
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(false)
        }
        agrega(campo, 8)

        val error = TextView(this).apply { setTextColor(Color.rgb(200, 40, 40)); textSize = 14f }
        agrega(error, 4)

        val botones = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val guardar = Button(this).apply { text = "Guardar" }
        val ejemplo = Button(this).apply { text = "Usar ejemplo" }
        botones.addView(guardar)
        botones.addView(ejemplo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) })
        agrega(botones, 4)

        val oscuro = CheckBox(this).apply {
            text = "Letras oscuras (para fondos de pantalla claros)"
            isChecked = Datos.oscuro(this@MainActivity)
        }
        agrega(oscuro, 20)

        guardar.setOnClickListener {
            val v = campo.text.toString()
            val r = Horario.parse(v)
            when {
                v.isBlank() -> error.text = "Pega tu horario primero."
                r.errores.isNotEmpty() -> error.text = "Revisa la línea " + r.errores.joinToString(", ") + ". Ejemplo: L Ana 16:00-21:00"
                r.semana.none { it.yo != null } -> error.text = "Falta tu turno. Usa TU como nombre, por ejemplo: L TU 16:00-22:00"
                else -> {
                    error.text = ""
                    prefs.edit().putString("texto", v).apply()
                    MiTurnoWidget.actualizarTodo(this)
                    muestra()
                    Toast.makeText(this, "Horario guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ejemplo.setOnClickListener { campo.setText(Horario.EJEMPLO); error.text = "" }
        oscuro.setOnCheckedChangeListener { _, marcado ->
            prefs.edit().putBoolean("oscuro", marcado).apply()
            MiTurnoWidget.actualizarTodo(this)
            muestra()
        }

        setContentView(ScrollView(this).apply { addView(col) })
        muestra()
    }

    override fun onResume() {
        super.onResume()
        muestra()
    }

    private fun muestra() {
        val dm = resources.displayMetrics
        val w = dm.widthPixels - (52 * dm.density).toInt()
        val h = (w * 0.78f).toInt()
        vista.setImageBitmap(
            Dibujo(dm.density, Datos.oscuro(this)).dibujar(Datos.semana(this), w, h, Calendar.getInstance(), Datos.esEjemplo(this))
        )
    }
}
