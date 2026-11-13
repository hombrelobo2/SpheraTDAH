/*
 * Sphera — avisos de versión nueva. Basado en el sistema de MediTag (mismo autor, GPL-3.0):
 * la app solo descarga un version.json pequeño de GitHub; no envía ningún dato.
 */
package app.sphera.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.sphera.BuildConfig
import app.sphera.R
import app.sphera.social.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

const val REPO_URL = "https://github.com/hombrelobo2/SpheraTDAH"
/** Manual de uso en PDF: siempre la última versión publicada en el repositorio. */
const val MANUAL_URL = "$REPO_URL/raw/main/docs/Sphera-Guia-de-uso.pdf"
const val ISSUES_URL = "$REPO_URL/issues"
private const val VERSION_URL = "$REPO_URL/releases/download/ultima/version.json"

/** APK de una versión concreta, con su número en el nombre. */
fun versionedApkUrl(version: String) = "$REPO_URL/releases/download/v$version/sphera_$version.apk"

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

object UpdateCheck {
    sealed interface Result
    data class UpToDate(val name: String) : Result
    data class Available(val name: String, val url: String) : Result
    data class Failed(val reason: String) : Result

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            var url = URL(VERSION_URL)
            var hops = 0
            val conn: HttpURLConnection
            while (true) { // GitHub redirige a su CDN
                val c = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                }
                val code = c.responseCode
                if (code in 300..399 && hops++ < 5) {
                    url = URL(url, c.getHeaderField("Location"))
                    c.disconnect()
                    continue
                }
                if (code != 200) return@withContext Failed("HTTP $code")
                conn = c
                break
            }
            val o = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            conn.disconnect()
            val remoteCode = o.getInt("versionCode")
            val remoteName = o.optString("versionName", remoteCode.toString())
            val apkUrl = o.optString("apkUrl").ifBlank { versionedApkUrl(remoteName) }
            if (remoteCode > BuildConfig.VERSION_CODE) Available(remoteName, apkUrl) else UpToDate(BuildConfig.VERSION_NAME)
        } catch (e: IOException) {
            Failed("offline")
        } catch (e: Exception) {
            Failed(e.message ?: "?")
        }
    }
}

/** Comprobación automática una vez por semana, solo con internet. Avisa una sola vez por versión. */
object AutoUpdate {
    private const val WORK = "update-check"
    private const val CHANNEL = "updates"
    private const val NOTIF_ID = 7001

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("auto_update", true)

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("auto_update", on).apply()
        schedule(ctx)
    }

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!enabled(ctx)) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<UpdateWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun notifyIfNew(ctx: Context, version: String, url: String) {
        val p = prefs(ctx)
        if (p.getString("update_notified", null) == version || !Notifier.canNotify(ctx)) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, NOTIF_ID, Intent(Intent.ACTION_VIEW, Uri.parse(url)), PendingIntent.FLAG_IMMUTABLE)
        val text = ctx.getString(R.string.update_notif_text)
        runCatching {
            nm.notify(
                NOTIF_ID,
                Notification.Builder(ctx, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(ctx.getString(R.string.update_notif_title, version))
                    .setContentText(text)
                    .setStyle(Notification.BigTextStyle().bigText(text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build(),
            )
            p.edit().putString("update_notified", version).apply()
        }
    }
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!AutoUpdate.enabled(applicationContext)) return Result.success()
        return when (val r = UpdateCheck.check()) {
            is UpdateCheck.Available -> { AutoUpdate.notifyIfNew(applicationContext, r.name, r.url); Result.success() }
            is UpdateCheck.UpToDate -> Result.success()
            is UpdateCheck.Failed -> Result.retry()
        }
    }
}
