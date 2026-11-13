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

/** Publica notificaciones que, al tocarlas, abren el universo (y la ficha del contacto si se indica). */
object Notifier {
    fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(ctx: Context, notificationId: Int, channel: String, channelName: Int, title: String, text: String, contactId: Long? = null) {
        if (!canNotify(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(channel, ctx.getString(channelName), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_SOCIAL, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        contactId?.let { intent.putExtra(MainActivity.EXTRA_CONTACT_ID, it) }
        // requestCode distinto por notificación para que cada una conserve su contacto.
        val open = PendingIntent.getActivity(
            ctx, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            notificationId,
            Notification.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}

/** Avisos sueltos: alguien se pasa de su frecuencia, cumpleaños y fin de un aplazamiento. Cada uno se activa en Ajustes. */
object Alerts {
    const val DRIFT = "alert_drift"
    const val BIRTHDAY = "alert_birthday"
    const val SNOOZE_END = "alert_snooze"
    private const val CHANNEL = "alerts"
    private const val CHECK_EVERY = 3 * 60 * 60_000L
    private val QUIET_UNTIL = 9 // no se avisa de noche: solo de 9:00 a 22:00
    private val QUIET_FROM = 22

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun isEnabled(ctx: Context, key: String) = prefs(ctx).getBoolean(key, false)

    fun set(ctx: Context, key: String, on: Boolean) {
        val edit = prefs(ctx).edit().putBoolean(key, on)
        if (key == DRIFT && on) {
            // Quien ya iba tarde al activarlo cuenta como avisado: así no llega una avalancha de golpe.
            Store.init(ctx)
            val now = System.currentTimeMillis()
            drifting(Store.contacts, now).forEach { edit.putLong("drift_${it.id}", it.lastContact) }
        }
        edit.apply()
        schedule(ctx)
    }

    /** Revisión cada ~3 horas mientras haya algún aviso activo. */
    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, AlertsReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.cancel(pi)
        if (listOf(DRIFT, BIRTHDAY, SNOOZE_END).none { isEnabled(ctx, it) }) return
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 15 * 60_000L, CHECK_EVERY, pi)
    }

    fun check(ctx: Context) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (cal.get(Calendar.HOUR_OF_DAY) !in QUIET_UNTIL until QUIET_FROM) return
        Store.init(ctx)
        Interactions.sync(ctx)
        val p = prefs(ctx)
        val edit = p.edit()
        val year = cal.get(Calendar.YEAR)
        Store.contacts.toList().forEach { c ->
            val name = displayName(c)
            val nid = 1000 + (c.id % 1_000_000).toInt()
            // Se recuerda el lastContact avisado: si vuelve a alejarse tras hablar, se avisa otra vez.
            if (isEnabled(ctx, DRIFT) && !isSnoozed(c, now) && daysSince(c, now) >= c.frequencyDays &&
                p.getLong("drift_${c.id}", -1) != c.lastContact
            ) {
                Notifier.post(
                    ctx, nid, CHANNEL, R.string.alerts_title,
                    ctx.getString(R.string.alert_drift, name),
                    ctx.resources.getQuantityString(R.plurals.days_since, daysSince(c, now), daysSince(c, now)), c.id,
                )
                edit.putLong("drift_${c.id}", c.lastContact)
            }
            if (isEnabled(ctx, BIRTHDAY) && isBirthday(c, now) && p.getInt("bday_${c.id}", 0) != year) {
                Notifier.post(
                    ctx, nid + 1, CHANNEL, R.string.alerts_title,
                    ctx.getString(R.string.summary_birthday, name), ctx.getString(R.string.alert_tap_to_write), c.id,
                )
                edit.putInt("bday_${c.id}", year)
            }
            // Solo aplazamientos que terminan solos: los que se quitan a mano o al hablar ya tienen snoozeNotified.
            if (isEnabled(ctx, SNOOZE_END) && c.snoozeUntil in 1..now && !c.snoozeNotified) {
                Notifier.post(
                    ctx, nid + 2, CHANNEL, R.string.alerts_title,
                    ctx.getString(R.string.alert_snooze_end, name), ctx.getString(R.string.alert_tap_to_write), c.id,
                )
                Store.update(c.id) { it.copy(snoozeNotified = true) }
            }
        }
        edit.apply()
    }
}

class AlertsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Alerts.check(ctx)
}
