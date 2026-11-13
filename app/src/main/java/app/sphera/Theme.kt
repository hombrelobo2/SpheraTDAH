package app.sphera

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Estilo "Noche serena": azul noche casi negro, tarjetas un tono más claras y un color apagado por módulo.
 * Siempre oscuro y con colores propios (no los del fondo de pantalla). Santuario, Aterrizaje y Punto de Retorno
 * siguen en negro puro por diseño.
 */
object Palette {
    val night = Color(0xFF0F1218)
    val card = Color(0xFF181D26)
    val cardHigh = Color(0xFF1F2531)
    val line = Color(0xFF2A3140)
    val text = Color(0xFFE7E9EE)
    val textSoft = Color(0xFFA9B0BF)
    val textFaint = Color(0xFF6E7687)
    val accent = Color(0xFF9DB7E0)

    // Un color por módulo: se repite en su casilla, su caja de Ajustes y sus botones.
    val social = Color(0xFF8FB8A8)
    val sanctuary = Color(0xFFA9A1D6)
    val returnPoint = Color(0xFFD9B98C)
    val rabbit = Color(0xFFC99A8E)
    val steps = Color(0xFF8EB6D9)
    val body = Color(0xFFD99AAE)
    val cards = Color(0xFF7CC6C0)
    val soon = Color(0xFF6E7687)
}

private val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private val SpheraColors = darkColorScheme(
    primary = Palette.accent, onPrimary = Palette.night,
    primaryContainer = Color(0xFF243247), onPrimaryContainer = Palette.text,
    secondary = Palette.social, onSecondary = Palette.night,
    secondaryContainer = Color(0xFF223330), onSecondaryContainer = Palette.text,
    tertiary = Palette.sanctuary, onTertiary = Palette.night,
    tertiaryContainer = Color(0xFF2A2840), onTertiaryContainer = Palette.text,
    background = Palette.night, onBackground = Palette.text,
    surface = Palette.night, onSurface = Palette.text,
    surfaceVariant = Palette.card, onSurfaceVariant = Palette.textSoft,
    surfaceContainerLowest = Palette.night, surfaceContainerLow = Palette.card,
    surfaceContainer = Palette.card, surfaceContainerHigh = Palette.cardHigh, surfaceContainerHighest = Palette.cardHigh,
    outline = Palette.textFaint, outlineVariant = Palette.line,
    error = Color(0xFFE57373), onError = Palette.night,
)

private val SpheraType = Typography().let { t ->
    fun TextStyle.a() = copy(fontFamily = Atkinson)
    Typography(
        displayLarge = t.displayLarge.a(), displayMedium = t.displayMedium.a(), displaySmall = t.displaySmall.a(),
        headlineLarge = t.headlineLarge.a(), headlineMedium = t.headlineMedium.a(), headlineSmall = t.headlineSmall.a(),
        titleLarge = t.titleLarge.a().copy(fontWeight = FontWeight.Bold), titleMedium = t.titleMedium.a().copy(fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.a(),
        bodyLarge = t.bodyLarge.a().copy(fontSize = 17.sp, lineHeight = 24.sp), bodyMedium = t.bodyMedium.a().copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = t.bodySmall.a().copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = t.labelLarge.a().copy(fontSize = 15.sp), labelMedium = t.labelMedium.a(), labelSmall = t.labelSmall.a(),
    )
}

/**
 * Tamaño de letra de toda la app (idea de Olauncher): multiplica la escala de letra del sistema.
 * Se elige en Ajustes → General y se aplica al momento.
 */
object TextScale {
    val OPTIONS = listOf(0.9f, 1f, 1.15f, 1.3f)
    var value by mutableFloatStateOf(1f)
        private set

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun load(ctx: Context) {
        value = prefs(ctx).getFloat("text_scale", 1f)
    }

    fun set(ctx: Context, scale: Float) {
        value = scale
        prefs(ctx).edit().putFloat("text_scale", scale).apply()
    }
}

@Composable
fun SpheraTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    MaterialTheme(colorScheme = SpheraColors, typography = SpheraType) {
        // La tipografía también para los textos que no usan un estilo del tema.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Atkinson),
            LocalDensity provides Density(density.density, density.fontScale * TextScale.value),
            content = content,
        )
    }
}

/** Tiñe botones, campos activos y casillas de un módulo con su color. */
@Composable
fun ModuleTheme(color: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = color, onPrimary = Palette.night,
            primaryContainer = color.copy(alpha = 0.22f).compositeOver(Palette.night), onPrimaryContainer = Palette.text,
        ),
        typography = MaterialTheme.typography,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(@StringRes title: Int, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        },
        actions = actions,
    )
}

/** Desplegable sencillo: muestra [options][selected] y devuelve el índice elegido. */
@Composable
fun Picker(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options.getOrElse(selected) { "" } + "  ▾")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}
