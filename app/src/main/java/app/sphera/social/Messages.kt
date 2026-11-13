package app.sphera.social

import android.content.Context
import app.sphera.R
import org.json.JSONArray

/** Mensajes para empezar. Editables en Ajustes; mientras no se toquen, se usan los predefinidos del idioma. */
object Messages {
    const val TOKEN = "{alias}"
    private const val KEY = "icebreakers"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun list(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY, null) ?: return ctx.resources.getStringArray(R.array.icebreakers).toList()
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.getString(it) }
    }

    fun save(ctx: Context, list: List<String>) {
        prefs(ctx).edit().putString(KEY, JSONArray(list).toString()).apply()
    }

    fun reset(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }

    /** Pone el alias donde esté {alias}; sin alias, quita el hueco con su coma o espacio ("¡Hola {alias}!" → "¡Hola!"). */
    fun fill(template: String, alias: String?): String {
        val a = alias?.trim().orEmpty()
        return if (a.isNotEmpty()) template.replace(TOKEN, a)
        else template.replace(Regex("[ ,]*\\{alias\\}"), "").trim()
    }
}
