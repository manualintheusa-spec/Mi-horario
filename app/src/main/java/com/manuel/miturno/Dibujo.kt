package com.manuel.miturno

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Dibuja el widget completo como una imagen de cristal. */
class Dibujo(private val d: Float, private val aj: Ajustes, private val oscuro: Boolean) {

    private val t = aj.escala / 100f
    private val ink = if (oscuro) Color.rgb(21, 23, 26) else Color.WHITE
    private val soft = if (oscuro) Color.argb(170, 21, 23, 26) else Color.argb(210, 255, 255, 255)
    private val rojoCierre = Color.rgb(255, 92, 92)

    /** Colores para compañeros: cada nombre siempre recibe el mismo. */
    private val paleta = intArrayOf(
        Color.rgb(167, 139, 250), Color.rgb(245, 166, 35), Color.rgb(255, 122, 89),
        Color.rgb(240, 111, 164), Color.rgb(79, 209, 197), Color.rgb(255, 209, 102),
        Color.rgb(142, 197, 255), Color.rgb(195, 232, 141), Color.rgb(250, 160, 200),
        Color.rgb(120, 220, 140)
    )

    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(7f * d, BlurMaskFilter.Blur.NORMAL)
    }
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private val familia = when (aj.fuente) { 1 -> "sans-serif-condensed"; 2 -> "serif"; else -> "sans-serif" }
    private val normal: Typeface = Typeface.create(if (aj.fuente == 0) "sans-serif-medium" else familia, Typeface.NORMAL)
    private val negrita: Typeface = Typeface.create(familia, Typeface.BOLD)

    private fun alfa(c: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
    private fun aclara(c: Int, f: Float) = Color.rgb(
        (Color.red(c) + (255 - Color.red(c)) * f).roundToInt(),
        (Color.green(c) + (255 - Color.green(c)) * f).roundToInt(),
        (Color.blue(c) + (255 - Color.blue(c)) * f).roundToInt()
    )

    fun colorDe(nombre: String): Int = paleta[Math.floorMod(nombre.trim().lowercase().hashCode(), paleta.size)]

    // ---------- Texto ----------

    private fun prepara(size: Float, color: Int, bold: Boolean, align: Paint.Align) {
        tp.textSize = size * d * t
        tp.color = color
        tp.typeface = if (bold) negrita else normal
        tp.textAlign = align
        if (oscuro) tp.clearShadowLayer() else tp.setShadowLayer(2.2f * d, 0f, 0.6f * d, Color.argb(95, 0, 0, 0))
    }

    private fun texto(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, maxW: Float = 0f
    ) {
        prepara(size, color, bold, align)
        val s2 = if (maxW > 0f) TextUtils.ellipsize(s, tp, maxW, TextUtils.TruncateAt.END).toString() else s
        c.drawText(s2, x, y, tp)
    }

    private fun ancho(s: String, size: Float, bold: Boolean = false): Float {
        prepara(size, ink, bold, Paint.Align.LEFT)
        return tp.measureText(s)
    }

    // ---------- Cristal ----------

    private fun vidrio(
        c: Canvas, r: RectF, rad: Float,
        arriba: Int = alfa(aj.tinte, (aj.opacidad * 2.55f * 1.35f).roundToInt()),
        abajo: Int = alfa(aj.tinte, (aj.opacidad * 2.55f * 0.75f).roundToInt()),
        bordeFijo: Int? = null, grosor: Float = 1f, conSombra: Boolean = true
    ) {
        val R = min(rad * d, min(r.width(), r.height()) / 2f)
        if (conSombra) {
            // Sombra suave solo afuera del cristal, para que no lo oscurezca
            val p = Path().apply { addRoundRect(r, R, R, Path.Direction.CW) }
            c.save()
            c.clipOutPath(p)
            sombra.color = Color.argb(if (oscuro) 24 else 40, 0, 0, 0)
            c.drawRoundRect(RectF(r.left + 1f * d, r.top + 3f * d, r.right - 1f * d, r.bottom + 3f * d), R, R, sombra)
            c.restore()
        }
        relleno.shader = LinearGradient(0f, r.top, 0f, r.bottom, arriba, abajo, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, R, R, relleno)
        relleno.shader = null

        linea.strokeWidth = grosor * d
        linea.pathEffect = null
        if (bordeFijo != null) {
            linea.color = bordeFijo
        } else {
            val fuerte = if (oscuro) 230 else 175
            linea.shader = LinearGradient(0f, r.top, 0f, r.bottom,
                Color.argb(fuerte, 255, 255, 255), Color.argb(35, 255, 255, 255), Shader.TileMode.CLAMP)
        }
        val i = linea.strokeWidth / 2f
        c.drawRoundRect(RectF(r.left + i, r.top + i, r.right - i, r.bottom - i), R - i, R - i, linea)
        linea.shader = null
    }

    private fun colorHoras(h: Double): Int = when {
        h < aj.verdeMenos / 2.0 -> Color.rgb(88, 190, 80)
        h <= aj.rojoMas / 2.0 -> Color.rgb(246, 150, 40)
        else -> Color.rgb(232, 68, 68)
    }

    // ---------- Widget ----------

    fun dibujar(semana: List<Dia>, W: Int, H: Int, ahora: Calendar, ejemplo: Boolean = false): Bitmap {
        val bmp = Bitmap.createBitmap(max(W, 1), max(H, 1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val hoy = ahora.get(Calendar.DAY_OF_WEEK) - 1
        val nm = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)

        val pad = 6f * d
        val gap = 7f * d
        val diasH = if (aj.horasDias) min(48f * d * max(1f, t * 0.95f), H * 0.21f) else min(38f * d * max(1f, t), H * 0.17f)
        val conIconos = aj.iconos && H > 200f * d
        val iconosH = if (conIconos) min(46f * d, H * 0.17f) else 0f
        val radioDia = min(aj.radio * 0.7f, 16f)

        // Días de la semana
        val orden = if (aj.lunes) listOf(1, 2, 3, 4, 5, 6, 0) else listOf(0, 1, 2, 3, 4, 5, 6)
        val tw = (W - 2 * pad - 6 * gap) / 7f
        for ((pos, i) in orden.withIndex()) {
            val x = pad + pos * (tw + gap)
            val r = RectF(x, pad, x + tw, pad + diasH)
            val yo = semana[i].yo
            val esHoy = i == hoy
            if (yo == null) {
                vidrio(c, r, radioDia,
                    alfa(aj.tinte, (aj.opacidad * 1.1f).roundToInt()), alfa(aj.tinte, (aj.opacidad * 0.5f).roundToInt()),
                    bordeFijo = if (esHoy) ink else null, grosor = if (esHoy) 2f else 1f, conSombra = false)
            } else {
                val col = colorHoras(Horario.horas(yo))
                vidrio(c, r, radioDia, alfa(aclara(col, 0.12f), 190), alfa(col, 140),
                    bordeFijo = if (esHoy) ink else null, grosor = if (esHoy) 2f else 1f)
            }
            val letra = if (yo == null) alfa(ink, 105) else ink
            if (aj.horasDias) {
                texto(c, Horario.LET[i], r.centerX(), r.top + diasH * 0.5f, 15f, letra, true, Paint.Align.CENTER)
                texto(c, if (yo == null) "–" else Horario.horasTxt(yo), r.centerX(), r.top + diasH * 0.8f, 11f,
                    if (yo == null) alfa(ink, 105) else soft, false, Paint.Align.CENTER)
            } else {
                texto(c, Horario.LET[i], r.centerX(), r.centerY() + 5.5f * d * t, 15f, letra, true, Paint.Align.CENTER)
            }
        }

        // Cuadro de en medio
        val card = RectF(pad, pad + diasH + gap, W - pad, H - pad - (if (conIconos) iconosH + gap else 0f))
        vidrio(c, card, aj.radio.toFloat())

        // Espacios para íconos
        if (conIconos) {
            val iw = (W - 2 * pad - 3 * gap) / 4f
            for (k in 0..3) {
                val x = pad + k * (iw + gap)
                vidrio(c, RectF(x, H - pad - iconosH, x + iw, H - pad), min(aj.radio * 0.8f, 16f))
            }
        }

        val yoHoy = semana[hoy].yo
        if (yoHoy != null && nm >= yoHoy.s - aj.antes && nm < yoHoy.e) {
            reloj(c, card, semana[hoy], hoy, nm)
        } else {
            var i = hoy
            if (!(yoHoy != null && nm < yoHoy.s - aj.antes)) {
                i = -1
                for (k in 1..7) {
                    val j = (hoy + k) % 7
                    if (semana[j].yo != null) { i = j; break }
                }
            }
            val dia = if (i < 0) hoy else i
            val faltan = if (dia == hoy && yoHoy != null) yoHoy.s - nm
                else if (i >= 0) ((dia - hoy + 7) % 7) * 1440 + (semana[dia].yo?.s ?: 0) - nm else -1
            tarjeta(c, card, semana[dia], dia, hoy, ejemplo, faltan)
        }
        return bmp
    }

    private fun relativo(i: Int, hoy: Int) = when (i) {
        hoy -> "Hoy · " + Horario.NOM[i]
        (hoy + 1) % 7 -> "Mañana · " + Horario.NOM[i]
        else -> Horario.NOM[i]
    }

    /** Modo normal: día, hora de entrada y con quién trabajas. */
    private fun tarjeta(c: Canvas, card: RectF, dia: Dia, i: Int, hoy: Int, ejemplo: Boolean, faltan: Int) {
        val x = card.left + 16f * d
        val xR = card.right - 16f * d
        val maxW = card.width() - 32f * d
        var y = card.top + 26f * d * min(1.1f, t)
        val cuenta = if (faltan in 1..(36 * 60)) "en " + Horario.dur(faltan) else ""
        val wCuenta = if (cuenta.isEmpty()) 0f else ancho(cuenta, 12f, true) + 8f * d
        texto(c, relativo(i, hoy) + if (ejemplo) " · ejemplo" else "", x, y, 12f, soft, maxW = maxW - wCuenta)
        if (cuenta.isNotEmpty()) texto(c, cuenta, xR, y, 12f, ink, true, Paint.Align.RIGHT)

        val yo = dia.yo
        if (yo == null) {
            y += 32f * d * t
            texto(c, "Descansas", x, y, 26f, ink, true, maxW = maxW)
            y += 22f * d * t
            val txt = if (dia.gente.isEmpty()) "No hay turnos registrados" else "Trabajan: " + dia.gente.joinToString(", ") { it.nombre }
            texto(c, txt, x, y, 13f, soft, maxW = maxW)
            return
        }
        y += 32f * d * t
        texto(c, "Entras " + Horario.fmt(yo.s, aj.h24), x, y, 26f, ink, true, maxW = maxW)
        y += 22f * d * t
        val cierra = yo.e > dia.cierre
        texto(c, "Sales " + Horario.fmt(yo.e, aj.h24) + " · " + Horario.horasTxt(yo) + if (cierra) " · te toca cerrar" else "",
            x, y, 13f, soft, maxW = maxW)

        val con = dia.gente.filter { Horario.seTraslapa(it, yo) }.sortedBy { it.s }
        y += 14f * d
        if (y + 24f * d > card.bottom - 10f * d) return
        if (con.isEmpty()) {
            texto(c, "Nadie más registrado", x, y + 16f * d, 13f, soft)
            return
        }
        val chipH = 26f * d * max(1f, t * 0.95f)
        val base = chipH * 0.64f
        var cx = x
        var cy = y
        for ((k, p) in con.withIndex()) {
            val horas = Horario.corta(p.s, aj.h24) + "–" + Horario.corta(p.e, aj.h24)
            val wN = ancho(p.nombre, 13f, true)
            val wH = ancho(horas, 11f)
            val w = min(wN + wH + 34f * d, maxW)
            var nx = cx
            var ny = cy
            if (nx + w > x + maxW) { nx = x; ny += chipH + 6f * d }
            if (ny + chipH > card.bottom - 10f * d) {
                val resto = "+" + (con.size - k) + " más"
                if (cx + ancho(resto, 12f) <= x + maxW) texto(c, resto, cx + 2f * d, cy + base, 12f, soft, true)
                break
            }
            cx = nx
            cy = ny
            val r = RectF(cx, cy, cx + w, cy + chipH)
            vidrio(c, r, chipH / 2f / d, conSombra = false)
            relleno.color = colorDe(p.nombre)
            c.drawCircle(cx + 11f * d, cy + chipH / 2f, 3.5f * d, relleno)
            texto(c, p.nombre, cx + 20f * d, cy + base, 13f, ink, true, maxW = w - wH - 34f * d)
            texto(c, horas, cx + w - 10f * d, cy + base, 11f, soft, align = Paint.Align.RIGHT)
            cx += w + 6f * d
        }
    }

    /** Modo reloj: línea del día con tu barra y la de tus compañeros. */
    private fun reloj(c: Canvas, card: RectF, dia: Dia, hoy: Int, nm: Int) {
        val yo = dia.yo ?: return
        val x0 = card.left + 14f * d
        val xR = card.right - 16f * d
        val maxW = card.width() - 30f * d
        val cierra = yo.e > dia.cierre

        var y = card.top + 24f * d * min(1.1f, t)
        texto(c, "Hoy · " + Horario.NOM[hoy] + " · " + Horario.fmt(yo.s, aj.h24) + " a " + Horario.fmt(yo.e, aj.h24),
            x0, y, 12f, soft, maxW = maxW)
        val msg = when {
            nm < yo.s -> "Entras en " + Horario.dur(yo.s - nm)
            cierra && nm < dia.cierre -> "Faltan " + Horario.dur(dia.cierre - nm) + " para cerrar"
            else -> "Faltan " + Horario.dur(yo.e - nm) + " para salir"
        }
        y += 25f * d * t
        texto(c, msg, x0, y, 18f, ink, true, maxW = maxW)

        fun estado(p: Turno) = if (nm < p.s) 1 else if (nm >= p.e) 2 else 0
        val gente = dia.gente.filter { Horario.seTraslapa(it, yo) }
            .sortedWith(compareBy<Turno>({ estado(it) }, { it.s }))

        val lo = (floor((listOf(yo.s) + gente.map { it.s }).minOrNull()!! / 60.0) * 60).toInt()
        val hi = (ceil((listOf(yo.e) + gente.map { it.e }).maxOrNull()!! / 60.0) * 60).toInt()
        val rango = max(60, hi - lo).toFloat()

        val ticksH = 15f * d * t
        val top = y + 14f * d
        val bottom = card.bottom - 10f * d - ticksH
        val nc = 58f * d * min(1.15f, t)
        val tL = x0 + nc
        val tR = xR
        fun X(m: Int): Float = tL + (tR - tL) * ((m - lo) / rango).coerceIn(0f, 1f)

        // Líneas de cada hora y números de abajo
        val paso = if ((hi - lo) / 60 > 9) 2 else 1
        var m = lo
        while (m <= hi) {
            linea.color = alfa(ink, 40)
            linea.strokeWidth = 1f * d
            linea.pathEffect = null
            c.drawLine(X(m), top, X(m), bottom, linea)
            val esCierre = cierra && m == dia.cierre
            texto(c, Horario.corta(m, aj.h24), X(m), bottom + 13f * d * t, 11f, if (esCierre) rojoCierre else soft, esCierre, Paint.Align.CENTER)
            m += 60 * paso
        }

        // Tu barra: lo que falta (claro) y lo que ya pasó (fuerte)
        val meH = 22f * d
        val bTop = top + 2f * d
        val bBot = top + meH
        val rad = (bBot - bTop) / 2f
        val corte = max(yo.s, min(nm, yo.e))
        relleno.shader = LinearGradient(0f, bTop, 0f, bBot, alfa(aclara(aj.colorFalta, 0.2f), 185), alfa(aj.colorFalta, 140), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(X(yo.s), bTop, X(yo.e), bBot), rad, rad, relleno)
        if (corte > yo.s) {
            val fin = max(X(corte), X(yo.s) + 2 * rad)
            relleno.shader = LinearGradient(X(yo.s), 0f, fin, 0f, aclara(aj.colorTu, 0.25f), aj.colorTu, Shader.TileMode.CLAMP)
            c.drawRoundRect(RectF(X(yo.s), bTop, fin, bBot), rad, rad, relleno)
        }
        relleno.shader = null
        texto(c, "TÚ", x0, top + 16f * d, 13f, ink, true)
        if (aj.porcentaje && nm >= yo.s) {
            val pct = (((nm - yo.s).toFloat() / max(1, yo.e - yo.s)) * 100).roundToInt().coerceIn(0, 100)
            val s = "$pct%"
            if (X(yo.e) - X(yo.s) > ancho(s, 11f, true) + 3 * rad) {
                texto(c, s, X(yo.e) - rad * 0.8f, top + 16f * d, 11f, Color.WHITE, true, Paint.Align.RIGHT)
            }
        }

        // Compañeros
        val filaH = 16f * d * max(1f, t * 0.95f)
        val disponible = bottom - (top + meH + 5f * d)
        val maxFilas = max(0, floor(disponible / filaH).toInt())
        val mostrar = if (gente.size <= maxFilas) gente else gente.take(max(0, maxFilas - 1))
        var ry = top + meH + 5f * d
        for (p in mostrar) {
            val st = estado(p)
            val a = when (st) { 0 -> 255; 1 -> 95; else -> 50 }
            val base = if (st == 2) Color.rgb(160, 160, 160) else colorDe(p.nombre)
            texto(c, p.nombre, x0, ry + filaH * 0.72f, 12f, alfa(ink, if (st == 0) 255 else if (st == 1) 150 else 90), maxW = nc - 6f * d)
            relleno.color = alfa(base, a)
            c.drawRoundRect(RectF(X(p.s), ry + filaH / 2 - 3f * d, X(p.e), ry + filaH / 2 + 3f * d), 3f * d, 3f * d, relleno)
            ry += filaH
        }
        val extra = gente.size - mostrar.size
        if (extra > 0) texto(c, "+$extra más", x0, ry + filaH * 0.72f, 12f, soft)
        if (gente.isEmpty() && ry + filaH < bottom) texto(c, "Nadie más registrado", tL, ry + filaH * 0.72f, 12f, soft)

        // Línea de cierre (solo si te toca cerrar)
        if (cierra) {
            linea.color = rojoCierre
            linea.strokeWidth = 2f * d
            linea.pathEffect = DashPathEffect(floatArrayOf(4f * d, 3.5f * d), 0f)
            c.drawLine(X(dia.cierre), top - 5f * d, X(dia.cierre), bottom, linea)
            linea.pathEffect = null
        }
        // Línea de "ahora" con su puntito
        if (nm in lo..hi) {
            linea.color = ink
            linea.strokeWidth = 2f * d
            c.drawLine(X(nm), top - 3f * d, X(nm), bottom, linea)
            relleno.color = ink
            c.drawCircle(X(nm), top - 4f * d, 3.5f * d, relleno)
        }
    }
}
