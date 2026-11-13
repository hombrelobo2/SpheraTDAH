package app.sphera.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R
import app.sphera.SpheraTheme
import app.sphera.TextScale

/** Al poner (o cambiar) el widget de accesos: elegir hasta cuatro, en el orden en que se tocan. */
class WidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Si se sale sin guardar, el widget no se pone.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        TextScale.load(this)
        val initial = WidgetActions.forWidget(this, id).map { it.key }
        setContent {
            SpheraTheme {
                val chosen = remember { mutableStateListOf<String>().apply { addAll(initial) } }
                Column(Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(20.dp)) {
                    Text(stringResource(R.string.wc_title), color = Palette.text, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.wc_desc, WidgetActions.MAX), color = Palette.textFaint, fontSize = 14.sp, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetActions.ALL.forEach { action ->
                            val pos = chosen.indexOf(action.key)
                            val color = Color(action.color)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (pos >= 0) color.copy(alpha = 0.16f) else Palette.card)
                                    .border(1.dp, if (pos >= 0) color else Palette.line, RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (pos >= 0) chosen.remove(action.key)
                                        else if (chosen.size < WidgetActions.MAX) chosen.add(action.key)
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(action.emoji, fontSize = 24.sp)
                                Text(stringResource(action.label), color = Palette.text, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                                // Número de posición en el widget
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(if (pos >= 0) color else Color.Transparent)
                                        .border(1.dp, if (pos >= 0) color else Palette.line, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { if (pos >= 0) Text("${pos + 1}", color = Palette.night, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { finish() }) { Text(stringResource(R.string.cancel), color = Palette.textSoft) }
                        Box(Modifier.weight(1f))
                        Button(enabled = chosen.isNotEmpty(), modifier = Modifier.height(52.dp), onClick = {
                            WidgetActions.save(this@WidgetConfigActivity, id, chosen.toList())
                            SpheraWidget.draw(this@WidgetConfigActivity, AppWidgetManager.getInstance(this@WidgetConfigActivity), id)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        }) { Text(stringResource(R.string.rp_save)) }
                    }
                }
            }
        }
    }
}
