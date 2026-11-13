/*
 * Sphera — respiración guiada del Santuario.
 * Modelo de fases y patrones adaptado de Brethap (https://github.com/jithware/brethap), © jithware, GPL-3.0:
 * inhalar, mantener, segundo impulso, exhalar y mantener vacío, cada uno con su duración.
 */
package app.sphera.sanctuary

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.R
import kotlinx.coroutines.delay

/** Una fase: qué se muestra, cuánto dura y hasta qué tamaño va el círculo (null = se queda como está). */
class BreathPhase(@StringRes val label: Int, val ms: Int, val scale: Float?)

class BreathPattern(val key: String, @StringRes val name: Int, val phases: List<BreathPhase>)

private const val SMALL = 0.45f
private const val FULL = 1f

private fun inhale(ms: Int) = BreathPhase(R.string.breath_in, ms, FULL)
private fun exhale(ms: Int) = BreathPhase(R.string.breath_out, ms, SMALL)
private fun hold(ms: Int) = BreathPhase(R.string.breath_hold, ms, null)

object Breathing {
    val PATTERNS = listOf(
        // Patrón por defecto de Brethap: 4 s dentro, 4 s fuera.
        BreathPattern("even", R.string.breath_even, listOf(inhale(4000), exhale(4000))),
        BreathPattern("long_out", R.string.breath_long_out, listOf(inhale(4000), exhale(6000))),
        BreathPattern("box", R.string.breath_box, listOf(inhale(4000), hold(4000), exhale(4000), hold(4000))),
        BreathPattern("478", R.string.breath_478, listOf(inhale(4000), hold(7000), exhale(8000))),
        // Suspiro fisiológico de Brethap (1 s + 0,5 s + 0,5 s + 3 s) a la mitad de velocidad, más cómodo de seguir.
        BreathPattern(
            "sigh", R.string.breath_sigh,
            listOf(inhale(2000), hold(1000), BreathPhase(R.string.breath_in_more, 1000, 1.12f), exhale(6000)),
        ),
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sanctuary", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("breath", false)
    fun setEnabled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("breath", on).apply()
    fun pattern(ctx: Context) = PATTERNS.firstOrNull { it.key == prefs(ctx).getString("breath_pattern", null) } ?: PATTERNS.first()
    fun setPattern(ctx: Context, key: String) = prefs(ctx).edit().putString("breath_pattern", key).apply()
}

// Curva suave de subida y bajada, como una respiración (sin arranques ni frenazos).
private val BREATH_EASING = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/** Círculo que se hincha y se deshincha con la palabra de la fase debajo. Sin números ni cuenta atrás. */
@Composable
fun BreathingCircle(pattern: BreathPattern, color: Color, modifier: Modifier = Modifier) {
    val size = remember { Animatable(SMALL) }
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(pattern) {
        while (true) {
            pattern.phases.forEachIndexed { i, p ->
                phase = i
                val target = p.scale
                if (target == null) delay(p.ms.toLong()) else size.animateTo(target, tween(p.ms, easing = BREATH_EASING))
            }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(240.dp).scale(size.value)
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.12f))), CircleShape),
            )
        }
        Text(stringResource(pattern.phases[phase].label), color = Color(0xFF9A9A9A), fontSize = 26.sp, lineHeight = 32.sp)
    }
}
