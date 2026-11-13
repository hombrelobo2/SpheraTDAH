package app.sphera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.sphera.gate.SlowGate
import app.sphera.update.AutoUpdate
import app.sphera.update.ISSUES_URL
import app.sphera.update.MANUAL_URL
import app.sphera.update.REPO_URL
import app.sphera.update.UpdateCheck
import app.sphera.update.openUrl
import kotlinx.coroutines.launch
import app.sphera.sanctuary.Breathing
import app.sphera.sanctuary.CalmPhrases
import app.sphera.sanctuary.SanctuarySound
import app.sphera.sanctuary.SensoryMode
import app.sphera.social.Alerts
import app.sphera.social.DailySummary
import app.sphera.social.Interactions
import app.sphera.social.Messages
import app.sphera.social.Notifier

/** Nombre visible de un archivo elegido con el selector del sistema. */
private fun fileName(ctx: Context, uri: Uri): String =
    runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()

private val ALERTS = listOf(
    Alerts.DRIFT to R.string.alert_drift_setting,
    Alerts.BIRTHDAY to R.string.alert_birthday_setting,
    Alerts.SNOOZE_END to R.string.alert_snooze_setting,
)
private val HOURS = (0..23).map { "%02d:00".format(it) }
private val LANGUAGES = listOf("" to R.string.lang_system, "es" to R.string.lang_es, "en" to R.string.lang_en)
private val TEXT_SIZES = listOf(R.string.text_size_small, R.string.text_size_normal, R.string.text_size_large, R.string.text_size_xlarge)

/** Un permiso que falta: qué es, para qué sirve y cómo se concede. */
private class MissingPermission(@StringRes val title: Int, @StringRes val desc: Int, val grant: () -> Unit)

/**
 * Ajustes en tres capas: arriba solo lo que falta por activar, luego una fila plegada por módulo
 * que ya dice cómo está, y dentro, filas iguales (título, una línea gris y el control a la derecha).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onCredits: () -> Unit, onMessages: () -> Unit) {
    val ctx = LocalContext.current
    // Se incrementa al volver a la pantalla para releer los permisos.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val missing = remember(refresh) {
        buildList {
            if (!Interactions.hasPermissions(ctx)) {
                add(MissingPermission(R.string.perm_calls, R.string.perm_calls_desc) { permLauncher.launch(Interactions.PERMISSIONS) })
            }
            if (!NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)) {
                add(MissingPermission(R.string.perm_notifications, R.string.perm_notifications_desc) {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
            }
            if (ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) != true) {
                add(MissingPermission(R.string.perm_battery, R.string.perm_battery_desc) {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
                })
            }
            if (!SensoryMode.canChangeDnd(ctx)) {
                add(MissingPermission(R.string.perm_dnd, R.string.perm_dnd_desc) { ctx.startActivity(SensoryMode.dndSettings()) })
            }
        }
    }
    // Solo una sección abierta a la vez; al entrar, todas cerradas.
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(key: String) { open = if (open == key) null else key }

    Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding()) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Palette.textSoft) }
            Text(stringResource(R.string.settings), color = Palette.text, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (missing.isNotEmpty()) PermissionsCard(missing)
            GeneralSection(open == "general") { toggle("general") }
            HomeSection(open == "home") { toggle("home") }
            SocialSection(open == "social", refresh, onMessages) { toggle("social") }
            SanctuarySection(open == "sanctuary") { toggle("sanctuary") }
            GateSection(open == "gate") { toggle("gate") }

            AboutSection(open == "about", onCredits) { toggle("about") }
        }
    }
}

// ——— Piezas comunes ———

/** Solo aparece si falta algún permiso; cuando están todos, desaparece. */
@Composable
private fun PermissionsCard(missing: List<MissingPermission>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.cardHigh)
            .border(1.dp, Palette.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_missing_title), color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.settings_missing_desc), color = Palette.textFaint, fontSize = 14.sp, lineHeight = 19.sp)
        missing.forEachIndexed { i, perm ->
            if (i > 0) HorizontalDivider(color = Palette.line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(perm.title), color = Palette.text, fontSize = 17.sp)
                    Text(stringResource(perm.desc), color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Button(onClick = perm.grant) { Text(stringResource(R.string.grant)) }
            }
        }
    }
}

