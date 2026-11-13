package app.sphera.cards

import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R
import app.sphera.sanctuary.findActivity

private fun onColor(c: Color) = if (c.luminance() > 0.45f) Color(0xFF1A1A1A) else Color.White

/** Cuadrícula de tarjetas. Toque: a pantalla completa. Pulsación larga: editar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardsListScreen(onShow: (String) -> Unit, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    CardsStore.init(ctx)
    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.skill_cards), color = Palette.text, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.cards_hint), color = Palette.textFaint, fontSize = 15.sp, modifier = Modifier.padding(bottom = 12.dp))
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(CardsStore.cards, key = { it.id }) { card ->
                val bg = Color(card.color)
                Box(
                    Modifier.aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(bg)
                        .combinedClickable(onClick = { onShow(card.id) }, onLongClick = { onEdit(card.id) })
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        card.text, color = onColor(bg), fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        OutlinedButton(onClick = { onEdit("new") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp)) {
            Text(stringResource(R.string.cards_new))
        }
    }
}

/** Tarjeta a pantalla completa y con el brillo al máximo, para que la lea otra persona. */
@Composable
fun CardShowScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    CardsStore.init(ctx)
    val card = CardsStore.find(id) ?: run { onBack(); return }
    DisposableEffect(Unit) {
        val window = ctx.findActivity()?.window
        window?.let {
            it.attributes = it.attributes.apply { screenBrightness = 1f }
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.let {
                it.attributes = it.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    val bg = Color(card.color)
    val fg = onColor(bg)
    Column(Modifier.fillMaxSize().background(bg).safeDrawingPadding().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text(card.text, color = fg, fontSize = 46.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold)
            if (card.description.isNotBlank()) {
                Text(card.description, color = fg.copy(alpha = 0.85f), fontSize = 26.sp, lineHeight = 34.sp, modifier = Modifier.padding(top = 20.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = fg, fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { CardsStore.ring(ctx) }) { Text(stringResource(R.string.cards_ring), color = fg, fontSize = 18.sp) }
        }
    }
}

/** Crear o editar una tarjeta: mensaje, detalle opcional, color y posición. */
@Composable
fun CardEditScreen(id: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    CardsStore.init(ctx)
    val existing = remember { CardsStore.find(id) }
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var color by remember { mutableLongStateOf(existing?.color ?: CARD_COLORS.first()) }
    var askDelete by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Palette.text, unfocusedTextColor = Palette.text,
        unfocusedBorderColor = Palette.line, unfocusedLabelColor = Palette.textSoft,
    )

    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding().padding(20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Vista previa con el color elegido
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(Color(color)).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text.ifBlank { "…" }, color = onColor(Color(color)), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cards_text)) }, textStyle = TextStyle(fontSize = 22.sp), colors = fieldColors,
            )
            OutlinedTextField(
                value = desc, onValueChange = { desc = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.cards_desc)) }, textStyle = TextStyle(fontSize = 18.sp), colors = fieldColors,
            )
            Text(stringResource(R.string.cards_color), color = Palette.textSoft)
            CARD_COLORS.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { c ->
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(Color(c))
                                .border(if (c == color) 3.dp else 1.dp, if (c == color) Palette.text else Palette.line, CircleShape)
                                .clickable { color = c },
                        )
                    }
                }
            }
            if (existing != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { CardsStore.move(existing.id, -1) }) { Text(stringResource(R.string.cards_move_up)) }
                    OutlinedButton(onClick = { CardsStore.move(existing.id, 1) }) { Text(stringResource(R.string.cards_move_down)) }
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (existing != null) {
                TextButton(onClick = { askDelete = true }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text(stringResource(R.string.cancel), color = Palette.textSoft) }
            Button(onClick = {
                CardsStore.upsert(CommCard(existing?.id ?: CardsStore.newId(), text.trim(), desc.trim(), color))
                onDone()
            }, enabled = text.isNotBlank()) { Text(stringResource(R.string.rp_save)) }
        }
    }

    if (askDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text(stringResource(R.string.cards_delete_title)) },
            confirmButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel)) } },
            dismissButton = {
                TextButton(onClick = { CardsStore.delete(existing.id); onDone() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
