package app.sphera.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R
import app.sphera.appendDictation
import app.sphera.rememberDictation

private val PAGE = Modifier.fillMaxSize().background(Palette.night)

@Composable
private fun Title(text: String) =
    Text(text, color = Palette.text, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)

@Composable
private fun Section(text: String) =
    Text(text.uppercase(), color = Palette.textFaint, fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp))

@Composable
private fun ListRow(title: String, subtitle: String? = null, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.card).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.text, fontSize = 20.sp, lineHeight = 26.sp)
            subtitle?.let { Text(it, color = Palette.textSoft, fontSize = 15.sp, lineHeight = 20.sp) }
        }
        trailing?.invoke()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.text, unfocusedTextColor = Palette.text,
    unfocusedBorderColor = Palette.line, unfocusedLabelColor = Palette.textSoft,
)

/** Inicio del módulo: Primer paso, rutina a medias, rutinas propias y ejemplos. */
@Composable
fun StepsHomeScreen(onRun: (String) -> Unit, onEdit: (String) -> Unit, onFirstStep: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { StepsStore.init(ctx) }
    val session = remember { StepsStore.session(ctx) }
    Column(
        PAGE.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Title(stringResource(R.string.skill_steps))
        Button(onClick = onFirstStep, modifier = Modifier.fillMaxWidth().height(88.dp), shape = RoundedCornerShape(20.dp)) {
            Text(stringResource(R.string.steps_first), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        session?.let { s ->
            StepsStore.find(ctx, s.routineId)?.let { r ->
                Section(stringResource(R.string.steps_resume))
                ListRow(r.name, stringResource(R.string.steps_resume_at, s.index + 1, r.steps.size), onClick = { onRun(r.id) })
            }
        }
        Section(stringResource(R.string.steps_mine))
        StepsStore.routines.forEach { r ->
            ListRow(r.name, onClick = { onRun(r.id) }) {
                TextButton(onClick = { onEdit(r.id) }) { Text(stringResource(R.string.steps_edit), color = Palette.textSoft) }
            }
        }
        OutlinedButton(onClick = { onEdit("new") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.steps_new))
        }
        Section(stringResource(R.string.steps_examples))
        EXAMPLES.forEach { ex ->
            ListRow(stringResource(ex.name), onClick = { onRun(ex.id) }) {
                // Copiar un ejemplo crea una rutina propia; el original no cambia.
                TextButton(onClick = {
                    val r = ex.load(ctx)
                    StepsStore.upsert(r.copy(id = StepsStore.newId()))
                }) { Text(stringResource(R.string.steps_copy), color = Palette.textSoft) }
            }
        }
    }
}

/** Seguir una rutina: solo el paso actual, en grande. */
@Composable
fun RoutineRunnerScreen(routineId: String, onExit: () -> Unit) {
    val ctx = LocalContext.current
    StepsStore.init(ctx)
    val routine = remember { StepsStore.find(ctx, routineId) } ?: run { onExit(); return }
    val saved = remember { StepsStore.session(ctx)?.takeIf { it.routineId == routineId }?.index ?: 0 }
    var index by remember { mutableIntStateOf(saved.coerceIn(0, routine.steps.size)) }
    fun go(i: Int) {
        index = i
        StepsStore.saveSession(ctx, if (i < routine.steps.size) Session(routineId, i) else null)
    }
    LaunchedEffect(Unit) { go(index) } // empezar ya cuenta como "a medias", por si se sale sin pulsar nada

    Column(PAGE.safeDrawingPadding().padding(24.dp)) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text(routine.name, color = Palette.textSoft, fontSize = 17.sp, modifier = Modifier.weight(1f))
            // Pausa: se recuerda el paso y se puede seguir luego desde el inicio del módulo.
            if (index < routine.steps.size) TextButton(onClick = onExit) { Text(stringResource(R.string.steps_pause), color = Palette.textSoft) }
        }
        if (index >= routine.steps.size) {
            Spacer(Modifier.weight(1f))
            Title(stringResource(R.string.steps_done_title))
            Text(stringResource(R.string.steps_done_text), color = Palette.textSoft, fontSize = 20.sp, lineHeight = 27.sp, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.weight(1f))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(80.dp)) { Text(stringResource(R.string.back), fontSize = 22.sp) }
            TextButton(onClick = { go(0) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.steps_repeat), color = Palette.textSoft) }
            return@Column
        }
        Text(
            stringResource(R.string.steps_progress, index + 1, routine.steps.size),
            color = Palette.textFaint, fontSize = 15.sp, modifier = Modifier.padding(top = 24.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text(routine.steps[index], color = Palette.text, fontSize = 36.sp, lineHeight = 45.sp, modifier = Modifier.padding(vertical = 24.dp))
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { go((index - 1).coerceAtLeast(0)) }, enabled = index > 0, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(stringResource(R.string.steps_previous))
            }
            OutlinedButton(onClick = { go(index + 1) }, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(stringResource(R.string.steps_skip))
            }
        }
        Button(onClick = { go(index + 1) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(96.dp), shape = RoundedCornerShape(24.dp)) {
            Text(stringResource(R.string.steps_done), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { StepsStore.saveSession(ctx, null); onExit() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.steps_finish), color = Palette.textFaint)
        }
    }
}

