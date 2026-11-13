package app.sphera

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Dictado con el reconocedor de voz del propio Android (sin IA en Sphera).
 * Devuelve la acción para empezar a dictar, o null si el móvil no tiene reconocedor (y entonces no se muestra el botón).
 */
@Composable
fun rememberDictation(onText: (String) -> Unit): (() -> Unit)? {
    val ctx = LocalContext.current
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
    }
    val available = remember { intent.resolveActivity(ctx.packageManager) != null }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
    }
    return if (available) ({ launcher.launch(intent) }) else null
}

/** Añade texto dictado a lo ya escrito, con un espacio en medio. */
fun appendDictation(current: String, dictated: String) =
    listOf(current.trim(), dictated.trim()).filter { it.isNotEmpty() }.joinToString(" ")
