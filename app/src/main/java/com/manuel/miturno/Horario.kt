package com.manuel.miturno

import android.content.Context

data class Turno(val nombre: String, val s: Int, val e: Int)

class Dia {
    var yo: Turno? = null
    val gente = mutableListOf<Turno>()
    var cierre: Int = -1
}

class Resultado(val semana: List<Dia>, val errores: List<Int>)

object Horario {
    val COD = listOf("D", "L", "M", "X", "J", "V", "S")
    val LET = listOf("D", "L", "M", "Mi", "J", "V", "S")
    val NOM = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    val EJEMPLO = listOf(
        "# Horario de ejemplo",
        "CIERRE 21:00",
        "L TU 10:00-17:30",
        "L Ana 9:00-15:00",
        "L Luis 11:00-18:00",
        "X TU 17:00-22:00",
        "X Ana 15:00-21:00",
        "X Carlos 16:00-22:00",
        "X Rosa 18:00-22:00",
        "X Luis 12:00-17:30",
        "J TU 11:00-14:30",
        "J Beto 10:00-15:00",
        "J Rosa 11:00-16:00",
        "V TU 15:00-22:00",
        "V Carlos 16:00-22:00",
        "V Ana 11:00-17:00",
        "S TU 12:00-18:00",
        "S Luis 12:00-20:00",
        "S Beto 14:00-22:00"
    ).joinToString("\n")

    private val reHora = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$", RegexOption.IGNORE_CASE)
    private val reTu = Regex("^t[uú]$", RegexOption.IGNORE_CASE)

    fun hm(t: String): Int? {
        val m = reHora.find(t.trim()) ?: return null
        var h = m.groupValues[1].toInt()
        val mi = m.groupValues[2].ifEmpty { "0" }.toInt()
        val ap = m.groupValues[3].lowercase()
        if (ap == "pm" && h < 12) h += 12
        if (ap == "am" && h == 12) h = 0
        if (h > 24 || mi > 59) return null
        return h * 60 + mi
    }

    fun parse(texto: String): Resultado {
        val semana = List(7) { Dia() }
        val errores = mutableListOf<Int>()
        var cierreGeneral = 21 * 60
        texto.split("\n").forEachIndexed { n, raw ->
            val linea = raw.trim()
            if (linea.isEmpty() || linea.startsWith("#")) return@forEachIndexed
            val t = linea.split(Regex("\\s+"))
            var c = t[0].uppercase()
            if (c == "MI") c = "X"
            if (c == "CIERRE") {
                val v = hm(t.getOrElse(1) { "" })
                if (v == null) errores.add(n + 1) else cierreGeneral = v
                return@forEachIndexed
            }
            val i = COD.indexOf(c)
            if (i < 0 || t.size < 3) { errores.add(n + 1); return@forEachIndexed }
            if (t[1].uppercase() == "CIERRE") {
                val v = hm(t[2])
                if (v == null) errores.add(n + 1) else semana[i].cierre = v
                return@forEachIndexed
            }
            val r = t.last().split("-")
            val s = hm(r.getOrElse(0) { "" })
            var e = hm(r.getOrElse(1) { "" })
            if (s == null || e == null) { errores.add(n + 1); return@forEachIndexed }
            if (e <= s) e += 1440
            val nombre = t.subList(1, t.size - 1).joinToString(" ")
            if (reTu.matches(nombre)) semana[i].yo = Turno("TÚ", s, e)
            else semana[i].gente.add(Turno(nombre, s, e))
        }
        semana.forEach { if (it.cierre < 0) it.cierre = cierreGeneral }
        return Resultado(semana, errores)
    }

    fun fmt(m0: Int, h24: Boolean = false): String {
        val m = ((m0 % 1440) + 1440) % 1440
        var h = m / 60
        val mi = m % 60
        if (h24) return "$h:${mi.toString().padStart(2, '0')}"
        val ap = if (h >= 12) "pm" else "am"
        h = if (h % 12 == 0) 12 else h % 12
        return "$h:${mi.toString().padStart(2, '0')} $ap"
    }

    fun corta(m0: Int, h24: Boolean = false): String {
        val m = ((m0 % 1440) + 1440) % 1440
        val h = if (h24) m / 60 else if ((m / 60) % 12 == 0) 12 else (m / 60) % 12
        val mi = m % 60
        return if (mi != 0) "$h:${mi.toString().padStart(2, '0')}" else "$h"
    }

    fun dur(m0: Int): String {
        val m = maxOf(0, m0)
        val h = m / 60
        val mi = m % 60
        return when {
            h > 0 && mi > 0 -> "$h h $mi min"
            h > 0 -> "$h h"
            else -> "$mi min"
        }
    }

    fun horas(t: Turno): Double = Math.round((t.e - t.s) / 6.0) / 10.0

    fun horasTxt(t: Turno): String {
        val h = horas(t)
        return if (h == Math.floor(h)) "${h.toInt()}h" else "${h}h"
    }

    fun seTraslapa(p: Turno, yo: Turno) = p.s < yo.e && p.e > yo.s
}

object Datos {
    const val PREFS = "miturno"

    fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun texto(ctx: Context): String = prefs(ctx).getString("texto", null) ?: Horario.EJEMPLO
    fun esEjemplo(ctx: Context): Boolean = prefs(ctx).getString("texto", null) == null
    fun semana(ctx: Context): List<Dia> = Horario.parse(texto(ctx)).semana
}
