package app.sphera.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.annotation.StringRes
import app.sphera.R
import app.sphera.Routes

/** Un acceso que se puede poner en el widget de accesos. */
class WidgetAction(val key: String, val emoji: String, @StringRes val label: Int, val link: String, val color: Int)

object WidgetActions {
    val ALL = listOf(
        WidgetAction("sanctuary", "🌑", R.string.wa_sanctuary, Routes.LINK_SANCTUARY, 0xFFA9A1D6.toInt()),
        WidgetAction("return_new", "📍", R.string.wa_return, Routes.LINK_RETURN_NEW, 0xFFD9B98C.toInt()),
        WidgetAction("rabbit", "🐇", R.string.wa_rabbit, Routes.LINK_RABBIT, 0xFFC99A8E.toInt()),
        WidgetAction("cards", "🗣️", R.string.wa_cards, Routes.LINK_CARDS, 0xFF7CC6C0.toInt()),
        WidgetAction("body", "🫀", R.string.wa_body, Routes.LINK_BODY, 0xFFD99AAE.toInt()),
        WidgetAction("steps", "👣", R.string.wa_steps, Routes.LINK_STEPS, 0xFF8EB6D9.toInt()),
        WidgetAction("social", "🪐", R.string.wa_social, Routes.LINK_SOCIAL, 0xFF8FB8A8.toInt()),
        WidgetAction("gate", "🚪", R.string.wa_gate, Routes.LINK_GATE, 0xFFB9C47E.toInt()),
    )
    const val MAX = 4
    private val DEFAULT = listOf("sanctuary", "return_new", "rabbit")

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    /** Accesos de un widget; los que se pusieron antes de poder elegir llevan los tres de siempre. */
    fun forWidget(ctx: Context, id: Int): List<WidgetAction> {
        val keys = prefs(ctx).getString("w_$id", null)?.split(',')?.filter { it.isNotBlank() } ?: DEFAULT
        return keys.mapNotNull { k -> ALL.firstOrNull { it.key == k } }.take(MAX)
    }

    fun save(ctx: Context, id: Int, keys: List<String>) = prefs(ctx).edit().putString("w_$id", keys.joinToString(",")).apply()
    fun forget(ctx: Context, id: Int) = prefs(ctx).edit().remove("w_$id").apply()

    /** Redibuja todos los widgets de un tipo. */
    fun refreshAll(ctx: Context, provider: Class<*>, draw: (Context, AppWidgetManager, Int) -> Unit) {
        val manager = AppWidgetManager.getInstance(ctx)
        manager.getAppWidgetIds(ComponentName(ctx, provider)).forEach { draw(ctx, manager, it) }
    }
}
