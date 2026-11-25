package com.manuel.miturno

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.min

class MiTurnoWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        actualizarTodo(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opciones: Bundle) {
        actualizarTodo(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACCION_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_BOOT_COMPLETED ->
                actualizarTodo(ctx)
        }
    }

    override fun onDisabled(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pendiente(ctx))
    }

    companion object {
        const val ACCION_TICK = "com.manuel.miturno.TICK"

        private fun pendiente(ctx: Context): PendingIntent {
            val i = Intent(ctx, MiTurnoWidget::class.java).setAction(ACCION_TICK)
            return PendingIntent.getBroadcast(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun actualizarTodo(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, MiTurnoWidget::class.java))
            if (ids.isEmpty()) return
            val semana = Datos.semana(ctx)
            val ahora = Calendar.getInstance()
            val dm = ctx.resources.displayMetrics
            val aj = Ajustes.cargar(ctx)
            val oscuro = aj.letrasOscuras(ctx)
            val abrir = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            for (id in ids) {
                val op = mgr.getAppWidgetOptions(id)
                var wDp = op.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                var hDp = op.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                if (wDp <= 0) wDp = 320
                if (hDp <= 0) hDp = 260
                // Limita el tamaño de la imagen para no gastar memoria
                val escala = min(1f, 1300f / (maxOf(wDp, hDp) * dm.density))
                val w = (wDp * dm.density * escala).toInt()
                val h = (hDp * dm.density * escala).toInt()
                val bmp = Dibujo(dm.density * escala, aj, oscuro).dibujar(semana, w, h, ahora, Datos.esEjemplo(ctx))
                val rv = RemoteViews(ctx.packageName, R.layout.widget)
                rv.setImageViewBitmap(R.id.img, bmp)
                rv.setOnClickPendingIntent(R.id.img, abrir)
                mgr.updateAppWidget(id, rv)
            }
            programar(ctx, semana, ahora, aj.antes)
        }

        /**
         * Programa la siguiente actualización.
         * Cada minuto solo desde 2 h antes de tu entrada hasta tu salida; si no, cada 30 min.
         * Usa una alarma que NO despierta el celular: con la pantalla apagada no hace nada.
         */
        private fun programar(ctx: Context, semana: List<Dia>, ahora: Calendar, antes: Int) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            val nm = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)
            val base = ahora.timeInMillis - ahora.get(Calendar.SECOND) * 1000L - ahora.get(Calendar.MILLISECOND)
            val yo = semana[ahora.get(Calendar.DAY_OF_WEEK) - 1].yo

            var minutos = 30
            if (yo != null && nm >= yo.s - antes && nm < yo.e) minutos = 1
            else if (yo != null && nm < yo.s - antes) minutos = min(minutos, yo.s - antes - nm)
            minutos = min(minutos, 1440 - nm) // cambio de día
            if (minutos < 1) minutos = 1
            val cuando = base + minutos * 60_000L + 500L

            val pi = pendiente(ctx)
            val exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (exacta) am.setExact(AlarmManager.RTC, cuando, pi) else am.set(AlarmManager.RTC, cuando, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC, cuando, pi)
            }
        }
    }
}
