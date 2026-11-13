package app.sphera.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import app.sphera.MainActivity
import app.sphera.R

/** Abre Sphera desde cero en la pantalla pedida, aunque ya estuviera abierta en otra. */
internal fun linkIntent(ctx: Context, uri: String, code: Int): PendingIntent = PendingIntent.getActivity(
    ctx, code,
    Intent(Intent.ACTION_VIEW, Uri.parse(uri), ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** Widget de accesos: hasta cuatro, elegidos al ponerlo (y cambiables manteniéndolo pulsado en Android 12+). */
class SpheraWidget : AppWidgetProvider() {
    companion object {
        private val SLOTS = intArrayOf(R.id.widget_slot0, R.id.widget_slot1, R.id.widget_slot2, R.id.widget_slot3)

        fun draw(ctx: Context, manager: AppWidgetManager, id: Int) {
            val actions = WidgetActions.forWidget(ctx, id)
            val views = RemoteViews(ctx.packageName, R.layout.widget_sphera)
            SLOTS.forEachIndexed { i, slot ->
                val action = actions.getOrNull(i)
                if (action == null) {
                    views.setViewVisibility(slot, View.GONE)
                } else {
                    views.setViewVisibility(slot, View.VISIBLE)
                    views.setTextViewText(slot, action.emoji + "\n" + ctx.getString(action.label))
                    views.setTextColor(slot, action.color)
                    // Código distinto por widget y casilla para que cada botón abra lo suyo.
                    views.setOnClickPendingIntent(slot, linkIntent(ctx, action.link, 1000 + id * 10 + i))
                }
            }
            manager.updateAppWidget(id, views)
        }

        fun refresh(ctx: Context) = WidgetActions.refreshAll(ctx, SpheraWidget::class.java, ::draw)
    }

    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { draw(ctx, manager, it) }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { WidgetActions.forget(ctx, it) }
    }
}
