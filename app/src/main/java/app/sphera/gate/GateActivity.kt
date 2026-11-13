package app.sphera.gate

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import app.sphera.ModuleTheme
import app.sphera.Palette
import app.sphera.SpheraTheme
import app.sphera.TextScale

/**
 * La pausa en sí. Va en su propia tarea para no mezclarse con Sphera ni con su Punto de Retorno:
 * al abrir la app o decir "Mejor no", desaparece.
 */
class GateActivity : AppCompatActivity() {
    private val pkg = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        TextScale.load(this)
        SlowGate.init(this)
        pkg.value = intent.getStringExtra(SlowGate.EXTRA_PKG)
        setContent {
            SpheraTheme {
                ModuleTheme(Palette.gate) {
                    val p = pkg.value
                    // La clave reinicia la pausa si llega otro acceso mientras está abierta.
                    androidx.compose.runtime.key(p) {
                        GatePauseScreen(p, onOpen = ::openTarget, onLeave = { finish() })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pkg.value = intent.getStringExtra(SlowGate.EXTRA_PKG)
    }

    private fun openTarget(target: String) {
        packageManager.getLaunchIntentForPackage(target)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        finish()
    }
}
