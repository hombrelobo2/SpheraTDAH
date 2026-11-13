package app.sphera.social

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentUris
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.annotation.StringRes
import app.sphera.R
import java.io.File
import java.util.Locale

class Picked(val name: String, val phone: String, val photoUri: String?)
class Channel(@StringRes val label: Int, val intent: Intent)

/** Todo lo que conecta la app con llamadas, contactos y apps de mensajería. */
object Interactions {
    val PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG)

    // Apps cuyas notificaciones cuentan como "esta persona me ha escrito".
    val MESSAGING_PACKAGES = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus", "com.instagram.android",
        "org.thoughtcrime.securesms", "com.viber.voip",
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
    )

    fun hasPermissions(ctx: Context) =
        PERMISSIONS.all { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    // shortcut: compara los últimos 9 dígitos (números españoles); usar libphonenumber si hay contactos de otros países.
    fun samePhone(a: String, b: String): Boolean {
        val x = a.filter(Char::isDigit)
        val y = b.filter(Char::isDigit)
        return x.length >= 6 && y.length >= 6 && x.takeLast(9) == y.takeLast(9)
    }

    /** Fecha de la última llamada con conversación (duración > 0) con ese número, o null. */
    fun lastCall(ctx: Context, phone: String): Long? {
        if (ctx.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return null
        ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
            "${CallLog.Calls.DURATION} > 0", null, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                if (samePhone(number, phone)) return c.getLong(1)
            }
        }
        return null
    }

    /** Actualiza llamadas y cumpleaños desde el móvil. */
    // shortcut: se lee en el hilo principal; mover a segundo plano si con muchos contactos se nota lento.
    fun sync(ctx: Context) {
        Store.contacts.toList().forEach { c ->
            lastCall(ctx, c.phone)?.let { Store.touch(c.id, it) }
            val bday = birthday(ctx, c.phone)
            if (bday != c.birthday) Store.update(c.id) { it.copy(birthday = bday) }
        }
    }

    private fun contactId(ctx: Context, phone: String): Long? = ctx.contentResolver.query(
        Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)),
        arrayOf(ContactsContract.PhoneLookup._ID), null, null, null,
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    /** Cumpleaños guardado en la agenda para ese número, como "MM-dd". */
    fun birthday(ctx: Context, phone: String): String? = try {
        contactId(ctx, phone)?.let { id ->
            ctx.contentResolver.query(
                ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Event.START_DATE),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND " +
                    "${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
                arrayOf(
                    id.toString(), ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
                ),
                null,
            )?.use { if (it.moveToFirst()) parseBirthday(it.getString(0)) else null }
        }
    } catch (e: SecurityException) {
        null
    }

    fun readPicked(ctx: Context, uri: Uri): Picked? =
        ctx.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_URI), null, null, null)
            ?.use { c ->
                if (!c.moveToFirst()) return@use null
                val phone = c.getString(1) ?: return@use null
                Picked(c.getString(0) ?: phone, phone, c.getString(2))
            }

    /** Copia la foto del contacto dentro de la app; null si no tiene o no se puede leer. */
    fun savePhoto(ctx: Context, photoUri: String?, id: Long): String? {
        if (photoUri == null) return null
        val f = File(ctx.filesDir, "photo_$id.jpg")
        return try {
            ctx.contentResolver.openInputStream(Uri.parse(photoUri))?.use { input ->
                f.outputStream().use { input.copyTo(it) }
                f.path
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Número en formato internacional solo con dígitos (34600111222), como lo piden WhatsApp y Telegram. */
    private fun intlDigits(ctx: Context, phone: String): String {
        val iso = ctx.getSystemService(TelephonyManager::class.java)?.networkCountryIso
            ?.takeIf { it.isNotBlank() } ?: Locale.getDefault().country
        return (PhoneNumberUtils.formatNumberToE164(phone, iso.uppercase()) ?: phone).filter(Char::isDigit)
    }

    private fun installed(ctx: Context, pkg: String) = try {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Formas de contactar: apps de mensajería instaladas, más SMS y llamada. */
    /** [text]: mensaje para empezar; WhatsApp y SMS lo traen ya escrito, en el resto se pega desde el portapapeles. */
    fun channels(ctx: Context, c: Contact, text: String? = null): List<Channel> {
        val phone = c.phone
        val intl = intlDigits(ctx, phone)
        val waText = text?.let { "?text=" + Uri.encode(it) } ?: ""
        val apps = listOf(
            Triple(R.string.ch_whatsapp, "com.whatsapp", "https://wa.me/$intl$waText"),
            Triple(R.string.ch_whatsapp_business, "com.whatsapp.w4b", "https://wa.me/$intl$waText"),
            Triple(R.string.ch_signal, "org.thoughtcrime.securesms", "https://signal.me/#p/+$intl"),
            Triple(R.string.ch_viber, "com.viber.voip", "viber://chat?number=%2B$intl"),
        ).filter { installed(ctx, it.second) }
            .map { (label, pkg, url) -> Channel(label, Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg)) }
        // Telegram sin paquete fijo: vale cualquier versión (Play, telegram.org, F-Droid, Telegram X, Plus).
        val tgInstalled = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve")).resolveActivity(ctx.packageManager) != null
        val tg = if (tgInstalled) listOf(Channel(R.string.ch_telegram, telegramIntent(ctx, c, intl))) else emptyList()
        // Instagram solo por usuario: ig.me/m/ abre los mensajes privados; Android decide si en la app o el navegador.
        val ig = c.instagram?.let { listOf(Channel(R.string.ch_instagram, Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/m/$it")))) }
            ?: emptyList()
        return apps + tg + ig +
            Channel(R.string.ch_sms, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone, null)).putExtra("sms_body", text)) +
            Channel(R.string.ch_call, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
    }

    /**
     * Orden: ID numérico (abre el chat con los datos que Telegram ya tiene, sin buscar en red) → usuario → el acceso a Telegram que la propia app guarda en la agenda
     * (el mismo que usa la ficha del contacto) → por número, que no todas las versiones aceptan.
     */
    private fun telegramIntent(ctx: Context, c: Contact, intl: String): Intent {
        c.telegram?.let { handle ->
            val uri = if (handle.all(Char::isDigit)) "tg://openmessage?user_id=$handle" else "tg://resolve?domain=$handle"
            return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        }
        agendaChat(ctx, c.phone, "telegram")?.let { return it }
        return Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$intl"))
    }

    /** Busca en la agenda la entrada que crea una app de mensajería (p. ej. "Mensaje a +34…" de Telegram) para ese número. */
    private fun agendaChat(ctx: Context, phone: String, app: String): Intent? = try {
        contactId(ctx, phone)?.let { id ->
            ctx.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} LIKE ?",
                arrayOf(id.toString(), "%$app%profile%"), null,
            )?.use {
                if (!it.moveToFirst()) null
                else Intent(Intent.ACTION_VIEW).setDataAndType(
                    ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, it.getLong(0)), it.getString(1),
                )
            }
        }
    } catch (e: SecurityException) {
        null // sin permiso de contactos
    }

    fun copy(ctx: Context, text: String) {
        ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Sphera", text))
        Toast.makeText(ctx, R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    fun open(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.no_app, Toast.LENGTH_SHORT).show()
        false
    }
}
