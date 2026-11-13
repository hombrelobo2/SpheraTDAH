/*
 * Sphera — Tarjetas de comunicación.
 * Adaptado de Aucards (https://github.com/vadimerenkov/Aucards), © Vadim Erenkov, GPL-3.0:
 * tarjetas de colores con un mensaje, a pantalla completa con un toque, y un sonido para llamar la atención.
 */
package app.sphera.cards

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.compose.runtime.mutableStateListOf
import app.sphera.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class CommCard(val id: String, val text: String, val description: String, val color: Long)

/** Colores suaves para las tarjetas (fondo); el texto se pone negro o blanco según el contraste. */
val CARD_COLORS = listOf(
    0xFFF4D35EL, 0xFFF6A97AL, 0xFFE57373L, 0xFFF48FB1L, 0xFFB39DDBL,
    0xFF90CAF9L, 0xFF80CBC4L, 0xFFA5D6A7L, 0xFFE0E0E0L, 0xFF263238L,
)

/** Tarjetas en cards.json (solo en el móvil). La primera vez se crean unas de ejemplo. Usar desde el hilo principal. */
object CardsStore {
    private var file: File? = null
    val cards = mutableStateListOf<CommCard>()

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.applicationContext.filesDir, "cards.json")
        file = f
        if (!f.exists()) {
            val texts = ctx.resources.getStringArray(R.array.cards_defaults)
            texts.forEachIndexed { i, line ->
                val (t, d) = line.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }
                cards += CommCard(newId(), t, d, CARD_COLORS[i % CARD_COLORS.size])
            }
            save()
            return
        }
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cards += CommCard(o.getString("id"), o.getString("text"), o.optString("description"), o.getLong("color"))
            }
        } catch (e: Exception) {
            f.renameTo(File(f.parentFile, "cards.json.bad-${System.currentTimeMillis()}"))
            cards.clear()
        }
    }

    fun newId() = UUID.randomUUID().toString()
    fun find(id: String) = cards.firstOrNull { it.id == id }

    fun upsert(c: CommCard) {
        val i = cards.indexOfFirst { it.id == c.id }
        if (i >= 0) cards[i] = c else cards += c
        save()
    }

    fun delete(id: String) {
        cards.removeAll { it.id == id }
        save()
    }

    /** Mueve una tarjeta una posición (-1 arriba, +1 abajo). */
    fun move(id: String, by: Int) {
        val i = cards.indexOfFirst { it.id == id }
        val j = i + by
        if (i < 0 || j !in cards.indices) return
        val c = cards.removeAt(i)
        cards.add(j, c)
        save()
    }

    private fun save() {
        val f = file ?: return
        val arr = JSONArray()
        cards.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("description", it.description).put("color", it.color)) }
        val tmp = File(f.parentFile, "cards.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }

    /** Sonido para que los demás miren: el de notificación del móvil (como hace Aucards con un tono). */
    fun ring(ctx: Context) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        RingtoneManager.getRingtone(ctx, uri)?.apply {
            audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()
            play()
        }
    }
}
