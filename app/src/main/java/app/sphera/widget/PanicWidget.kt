package app.sphera.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import app.sphera.R
import app.sphera.Routes

/** Botón de pánico 1×1: un círculo que entra directo al Santuario. */
class PanicWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_panic)
        views.setOnClickPendingIntent(R.id.widget_panic_root, linkIntent(ctx, Routes.LINK_SANCTUARY, 30))
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
