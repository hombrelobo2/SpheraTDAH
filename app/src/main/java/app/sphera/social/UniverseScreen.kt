package app.sphera.social

import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.sphera.BackBar
import app.sphera.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DAY = 86_400_000L
private val FREQUENCIES = listOf(
    7 to R.string.freq_weekly, 14 to R.string.freq_biweekly,
    30 to R.string.freq_monthly, 90 to R.string.freq_quarterly,
)
private val NEAR = Color(0xFF66BB6A)
private val FAR = Color(0xFF78909C)

fun daysSince(c: Contact, now: Long) = ((now - c.lastContact) / DAY).toInt().coerceAtLeast(0)

/** 0 = pegado al sol, 1 = órbita exterior (se alcanza al pasar el doble de la frecuencia elegida). */
fun distance(c: Contact, now: Long) = (daysSince(c, now) / (2f * c.frequencyDays)).coerceIn(0f, 1f)

@Composable
fun UniverseScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var picked by remember { mutableStateOf<Picked?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Interactions.syncCallLog(ctx)
    }
    LaunchedEffect(Unit) {
        if (!Interactions.hasPermissions(ctx)) permLauncher.launch(Interactions.PERMISSIONS)
    }
    LifecycleResumeEffect(Unit) {
        Interactions.syncCallLog(ctx)
        now = System.currentTimeMillis()
        onPauseOrDispose { }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val p = result.data?.data?.let { Interactions.readPicked(ctx, it) } ?: return@rememberLauncherForActivityResult
        if (Store.contacts.any { Interactions.samePhone(it.phone, p.phone) }) {
            Toast.makeText(ctx, R.string.already_added, Toast.LENGTH_SHORT).show()
        } else {
            picked = p
        }
    }

    Scaffold(
        topBar = { BackBar(R.string.skill_social, onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }) { Icon(Icons.Default.Add, stringResource(R.string.add_contact)) }
        },
    ) { padding ->
        Space(Store.contacts, now, Modifier.padding(padding)) { selectedId = it.id }
    }

    picked?.let { p ->
        AddDialog(p, onDismiss = { picked = null }) { freq ->
            val id = System.currentTimeMillis()
            val last = Interactions.lastCall(ctx, p.phone) ?: id
            Store.upsert(Contact(id, p.name, p.phone, Interactions.savePhoto(ctx, p.photoUri, id), freq, last))
            picked = null
        }
    }
    Store.contacts.firstOrNull { it.id == selectedId }?.let { c ->
        PlanetDialog(c, now, onDismiss = { selectedId = null })
    }
}

@Composable
private fun Space(contacts: List<Contact>, now: Long, modifier: Modifier, onTap: (Contact) -> Unit) {
    // Una vuelta completa cada 4 minutos; se lee solo al dibujar para no recomponer en cada fotograma.
    val time = rememberInfiniteTransition(label = "orbit").animateFloat(
        0f, 1f, infiniteRepeatable(tween(240_000, easing = LinearEasing)), label = "t",
    )
    BoxWithConstraints(
        modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1A2340), Color(0xFF05070D)))),
    ) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val planet = with(density) { 56.dp.toPx() }
        val labelHalf = with(density) { 40.dp.toPx() }
        val minR = with(density) { 72.dp.toPx() }
        // Órbitas elípticas para aprovechar toda la pantalla.
        fun radii(d: Float) = Pair(
            minR + d * (w / 2 - planet - minR).coerceAtLeast(0f),
            minR + d * (h / 2 - planet - minR).coerceAtLeast(0f),
        )

        Canvas(Modifier.fillMaxSize()) {
            contacts.forEach { c ->
                val (rx, ry) = radii(distance(c, now))
                drawOval(
                    Color.White.copy(alpha = 0.08f), Offset(w / 2 - rx, h / 2 - ry), Size(2 * rx, 2 * ry),
                    style = Stroke(1.dp.toPx()),
                )
            }
        }

        Box(
            Modifier.align(Alignment.Center).size(80.dp).background(
                Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFFF6F00))), CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.me), color = Color(0xFF5D2E00), fontWeight = FontWeight.Bold)
        }

        if (contacts.isEmpty()) {
            Text(
                stringResource(R.string.empty_universe),
                Modifier.align(Alignment.BottomCenter).padding(32.dp),
                color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
            )
        }

        contacts.forEachIndexed { i, c ->
            val d = distance(c, now)
            val (rx, ry) = radii(d)
            val laps = 1 + ((1 - d) * 3).roundToInt() // más cerca = más rápido; entero para que no salte al reiniciar
            val phase = i * 2.39996f // ángulo áureo: reparte los planetas sin que se amontonen
            Planet(
                c, d,
                Modifier.offset {
                    val a = phase + time.value * 2 * PI.toFloat() * laps
                    IntOffset((w / 2 + rx * cos(a) - labelHalf).roundToInt(), (h / 2 + ry * sin(a) - planet / 2).roundToInt())
                },
            ) { onTap(c) }
        }
    }
}

@Composable
private fun Planet(c: Contact, d: Float, modifier: Modifier, onClick: () -> Unit) {
    val photo = remember(c.photo) { c.photo?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    Column(
        modifier.width(80.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(lerp(NEAR, FAR, d))
                .border(2.dp, if (d >= 1f) Color(0xFFEF5350) else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, c.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                val initials = c.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Text(c.name.substringBefore(' '), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyChips(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FREQUENCIES.forEach { (days, label) ->
            FilterChip(selected = selected == days, onClick = { onSelect(days) }, label = { Text(stringResource(label)) })
        }
    }
}

@Composable
private fun AddDialog(p: Picked, onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var freq by remember { mutableIntStateOf(30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.frequency))
                FrequencyChips(freq) { freq = it }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(freq) }) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PlanetDialog(c: Contact, now: Long, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val days = daysSince(c, now)
    val channels = remember(c.phone) { Interactions.channels(ctx, c.phone) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(c.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (days == 0) stringResource(R.string.days_today)
                    else pluralStringResource(R.plurals.days_since, days, days),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.send_via), style = MaterialTheme.typography.labelLarge)
                channels.forEach { ch ->
                    OutlinedButton(
                        onClick = {
                            // Escribir desde aquí cuenta como contacto: no podemos ver los mensajes que envías.
                            if (Interactions.open(ctx, ch.intent)) Store.touch(c.id, System.currentTimeMillis())
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(ch.label)) }
                }
                Button(
                    onClick = { Store.touch(c.id, System.currentTimeMillis()); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.mark_contacted)) }
                Text(stringResource(R.string.frequency), style = MaterialTheme.typography.labelLarge)
                FrequencyChips(c.frequencyDays) { Store.upsert(c.copy(frequencyDays = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = {
            TextButton(onClick = { if (confirmDelete) { Store.remove(c); onDismiss() } else confirmDelete = true }) {
                Text(
                    stringResource(if (confirmDelete) R.string.delete_confirm else R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
