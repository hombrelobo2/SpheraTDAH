package app.sphera.rabbit

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import app.sphera.R
import app.sphera.returnpoint.SpheraDb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GREY = Color(0xFF9E9E9E)

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
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.rabbit_title), color = Color.White, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.rabbit_capture_hint), color = GREY, fontSize = 20.sp, lineHeight = 26.sp)
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            textStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, color = Color.White),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            colors = darkFieldColors(),
        )
        Button(
            onClick = { save() }, enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(96.dp),
        ) { Text(stringResource(R.string.rabbit_save), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        if (hasAny) {
            OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Text(stringResource(R.string.rabbit_explore), fontSize = 22.sp, lineHeight = 28.sp)
            }
        }
    }
}

/** Una sola curiosidad al azar, con notas debajo. Nada se tacha ni se completa. */
@Composable
fun RabbitExploreScreen() {
    val dao = SpheraDb.get(LocalContext.current).rabbitHoles()
    val scope = rememberCoroutineScope()
    var hole by remember { mutableStateOf<RabbitHole?>(null) }
    var notes by remember { mutableStateOf("") }
    var askDelete by remember { mutableStateOf(false) }

    fun show(next: RabbitHole?) {
        hole = next
        notes = next?.notes.orEmpty()
    }
    LaunchedEffect(Unit) { show(dao.random()) }
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
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                hole?.question ?: stringResource(R.string.rabbit_empty),
                color = Color.White, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
            )
            if (hole != null) {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rabbit_notes), fontSize = 18.sp) },
                    textStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, color = Color.White),
                    minLines = 4,
                    colors = darkFieldColors(),
                )
            }
        }
        if (hole != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Borrar existe para erratas o lo que ya no interesa; es discreto y pide confirmación.
                TextButton(onClick = { askDelete = true }, modifier = Modifier.height(96.dp)) {
                    Text(stringResource(R.string.delete), color = GREY, fontSize = 18.sp)
                }
                Button(
                    onClick = { scope.launch { show(dao.random(exclude = hole?.id ?: -1)) } },
                    modifier = Modifier.weight(1f).height(96.dp),
                ) { Text(stringResource(R.string.rabbit_next), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold) }
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
                    val id = hole?.id ?: return@TextButton
                    scope.launch {
                        dao.delete(id)
                        show(dao.random())
                    }
                }) { Text(stringResource(R.string.delete)) }
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
