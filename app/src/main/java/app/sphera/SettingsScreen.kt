package app.sphera

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.sphera.social.Interactions

private val LANGUAGES = listOf("" to R.string.lang_system, "es" to R.string.lang_es, "en" to R.string.lang_en)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    Scaffold(topBar = { BackBar(R.string.settings, onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

            HorizontalDivider()
            Text(stringResource(R.string.permissions), style = MaterialTheme.typography.titleMedium)
            PermissionRow(R.string.perm_calls, R.string.perm_calls_desc, callsGranted) {
                permLauncher.launch(Interactions.PERMISSIONS)
            }
            PermissionRow(R.string.perm_notifications, R.string.perm_notifications_desc, notifGranted) {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }

            HorizontalDivider()
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
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
