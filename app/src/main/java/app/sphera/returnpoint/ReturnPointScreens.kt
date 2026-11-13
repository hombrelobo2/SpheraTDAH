package app.sphera.returnpoint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.R
import kotlinx.coroutines.launch

private val GREY = Color(0xFF9E9E9E)

/** Tarjeta que sustituye al inicio mientras haya un Punto de Retorno guardado. */
@Composable
fun ReturnCardScreen(onDone: () -> Unit) {
    val dao = SpheraDb.get(LocalContext.current).returnPoints()
    val point by remember { dao.observe() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp)) {
        // El texto puede ser largo; el scroll solo aparece si no cabe.
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.rp_title), color = GREY, fontSize = 22.sp, lineHeight = 28.sp)
            point?.let { p ->
                Field(R.string.rp_where, p.contextLocation)
                Field(R.string.rp_next, p.nextStep, big = true)
                Field(R.string.rp_tools, p.requiredTools)
            }
        }
        Button(
            onClick = { scope.launch { dao.clear(); onDone() } },
            modifier = Modifier.fillMaxWidth().height(120.dp),
        ) { Text(stringResource(R.string.rp_resume), fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Field(label: Int, value: String, big: Boolean = false) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label), color = GREY, fontSize = 18.sp, lineHeight = 22.sp)
        Text(value, color = Color.White, fontSize = if (big) 36.sp else 26.sp, lineHeight = if (big) 45.sp else 33.sp, fontWeight = if (big) FontWeight.Bold else null)
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
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.rp_form_title), color = Color.White, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        BigField(R.string.rp_where, where) { where = it }
        BigField(R.string.rp_next, next) { next = it }
        BigField(R.string.rp_tools, tools) { tools = it }
        Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f).height(96.dp)) {
                Text(stringResource(R.string.cancel), fontSize = 22.sp, lineHeight = 28.sp)
            }
            Button(
                enabled = next.isNotBlank(),
                onClick = {
                    scope.launch {
                        dao.save(ReturnPoint(contextLocation = where.trim(), nextStep = next.trim(), requiredTools = tools.trim()))
                        onDone()
                    }
                },
                modifier = Modifier.weight(1f).padding(start = 12.dp).height(96.dp),
                colors = ButtonDefaults.buttonColors(),
            ) { Text(stringResource(R.string.rp_save), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BigField(label: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label), fontSize = 18.sp, lineHeight = 22.sp) },
        textStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, color = Color.White),
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedLabelColor = Color.White, unfocusedLabelColor = GREY,
            unfocusedBorderColor = GREY, cursorColor = Color.White,
        ),
    )
}
