/*
 * Sphera — Puerta lenta.
 * Idea inspirada en Defang (GPL-3.0): antes de abrir una app que engancha, una pausa breve.
 * Aquí sin servicio de accesibilidad: Sphera pone en la pantalla de inicio un acceso directo que pasa por la pausa.
 */
package app.sphera.gate

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf

/** Una app que se puede abrir desde el móvil. */
class LaunchableApp(val pkg: String, val label: String, val icon: Drawable)

object SlowGate {
    const val EXTRA_PKG = "gate_pkg"

    /** Apps con Puerta lenta, por orden de alta. */
    val apps = mutableStateListOf<String>()
    /** Respiraciones antes de que aparezca el botón de abrir (1-3). */
    val breaths = mutableIntStateOf(2)
    private var loaded = false

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("gate", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        apps.addAll(p.getString("apps", "").orEmpty().split(',').filter { it.isNotBlank() })
        breaths.intValue = p.getInt("breaths", 2)
    }

    private fun save(ctx: Context) = prefs(ctx).edit()
        .putString("apps", apps.joinToString(","))
        .putInt("breaths", breaths.intValue)
        .apply()

    fun setBreaths(ctx: Context, n: Int) {
        breaths.intValue = n.coerceIn(1, 3)
        save(ctx)
    }

    fun add(ctx: Context, pkg: String) {
        if (pkg !in apps) apps.add(pkg)
        save(ctx)
    }

    fun remove(ctx: Context, pkg: String, disabledMessage: String) {
        apps.remove(pkg)
        save(ctx)
        // El acceso que quedara en la pantalla de inicio se queda en gris y ya no abre nada.
        runCatching { ctx.getSystemService(ShortcutManager::class.java).disableShortcuts(listOf(shortcutId(pkg)), disabledMessage) }
    }

    /** Todas las apps con icono en el cajón, sin Sphera, por nombre. */
    fun launchable(ctx: Context): List<LaunchableApp> {
        val pm = ctx.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { LaunchableApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }

    fun find(ctx: Context, pkg: String): LaunchableApp? = runCatching {
        val pm = ctx.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        LaunchableApp(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
    }.getOrNull()

    /** Una pregunta al azar de la lista, sin repetir la de la última vez. */
    fun nextQuestion(ctx: Context): String {
        val all = ctx.resources.getStringArray(app.sphera.R.array.gate_questions)
        val last = prefs(ctx).getInt("last_question", -1)
        var i = all.indices.random()
        if (all.size > 1 && i == last) i = (i + 1) % all.size
        prefs(ctx).edit().putInt("last_question", i).apply()
        return all[i]
    }

    private fun shortcutId(pkg: String) = "gate_$pkg"

    /** Pide a la pantalla de inicio que añada el acceso con pausa. Devuelve false si no lo permite. */
    fun pin(ctx: Context, app: LaunchableApp): Boolean {
        val sm = ctx.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false
        val intent = Intent(ctx, GateActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_PKG, app.pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val info = ShortcutInfo.Builder(ctx, shortcutId(app.pkg))
            .setShortLabel(app.label)
            .setIcon(Icon.createWithBitmap(app.icon.toBitmap(192)))
            .setIntent(intent)
            .build()
        // Si ya existía y se había desactivado, se vuelve a activar.
        runCatching { sm.enableShortcuts(listOf(info.id)) }
        return runCatching { sm.requestPinShortcut(info, null) }.getOrDefault(false)
    }
}

fun Drawable.toBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, size, size)
    draw(canvas)
    return bmp
}
