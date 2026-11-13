package app.sphera.social

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Cuando llega un mensaje de WhatsApp/Telegram/SMS de un contacto del universo, cuenta como contacto. */
class NotifListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Interactions.MESSAGING_PACKAGES) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        Store.init(this)
        // shortcut: se compara por nombre (es lo que muestran las notificaciones); "Ana" también casa con "Anabel".
        Store.contacts.filter { c ->
            listOfNotNull(c.name, c.telegram, c.instagram).any { title.contains(it, ignoreCase = true) }
        }
            .forEach { Store.touch(it.id, sbn.postTime) }
    }
}
