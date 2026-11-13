package app.sphera.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.sphera.MainActivity
import app.sphera.R
import app.sphera.Routes
import app.sphera.returnpoint.SpheraDb
import kotlinx.coroutines.runBlocking

/** Muestra en la pantalla de inicio lo que apuntaste en el Punto de Retorno; se actualiza al guardarlo o cerrarlo. */
class ReturnWidget : AppWidgetProvider() {
    companion object {
        private fun draw(ctx: Context, manager: AppWidgetManager, ids: IntArray, text: String?) {
            val views = RemoteViews(ctx.packageName, R.layout.widget_return)
            views.setTextViewText(R.id.widget_return_text, text ?: ctx.getString(R.string.widget_return_empty))
            views.setTextColor(R.id.widget_return_text, if (text != null) 0xFFE7E9EE.toInt() else 0xFF6E7687.toInt())
            // Con algo apuntado, abrir Sphera ya muestra la tarjeta para retomarlo; sin nada, se abre para apuntar.
            val click = if (text != null) {
                PendingIntent.getActivity(
                    ctx, 20,
                    Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } else {
                linkIntent(ctx, Routes.LINK_RETURN_NEW, 21)
            }
            views.setOnClickPendingIntent(R.id.widget_return_root, click)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun current(ctx: Context): String? =
            runCatching { runBlocking { SpheraDb.get(ctx).returnPoints().get() } }.getOrNull()
                ?.let { p -> listOf(p.nextStep, p.contextLocation).firstOrNull { it.isNotBlank() } }

        /** Llamar tras guardar o cerrar el Punto de Retorno (desde un hilo de fondo o una corrutina). */
        fun refresh(ctx: Context) {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(ctx, ReturnWidget::class.java))
            if (ids.isNotEmpty()) draw(ctx, manager, ids, current(ctx))
        }
    }

    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Thread {
            try {
                draw(ctx, manager, ids, current(ctx))
            } finally {
                pending.finish()
            }
        }.start()
    }
}
