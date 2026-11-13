package app.sphera.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import app.sphera.MainActivity
import app.sphera.R
import app.sphera.Routes

/** Widget de escritorio con tres accesos: Santuario, apuntar un Punto de Retorno y aparcar una curiosidad. */
class SpheraWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_sphera).apply {
            setOnClickPendingIntent(R.id.widget_sanctuary, link(ctx, Routes.LINK_SANCTUARY, 10))
            setOnClickPendingIntent(R.id.widget_return, link(ctx, Routes.LINK_RETURN_NEW, 11))
            setOnClickPendingIntent(R.id.widget_rabbit, link(ctx, Routes.LINK_RABBIT, 12))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    // Abre la app desde cero en la pantalla pedida, aunque ya estuviera abierta en otra.
    private fun link(ctx: Context, uri: String, code: Int) = PendingIntent.getActivity(
        ctx, code,
        Intent(Intent.ACTION_VIEW, Uri.parse(uri), ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
