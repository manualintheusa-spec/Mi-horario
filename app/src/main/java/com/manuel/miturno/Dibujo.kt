package com.manuel.miturno

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Dibuja el widget completo como una imagen de cristal transparente. */
class Dibujo(private val d: Float, private val oscuro: Boolean) {

    private val ink = if (oscuro) Color.rgb(21, 23, 26) else Color.WHITE
    private val soft = if (oscuro) Color.argb(175, 21, 23, 26) else Color.argb(205, 255, 255, 255)
    private val vidrioColor = if (oscuro) Color.argb(105, 255, 255, 255) else Color.argb(46, 255, 255, 255)
    private val vidrioBorde = if (oscuro) Color.argb(210, 255, 255, 255) else Color.argb(115, 255, 255, 255)
    private val apagado = if (oscuro) Color.argb(50, 255, 255, 255) else Color.argb(20, 255, 255, 255)
    private val rojoCierre = Color.rgb(255, 90, 90)

    private val colores = intArrayOf(
        Color.rgb(167, 139, 250), Color.rgb(245, 166, 35), Color.rgb(255, 122, 89),
        Color.rgb(240, 111, 164), Color.rgb(79, 209, 197), Color.rgb(255, 209, 102),
        Color.rgb(142, 197, 255), Color.rgb(195, 232, 141)
    )

    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val normal = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val negrita = Typeface.create("sans-serif", Typeface.BOLD)