/** Fila plegable de un módulo: emoji, nombre, cómo está ahora y, al tocar, sus ajustes. */
@Composable
private fun ModuleSection(
    emoji: String, title: String, summary: String, color: Color,
    expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    val arrow by animateFloatAsState(if (expanded) 90f else 0f, label = "arrow")
    ModuleTheme(color) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.card)
                .drawBehind { drawRect(color, size = Size(5.dp.toPx(), size.height)) },
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = 26.sp)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(title, color = Palette.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(summary, color = Palette.textFaint, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("›", color = Palette.textSoft, fontSize = 26.sp, modifier = Modifier.rotate(arrow))
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(
                    Modifier.padding(start = 20.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
            }
        }
    }
}

/** Fila estándar: título, una línea gris opcional y el control a la derecha. */
@Composable
private fun SettingRow(title: String, desc: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Palette.text, fontSize = 17.sp)
            if (desc != null) Text(desc, color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
        }
        trailing()
    }
}

@Composable
private fun SwitchRow(title: String, desc: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) =
    SettingRow(title, desc) { Switch(checked = checked, onCheckedChange = onChange) }

/** Desplegable pequeño para poner a la derecha de una fila. */
@Composable
private fun CompactPicker(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(options.getOrElse(selected) { "" } + "  ▾", maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(i); expanded = false })
            }
        }
    }
}

// ——— Secciones ———

