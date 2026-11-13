package app.sphera

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.sphera.social.Alerts
import app.sphera.social.DailySummary
import app.sphera.social.Store
import app.sphera.social.UniverseScreen

enum class Screen { HOME, SOCIAL, SETTINGS }

// AppCompatActivity para poder cambiar el idioma desde Ajustes en cualquier versión de Android.
class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_SOCIAL = "open_social"
        const val EXTRA_CONTACT_ID = "contact_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        DailySummary.schedule(this)
        Alerts.schedule(this)
        val start = if (intent.getBooleanExtra(EXTRA_OPEN_SOCIAL, false)) Screen.SOCIAL else Screen.HOME
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1).takeIf { it >= 0 }
        setContent { SpheraTheme { App(start, contactId) } }
    }
}

@Composable
private fun App(start: Screen, contactId: Long?) {
    var screen by rememberSaveable { mutableStateOf(start) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    val home = { screen = Screen.HOME }
    when (screen) {
        Screen.HOME -> HomeScreen(onOpen = { screen = it })
        Screen.SOCIAL -> UniverseScreen(onBack = home, openContact = contactId)
        Screen.SETTINGS -> SettingsScreen(onBack = home)
    }
}
