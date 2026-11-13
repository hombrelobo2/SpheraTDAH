package app.sphera.returnpoint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.R
import kotlinx.coroutines.launch

// Estilo inspirado en Olauncher (https://github.com/tanujnotes/Olauncher, GPL-3.0):
// solo texto sobre negro, alineado a la izquierda, márgenes amplios y textos que se tocan sin forma de botón.
private val WHITE = Color(0xFFF2F2F2)
private val SOFT = Color(0xFFB8B8B8)
private val FAINT = Color(0xFF6E6E6E)
private val LINE = Color(0xFF3A3A3A)
private val MARGIN = 28.dp

/** Etiqueta diminuta en mayúsculas, como en Olauncher. */
@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = FAINT, fontSize = 12.sp, letterSpacing = 2.sp)
}

/** Texto que actúa como botón: grande, con un subrayado fino y sin caja. */
@Composable
private fun TextAction(text: String, color: Color, fontSize: TextUnit, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text, color = if (enabled) color else FAINT, fontSize = fontSize, lineHeight = (fontSize.value * 1.25f).sp,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp)
            .drawBehind {
                val y = size.height + 4.dp.toPx()
                drawLine(if (enabled) LINE else Color.Transparent, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            },
    )
}

/** Tarjeta que sustituye al inicio mientras haya un Punto de Retorno guardado. */
@Composable
fun ReturnCardScreen(onDone: () -> Unit) {
    val dao = SpheraDb.get(LocalContext.current).returnPoints()
    val point by remember { dao.observe() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(horizontal = MARGIN, vertical = 32.dp)) {
        Label(stringResource(R.string.rp_title))
        Spacer(Modifier.height(28.dp))
        // El scroll solo aparece si el texto no cabe.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(36.dp)) {
            point?.let { p ->
                // El siguiente paso es lo único que importa al volver: es el protagonista.
                Text(p.nextStep, color = WHITE, fontSize = 40.sp, lineHeight = 48.sp)
                Detail(stringResource(R.string.rp_where), p.contextLocation)
                Detail(stringResource(R.string.rp_tools), p.requiredTools)
            }
        }
        TextAction(stringResource(R.string.rp_resume), WHITE, 28.sp) { scope.launch { dao.clear(); onDone() } }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label(label)
        Text(value, color = SOFT, fontSize = 22.sp, lineHeight = 29.sp)
    }
}

/** Formulario para apuntar dónde lo dejas. Se abre desde el inicio o desde el widget. */
@Composable
fun ReturnFormScreen(onDone: () -> Unit) {
    val dao = SpheraDb.get(LocalContext.current).returnPoints()
    val scope = rememberCoroutineScope()
    var where by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var tools by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().imePadding()
            .padding(horizontal = MARGIN, vertical = 32.dp),
    ) {
        Label(stringResource(R.string.rp_form_title))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            LineField(stringResource(R.string.rp_where), where, 24.sp, Modifier.focusRequester(focus)) { where = it }
            LineField(stringResource(R.string.rp_next), next, 30.sp) { next = it }
            LineField(stringResource(R.string.rp_tools), tools, 24.sp) { tools = it }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction(stringResource(R.string.cancel), SOFT, 22.sp, onClick = onDone)
            TextAction(stringResource(R.string.rp_save), WHITE, 28.sp, enabled = next.isNotBlank()) {
                scope.launch {
                    dao.save(ReturnPoint(contextLocation = where.trim(), nextStep = next.trim(), requiredTools = tools.trim()))
                    onDone()
                }
            }
        }
    }
}

/** Campo sin caja: etiqueta diminuta encima y una línea fina debajo. */
@Composable
private fun LineField(label: String, value: String, fontSize: TextUnit, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(label)
        BasicTextField(
            value = value, onValueChange = onChange, maxLines = 3,
            textStyle = LocalTextStyle.current.merge(TextStyle(color = WHITE, fontSize = fontSize, lineHeight = (fontSize.value * 1.25f).sp)),
            cursorBrush = SolidColor(WHITE),
            modifier = modifier.fillMaxWidth().drawBehind {
                val y = size.height + 6.dp.toPx()
                drawLine(LINE, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            },
        )
    }
}
