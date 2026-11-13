package app.sphera

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Licencia de Sphera y créditos de lo que usa de terceros (texto en res/raw/credits.txt). */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember { ctx.resources.openRawResource(R.raw.credits).bufferedReader().use { it.readText() } }
    Scaffold(topBar = { BackBar(R.string.credits_title, onBack) }) { padding ->
        Text(
            text, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        )
    }
}
