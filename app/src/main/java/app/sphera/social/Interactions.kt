package app.sphera.social

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
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
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
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

    // shortcut: se lee en el hilo principal; mover a segundo plano si con muchos contactos se nota lento.
    fun syncCallLog(ctx: Context) {
        Store.contacts.toList().forEach { c -> lastCall(ctx, c.phone)?.let { Store.touch(c.id, it) } }
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
    fun channels(ctx: Context, phone: String): List<Channel> {
        val intl = intlDigits(ctx, phone)
        val apps = listOf(
            Triple(R.string.ch_whatsapp, "com.whatsapp", "https://wa.me/$intl"),
            Triple(R.string.ch_whatsapp_business, "com.whatsapp.w4b", "https://wa.me/$intl"),
            Triple(R.string.ch_telegram, "org.telegram.messenger", "tg://resolve?phone=$intl"),
        ).filter { installed(ctx, it.second) }
            .map { (label, pkg, url) -> Channel(label, Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg)) }
        return apps +
            Channel(R.string.ch_sms, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone, null))) +
            Channel(R.string.ch_call, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
    }

    fun open(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.no_app, Toast.LENGTH_SHORT).show()
        false
    }
}
