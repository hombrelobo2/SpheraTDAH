package app.sphera

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.os.SystemClock
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.sphera.andamio.AndamioScreen
import app.sphera.body.BodyCheckScreen
import app.sphera.rabbit.RabbitCaptureScreen
import app.sphera.rabbit.RabbitDetailScreen
import app.sphera.rabbit.RabbitListScreen
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

private const val RETURN_POINT_AWAY_MS = 60_000L

// Pantallas que el Punto de Retorno no interrumpe nunca.
private val RETURN_POINT_SKIP = setOf(
    Routes.SANCTUARY, Routes.LANDING, Routes.COMM_CARD, Routes.RETURN_CARD, Routes.RETURN_FORM,
)

object Routes {
    const val HOME = "home"
    const val SOCIAL = "social"
    const val SETTINGS = "settings"
    const val CREDITS = "credits"
    const val SANCTUARY = "sanctuary"
    const val LANDING = "landing"
    const val COMM_CARD = "comm_card"
    const val RETURN_CARD = "return_card"
    const val RETURN_FORM = "return_form"
    const val RABBIT = "rabbit"
    const val RABBIT_EXPLORE = "rabbit_explore"
    const val RABBIT_DETAIL = "rabbit/{id}"
    const val ANDAMIO = "andamio"
    const val BODY = "body"

    // Enlaces que usa el widget (y que cualquiera puede abrir con sphera://…).
    const val LINK_SANCTUARY = "sphera://sanctuary"
    const val LINK_RETURN_NEW = "sphera://return/new"
    const val LINK_RABBIT = "sphera://rabbit"
}

// AppCompatActivity para poder cambiar el idioma desde Ajustes en cualquier versión de Android.
class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_SOCIAL = "open_social"
        const val EXTRA_CONTACT_ID = "contact_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Store.init(this)
        TextScale.load(this)
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
        intent.dataString == Routes.LINK_RABBIT -> Routes.RABBIT
        intent.getBooleanExtra(EXTRA_OPEN_SOCIAL, false) -> Routes.SOCIAL
        else -> null
    }
}

@Composable
private fun App(target: String?, fromLink: Boolean, contactId: Long?) {
    val ctx = LocalContext.current
    // Si hay un Punto de Retorno guardado, es lo único que se ve al entrar, venga de donde venga.
    // Excepciones: el Santuario (es un botón de pánico) y las capturas rápidas del widget (Punto de Retorno, conejo).
    val bypass = target == Routes.SANCTUARY || target == Routes.RETURN_FORM || (fromLink && target == Routes.RABBIT)
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

    // Al volver a la app tras un rato fuera, el Punto de Retorno "secuestra" la pantalla aunque Android
    // no haya reiniciado la app. Un minuto de margen para no saltar al volver de elegir un contacto o enviar un WhatsApp.
    val scope = rememberCoroutineScope()
    var pausedAt by remember { mutableLongStateOf(0L) }
    LifecycleResumeEffect(Unit) {
        val away = pausedAt > 0 && SystemClock.elapsedRealtime() - pausedAt > RETURN_POINT_AWAY_MS
        val route = nav.currentDestination?.route
        if (away && route !in RETURN_POINT_SKIP) {
            scope.launch {
                if (SpheraDb.get(ctx).returnPoints().get() != null) {
                    nav.navigate(Routes.RETURN_CARD) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }
        }
        onPauseOrDispose { pausedAt = SystemClock.elapsedRealtime() }
    }

    NavHost(nav, startDestination = startRoute) {
        composable(Routes.HOME) { HomeScreen(onOpen = { nav.navigate(it) }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { back() }, onCredits = { nav.navigate(Routes.CREDITS) }) }
        composable(Routes.CREDITS) { CreditsScreen(onBack = { back() }) }
        composable(Routes.SOCIAL) {
            ModuleTheme(Palette.social) { UniverseScreen(onBack = { back() }, openContact = contactId) }
        }

        composable(Routes.SANCTUARY) {
            ModuleTheme(Palette.sanctuary) {
                SanctuaryScreen(onExit = {
                    nav.navigate(Routes.LANDING) { popUpTo(Routes.SANCTUARY) { inclusive = true } }
                })
            }
        }
        composable(Routes.LANDING) {
            ModuleTheme(Palette.sanctuary) {
                LandingScreen(
                    onStayCalm = { nav.navigate(Routes.SANCTUARY) { popUpTo(Routes.LANDING) { inclusive = true } } },
                    onCannotTalk = { nav.navigate(Routes.COMM_CARD) },
                    onBack = { goHome() },
                )
            }
        }
        composable(Routes.COMM_CARD) { CommunicationCardScreen() }

        composable(Routes.RABBIT) {
            ModuleTheme(Palette.rabbit) {
                RabbitCaptureScreen(onSaved = { if (fromLink) closeApp() else back() }, onExplore = { nav.navigate(Routes.RABBIT_EXPLORE) })
            }
        }
        composable(Routes.RABBIT_EXPLORE) {
            ModuleTheme(Palette.rabbit) { RabbitListScreen(onOpen = { nav.navigate("rabbit/$it") }) }
        }
        composable(Routes.RABBIT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            ModuleTheme(Palette.rabbit) { RabbitDetailScreen(entry.arguments?.getLong("id") ?: -1, onBack = { back() }) }
        }
        composable(Routes.ANDAMIO) { ModuleTheme(Palette.andamio) { AndamioScreen() } }
        composable(Routes.BODY) {
            ModuleTheme(Palette.body) {
                BodyCheckScreen(onSanctuary = { nav.navigate(Routes.SANCTUARY) }, onAndamio = { nav.navigate(Routes.ANDAMIO) })
            }
        }

        composable(Routes.RETURN_CARD) { ModuleTheme(Palette.returnPoint) { ReturnCardScreen(onDone = { goHome() }) } }
        // Desde el widget se apunta y se vuelve a lo que se estaba haciendo: la app se cierra al guardar.
        composable(Routes.RETURN_FORM) {
            ModuleTheme(Palette.returnPoint) { ReturnFormScreen(onDone = { if (fromLink) closeApp() else back() }) }
        }
    }
}
