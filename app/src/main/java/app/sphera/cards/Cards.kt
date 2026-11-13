/*
 * Sphera — Tarjetas de comunicación.
 * Adaptado de Aucards (https://github.com/vadimerenkov/Aucards), © Vadim Erenkov, GPL-3.0:
 * tarjetas de colores con un mensaje, a pantalla completa con un toque, y un sonido para llamar la atención.
 */
package app.sphera.cards

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.compose.runtime.mutableStateListOf
import app.sphera.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * [def]: posición en la lista de ejemplos (cards_defaults) mientras no se edite el texto.
 * Esas tarjetas se muestran siempre en el idioma de la app; al editarlas pasan a ser tuyas (def = null).
 */
data class CommCard(val id: String, val text: String, val description: String, val color: Long, val def: Int? = null)

private fun parseDefault(line: String) = line.split('|', limit = 2).let { it[0].trim() to it.getOrElse(1) { "" }.trim() }

/** Colores suaves para las tarjetas (fondo); el texto se pone negro o blanco según el contraste. */
val CARD_COLORS = listOf(
    0xFFF4D35EL, 0xFFF6A97AL, 0xFFE57373L, 0xFFF48FB1L, 0xFFB39DDBL,
    0xFF90CAF9L, 0xFF80CBC4L, 0xFFA5D6A7L, 0xFFE0E0E0L, 0xFF263238L,
)

/** Tarjetas en cards.json (solo en el móvil). La primera vez se crean unas de ejemplo. Usar desde el hilo principal. */
object CardsStore {
    private var file: File? = null
    val cards = mutableStateListOf<CommCard>()

    /** Carga las tarjetas (una vez) y pone las de ejemplo en el idioma actual (siempre). */
    fun init(ctx: Context) {
        if (file == null) load(ctx)
        localize(ctx)
    }

    private fun load(ctx: Context) {
        val f = File(ctx.applicationContext.filesDir, "cards.json")
        file = f
        if (!f.exists()) {
            ctx.resources.getStringArray(R.array.cards_defaults).forEachIndexed { i, line ->
                val (t, d) = parseDefault(line)
                cards += CommCard(newId(), t, d, CARD_COLORS[i % CARD_COLORS.size], def = i)
            }
            save()
            return
        }
        try {
            val arr = JSONArray(f.readText())
            var migrated = false
            val known = knownDefaults(ctx)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val text = o.getString("text")
                val desc = o.optString("description")
                var def = if (o.has("def")) o.getInt("def") else null
                // Tarjetas creadas antes de 1.0.1: si coinciden con un ejemplo (en cualquier idioma), vuelven a serlo.
                if (!o.has("def")) {
                    def = known.entries.firstOrNull { (_, langs) -> langs.any { it.first == text && it.second == desc } }?.key
                    migrated = true
                }
                cards += CommCard(o.getString("id"), text, desc, o.getLong("color"), def)
            }
            if (migrated) save()
        } catch (e: Exception) {
            f.renameTo(File(f.parentFile, "cards.json.bad-${System.currentTimeMillis()}"))
            cards.clear()
        }
    }

    /** Los ejemplos en todos los idiomas de la app: posición → [(texto, detalle)]. */
    private fun knownDefaults(ctx: Context): Map<Int, List<Pair<String, String>>> {
        val langs = listOf("es", "en", "eu").map { tag ->
            val conf = Configuration(ctx.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            ctx.createConfigurationContext(conf).resources.getStringArray(R.array.cards_defaults).map(::parseDefault)
        }
        return langs.first().indices.associateWith { i -> langs.mapNotNull { it.getOrNull(i) } }
    }

    private fun localize(ctx: Context) {
        val defaults = ctx.resources.getStringArray(R.array.cards_defaults).map(::parseDefault)
        cards.forEachIndexed { i, c ->
            val (t, d) = c.def?.let { defaults.getOrNull(it) } ?: return@forEachIndexed
            if (c.text != t || c.description != d) cards[i] = c.copy(text = t, description = d)
        }
    }

    /** Ejemplos que se borraron y se pueden recuperar. */
    fun missingDefaults(ctx: Context): List<Int> {
        val present = cards.mapNotNull { it.def }.toSet()
        return ctx.resources.getStringArray(R.array.cards_defaults).indices.filter { it !in present }
    }

    /** Vuelve a poner al final los ejemplos borrados; las tarjetas propias no se tocan. */
    fun restoreDefaults(ctx: Context) {
        val defaults = ctx.resources.getStringArray(R.array.cards_defaults).map(::parseDefault)
        missingDefaults(ctx).forEach { i ->
            val (t, d) = defaults[i]
            cards += CommCard(newId(), t, d, CARD_COLORS[i % CARD_COLORS.size], def = i)
        }
        save()
    }

    fun newId() = UUID.randomUUID().toString()
    fun find(id: String) = cards.firstOrNull { it.id == id }

    /** Guardar desde el editor: si el texto no ha cambiado (solo color, por ejemplo), sigue siendo un ejemplo. */
    fun upsert(c: CommCard) {
        val i = cards.indexOfFirst { it.id == c.id }
        val old = cards.getOrNull(i)
        val keepDef = old?.def != null && old.text == c.text && old.description == c.description
        val card = c.copy(def = if (keepDef) old?.def else null)
        if (i >= 0) cards[i] = card else cards += card
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
        cards.forEach {
            val o = JSONObject().put("id", it.id).put("text", it.text).put("description", it.description).put("color", it.color)
            it.def?.let { d -> o.put("def", d) }
            arr.put(o)
        }
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
