/*
 * Sphera — mezclador ambiental del Santuario.
 * Motor de reproducción adaptado de SoundManager de Blankee (https://github.com/itsPronay/blankee),
 * © Pronay Sarker, GPL-3.0. Iconos de sonido de Blankee (GPL-3.0; algunos basados en Material Icons, Apache-2.0).
 * Sonidos de Blankie/Blanket, cada uno con su licencia: ver SOUNDS_LICENSING.md y Ajustes → Licencias y créditos.
 */
package app.sphera.sanctuary

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.sphera.R
import org.json.JSONObject

class AmbientSound(val key: String, @StringRes val title: Int, @DrawableRes val icon: Int, @RawRes val raw: Int, @StringRes val group: Int)

object Ambient {
    val SOUNDS = listOf(
        AmbientSound("rain", R.string.amb_rain, R.drawable.snd_ic_rain, R.raw.snd_rain, R.string.amb_group_nature),
        AmbientSound("storm", R.string.amb_storm, R.drawable.snd_ic_storm, R.raw.snd_storm, R.string.amb_group_nature),
        AmbientSound("wind", R.string.amb_wind, R.drawable.snd_ic_wind, R.raw.snd_wind, R.string.amb_group_nature),
        AmbientSound("waves", R.string.amb_waves, R.drawable.snd_ic_wave, R.raw.snd_waves, R.string.amb_group_nature),
        AmbientSound("stream", R.string.amb_stream, R.drawable.snd_ic_stream, R.raw.snd_stream, R.string.amb_group_nature),
        AmbientSound("birds", R.string.amb_birds, R.drawable.snd_ic_birds, R.raw.snd_birds, R.string.amb_group_nature),
        AmbientSound("summer_night", R.string.amb_summer_night, R.drawable.snd_ic_moon, R.raw.snd_summer_night, R.string.amb_group_nature),
        AmbientSound("forest", R.string.amb_forest, R.drawable.snd_ic_forest, R.raw.snd_forest, R.string.amb_group_nature),
        AmbientSound("train", R.string.amb_train, R.drawable.snd_ic_train, R.raw.snd_train, R.string.amb_group_travel),
        AmbientSound("boat", R.string.amb_boat, R.drawable.snd_ic_sailboat, R.raw.snd_boat, R.string.amb_group_travel),
        AmbientSound("city", R.string.amb_city, R.drawable.snd_ic_city, R.raw.snd_city, R.string.amb_group_travel),
        AmbientSound("airplane", R.string.amb_airplane, R.drawable.snd_ic_airplane, R.raw.snd_airplane, R.string.amb_group_travel),
        AmbientSound("coffee_shop", R.string.amb_coffee_shop, R.drawable.snd_ic_coffee, R.raw.snd_coffee_shop, R.string.amb_group_indoors),
        AmbientSound("fireplace", R.string.amb_fireplace, R.drawable.snd_ic_fireplace, R.raw.snd_fireplace, R.string.amb_group_indoors),
        AmbientSound("fan", R.string.amb_fan, R.drawable.snd_ic_fan, R.raw.snd_fan, R.string.amb_group_indoors),
        AmbientSound("laundry_room", R.string.amb_laundry_room, R.drawable.snd_ic_laundry_room, R.raw.snd_laundry_room, R.string.amb_group_indoors),
        AmbientSound("pink_noise", R.string.amb_pink_noise, R.drawable.snd_ic_pink_noise, R.raw.snd_pink_noise, R.string.amb_group_noise),
        AmbientSound("white_noise", R.string.amb_white_noise, R.drawable.snd_ic_white_noise, R.raw.snd_white_noise, R.string.amb_group_noise),
        AmbientSound("brown_noise", R.string.amb_brown_noise, R.drawable.snd_ic_brown_noise, R.raw.snd_brown_noise, R.string.amb_group_noise),
        AmbientSound("green_noise", R.string.amb_green_noise, R.drawable.snd_ic_green_noise, R.raw.snd_green_noise, R.string.amb_group_noise),
        AmbientSound("ambient_synth", R.string.amb_ambient_synth, R.drawable.snd_ic_ambient_synth, R.raw.snd_ambient_synth, R.string.amb_group_music),
        AmbientSound("gentle_guitar", R.string.amb_gentle_guitar, R.drawable.snd_ic_gentle_guitar, R.raw.snd_gentle_guitar, R.string.amb_group_music),
        AmbientSound("lofi_beats", R.string.amb_lofi_beats, R.drawable.snd_ic_lofi_beats, R.raw.snd_lofi_beats, R.string.amb_group_music),
        AmbientSound("warm_piano", R.string.amb_warm_piano, R.drawable.snd_ic_warm_piano, R.raw.snd_warm_piano, R.string.amb_group_music),
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("ambient_mix", Context.MODE_PRIVATE)

    /** Mezcla guardada: sonido → volumen (0..1). Solo los que están sonando. */
    fun loadMix(ctx: Context): Map<String, Float> = try {
        val o = JSONObject(prefs(ctx).getString("mix", "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getDouble(it).toFloat() }.filterKeys { k -> SOUNDS.any { it.key == k } }
    } catch (e: Exception) {
        emptyMap()
    }

    fun saveMix(ctx: Context, mix: Map<String, Float>) {
        prefs(ctx).edit().putString("mix", JSONObject(mix.mapValues { it.value.toDouble() }).toString()).apply()
    }
}

/**
 * Varios sonidos en bucle a la vez. Usa ExoPlayer con REPEAT_MODE_ONE (bucle sin hueco, a diferencia de
 * MediaPlayer.isLooping) y sin foco de audio, para que los sonidos no se pausen entre sí.
 * Los reproductores se crean al primer uso. Usar solo desde el hilo principal.
 */
class AmbientPlayer(private val ctx: Context) {
    private val players = mutableMapOf<String, ExoPlayer>()
    private val main = Handler(Looper.getMainLooper())

    private fun create(sound: AmbientSound): ExoPlayer =
        ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                false, // sin foco de audio: si no, cada sonido pausaría al anterior
            ).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // Nunca liberar dentro de su propio callback.
                        main.post { players.remove(sound.key)?.release() }
                    }
                })
                setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${ctx.packageName}/${sound.raw}")))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }

    fun play(key: String, volume: Float) {
        val sound = Ambient.SOUNDS.firstOrNull { it.key == key } ?: return
        val p = players.getOrPut(key) { create(sound) }
        p.volume = volume.coerceIn(0f, 1f)
        if (!p.playWhenReady) p.play()
    }

    fun setVolume(key: String, volume: Float) {
        players[key]?.volume = volume.coerceIn(0f, 1f)
    }

    fun stop(key: String) {
        players.remove(key)?.release()
    }

    fun playAll(mix: Map<String, Float>) = mix.forEach { (k, v) -> play(k, v) }

    fun release() {
        main.removeCallbacksAndMessages(null)
        players.values.forEach { it.release() }
        players.clear()
    }
}
