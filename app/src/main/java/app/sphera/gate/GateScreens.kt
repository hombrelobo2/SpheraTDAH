package app.sphera.gate

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.Picker
import app.sphera.R
import app.sphera.appendDictation
import app.sphera.rememberDictation
import app.sphera.sanctuary.Breathing
import app.sphera.sanctuary.BreathingCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private class AppRow(val pkg: String, val label: String, val icon: ImageBitmap)

private fun LaunchableApp.row() = AppRow(pkg, label, icon.toBitmap(96).asImageBitmap())

@Composable
private fun AppIcon(icon: ImageBitmap, size: Int = 44) {
    Image(icon, null, Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp)))
}

/** Pantalla del módulo: las apps con pausa, cuánto dura la pausa y cómo ponerla en la pantalla de inicio. */
@Composable
fun GateHomeScreen(onAdd: () -> Unit) {
    val ctx = LocalContext.current
    SlowGate.init(ctx)
    val disabled = stringResource(R.string.gate_disabled)
    val noPin = stringResource(R.string.gate_no_pin)
    val rows by produceState<List<AppRow>?>(null, SlowGate.apps.toList()) {
        value = withContext(Dispatchers.IO) { SlowGate.apps.mapNotNull { SlowGate.find(ctx, it)?.row() } }
    }
    val breathOptions = listOf(R.string.gate_breaths_1, R.string.gate_breaths_2, R.string.gate_breaths_3).map { stringResource(it) }

    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.skill_gate), color = Palette.text, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.gate_hint), color = Palette.textFaint, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        Box(Modifier.weight(1f)) {
            val list = rows
            when {
                list == null -> Unit
                list.isEmpty() -> Text(stringResource(R.string.gate_empty), color = Palette.textSoft, fontSize = 18.sp, lineHeight = 25.sp)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.pkg }) { app ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.card).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.icon)
                            Text(
                                app.label, color = Palette.text, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            TextButton(onClick = {
                                val ok = SlowGate.find(ctx, app.pkg)?.let { SlowGate.pin(ctx, it) } ?: false
                                if (!ok) Toast.makeText(ctx, noPin, Toast.LENGTH_LONG).show()
                            }) { Text(stringResource(R.string.gate_pin), color = MaterialTheme.colorScheme.primary) }
                            TextButton(onClick = { SlowGate.remove(ctx, app.pkg, disabled) }) {
                                Text(stringResource(R.string.gate_remove), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            }
                        }
                    }
                    item { Text(stringResource(R.string.gate_tip), color = Palette.textFaint, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
        Text(stringResource(R.string.gate_breaths), color = Palette.textSoft, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Picker(breathOptions, SlowGate.breaths.intValue - 1) { SlowGate.setBreaths(ctx, it + 1) }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp)) {
            Text(stringResource(R.string.gate_add))
        }
    }
}

/** Elegir una app instalada. Al elegirla se pide a la pantalla de inicio que añada su acceso con pausa. */
@Composable
fun GatePickScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    SlowGate.init(ctx)
    val noPin = stringResource(R.string.gate_no_pin)
    var query by remember { mutableStateOf("") }
    val all by produceState<List<AppRow>?>(null) {
        value = withContext(Dispatchers.IO) { SlowGate.launchable(ctx).filter { it.pkg !in SlowGate.apps }.map { it.row() } }
    }
    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(16.dp)) {
        Text(stringResource(R.string.gate_pick_title), color = Palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text(stringResource(R.string.gate_search)) }, textStyle = TextStyle(fontSize = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Palette.text, unfocusedTextColor = Palette.text,
                unfocusedBorderColor = Palette.line, unfocusedLabelColor = Palette.textSoft,
            ),
        )
        Box(Modifier.weight(1f)) {
            val list = all
            if (list == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val shown = list.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
                LazyColumn {
                    items(shown, key = { it.pkg }) { app ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                SlowGate.add(ctx, app.pkg)
                                val ok = SlowGate.find(ctx, app.pkg)?.let { SlowGate.pin(ctx, it) } ?: false
                                if (!ok) Toast.makeText(ctx, noPin, Toast.LENGTH_LONG).show()
                                onDone()
                            }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.icon, 40)
                            Text(app.label, color = Palette.text, fontSize = 18.sp, modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                }
            }
        }
        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel), color = Palette.textSoft) }
    }
}

/**
 * La pausa: el círculo de respiración, "¿A qué vas?" y, tras las respiraciones elegidas, el botón de abrir.
 * Sin cuenta atrás visible: el botón simplemente aparece.
 */
@Composable
fun GatePauseScreen(pkg: String?, onOpen: (String, String) -> Unit, onLeave: () -> Unit) {
    val ctx = LocalContext.current
    val app = remember(pkg) { pkg?.let { SlowGate.find(ctx, it) } }
    val icon = remember(app) { app?.icon?.toBitmap(96)?.asImageBitmap() }
    val launchable = remember(pkg) { pkg != null && ctx.packageManager.getLaunchIntentForPackage(pkg) != null }
    val pattern = remember { Breathing.pattern(ctx) }
    var ready by remember { mutableStateOf(false) }
    var intention by remember { mutableStateOf("") }
    val dictate = rememberDictation { intention = appendDictation(intention, it).take(200) }
    LaunchedEffect(pkg) {
        delay(pattern.phases.sumOf { it.ms }.toLong() * SlowGate.breaths.intValue)
        ready = true
    }
    val openAlpha by animateFloatAsState(if (ready) 1f else 0f, tween(1200), label = "openAlpha")

    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (app == null || !launchable) {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.gate_missing), color = Palette.text, fontSize = 22.sp, lineHeight = 30.sp)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.back)) }
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                icon?.let { AppIcon(it, 36) }
                Text(
                    stringResource(R.string.gate_opening, app.label), color = Palette.textSoft, fontSize = 20.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            BreathingCircle(pattern, diameter = 190.dp, labelSize = 28.sp)
            OutlinedTextField(
                value = intention, onValueChange = { intention = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.gate_why)) }, textStyle = TextStyle(fontSize = 20.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Palette.text, unfocusedTextColor = Palette.text,
                    unfocusedBorderColor = Palette.line, unfocusedLabelColor = Palette.textSoft,
                ),
            )
            if (dictate != null) TextButton(onClick = dictate) { Text(stringResource(R.string.rp_dictate), color = Palette.textSoft) }
        }
        Button(
            onClick = { onOpen(app.pkg, intention) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(56.dp).alpha(openAlpha),
        ) { Text(stringResource(R.string.gate_open, app.label), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp)) {
            Text(stringResource(R.string.gate_not_now), fontSize = 18.sp)
        }
    }
}
