package app.sphera.sanctuary

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.sphera.R
import kotlinx.coroutines.delay

/** Frases tranquilas del Santuario (lista "sanctuary_phrases" en strings.xml). Se pueden desactivar en Ajustes. */
object CalmPhrases {
    const val CHANGE_EVERY_MS = 30_000L
    const val FADE_MS = 3_000 // fundido lento: un cambio brusco en pantalla negra sobresalta

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sanctuary", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("phrases", true)
    fun setEnabled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("phrases", on).apply()
}

/** Muestra una frase y la cambia cada 30 s con fundido, en orden aleatorio y sin repetir hasta pasar por todas. */
@Composable
fun CalmPhrase(modifier: Modifier, fontSize: TextUnit, color: Color, center: Boolean = true) {
    val ctx = LocalContext.current
    val enabled = remember { CalmPhrases.enabled(ctx) }
    val fallback = stringResource(R.string.sanctuary_text)
    val phrases = stringArrayResource(R.array.sanctuary_phrases)
    var current by remember { mutableStateOf(if (enabled && phrases.isNotEmpty()) phrases.random() else fallback) }
    if (enabled && phrases.size > 1) {
        LaunchedEffect(phrases) {
            var deck = phrases.filter { it != current }.shuffled()
            while (true) {
                delay(CalmPhrases.CHANGE_EVERY_MS)
                if (deck.isEmpty()) deck = phrases.filter { it != current }.shuffled()
                current = deck.first()
                deck = deck.drop(1)
            }
        }
    }
    Crossfade(current, modifier, animationSpec = tween(CalmPhrases.FADE_MS), label = "phrase") { text ->
        Text(
            text, color = color, fontSize = fontSize, lineHeight = (fontSize.value * 1.3f).sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
        )
    }
}
