package app.sphera.sanctuary

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.DocumentsContract
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.R

private const val TRIPLE_TAP_WINDOW_MS = 1_500L // los tres toques deben caber en este tiempo
private val LANDING_BG = Color(0xFF121212)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Sonido del Santuario: uno de los ruidos incluidos (generados para Sphera), un audio del móvil
 * o todas las canciones de una carpeta, en bucle y opcionalmente en orden aleatorio.
 */
object SanctuarySound {
    const val CUSTOM = "custom"
    const val FOLDER = "folder"
    const val MIXER = "mixer"

    /** Ruidos incluidos: clave guardada, nombre y archivo. */
    val BUILT_IN = listOf(
        Triple("brown", R.string.sound_brown, R.raw.calm),
        Triple("pink", R.string.sound_pink, R.raw.pink),
        Triple("white", R.string.sound_white, R.raw.white),
        Triple("waves", R.string.sound_waves, R.raw.waves),
    )

    /** Lo que está sonando; release() lo para y libera. */
    fun interface Playback {
        fun release()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("sanctuary_sound", Context.MODE_PRIVATE)
    fun custom(ctx: Context): Uri? = prefs(ctx).getString("uri", null)?.let(Uri::parse)
    fun folder(ctx: Context): Uri? = prefs(ctx).getString("folder", null)?.let(Uri::parse)
    fun shuffle(ctx: Context) = prefs(ctx).getBoolean("shuffle", false)
    fun setShuffle(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("shuffle", on).apply()

    /** Sonido elegido: una clave de [BUILT_IN], [CUSTOM] o [FOLDER]. */
    fun choice(ctx: Context): String =
        prefs(ctx).getString("choice", null) ?: if (custom(ctx) != null) CUSTOM else BUILT_IN.first().first

    fun choose(ctx: Context, key: String) = prefs(ctx).edit().putString("choice", key).apply()

    fun setCustom(ctx: Context, uri: Uri) = prefs(ctx).edit().putString("uri", uri.toString()).putString("choice", CUSTOM).apply()
    fun setFolder(ctx: Context, tree: Uri) = prefs(ctx).edit().putString("folder", tree.toString()).putString("choice", FOLDER).apply()

    /** Documento de la propia carpeta (para mostrar su nombre). */
    fun folderDocument(tree: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** Canciones de la carpeta (sin subcarpetas), por nombre. Se leen cada vez: lo que añadas aparece solo. */
    private fun tracks(ctx: Context, tree: Uri): List<Uri> = try {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        ctx.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null, null, null,
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    if (c.getString(1)?.startsWith("audio/") == true) add(c.getString(2).orEmpty() to c.getString(0))
                }
            }.sortedBy { it.first.lowercase() }.map { DocumentsContract.buildDocumentUriUsingTree(tree, it.second) }
        }.orEmpty()
    } catch (e: Exception) {
        emptyList()
    }

    private fun loop(player: MediaPlayer?): Playback? = player?.run {
        isLooping = true
        start()
        Playback { release() }
    }

    private fun builtIn(ctx: Context, key: String) =
        loop(MediaPlayer.create(ctx, (BUILT_IN.firstOrNull { it.first == key } ?: BUILT_IN.first()).third))

    private fun mediaPlayer(ctx: Context, uri: Uri) = MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
        setDataSource(ctx, uri)
        prepare()
    }

    fun start(ctx: Context): Playback? = try {
        val uri = custom(ctx)
        val tree = folder(ctx)
        when (val choice = choice(ctx)) {
            MIXER -> AmbientPlayer(ctx).run { playAll(Ambient.loadMix(ctx)); Playback { release() } }
            CUSTOM -> uri?.let { loop(mediaPlayer(ctx, it)) } ?: builtIn(ctx, "brown")
            FOLDER -> tree?.let { t -> tracks(ctx, t).takeIf { it.isNotEmpty() }?.let { FolderPlayback(ctx, it, shuffle(ctx)) } }
                ?: builtIn(ctx, "brown")
            else -> builtIn(ctx, choice)
        }
    } catch (e: Exception) {
        // Si lo elegido ya no existe o no se puede leer, suena el ruido marrón.
        builtIn(ctx, "brown")
    }

    /** Reproduce las canciones una tras otra y vuelve a empezar. Las que fallan se saltan sin cortar. */
    private class FolderPlayback(private val ctx: Context, tracks: List<Uri>, private val shuffle: Boolean) : Playback {
        private var order = if (shuffle) tracks.shuffled() else tracks
        private var index = -1
        private var current: MediaPlayer? = null
        private var fallback: Playback? = null
        private var failures = 0
        private var released = false

        init {
            next()
        }

        private fun next() {
            if (released) return
            current?.release()
            current = null
            if (failures >= order.size) { // ninguna se puede leer
                fallback = builtIn(ctx, "brown")
                return
            }
            index++
            if (index >= order.size) {
                index = 0
                if (shuffle) order = order.shuffled()
            }
            try {
                current = mediaPlayer(ctx, order[index]).apply {
                    setOnCompletionListener { failures = 0; next() }
                    setOnErrorListener { _, _, _ -> failures++; next(); true }
                    start()
                }
            } catch (e: Exception) {
                failures++
                next()
            }
        }

        override fun release() {
            released = true
            current?.release()
            fallback?.release()
        }
    }
}

