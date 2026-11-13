package app.sphera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val today = remember { DateFormat.getDateInstance(DateFormat.FULL).format(Date()).replaceFirstChar { it.uppercase() } }
    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(today, color = Palette.textSoft, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.app_name), color = Palette.text, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onOpen(Routes.SETTINGS) }) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = Palette.textSoft)
            }
        }
        // Con pocas casillas, una por fila y más grandes; si una fila queda con una sola, ocupa todo el ancho.
        val skills = HomeLayout.visible()
        skills.chunked(if (skills.size <= 3) 1 else 2).forEach { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { s -> SkillTile(s, Modifier.weight(1f).fillMaxHeight()) { onOpen(s.target) } }
            }
        }
    }
}

@Composable
private fun SkillTile(s: Skill, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(Palette.card)
            .border(1.dp, Palette.line, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // El emoji sobre un círculo suave del color del módulo.
        Box(Modifier.size(48.dp).clip(CircleShape).background(s.color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(s.emoji, fontSize = 24.sp)
        }
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.size(width = 24.dp, height = 3.dp).clip(CircleShape).background(s.color))
            Text(
                stringResource(s.label), color = Palette.text, fontSize = 18.sp, lineHeight = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
