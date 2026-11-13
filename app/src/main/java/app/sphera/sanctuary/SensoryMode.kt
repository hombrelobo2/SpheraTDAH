package app.sphera.sanctuary

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Modo Santuario a nivel de sistema: brillo al mínimo y No molestar.
 * Guarda el estado anterior la primera vez y lo restaura con [restore] ("Volver" en el Aterrizaje).
 * Sin permisos no rompe nada: el brillo de la propia pantalla del Santuario se baja igualmente (ver SanctuaryScreen).
 */
object SensoryMode {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sanctuary", Context.MODE_PRIVATE)

    fun canChangeDnd(ctx: Context) =
        ctx.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun canChangeBrightness(ctx: Context) = Settings.System.canWrite(ctx)

    fun dndSettings() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun brightnessSettings(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))

    fun enter(ctx: Context) {
        val p = prefs(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val cr = ctx.contentResolver
        // Si ya estaba activo (se entra dos veces seguidas), no se pisa el estado original guardado.
        if (!p.getBoolean("active", false)) {
            val e = p.edit().putBoolean("active", true)
            if (canChangeDnd(ctx) && nm != null) e.putInt("dnd", nm.currentInterruptionFilter)
            if (canChangeBrightness(ctx)) {
                e.putInt("mode", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0))
                e.putInt("level", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128))
            }
            e.apply()
        }
        // Prioridad respeta las excepciones que la persona tenga configuradas (alarmas, favoritos…).
        if (canChangeDnd(ctx)) nm?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        if (canChangeBrightness(ctx)) {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, 1)
        }
    }

    fun restore(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean("active", false)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (canChangeDnd(ctx) && p.contains("dnd")) nm?.setInterruptionFilter(p.getInt("dnd", NotificationManager.INTERRUPTION_FILTER_ALL))
        if (canChangeBrightness(ctx) && p.contains("mode")) {
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, p.getInt("level", 128))
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt("mode", 0))
        }
        p.edit().clear().apply()
    }
}
