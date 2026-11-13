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
import app.sphera.social.Store
import app.sphera.social.UniverseScreen

enum class Screen { HOME, SOCIAL, SETTINGS }

// AppCompatActivity para poder cambiar el idioma desde Ajustes en cualquier versión de Android.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        setContent { SpheraTheme { App() } }
    }
}

@Composable
private fun App() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    val home = { screen = Screen.HOME }
    when (screen) {
        Screen.HOME -> HomeScreen(onOpen = { screen = it })
        Screen.SOCIAL -> UniverseScreen(onBack = home)
        Screen.SETTINGS -> SettingsScreen(onBack = home)
    }
}
