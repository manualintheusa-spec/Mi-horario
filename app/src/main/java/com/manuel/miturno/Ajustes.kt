package com.manuel.miturno

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build

/** Todo lo que se puede personalizar del widget. */
data class Ajustes(
    var tinte: Int = Color.WHITE,
    var opacidad: Int = 18,
    var radio: Int = 18,
    var letras: Int = 0,          // 0 automático, 1 claras, 2 oscuras
    var fuente: Int = 0,          // 0 moderna, 1 compacta, 2 clásica
    var escala: Int = 100,        // % del tamaño del texto
    var colorTu: Int = 0xFF2F7CF6.toInt(),
    var colorFalta: Int = 0xFF6EE1C8.toInt(),
    var verdeMenos: Int = 8,      // en medias horas: 8 = 4 h
    var rojoMas: Int = 12,        // en medias horas: 12 = 6 h
    var antes: Int = 120,         // minutos antes de entrar para el modo reloj
    var h24: Boolean = false,
    var lunes: Boolean = false,
    var horasDias: Boolean = true,
    var iconos: Boolean = true,
    var porcentaje: Boolean = true,
    var fondoVista: Int = 0xFF2F4A33.toInt()
) {
    fun guardar(ctx: Context) {
        Datos.prefs(ctx).edit()
            .putInt("tinte", tinte).putInt("opacidad", opacidad).putInt("radio", radio)
            .putInt("letras", letras).putInt("fuente", fuente).putInt("escala", escala)
            .putInt("colorTu", colorTu).putInt("colorFalta", colorFalta)
            .putInt("verdeMenos", verdeMenos).putInt("rojoMas", rojoMas).putInt("antes", antes)
            .putBoolean("h24", h24).putBoolean("lunes", lunes).putBoolean("horasDias", horasDias)
            .putBoolean("iconos", iconos).putBoolean("porcentaje", porcentaje)
            .putInt("fondoVista", fondoVista)
            .apply()
    }

    /** Decide si las letras van oscuras. En automático lo decide según el fondo de pantalla. */
    fun letrasOscuras(ctx: Context): Boolean = when (letras) {
        1 -> false
        2 -> true
        else -> fondoClaro(ctx)
    }

    private fun fondoClaro(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return false
        return try {
            val wc: WallpaperColors = WallpaperManager.getInstance(ctx)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (wc.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
            } else {
                Color.luminance(wc.primaryColor.toArgb()) > 0.6f
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun cargar(ctx: Context): Ajustes {
            val p = Datos.prefs(ctx)
            val d = Ajustes()
            return Ajustes(
                tinte = p.getInt("tinte", d.tinte),
                opacidad = p.getInt("opacidad", d.opacidad),
                radio = p.getInt("radio", d.radio),
                letras = p.getInt("letras", if (p.getBoolean("oscuro", false)) 2 else d.letras),
                fuente = p.getInt("fuente", d.fuente),
                escala = p.getInt("escala", d.escala),
                colorTu = p.getInt("colorTu", d.colorTu),
                colorFalta = p.getInt("colorFalta", d.colorFalta),
                verdeMenos = p.getInt("verdeMenos", d.verdeMenos),
                rojoMas = p.getInt("rojoMas", d.rojoMas),
                antes = p.getInt("antes", d.antes),
                h24 = p.getBoolean("h24", d.h24),
                lunes = p.getBoolean("lunes", d.lunes),
                horasDias = p.getBoolean("horasDias", d.horasDias),
                iconos = p.getBoolean("iconos", d.iconos),
                porcentaje = p.getBoolean("porcentaje", d.porcentaje),
                fondoVista = p.getInt("fondoVista", d.fondoVista)
            )
        }
    }
}
