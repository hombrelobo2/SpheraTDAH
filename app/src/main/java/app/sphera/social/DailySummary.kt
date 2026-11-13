package app.sphera.social

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.sphera.MainActivity
import app.sphera.R
import java.util.Calendar

/** Una notificación al día con quién se está alejando. Solo aparece si hay alguien. */
object DailySummary {
    private const val CHANNEL = "summary"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun isEnabled(ctx: Context) = prefs(ctx).getBoolean("summary_enabled", false)
    fun hour(ctx: Context) = prefs(ctx).getInt("summary_hour", 19)

    fun configure(ctx: Context, enabled: Boolean, hour: Int) {
        prefs(ctx).edit().putBoolean("summary_enabled", enabled).putInt("summary_hour", hour).apply()
        schedule(ctx)
    }

    /** Programa (o cancela) el próximo aviso. Se llama al abrir la app, al reiniciar el móvil y tras cada aviso. */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, SummaryReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.cancel(pi)
        if (!isEnabled(ctx)) return
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour(ctx))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Ventana de 15 min: no necesita permiso de alarmas exactas y gasta menos batería.
        am.setWindow(AlarmManager.RTC_WAKEUP, next.timeInMillis, 15 * 60_000L, pi)
    }

    fun show(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        Store.init(ctx)
        Interactions.syncCallLog(ctx)
        val now = System.currentTimeMillis()
        val list = drifting(Store.contacts, now)
        val first = list.firstOrNull() ?: return
        val others = list.drop(1).map(::displayName)
        val text = if (others.isEmpty()) {
            ctx.resources.getQuantityString(R.plurals.days_since, daysSince(first, now), daysSince(first, now))
        } else {
            val names = others.take(3).joinToString(", ") + if (others.size > 3) ", …" else ""
            ctx.resources.getQuantityString(R.plurals.summary_others, others.size, names)
        }

        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.summary_title), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_SOCIAL, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            1,
            Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(ctx.getString(R.string.today_write_to, displayName(first)))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}

class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) DailySummary.show(ctx)
        DailySummary.schedule(ctx)
    }
}