@Composable
private fun GeneralSection(expanded: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val langIndex = LANGUAGES.indexOfFirst { it.first == currentLang }.coerceAtLeast(0)
    val langLabels = LANGUAGES.map { stringResource(it.second) }
    val sizeIndex = TextScale.OPTIONS.indexOf(TextScale.value).let { if (it < 0) 1 else it }
    val sizeLabel = stringResource(TEXT_SIZES[sizeIndex])
    ModuleSection(
        "⚙️", stringResource(R.string.settings_general),
        stringResource(R.string.settings_sum_general, langLabels[langIndex], sizeLabel.lowercase()),
        Palette.accent, expanded, onToggle,
    ) {
        SettingRow(stringResource(R.string.language)) {
            CompactPicker(langLabels, langIndex) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(LANGUAGES[it].first))
            }
        }
        Text(stringResource(R.string.text_size), color = Palette.text, fontSize = 17.sp)
        // Cuatro "A" de tamaño creciente: se nota el cambio al momento.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextScale.OPTIONS.forEachIndexed { i, scale ->
                val selected = i == sizeIndex
                Box(
                    Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Palette.accent.copy(alpha = 0.18f) else Palette.cardHigh)
                        .border(if (selected) 2.dp else 1.dp, if (selected) Palette.accent else Palette.line, RoundedCornerShape(14.dp))
                        .clickable { TextScale.set(ctx, scale) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = Palette.text, fontSize = (16 + i * 5).sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(sizeLabel, color = Palette.textFaint, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** Orden de las casillas del inicio (flechas) y cuáles se ven (interruptor). */
@Composable
private fun HomeSection(expanded: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val all = HomeLayout.ordered()
    val visible = HomeLayout.visible().size
    val summary = if (visible == all.size) stringResource(R.string.home_sum_all, all.size)
        else stringResource(R.string.home_sum_some, visible, all.size)
    ModuleSection("🏠", stringResource(R.string.home_title), summary, Palette.accent, expanded, onToggle) {
        Text(stringResource(R.string.home_desc), color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
        all.forEachIndexed { i, skill ->
            val shown = skill.key !in HomeLayout.hidden
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill.emoji, fontSize = 22.sp, modifier = Modifier.alpha(if (shown) 1f else 0.35f))
                Text(
                    stringResource(skill.label), color = if (shown) Palette.text else Palette.textFaint, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                IconButton(onClick = { HomeLayout.move(ctx, skill.key, -1) }, enabled = i > 0) {
                    Text("↑", fontSize = 20.sp, color = if (i > 0) Palette.textSoft else Palette.line)
                }
                IconButton(onClick = { HomeLayout.move(ctx, skill.key, 1) }, enabled = i < all.lastIndex) {
                    Text("↓", fontSize = 20.sp, color = if (i < all.lastIndex) Palette.textSoft else Palette.line)
                }
                Switch(
                    checked = shown, enabled = !shown || visible > 1,
                    onCheckedChange = { HomeLayout.setVisible(ctx, skill.key, it) },
                )
            }
        }
        if ("sanctuary" in HomeLayout.hidden) {
            Text(stringResource(R.string.home_sanctuary_hidden), color = Palette.sanctuary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SocialSection(expanded: Boolean, refresh: Int, onMessages: () -> Unit, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    var summaryOn by remember { mutableStateOf(DailySummary.isEnabled(ctx)) }
    var summaryHour by remember { mutableIntStateOf(DailySummary.hour(ctx)) }
    var alerts by remember(refresh) { mutableStateOf(ALERTS.associate { it.first to Alerts.isEnabled(ctx, it.first) }) }
    val messageCount = remember(refresh) { Messages.list(ctx).size }
    // Al activar cualquier aviso se pide antes el permiso de notificaciones; [afterPermission] aplica el cambio.
    var afterPermission by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        afterPermission?.invoke(granted)
        afterPermission = null
    }
    fun withNotifications(on: Boolean, apply: (Boolean) -> Unit) {
        if (on && !Notifier.canNotify(ctx)) {
            afterPermission = apply
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            apply(on)
        }
    }
    val alertsOn = alerts.count { it.value }
    val summary = listOf(
        if (summaryOn) stringResource(R.string.settings_sum_summary_on, HOURS[summaryHour]) else stringResource(R.string.settings_sum_summary_off),
        if (alertsOn == 0) stringResource(R.string.settings_sum_alerts_off) else pluralStringResource(R.plurals.settings_sum_alerts, alertsOn, alertsOn),
    ).joinToString(" · ")

    ModuleSection("🪐", stringResource(R.string.skill_social), summary, Palette.social, expanded, onToggle) {
        SwitchRow(stringResource(R.string.summary_title), stringResource(R.string.summary_desc), summaryOn) { on ->
            withNotifications(on) {
                summaryOn = it
                DailySummary.configure(ctx, it, summaryHour)
            }
        }
        if (summaryOn) {
            SettingRow(stringResource(R.string.summary_hour)) {
                CompactPicker(HOURS, summaryHour) {
                    summaryHour = it
                    DailySummary.configure(ctx, true, it)
                }
            }
        }
        HorizontalDivider(color = Palette.line)
        SettingRow(stringResource(R.string.alerts_title), stringResource(R.string.alerts_desc))
        ALERTS.forEach { (key, label) ->
            SwitchRow(stringResource(label), checked = alerts[key] == true) { on ->
                withNotifications(on) {
                    Alerts.set(ctx, key, it)
                    alerts = alerts + (key to it)
                }
            }
        }
        HorizontalDivider(color = Palette.line)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onMessages).padding(vertical = 6.dp)) {
            SettingRow(stringResource(R.string.settings_messages_row, messageCount)) {
                Text("›", color = Palette.textSoft, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun SanctuarySection(expanded: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    var phrasesOn by remember { mutableStateOf(CalmPhrases.enabled(ctx)) }
    var breathKey by remember { mutableStateOf(Breathing.pattern(ctx).key) }
    var soundChoice by remember { mutableStateOf(SanctuarySound.choice(ctx)) }
    var customSound by remember { mutableStateOf(SanctuarySound.custom(ctx)) }
    var folderSound by remember { mutableStateOf(SanctuarySound.folder(ctx)) }
    var shuffle by remember { mutableStateOf(SanctuarySound.shuffle(ctx)) }
    // Prueba del sonido elegido sin entrar al Santuario.
    var preview by remember { mutableStateOf<SanctuarySound.Playback?>(null) }
    fun stopPreview() {
        preview?.release()
        preview = null
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }
    fun choose(key: String) {
        soundChoice = key
        SanctuarySound.choose(ctx, key)
        if (preview != null) { stopPreview(); preview = SanctuarySound.start(ctx) }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            SanctuarySound.setFolder(ctx, tree)
            folderSound = tree
            choose(SanctuarySound.FOLDER)
        }
    }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Permiso permanente para poder leer el archivo también después de reiniciar.
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            SanctuarySound.setCustom(ctx, uri)
            customSound = uri
            choose(SanctuarySound.CUSTOM)
        }
    }
    // El audio y la carpeta propios están siempre en la lista; si aún no hay ninguno, elegirlos abre el selector.
    val soundKeys = listOf(SanctuarySound.MIXER) + SanctuarySound.BUILT_IN.map { it.first } + listOf(SanctuarySound.CUSTOM, SanctuarySound.FOLDER)
    val soundLabels = listOf(stringResource(R.string.sound_mixer)) + SanctuarySound.BUILT_IN.map { stringResource(it.second) } + listOf(
        customSound?.let { stringResource(R.string.sound_custom, fileName(ctx, it)) } ?: stringResource(R.string.sound_custom_option),
        folderSound?.let { stringResource(R.string.sound_folder, fileName(ctx, SanctuarySound.folderDocument(it))) }
            ?: stringResource(R.string.sound_folder_option),
    )
    val soundIndex = soundKeys.indexOf(soundChoice).coerceAtLeast(0)
    val pattern = Breathing.PATTERNS.firstOrNull { it.key == breathKey } ?: Breathing.PATTERNS.first()
    val shortSound = when (soundChoice) {
        SanctuarySound.CUSTOM -> stringResource(R.string.settings_sum_custom)
        SanctuarySound.FOLDER -> stringResource(R.string.settings_sum_folder)
        else -> soundLabels[soundIndex]
    }
    val summary = listOf(shortSound, stringResource(pattern.name)).joinToString(" · ")

    ModuleSection("🌑", stringResource(R.string.skill_sanctuary), summary, Palette.sanctuary, expanded, onToggle) {
        Text(stringResource(R.string.sanctuary_sound), color = Palette.text, fontSize = 17.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Picker(soundLabels, soundIndex, Modifier.weight(1f)) {
                when {
                    soundKeys[it] == SanctuarySound.CUSTOM && customSound == null -> soundPicker.launch(arrayOf("audio/*"))
                    soundKeys[it] == SanctuarySound.FOLDER && folderSound == null -> folderPicker.launch(null)
                    else -> choose(soundKeys[it])
                }
            }
            Button(onClick = { if (preview == null) preview = SanctuarySound.start(ctx) else stopPreview() }) {
                Text(stringResource(if (preview == null) R.string.sound_preview else R.string.sound_stop))
            }
        }
        when (soundChoice) {
            SanctuarySound.MIXER -> Text(stringResource(R.string.sound_mixer_desc), color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
            SanctuarySound.CUSTOM -> TextButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.sound_change_file)) }
            SanctuarySound.FOLDER -> {
                TextButton(onClick = { folderPicker.launch(null) }) { Text(stringResource(R.string.sound_change_folder)) }
                SwitchRow(stringResource(R.string.sound_shuffle), checked = shuffle) {
                    shuffle = it
                    SanctuarySound.setShuffle(ctx, it)
                }
            }
        }
        HorizontalDivider(color = Palette.line)
        SwitchRow(stringResource(R.string.phrases_title), stringResource(R.string.phrases_desc), phrasesOn) {
            phrasesOn = it
            CalmPhrases.setEnabled(ctx, it)
        }
        HorizontalDivider(color = Palette.line)
        SettingRow(stringResource(R.string.breath_title), stringResource(R.string.breath_desc))
        Picker(Breathing.PATTERNS.map { stringResource(it.name) }, Breathing.PATTERNS.indexOf(pattern)) {
            breathKey = Breathing.PATTERNS[it].key
            Breathing.setPattern(ctx, breathKey)
        }
    }
}

@Composable
private fun GateSection(expanded: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    SlowGate.init(ctx)
    val options = listOf(R.string.gate_breaths_1, R.string.gate_breaths_2, R.string.gate_breaths_3).map { stringResource(it) }
    val selected = SlowGate.breaths.intValue - 1
    ModuleSection("🚪", stringResource(R.string.skill_gate), options[selected], Palette.gate, expanded, onToggle) {
        SettingRow(stringResource(R.string.gate_breaths)) {
            CompactPicker(options, selected) { SlowGate.setBreaths(ctx, it + 1) }
        }
    }
}

/** Versión, avisos de versión nueva, manual de uso, código y créditos. */
@Composable
private fun AboutSection(expanded: Boolean, onCredits: () -> Unit, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(AutoUpdate.enabled(ctx)) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheck.Result?>(null) }
    ModuleSection("ℹ️", stringResource(R.string.about_title), stringResource(R.string.version, BuildConfig.VERSION_NAME), Palette.accent, expanded, onToggle) {
        Button(onClick = { openUrl(ctx, MANUAL_URL) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.about_manual))
        }
        HorizontalDivider(color = Palette.line)
        SwitchRow(stringResource(R.string.update_auto), stringResource(R.string.update_auto_desc), auto) {
            auto = it
            AutoUpdate.setEnabled(ctx, it)
        }
        OutlinedButton(enabled = !checking, modifier = Modifier.fillMaxWidth(), onClick = {
            checking = true
            result = null
            scope.launch {
                result = UpdateCheck.check()
                checking = false
            }
        }) { Text(stringResource(if (checking) R.string.update_checking else R.string.update_check)) }
        when (val r = result) {
            is UpdateCheck.UpToDate -> Text(stringResource(R.string.update_ok, r.name), color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
            is UpdateCheck.Available -> {
                Text(stringResource(R.string.update_available, r.name), color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.update_how), color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
                Button(onClick = { openUrl(ctx, r.url) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.update_download, r.name)) }
            }
            is UpdateCheck.Failed -> Text(stringResource(R.string.update_failed), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            null -> Unit
        }
        HorizontalDivider(color = Palette.line)
        Text(stringResource(R.string.about_free), color = Palette.textFaint, fontSize = 13.sp, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { openUrl(ctx, REPO_URL) }) { Text(stringResource(R.string.about_code)) }
            TextButton(onClick = { openUrl(ctx, ISSUES_URL) }) { Text(stringResource(R.string.about_issue)) }
        }
        TextButton(onClick = onCredits) { Text(stringResource(R.string.credits_title)) }
    }
}

// ——— Mensajes para empezar (pantalla propia) ———

@Composable
fun MessagesSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val messages = remember { mutableStateListOf<String>().apply { addAll(Messages.list(ctx)) } }
    var newMessage by remember { mutableStateOf("") }
    ModuleTheme(Palette.social) {
        Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().imePadding()) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Palette.textSoft) }
                Text(stringResource(R.string.messages_title), color = Palette.text, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.messages_desc), color = Palette.textFaint, fontSize = 14.sp, lineHeight = 19.sp)
                messages.forEachIndexed { i, m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.card).padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(m, color = Palette.text, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                        IconButton(onClick = {
                            messages.removeAt(i)
                            Messages.save(ctx, messages.toList())
                        }) { Icon(Icons.Default.Close, stringResource(R.string.delete), tint = Palette.textFaint, modifier = Modifier.size(20.dp)) }
                    }
                }
                TextButton(onClick = {
                    Messages.reset(ctx)
                    messages.clear()
                    messages.addAll(Messages.list(ctx))
                }) { Text(stringResource(R.string.messages_reset), color = Palette.textSoft) }
            }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newMessage, onValueChange = { newMessage = it },
                    label = { Text(stringResource(R.string.messages_new)) }, modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Palette.text, unfocusedTextColor = Palette.text,
                        unfocusedBorderColor = Palette.line, unfocusedLabelColor = Palette.textSoft,
                    ),
                )
                Button(enabled = newMessage.isNotBlank(), modifier = Modifier.padding(start = 8.dp), onClick = {
                    messages.add(newMessage.trim())
                    Messages.save(ctx, messages.toList())
                    newMessage = ""
                }) { Text(stringResource(R.string.add)) }
            }
        }
    }
}
