package app.sphera.sanctuary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R

private const val DEFAULT_VOLUME = 0.7f

/**
 * Santuario en modo "mezclador ambiental": cuadrícula de sonidos que se combinan, cada uno con su volumen.
 * La mezcla se guarda al momento y vuelve a sonar igual la próxima vez.
 */
@Composable
fun MixerSanctuary(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember { AmbientPlayer(ctx) }
    val mix = remember { mutableStateMapOf<String, Float>().apply { putAll(Ambient.loadMix(ctx)) } }
    DisposableEffect(Unit) {
        player.playAll(mix.toMap())
        onDispose { player.release() }
    }
    fun save() = Ambient.saveMix(ctx, mix.toMap())

    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(16.dp)) {
        val pattern = remember { Breathing.pattern(ctx) }
        var breathing by remember { mutableStateOf(Breathing.enabled(ctx)) }
        if (breathing) {
            BreathingCircle(pattern, Modifier.fillMaxWidth().padding(bottom = 8.dp), diameter = 120.dp, labelSize = 24.sp)
        } else {
            CalmPhrase(Modifier.fillMaxWidth().padding(bottom = 8.dp), fontSize = 19.sp, color = Palette.textSoft, center = false)
        }
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Ambient.SOUNDS.groupBy { it.group }.forEach { (group, sounds) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "g$group") {
                    Text(
                        stringResource(group), color = Palette.textFaint, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
                items(sounds, key = { it.key }) { sound ->
                    val volume = mix[sound.key]
                    SoundTile(
                        sound, volume,
                        onToggle = {
                            if (volume != null) {
                                mix.remove(sound.key)
                                player.stop(sound.key)
                            } else {
                                mix[sound.key] = DEFAULT_VOLUME
                                player.play(sound.key, DEFAULT_VOLUME)
                            }
                            save()
                        },
                        onVolume = {
                            mix[sound.key] = it
                            player.setVolume(sound.key, it)
                        },
                        onVolumeDone = { save() },
                    )
                }
            }
        }
        BreathToggle(breathing, Modifier.padding(top = 12.dp)) {
            breathing = it
            Breathing.setEnabled(ctx, it)
        }
        ExitButton(onExit, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SoundTile(sound: AmbientSound, volume: Float?, onToggle: () -> Unit, onVolume: (Float) -> Unit, onVolumeDone: () -> Unit) {
    val active = volume != null
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.clip(shape)
            .background(if (active) Palette.sanctuary.copy(alpha = 0.18f) else Palette.card)
            .border(1.dp, if (active) Palette.sanctuary else Palette.line, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(sound.icon), contentDescription = null,
            tint = if (active) Palette.sanctuary else Palette.textSoft, modifier = Modifier.size(34.dp),
        )
        Text(
            stringResource(sound.title), color = if (active) Palette.text else Palette.textSoft,
            fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (active) {
            Slider(
                value = volume ?: 0f, onValueChange = onVolume, onValueChangeFinished = onVolumeDone,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Palette.sanctuary, activeTrackColor = Palette.sanctuary, inactiveTrackColor = Palette.line,
                ),
            )
        }
    }
}