    private fun alfa(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun prepara(size: Float, color: Int, bold: Boolean, align: Paint.Align) {
        tp.textSize = size * d
        tp.color = color
        tp.typeface = if (bold) negrita else normal
        tp.textAlign = align
        if (oscuro) tp.clearShadowLayer() else tp.setShadowLayer(2f * d, 0f, 0.6f * d, Color.argb(100, 0, 0, 0))
    }

    private fun texto(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, maxW: Float = 0f
    ) {
        prepara(size, color, bold, align)
        val t = if (maxW > 0f) TextUtils.ellipsize(s, tp, maxW, TextUtils.TruncateAt.END).toString() else s
        c.drawText(t, x, y, tp)
    }

    private fun ancho(s: String, size: Float, bold: Boolean = false): Float {
        prepara(size, ink, bold, Paint.Align.LEFT)
        return tp.measureText(s)
    }

    private fun vidrio(c: Canvas, r: RectF, rad: Float, color: Int = vidrioColor, borde: Int = vidrioBorde, grosor: Float = 1f) {
        relleno.color = color
        c.drawRoundRect(r, rad * d, rad * d, relleno)
        linea.color = borde
        linea.strokeWidth = grosor * d
        linea.pathEffect = null
        c.drawRoundRect(r, rad * d, rad * d, linea)
    }

    private fun colorHoras(h: Double) = when {
        h < 4 -> Color.argb(150, 88, 190, 80)
        h <= 6 -> Color.argb(160, 246, 150, 40)
        else -> Color.argb(160, 232, 68, 68)
    }

    fun dibujar(semana: List<Dia>, W: Int, H: Int, ahora: Calendar, ejemplo: Boolean = false): Bitmap {
        val bmp = Bitmap.createBitmap(max(W, 1), max(H, 1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val hoy = ahora.get(Calendar.DAY_OF_WEEK) - 1
        val nm = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)

        val pad = 3f * d
        val gap = 6f * d
        val diasH = min(46f * d, H * 0.2f)
        val iconosH = min(44f * d, H * 0.17f)

        // Días de la semana
        val tw = (W - 2 * pad - 6 * gap) / 7f
        for (i in 0..6) {
            val r = RectF(pad + i * (tw + gap), pad, pad + i * (tw + gap) + tw, pad + diasH)
            val yo = semana[i].yo
            val fondo = if (yo == null) apagado else colorHoras(Horario.horas(yo))
            val esHoy = i == hoy
            vidrio(c, r, 12f, fondo, if (esHoy) ink else vidrioBorde, if (esHoy) 2f else 1f)
            val col = if (yo == null) alfa(ink, 110) else ink
            texto(c, Horario.LET[i], r.centerX(), r.top + diasH * 0.5f, 15f, col, true, Paint.Align.CENTER)
            texto(c, if (yo == null) "–" else Horario.horasTxt(yo), r.centerX(), r.top + diasH * 0.82f, 11f,
                if (yo == null) alfa(ink, 110) else soft, false, Paint.Align.CENTER)
        }

        // Cuadro de en medio
        val card = RectF(pad, pad + diasH + gap, W - pad, H - pad - iconosH - gap)
        vidrio(c, card, 18f)

        // Espacios para íconos (vacíos por ahora)
        val iw = (W - 2 * pad - 3 * gap) / 4f
        for (k in 0..3) {
            val r = RectF(pad + k * (iw + gap), H - pad - iconosH, pad + k * (iw + gap) + iw, H - pad)
            vidrio(c, r, 14f)
        }

        val yoHoy = semana[hoy].yo
        if (yoHoy != null && nm >= yoHoy.s - 120 && nm < yoHoy.e) {
            reloj(c, card, semana[hoy], hoy, nm)
        } else {
            var i = hoy
            if (!(yoHoy != null && nm < yoHoy.s - 120)) {
                i = -1
                for (k in 1..7) {
                    val j = (hoy + k) % 7
                    if (semana[j].yo != null) { i = j; break }
                }
            }
            tarjeta(c, card, semana[if (i < 0) hoy else i], if (i < 0) hoy else i, hoy, ejemplo)
        }
        return bmp
    }

    private fun relativo(i: Int, hoy: Int) = when (i) {
        hoy -> "Hoy · " + Horario.NOM[i]
        (hoy + 1) % 7 -> "Mañana · " + Horario.NOM[i]
        else -> Horario.NOM[i]
    }

    /** Modo normal: día, hora de entrada y con quién trabajas. */
    private fun tarjeta(c: Canvas, card: RectF, dia: Dia, i: Int, hoy: Int, ejemplo: Boolean) {
        val x = card.left + 14f * d
        val maxW = card.width() - 28f * d
        var y = card.top + 24f * d
        texto(c, relativo(i, hoy) + if (ejemplo) " · ejemplo" else "", x, y, 12f, soft, maxW = maxW)
        val yo = dia.yo
        if (yo == null) {
            y += 30f * d
            texto(c, "Descansas", x, y, 25f, ink, true, maxW = maxW)
            y += 22f * d
            val txt = if (dia.gente.isEmpty()) "No hay turnos registrados" else "Trabajan: " + dia.gente.joinToString(", ") { it.nombre }
            texto(c, txt, x, y, 13f, soft, maxW = maxW)
            return
        }
        y += 30f * d
        texto(c, "Entras " + Horario.fmt(yo.s), x, y, 25f, ink, true, maxW = maxW)
        y += 21f * d
        val cierra = yo.e > dia.cierre
        texto(c, "Sales " + Horario.fmt(yo.e) + " · " + Horario.horasTxt(yo) + if (cierra) " · te toca cerrar" else "",
            x, y, 13f, soft, maxW = maxW)
        y += 24f * d
        texto(c, "Trabajas con", x, y, 12f, soft)
        y += 8f * d

        val con = dia.gente.filter { Horario.seTraslapa(it, yo) }.sortedBy { it.s }
        if (con.isEmpty()) {
            texto(c, "Nadie más registrado", x, y + 16f * d, 13f, soft)
            return
        }
        val chipH = 24f * d
        var cx = x
        var cy = y
        for ((k, p) in con.withIndex()) {
            val horas = Horario.corta(p.s) + "–" + Horario.corta(p.e)
            val wN = ancho(p.nombre, 13f, true)
            val wH = ancho(horas, 11f)
            val w = min(wN + wH + 22f * d, maxW)
            var nx = cx
            var ny = cy
            if (nx + w > x + maxW) { nx = x; ny += chipH + 6f * d }
            if (ny + chipH > card.bottom - 8f * d) {
                val resto = "+" + (con.size - k) + " más"
                if (cx + ancho(resto, 12f) <= x + maxW) texto(c, resto, cx, cy + 16.5f * d, 12f, soft)
                break
            }
            cx = nx
            cy = ny
            vidrio(c, RectF(cx, cy, cx + w, cy + chipH), 12f)
            texto(c, p.nombre, cx + 9f * d, cy + 16.5f * d, 13f, ink, true, maxW = w - wH - 22f * d)
            texto(c, horas, cx + w - 9f * d, cy + 16.5f * d, 11f, soft, align = Paint.Align.RIGHT)
            cx += w + 6f * d
        }
    }

    /** Modo reloj: línea del día con tu barra y la de tus compañeros. */
    private fun reloj(c: Canvas, card: RectF, dia: Dia, hoy: Int, nm: Int) {
        val yo = dia.yo ?: return
        val x0 = card.left + 12f * d
        val xR = card.right - 14f * d
        val maxW = card.width() - 26f * d
        val cierra = yo.e > dia.cierre

        var y = card.top + 22f * d
        texto(c, "Hoy · " + Horario.NOM[hoy] + " · " + Horario.fmt(yo.s) + " a " + Horario.fmt(yo.e), x0, y, 12f, soft, maxW = maxW)
        val msg = when {
            nm < yo.s -> "Entras en " + Horario.dur(yo.s - nm)
            cierra && nm < dia.cierre -> "Faltan " + Horario.dur(dia.cierre - nm) + " para cerrar"
            else -> "Faltan " + Horario.dur(yo.e - nm) + " para salir"
        }
        y += 24f * d
        texto(c, msg, x0, y, 18f, ink, true, maxW = maxW)

        fun estado(p: Turno) = if (nm < p.s) 1 else if (nm >= p.e) 2 else 0
        val gente = dia.gente.filter { Horario.seTraslapa(it, yo) }
            .sortedWith(compareBy<Turno>({ estado(it) }, { it.s }))

        val lo = (floor((listOf(yo.s) + gente.map { it.s }).minOrNull()!! / 60.0) * 60).toInt()
        val hi = (Math.ceil((listOf(yo.e) + gente.map { it.e }).maxOrNull()!! / 60.0) * 60).toInt()
        val rango = max(60, hi - lo).toFloat()

        val ticksH = 14f * d
        val top = y + 12f * d
        val bottom = card.bottom - 10f * d - ticksH
        val nc = 56f * d
        val tL = x0 + nc
        val tR = xR
        fun X(m: Int): Float = tL + (tR - tL) * ((m - lo) / rango).coerceIn(0f, 1f)

        // Líneas de cada hora y números de abajo
        val paso = if ((hi - lo) / 60 > 9) 2 else 1
        var m = lo
        linea.pathEffect = null
        while (m <= hi) {
            linea.color = alfa(ink, 45)
            linea.strokeWidth = 1f * d
            c.drawLine(X(m), top, X(m), bottom, linea)
            val esCierre = cierra && m == dia.cierre
            texto(c, Horario.corta(m), X(m), bottom + 13f * d, 11f, if (esCierre) rojoCierre else soft, esCierre, Paint.Align.CENTER)
            m += 60 * paso
        }

        // Tu barra
        val meH = 22f * d
        val corte = max(yo.s, min(nm, yo.e))
        relleno.color = Color.argb(150, 110, 225, 200)
        c.drawRoundRect(RectF(X(yo.s), top + 2f * d, X(yo.e), top + meH), 10f * d, 10f * d, relleno)
        if (corte > yo.s) {
            relleno.color = Color.rgb(47, 124, 246)
            c.drawRoundRect(RectF(X(yo.s), top + 2f * d, max(X(corte), X(yo.s) + 20f * d), top + meH), 10f * d, 10f * d, relleno)
        }
        texto(c, "TÚ", x0, top + 16f * d, 13f, ink, true)

        // Compañeros
        val filaH = 16f * d
        val disponible = bottom - (top + meH + 4f * d)
        val maxFilas = max(0, floor(disponible / filaH).toInt())
        val mostrar = if (gente.size <= maxFilas) gente else gente.take(max(0, maxFilas - 1))
        var ry = top + meH + 4f * d
        for ((k, p) in mostrar.withIndex()) {
            val st = estado(p)
            val a = when (st) { 0 -> 255; 1 -> 95; else -> 55 }
            val base = if (st == 2) Color.rgb(160, 160, 160) else colores[k % colores.size]
            texto(c, p.nombre, x0, ry + filaH * 0.72f, 12f, alfa(ink, if (st == 0) 255 else if (st == 1) 150 else 90), maxW = nc - 6f * d)
            relleno.color = alfa(base, a)
            c.drawRoundRect(RectF(X(p.s), ry + filaH / 2 - 3f * d, X(p.e), ry + filaH / 2 + 3f * d), 3f * d, 3f * d, relleno)
            ry += filaH
        }
        val extra = gente.size - mostrar.size
        if (extra > 0) texto(c, "+$extra más", x0, ry + filaH * 0.72f, 12f, soft)
        if (gente.isEmpty()) texto(c, "Nadie más registrado", tL, ry + filaH * 0.72f, 12f, soft)

        // Línea de cierre (solo si te toca cerrar)
        if (cierra) {
            linea.color = rojoCierre
            linea.strokeWidth = 2f * d
            linea.pathEffect = DashPathEffect(floatArrayOf(4f * d, 3f * d), 0f)
            c.drawLine(X(dia.cierre), top - 4f * d, X(dia.cierre), bottom, linea)
            linea.pathEffect = null
        }
        // Línea de "ahora"
        if (nm in lo..hi) {
            linea.color = ink
            linea.strokeWidth = 2f * d
            c.drawLine(X(nm), top - 4f * d, X(nm), bottom, linea)
        }
    }
}
