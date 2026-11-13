package app.sphera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
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

@Composable
fun SettingsScreen(onBack: () -> Unit, onCredits: () -> Unit) {
    val ctx = LocalContext.current
    // Se incrementa al volver a la pantalla para releer los permisos.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val callsGranted = remember(refresh) { Interactions.hasPermissions(ctx) }
    val notifGranted = remember(refresh) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    val dndGranted = remember(refresh) { SensoryMode.canChangeDnd(ctx) }
    var phrasesOn by remember { mutableStateOf(CalmPhrases.enabled(ctx)) }
    var breathOn by remember { mutableStateOf(Breathing.enabled(ctx)) }
    var breathKey by remember { mutableStateOf(Breathing.pattern(ctx).key) }
    var soundChoice by remember { mutableStateOf(SanctuarySound.choice(ctx)) }
    var customSound by remember { mutableStateOf(SanctuarySound.custom(ctx)) }
    var folderSound by remember { mutableStateOf(SanctuarySound.folder(ctx)) }
    var shuffle by remember { mutableStateOf(SanctuarySound.shuffle(ctx)) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            SanctuarySound.setFolder(ctx, tree)
            folderSound = tree
            soundChoice = SanctuarySound.FOLDER
        }
    }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Permiso permanente para poder leer el archivo también después de reiniciar.
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            SanctuarySound.setCustom(ctx, uri)
            customSound = uri
            soundChoice = SanctuarySound.CUSTOM
        }
    }
    // Prueba del sonido elegido sin entrar al Santuario.
    var preview by remember { mutableStateOf<SanctuarySound.Playback?>(null) }
    fun stopPreview() {
        preview?.release()
        preview = null
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }
    val batteryFree = remember(refresh) {
        ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) == true
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val messages = remember { mutableStateListOf<String>().apply { addAll(Messages.list(ctx)) } }
    var newMessage by remember { mutableStateOf("") }
    var summaryOn by remember { mutableStateOf(DailySummary.isEnabled(ctx)) }
    var summaryHour by remember { mutableIntStateOf(DailySummary.hour(ctx)) }
    var alerts by remember(refresh) {
        mutableStateOf(ALERTS.associate { it.first to Alerts.isEnabled(ctx, it.first) })
    }
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

    Scaffold(topBar = { BackBar(R.string.settings, onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Una caja por cada parte de la app; los colores salen del tema, así se leen bien en claro y en oscuro.
            Section(stringResource(R.string.settings_general), Palette.card) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                LANGUAGES.forEach { (tag, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = currentLang == tag) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = currentLang == tag, onClick = null)
                        Text(stringResource(label), Modifier.padding(start = 12.dp))
                    }
                }
                PermissionRow(R.string.perm_battery, R.string.perm_battery_desc, batteryFree) {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)),
                    )
                }
                Text(stringResource(R.string.text_size), style = MaterialTheme.typography.titleMedium)
                val sizeLabels = listOf(
                    stringResource(R.string.text_size_small), stringResource(R.string.text_size_normal),
                    stringResource(R.string.text_size_large), stringResource(R.string.text_size_xlarge),
                )
                Picker(sizeLabels, TextScale.OPTIONS.indexOf(TextScale.value).coerceAtLeast(1)) {
                    TextScale.set(ctx, TextScale.OPTIONS[it])
                }
                TextButton(onClick = onCredits) { Text(stringResource(R.string.credits_title)) }
            }

            Section("🪐 " + stringResource(R.string.skill_social), Palette.social.copy(alpha = 0.12f).compositeOver(Palette.card)) {
                PermissionRow(R.string.perm_calls, R.string.perm_calls_desc, callsGranted) {
                    permLauncher.launch(Interactions.PERMISSIONS)
                }
                PermissionRow(R.string.perm_notifications, R.string.perm_notifications_desc, notifGranted) {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.summary_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = summaryOn, onCheckedChange = { on ->
                        withNotifications(on) {
                            summaryOn = it
                            DailySummary.configure(ctx, it, summaryHour)
                        }
                    })
                }
                if (summaryOn) {
                    Text(stringResource(R.string.summary_hour), style = MaterialTheme.typography.bodyLarge)
                    Picker(HOURS, summaryHour) {
                        summaryHour = it
                        DailySummary.configure(ctx, true, it)
                    }
                }

                HorizontalDivider()
                Text(stringResource(R.string.alerts_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.alerts_desc), style = MaterialTheme.typography.bodySmall)
                ALERTS.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(label), Modifier.weight(1f))
                        Switch(checked = alerts[key] == true, onCheckedChange = { on ->
                            withNotifications(on) {
                                Alerts.set(ctx, key, it)
                                alerts = alerts + (key to it)
                            }
                        })
                    }
                }

                HorizontalDivider()
                Text(stringResource(R.string.messages_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.messages_desc), style = MaterialTheme.typography.bodySmall)
                messages.forEachIndexed { i, m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m, Modifier.weight(1f))
                        IconButton(onClick = {
                            messages.removeAt(i)
                            Messages.save(ctx, messages.toList())
                        }) { Icon(Icons.Default.Close, stringResource(R.string.delete)) }
                    }
                }
                OutlinedTextField(
                    value = newMessage, onValueChange = { newMessage = it },
                    label = { Text(stringResource(R.string.messages_new)) }, modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    Button(enabled = newMessage.isNotBlank(), onClick = {
                        messages.add(newMessage.trim())
                        Messages.save(ctx, messages.toList())
                        newMessage = ""
                    }) { Text(stringResource(R.string.add)) }
                    TextButton(onClick = {
                        Messages.reset(ctx)
                        messages.clear()
                        messages.addAll(Messages.list(ctx))
                    }) { Text(stringResource(R.string.messages_reset)) }
                }
            }

            Section("🌑 " + stringResource(R.string.skill_sanctuary), Palette.sanctuary.copy(alpha = 0.12f).compositeOver(Palette.card)) {
                PermissionRow(R.string.perm_dnd, R.string.perm_dnd_desc, dndGranted) {
                    ctx.startActivity(SensoryMode.dndSettings())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.phrases_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.phrases_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = phrasesOn, onCheckedChange = {
                        phrasesOn = it
                        CalmPhrases.setEnabled(ctx, it)
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.breath_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.breath_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = breathOn, onCheckedChange = {
                        breathOn = it
                        Breathing.setEnabled(ctx, it)
                    })
                }
                if (breathOn) {
                    Picker(
                        Breathing.PATTERNS.map { stringResource(it.name) },
                        Breathing.PATTERNS.indexOfFirst { it.key == breathKey }.coerceAtLeast(0),
                    ) {
                        breathKey = Breathing.PATTERNS[it].key
                        Breathing.setPattern(ctx, breathKey)
                    }
                }

                HorizontalDivider()
                Text(stringResource(R.string.sanctuary_sound), style = MaterialTheme.typography.titleMedium)
                val soundKeys = listOf(SanctuarySound.MIXER) + SanctuarySound.BUILT_IN.map { it.first } +
                    (if (customSound != null) listOf(SanctuarySound.CUSTOM) else emptyList()) +
                    (if (folderSound != null) listOf(SanctuarySound.FOLDER) else emptyList())
                val soundLabels = listOf(stringResource(R.string.sound_mixer)) + SanctuarySound.BUILT_IN.map { stringResource(it.second) } +
                    (customSound?.let { listOf(stringResource(R.string.sound_custom, fileName(ctx, it))) } ?: emptyList()) +
                    (folderSound?.let {
                        listOf(stringResource(R.string.sound_folder, fileName(ctx, SanctuarySound.folderDocument(it))))
                    } ?: emptyList())
                Picker(soundLabels, soundKeys.indexOf(soundChoice).coerceAtLeast(0)) {
                    soundChoice = soundKeys[it]
                    SanctuarySound.choose(ctx, soundChoice)
                    if (preview != null) { stopPreview(); preview = SanctuarySound.start(ctx) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sanctuary_sound_pick))
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sound_pick_folder))
                    }
                }
                if (soundChoice == SanctuarySound.MIXER) {
                    Text(stringResource(R.string.sound_mixer_desc), style = MaterialTheme.typography.bodySmall)
                }
                if (soundChoice == SanctuarySound.FOLDER) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sound_shuffle), Modifier.weight(1f))
                        Switch(checked = shuffle, onCheckedChange = {
                            shuffle = it
                            SanctuarySound.setShuffle(ctx, it)
                        })
                    }
                }
                Button(onClick = { if (preview == null) preview = SanctuarySound.start(ctx) else stopPreview() }) {
                    Text(stringResource(if (preview == null) R.string.sound_preview else R.string.sound_stop))
                }
            }

            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Caja de color con el título de una parte de la app. */
@Composable
private fun Section(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PermissionRow(@StringRes title: Int, @StringRes desc: Int, granted: Boolean, onGrant: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(desc), style = MaterialTheme.typography.bodySmall)
        if (granted) {
            Text("✓ " + stringResource(R.string.granted), color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = onGrant) { Text(stringResource(R.string.grant)) }
        }
    }
}
