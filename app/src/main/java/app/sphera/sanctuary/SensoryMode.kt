package app.sphera.sanctuary

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
            if (canChangeDnd(ctx) && nm != null) {
                e.putInt("dnd", nm.currentInterruptionFilter)
                nm.notificationPolicy.let {
                    e.putInt("p_cat", it.priorityCategories).putInt("p_calls", it.priorityCallSenders)
                        .putInt("p_msgs", it.priorityMessageSenders).putInt("p_fx", it.suppressedVisualEffects)
                }
            }
            if (canChangeBrightness(ctx)) {
                e.putInt("mode", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0))
                e.putInt("level", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128))
            }
            e.apply()
        }
        // Prioridad respeta las excepciones que la persona tenga configuradas (alarmas, favoritos…).
        if (canChangeDnd(ctx) && nm != null) {
            // Algunos móviles silencian también el audio multimedia en No molestar: se permite para que suene el Santuario.
            val p = nm.notificationPolicy
            if (Build.VERSION.SDK_INT >= 28) nm.notificationPolicy = NotificationManager.Policy(
                p.priorityCategories or NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                p.priorityCallSenders, p.priorityMessageSenders, p.suppressedVisualEffects,
            )
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
        if (canChangeBrightness(ctx)) {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, 1)
        }
    }

    fun restore(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean("active", false)) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (canChangeDnd(ctx) && nm != null && p.contains("dnd")) {
            if (p.contains("p_cat")) {
                nm.notificationPolicy = NotificationManager.Policy(
                    p.getInt("p_cat", 0), p.getInt("p_calls", 0), p.getInt("p_msgs", 0), p.getInt("p_fx", 0),
                )
            }
            nm.setInterruptionFilter(p.getInt("dnd", NotificationManager.INTERRUPTION_FILTER_ALL))
        }
        if (canChangeBrightness(ctx) && p.contains("mode")) {
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, p.getInt("level", 128))
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt("mode", 0))
        }
        p.edit().clear().apply()
    }
}
