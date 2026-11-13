package app.sphera.andamio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Picker
import app.sphera.Palette
import app.sphera.R

private val GREY = Palette.textSoft

/** Tres preguntas frías → varios mensajes ya redactados para copiar o enviar. */
@Composable
fun AndamioScreen() {
    val ctx = LocalContext.current
    val templates = Andamio.parse(stringArrayResource(R.array.andamio_templates))
    val scenarios = remember(templates) { templates.map { it.scenario }.distinct() }
    var context by rememberSaveable { mutableStateOf("") }
    var need by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf("") }
    var showResults by rememberSaveable { mutableStateOf(false) }
    var scenario by rememberSaveable { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.andamio_title), color = Color.White, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        if (!showResults) {
            Text(stringResource(R.string.andamio_hint), color = GREY, fontSize = 18.sp, lineHeight = 24.sp)
            Question(R.string.andamio_q_context, R.string.andamio_ex_context, context) { context = it }
            Question(R.string.andamio_q_need, R.string.andamio_ex_need, need) { need = it }
            Question(R.string.andamio_q_action, R.string.andamio_ex_action, action) { action = it }
            Button(
                onClick = { showResults = true },
                enabled = context.isNotBlank() && need.isNotBlank() && action.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(88.dp),
            ) { Text(stringResource(R.string.andamio_build), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold) }
        } else {
            Picker(scenarios, scenario) { scenario = it }
            templates.filter { it.scenario == scenarios.getOrNull(scenario) }.forEach { t ->
                val text = Andamio.fill(t, context, need, action)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Palette.card, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(t.tone, color = GREY, fontSize = 16.sp, lineHeight = 20.sp)
                        Text(text, fontSize = 20.sp, lineHeight = 27.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                ctx.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(ClipData.newPlainText("Sphera", text))
                                Toast.makeText(ctx, R.string.andamio_copied, Toast.LENGTH_SHORT).show()
                            }) { Text(stringResource(R.string.andamio_copy)) }
                            OutlinedButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                                ctx.startActivity(Intent.createChooser(send, null))
                            }) { Text(stringResource(R.string.andamio_send)) }
                        }
                    }
                }
            }
            TextButton(onClick = { showResults = false }) {
                Text(stringResource(R.string.andamio_edit), color = GREY, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun Question(label: Int, example: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label), fontSize = 18.sp) },
        placeholder = { Text(stringResource(example), color = Color(0xFF5A5A5A), fontSize = 18.sp) },
        textStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, color = Color.White),
        maxLines = 3,
        // Sin mayúscula automática: las respuestas van en mitad de una frase.
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedLabelColor = Color.White, unfocusedLabelColor = GREY,
            unfocusedBorderColor = GREY, cursorColor = Color.White,
        ),
    )
}
