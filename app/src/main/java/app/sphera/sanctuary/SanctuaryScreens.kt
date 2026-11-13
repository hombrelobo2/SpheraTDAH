package app.sphera.sanctuary

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.R

private const val EXIT_HOLD_MS = 3_000L
private val LANDING_BG = Color(0xFF121212)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Sonido del Santuario: el elegido en Ajustes o, si no hay, el incluido (ruido marrón generado para Sphera). */
object SanctuarySound {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sanctuary_sound", Context.MODE_PRIVATE)
    fun custom(ctx: Context): Uri? = prefs(ctx).getString("uri", null)?.let(Uri::parse)
    fun setCustom(ctx: Context, uri: Uri?) = prefs(ctx).edit().putString("uri", uri?.toString()).apply()

    fun start(ctx: Context): MediaPlayer? = try {
        val player = custom(ctx)?.let { uri ->
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(ctx, uri)
                prepare()
            }
        } ?: MediaPlayer.create(ctx, R.raw.calm)
        player?.apply {
            isLooping = true
            start()
        }
    } catch (e: Exception) {
        // Si el archivo elegido ya no existe o no se puede leer, se usa el incluido.
        MediaPlayer.create(ctx, R.raw.calm)?.apply { isLooping = true; start() }
    }
}

/** Botón de pánico: pantalla negra, brillo mínimo, No molestar y sonido en bucle. Se sale manteniendo pulsado 3 s. */
@Composable
fun SanctuaryScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    BackHandler { } // el botón atrás no saca del Santuario: evita salidas accidentales

    LaunchedEffect(Unit) { SensoryMode.enter(ctx) }
    DisposableEffect(Unit) {
        // Brillo de esta ventana al mínimo y pantalla siempre encendida: funciona sin permisos especiales.
        val window = ctx.findActivity()?.window
        window?.let {
            it.attributes = it.attributes.apply { screenBrightness = 0.01f }
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val player = SanctuarySound.start(ctx)
        onDispose {
            player?.release()
            window?.let {
                it.attributes = it.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // Si se suelta antes de 3 s no pasa nada. Al cumplirse, una vibración avisa; al soltar, se sale.
                val releasedEarly = withTimeoutOrNull(EXIT_HOLD_MS) { waitForUpOrCancellation(); true }
                if (releasedEarly == null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onExit()
                }
            }
        },
    ) {
        Text(
            stringResource(R.string.sanctuary_text),
            Modifier.align(Alignment.Center).padding(32.dp),
            color = Color(0xFF8A8A8A), fontSize = 40.sp, textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.sanctuary_hint),
            Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(32.dp),
            color = Color(0xFF3A3A3A), fontSize = 18.sp, textAlign = TextAlign.Center,
        )
    }
}

/** Descompresión tras el Santuario: tres botones que llenan la pantalla. */
@Composable
fun LandingScreen(onStayCalm: () -> Unit, onCannotTalk: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(LANDING_BG).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BigButton(R.string.landing_stay, Color(0xFF1E2A2A), Modifier.weight(1f), onStayCalm)
        BigButton(R.string.landing_cannot_talk, Color(0xFF2A2420), Modifier.weight(1f), onCannotTalk)
        BigButton(R.string.landing_back, Color(0xFF20242E), Modifier.weight(1f)) {
            SensoryMode.restore(ctx)
            onBack()
        }
    }
}

@Composable
private fun BigButton(label: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFFE0E0E0)),
    ) { Text(stringResource(label), fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

/** Tarjeta para enseñar a otra persona cuando no se puede hablar. */
@Composable
fun CommunicationCardScreen() {
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.comm_title), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.comm_text), color = Color(0xFFCCCCCC), fontSize = 30.sp)
    }
}