/** Botón de pánico: pantalla negra, brillo mínimo, No molestar y sonido en bucle. */
@Composable
fun SanctuaryScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    // Con el mezclador ambiental se ve e interactúa: brillo normal en vez del mínimo.
    val mixer = remember { SanctuarySound.choice(ctx) == SanctuarySound.MIXER }
    BackHandler { } // el botón atrás no saca del Santuario: evita salidas accidentales

    LaunchedEffect(Unit) { SensoryMode.enter(ctx, dim = !mixer) }
    DisposableEffect(Unit) {
        // Brillo de esta ventana al mínimo y pantalla siempre encendida: funciona sin permisos especiales.
        val window = ctx.findActivity()?.window
        window?.let {
            if (!mixer) it.attributes = it.attributes.apply { screenBrightness = 0.01f }
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val player = if (mixer) null else SanctuarySound.start(ctx)
        onDispose {
            player?.release()
            window?.let {
                it.attributes = it.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    if (mixer) {
        MixerSanctuary(onExit)
        return
    }

    // Salida: tres toques seguidos en cualquier sitio, o el botón "Salir".
    val taps = remember { ArrayDeque<Long>() }
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectTapGestures(onTap = {
                val now = SystemClock.uptimeMillis()
                taps.addLast(now)
                while (taps.isNotEmpty() && now - taps.first() > TRIPLE_TAP_WINDOW_MS) taps.removeFirst()
                if (taps.size >= 3) {
                    taps.clear()
                    onExit()
                }
            })
        },
    ) {
        Text(
            stringResource(R.string.sanctuary_text),
            Modifier.align(Alignment.Center).padding(32.dp),
            color = Color(0xFF8A8A8A), fontSize = 40.sp, lineHeight = 50.sp, textAlign = TextAlign.Center,
        )
        ExitButton(onExit, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(32.dp))
    }
}

/** Botón "Salir" con halo que respira y una luz que recorre el borde. */
@Composable
fun ExitButton(onExit: () -> Unit, modifier: Modifier) {
    // Halo tenue que respira y una luz que recorre el borde despacio, como el lector de huella.
    val transition = rememberInfiniteTransition(label = "glow")
    val glow = transition.animateFloat(
        0.15f, 0.4f, infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha",
    )
    val angle = transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "angle",
    )
    OutlinedButton(
        onClick = onExit,
        modifier = modifier.fillMaxWidth().height(72.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val r = size.height / 2
                    val cx = size.width / 2
                    val cy = size.height / 2
                    // Halo de fondo
                    val halo = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 6.dp.toPx()
                        color = android.graphics.Color.argb((glow.value * 255).toInt(), 190, 205, 255)
                        maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, halo)
                    // Luz que gira: un degradado circular con un único destello, rotado según el ángulo
                    val light = 0xFFD6E0FF.toInt()
                    val clear = 0x00D6E0FF
                    val sweep = SweepGradient(cx, cy, intArrayOf(clear, clear, light, clear), floatArrayOf(0f, 0.7f, 0.88f, 1f))
                        .apply { setLocalMatrix(Matrix().apply { setRotate(angle.value, cx, cy) }) }
                    val comet = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        shader = sweep
                    }
                    comet.strokeWidth = 8.dp.toPx()
                    comet.maskFilter = BlurMaskFilter(10.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, comet)
                    comet.strokeWidth = 2.dp.toPx()
                    comet.maskFilter = null
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, comet)
                }
            },
        // Bien visible aunque discreto: mismo gris que el texto central, con borde marcado.
        border = BorderStroke(2.dp, Color(0xFF6A6A6A)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF141414), contentColor = Color(0xFFB0B0B0)),
    ) { Text(stringResource(R.string.sanctuary_exit), fontSize = 22.sp, lineHeight = 28.sp) }
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
    ) { Text(stringResource(label), fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

/** Tarjeta para enseñar a otra persona cuando no se puede hablar. */
@Composable
fun CommunicationCardScreen() {
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.comm_title), color = Color.White, fontSize = 44.sp, lineHeight = 55.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.comm_text), color = Color(0xFFCCCCCC), fontSize = 30.sp, lineHeight = 38.sp)
    }
}
