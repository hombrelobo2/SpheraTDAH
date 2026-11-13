package app.sphera

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Skill(val emoji: String, @StringRes val label: Int, val target: String?)

// Las 6 habilidades. Al desarrollar una nueva, sustituir un "próximamente".
private val SKILLS = listOf(
    Skill("🪐", R.string.skill_social, Routes.SOCIAL),
    Skill("🌑", R.string.skill_sanctuary, Routes.SANCTUARY),
    Skill("📍", R.string.skill_return, Routes.RETURN_FORM),
    Skill("🐇", R.string.skill_rabbit, Routes.RABBIT),
    Skill("💬", R.string.skill_andamio, Routes.ANDAMIO),
    Skill("🫀", R.string.skill_body, Routes.BODY),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onOpen(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SKILLS.chunked(2).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s ->
                        Card(
                            onClick = { s.target?.let(onOpen) },
                            enabled = s.target != null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(s.emoji, fontSize = 56.sp)
                                Text(
                                    stringResource(s.label),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
