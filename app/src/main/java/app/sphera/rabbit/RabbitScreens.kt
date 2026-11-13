package app.sphera.rabbit

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R
import app.sphera.appendDictation
import app.sphera.rememberDictation
import app.sphera.returnpoint.SpheraDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val GREY = Palette.textSoft
private const val MAX_QUESTION = 1000 // la captura es una idea, no un diario

/** Botón 🎤 de dictado. Si el móvil no tiene reconocedor de voz, no se muestra. */
@Composable
private fun DictateButton(modifier: Modifier, onText: (String) -> Unit) {
    val dictate = rememberDictation(onText) ?: return
    OutlinedButton(onClick = dictate, modifier = modifier) {
        Text("🎤", fontSize = 24.sp, lineHeight = 30.sp)
    }
}

private fun append(current: String, dictated: String) = appendDictation(current, dictated)

/** Captura en 5 segundos: el teclado sale solo, se escribe, se guarda y se vuelve a lo que se estaba haciendo. */
@Composable
fun RabbitCaptureScreen(onSaved: () -> Unit, onExplore: () -> Unit) {
    val ctx = LocalContext.current
    val dao = SpheraDb.get(ctx).rabbitHoles()
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // El botón de explorar solo aparece si ya hay alguna curiosidad aparcada.
    val hasAny by produceState(false) { value = dao.count() > 0 }

    val save = {
        if (text.isNotBlank()) {
            scope.launch {
                dao.add(RabbitHole(question = text.trim()))
                Toast.makeText(ctx, R.string.rabbit_saved, Toast.LENGTH_SHORT).show()
                onSaved()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.rabbit_title), color = Color.White, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.rabbit_capture_hint), color = GREY, fontSize = 20.sp, lineHeight = 26.sp)
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(MAX_QUESTION) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            textStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, color = Color.White),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            colors = darkFieldColors(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DictateButton(Modifier.weight(1f).height(96.dp)) { text = append(text, it).take(MAX_QUESTION) }
            Button(
                onClick = { save() }, enabled = text.isNotBlank(),
                modifier = Modifier.weight(2f).height(96.dp),
            ) { Text(stringResource(R.string.rabbit_save), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.weight(1f))
        if (hasAny) {
            OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Text(stringResource(R.string.rabbit_explore), fontSize = 22.sp, lineHeight = 28.sp)
            }
        }
    }
}

/** La Madriguera: todas las curiosidades, una por línea. Las largas se desplazan solas de lado a lado. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RabbitListScreen(onOpen: (Long) -> Unit) {
    val dao = SpheraDb.get(LocalContext.current).rabbitHoles()
    val holes by remember { dao.observeAll() }.collectAsState(initial = null)
    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.rabbit_explore), color = Color.White, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
        val list = holes ?: return@Column
        if (list.isEmpty()) {
            Text(stringResource(R.string.rabbit_empty), color = GREY, fontSize = 20.sp, lineHeight = 26.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { hole ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.card)
                        .clickable { onOpen(hole.id) }.padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Text(
                        hole.question.replace('\n', ' '),
                        Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
                        color = Color.White, fontSize = 22.sp, lineHeight = 28.sp, maxLines = 1,
                    )
                    if (hole.notes.isNotBlank()) Text("  📝", fontSize = 18.sp)
                }
            }
        }
    }
}

/** Ficha de una curiosidad: la pregunta completa y notas que se guardan solas. Nunca se marca como terminada. */
@Composable
fun RabbitDetailScreen(id: Long, onBack: () -> Unit) {
    val dao = SpheraDb.get(LocalContext.current).rabbitHoles()
    val scope = rememberCoroutineScope()
    var hole by remember { mutableStateOf<RabbitHole?>(null) }
    var notes by remember { mutableStateOf("") }
    var askDelete by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        hole = dao.get(id)
        notes = hole?.notes.orEmpty()
    }
    // Las notas se guardan solas, medio segundo después de dejar de escribir.
    LaunchedEffect(notes) {
        val current = hole ?: return@LaunchedEffect
        if (notes == current.notes) return@LaunchedEffect
        delay(500)
        val updated = current.copy(notes = notes)
        dao.update(updated)
        hole = updated
    }

    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(hole?.question.orEmpty(), color = Color.White, fontSize = 26.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rabbit_notes), fontSize = 18.sp) },
                textStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, color = Color.White),
                minLines = 4,
                colors = darkFieldColors(),
            )
            DictateButton(Modifier.height(56.dp)) { notes = append(notes, it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = GREY, fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            // Borrar existe para erratas o lo que ya no interesa; pide confirmación.
            TextButton(onClick = { askDelete = true }) {
                Text(stringResource(R.string.delete), color = Color(0xFFE57373), fontSize = 18.sp)
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(stringResource(R.string.rabbit_delete_title)) },
            confirmButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel)) } },
            dismissButton = {
                TextButton(onClick = {
                    askDelete = false
                    scope.launch {
                        dao.delete(id)
                        onBack()
                    }
                }) { Text(stringResource(R.string.delete), color = Color(0xFFE57373)) }
            },
        )
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White, unfocusedLabelColor = GREY,
    unfocusedBorderColor = GREY, cursorColor = Color.White,
)
