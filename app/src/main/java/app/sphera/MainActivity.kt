package app.sphera

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.sphera.returnpoint.ReturnCardScreen
import app.sphera.returnpoint.ReturnFormScreen
import app.sphera.returnpoint.SpheraDb
import app.sphera.sanctuary.CommunicationCardScreen
import app.sphera.sanctuary.LandingScreen
import app.sphera.sanctuary.SanctuaryScreen
import app.sphera.sanctuary.findActivity
import app.sphera.social.Alerts
import app.sphera.social.DailySummary
import app.sphera.social.Store
import app.sphera.social.UniverseScreen

object Routes {
    const val HOME = "home"
    const val SOCIAL = "social"
    const val SETTINGS = "settings"
    const val SANCTUARY = "sanctuary"
    const val LANDING = "landing"
    const val COMM_CARD = "comm_card"
    const val RETURN_CARD = "return_card"
    const val RETURN_FORM = "return_form"

    // Enlaces que usa el widget (y que cualquiera puede abrir con sphera://…).
    const val LINK_SANCTUARY = "sphera://sanctuary"
    const val LINK_RETURN_NEW = "sphera://return/new"
}

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
        val target = explicitTarget(intent)
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1).takeIf { it >= 0 }
        setContent { SpheraTheme { App(target, fromLink = intent.data != null, contactId) } }
    }

    /** Pantalla pedida expresamente (widget, enlace o notificación); null = apertura normal. */
    private fun explicitTarget(intent: Intent): String? = when {
        intent.dataString == Routes.LINK_SANCTUARY -> Routes.SANCTUARY
        intent.dataString == Routes.LINK_RETURN_NEW -> Routes.RETURN_FORM
        intent.getBooleanExtra(EXTRA_OPEN_SOCIAL, false) -> Routes.SOCIAL
        else -> null
    }
}

@Composable
private fun App(target: String?, fromLink: Boolean, contactId: Long?) {
    val ctx = LocalContext.current
    // Si hay un Punto de Retorno guardado, es lo único que se ve al entrar, venga de donde venga.
    // Excepciones: el Santuario (es un botón de pánico) y el formulario del widget (sirve para sobrescribirlo).
    val bypass = target == Routes.SANCTUARY || target == Routes.RETURN_FORM
    val start by produceState(initialValue = if (bypass) target else null) {
        if (value == null) {
            value = if (SpheraDb.get(ctx).returnPoints().get() != null) Routes.RETURN_CARD else target ?: Routes.HOME
        }
    }
    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(Color.Black)) // mientras se consulta la base de datos (instantes)
        return
    }

    val nav = rememberNavController()
    val goHome = { nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } } }
    val back = { if (nav.previousBackStackEntry != null) nav.popBackStack() else goHome() }
    val closeApp = { ctx.findActivity()?.finish() }

    NavHost(nav, startDestination = startRoute) {
        composable(Routes.HOME) { HomeScreen(onOpen = { nav.navigate(it) }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { back() }) }
        composable(Routes.SOCIAL) { UniverseScreen(onBack = { back() }, openContact = contactId) }

        composable(Routes.SANCTUARY) {
            SanctuaryScreen(onExit = {
                nav.navigate(Routes.LANDING) { popUpTo(Routes.SANCTUARY) { inclusive = true } }
            })
        }
        composable(Routes.LANDING) {
            LandingScreen(
                onStayCalm = { closeApp() },
                onCannotTalk = { nav.navigate(Routes.COMM_CARD) },
                onBack = { goHome() },
            )
        }
        composable(Routes.COMM_CARD) { CommunicationCardScreen() }

        composable(Routes.RETURN_CARD) { ReturnCardScreen(onDone = { goHome() }) }
        // Desde el widget se apunta y se vuelve a lo que se estaba haciendo: la app se cierra al guardar.
        composable(Routes.RETURN_FORM) {
            ReturnFormScreen(onDone = { if (fromLink) closeApp() else back() })
        }
    }
}