/** Editor: nombre y pasos (escritos o dictados), con orden ajustable. */
@Composable
fun RoutineEditorScreen(routineId: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    StepsStore.init(ctx)
    val existing = remember { StepsStore.routines.firstOrNull { it.id == routineId } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val steps = remember { mutableStateListOf<String>().apply { addAll(existing?.steps ?: listOf("")) } }
    var askDelete by remember { mutableStateOf(false) }
    val dictate = rememberDictation { text -> steps.add(text) }
    val canSave = name.isNotBlank() && steps.any { it.isNotBlank() }

    Column(PAGE.safeDrawingPadding().imePadding().padding(20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.steps_name)) },
                textStyle = TextStyle(fontSize = 22.sp), singleLine = true, colors = fieldColors(),
            )
            Section(stringResource(R.string.steps_list))
            steps.forEachIndexed { i, step ->
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step, onValueChange = { steps[i] = it }, modifier = Modifier.weight(1f),
                        label = { Text("${i + 1}") }, textStyle = TextStyle(fontSize = 19.sp), colors = fieldColors(),
                    )
                    Column {
                        TextButton(onClick = { if (i > 0) { val s = steps.removeAt(i); steps.add(i - 1, s) } }, enabled = i > 0) { Text("↑") }
                        TextButton(onClick = { if (i < steps.size - 1) { val s = steps.removeAt(i); steps.add(i + 1, s) } }, enabled = i < steps.size - 1) { Text("↓") }
                    }
                    TextButton(onClick = { steps.removeAt(i); if (steps.isEmpty()) steps.add("") }) { Text("✕", color = Palette.textSoft) }
                }
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { steps.add("") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.steps_add)) }
                if (dictate != null) OutlinedButton(onClick = dictate) { Text("🎤") }
            }
        }
        androidx.compose.foundation.layout.Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (existing != null) {
                TextButton(onClick = { askDelete = true }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text(stringResource(R.string.cancel), color = Palette.textSoft) }
            Button(onClick = {
                StepsStore.upsert(Routine(existing?.id ?: StepsStore.newId(), name.trim(), steps.map { it.trim() }.filter { it.isNotEmpty() }))
                onDone()
            }, enabled = canSave) { Text(stringResource(R.string.rp_save)) }
        }
    }

    if (askDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(stringResource(R.string.steps_delete_title)) },
            confirmButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel)) } },
            dismissButton = {
                TextButton(onClick = {
                    StepsStore.delete(existing.id)
                    if (StepsStore.session(ctx)?.routineId == existing.id) StepsStore.saveSession(ctx, null)
                    onDone()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

/**
 * Primer paso: contra la parálisis. Se escribe la tarea que bloquea y solo se pide el gesto más pequeño para empezar.
 * Al hacerlo, se pide el siguiente. Nunca se ve la tarea entera. Al final se puede guardar como rutina.
 */
@Composable
fun FirstStepScreen(onDone: () -> Unit) {
    var task by remember { mutableStateOf("") }
    var started by remember { mutableStateOf(false) }
    val done = remember { mutableStateListOf<String>() }
    var current by remember { mutableStateOf<String?>(null) } // gesto en marcha
    var input by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val dictate = rememberDictation { input = appendDictation(input, it) }
    LaunchedEffect(started, current, finished) { if (!finished && current == null) runCatching { focus.requestFocus() } }

    Column(PAGE.safeDrawingPadding().imePadding().padding(24.dp)) {
        Text(stringResource(R.string.steps_first), color = Palette.textSoft, fontSize = 17.sp)
        Spacer(Modifier.height(24.dp))
        when {
            finished -> {
                Title(stringResource(R.string.steps_first_finished))
                Text(stringResource(R.string.steps_first_finished_text), color = Palette.textSoft, fontSize = 20.sp, lineHeight = 27.sp, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.weight(1f))
                if (done.isNotEmpty()) {
                    OutlinedButton(onClick = {
                        StepsStore.upsert(Routine(StepsStore.newId(), task.trim().ifEmpty { done.first() }, done.toList()))
                        onDone()
                    }, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.steps_first_save)) }
                }
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(80.dp)) { Text(stringResource(R.string.back), fontSize = 22.sp) }
            }
            current != null -> {
                // Solo el gesto en marcha, en grande.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                    Text(current!!, color = Palette.text, fontSize = 36.sp, lineHeight = 45.sp)
                }
                Button(onClick = {
                    done += current!!
                    current = null
                }, modifier = Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(24.dp)) {
                    Text(stringResource(R.string.steps_done), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                val askingTask = !started
                Title(stringResource(if (askingTask) R.string.steps_first_q_task else if (done.isEmpty()) R.string.steps_first_q_step else R.string.steps_first_q_next))
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).focusRequester(focus),
                    placeholder = { Text(stringResource(if (askingTask) R.string.steps_first_ex_task else R.string.steps_first_ex_step), color = Palette.textFaint) },
                    textStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp), maxLines = 4, colors = fieldColors(),
                )
                if (dictate != null) TextButton(onClick = dictate) { Text(stringResource(R.string.rp_dictate), color = Palette.textSoft) }
                Spacer(Modifier.weight(1f))
                if (!askingTask && done.isNotEmpty()) {
                    TextButton(onClick = { finished = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.steps_first_stop), color = Palette.textSoft, fontSize = 18.sp)
                    }
                }
                Button(onClick = {
                    if (askingTask) { task = input.trim(); started = true } else current = input.trim()
                    input = ""
                }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    Text(stringResource(R.string.steps_first_go), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
