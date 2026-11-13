package app.sphera.body

import androidx.activity.compose.BackHandler
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sphera.Palette
import app.sphera.R

private val GREY = Palette.textSoft

/**
 * Una necesidad del cuerpo: la pregunta, qué respuesta indica que hace falta atenderla,
 * y qué se muestra en ese caso. Para añadir una, basta con otra línea aquí y sus textos en strings.xml.
 */
private class Need(
    val key: String,
    @StringRes val question: Int,
    val needsWhenYes: Boolean,
    @StringRes val title: Int,
    @ArrayRes val tips: Int,
    @StringRes val explanation: Int? = null,
    @StringRes val note: Int? = null,
    val sanctuary: Boolean = false,
)

private val NEEDS = listOf(
    Need("food", R.string.body_q_food, needsWhenYes = false, R.string.body_food, R.array.body_tips_food),
    Need("water", R.string.body_q_water, needsWhenYes = false, R.string.body_water, R.array.body_tips_water),
    Need("bathroom", R.string.body_q_bathroom, needsWhenYes = true, R.string.body_bathroom, R.array.body_tips_bathroom),
    Need("sleep", R.string.body_q_sleep, needsWhenYes = false, R.string.body_sleep, R.array.body_tips_sleep),
    Need("senses", R.string.body_q_senses, needsWhenYes = true, R.string.body_senses, R.array.body_tips_senses, sanctuary = true),
    Need(
        "crash", R.string.body_q_crash, needsWhenYes = true, R.string.body_crash, R.array.body_tips_crash,
        explanation = R.string.body_crash_explanation, note = R.string.body_crash_note,
    ),
)

private const val TIPS_SHOWN = 3 // pocas ideas cada vez para no abrumar; cambian en cada chequeo

/** Chequeo del cuerpo: una pregunta cada vez y, al final, solo lo que necesita atención. No guarda nada. */
@Composable
fun BodyCheckScreen(onSanctuary: () -> Unit, onAndamio: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pending = remember { mutableStateListOf<String>() } // claves de lo que hace falta atender
    BackHandler(enabled = step in 1 until NEEDS.size) {
        step--
        pending.remove(NEEDS[step].key)
    }

    Column(
        Modifier.fillMaxSize().background(Palette.night).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (step < NEEDS.size) {
            val need = NEEDS[step]
            Text(stringResource(R.string.body_title), color = GREY, fontSize = 20.sp, lineHeight = 26.sp)
            Spacer(Modifier.weight(1f))
            Text(stringResource(need.question), color = Color.White, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            fun answer(yes: Boolean) {
                if (yes == need.needsWhenYes) pending.add(need.key)
                step++
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnswerButton(R.string.body_yes, Modifier.weight(1f)) { answer(true) }
                AnswerButton(R.string.body_no, Modifier.weight(1f)) { answer(false) }
            }
        } else {
            Results(NEEDS.filter { it.key in pending }, onSanctuary, onAndamio) {
                pending.clear()
                step = 0
            }
        }
    }
}

@Composable
private fun AnswerButton(@StringRes label: Int, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.cardHigh, contentColor = Color.White),
    ) { Text(stringResource(label), fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Results(needs: List<Need>, onSanctuary: () -> Unit, onAndamio: () -> Unit, onRestart: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (needs.isEmpty()) {
            Text(stringResource(R.string.body_all_good), color = Color.White, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.body_all_good_text), color = GREY, fontSize = 20.sp, lineHeight = 26.sp)
            OutlinedButton(onClick = onSanctuary, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text("🌑 " + stringResource(R.string.skill_sanctuary), fontSize = 20.sp)
            }
            OutlinedButton(onClick = onAndamio, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text("💬 " + stringResource(R.string.skill_andamio), fontSize = 20.sp)
            }
        } else {
            Text(stringResource(R.string.body_results), color = Color.White, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
            needs.forEach { need ->
                val tips = stringArrayResource(need.tips)
                val shown = remember(need.key) { tips.toList().shuffled().take(TIPS_SHOWN) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Palette.card, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(need.title), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                        need.explanation?.let { Text(stringResource(it), color = Color(0xFFCCCCCC), fontSize = 18.sp, lineHeight = 24.sp) }
                        shown.forEach { Text("• $it", fontSize = 19.sp, lineHeight = 25.sp) }
                        need.note?.let { Text(stringResource(it), color = GREY, fontSize = 15.sp, lineHeight = 20.sp) }
                        if (need.sanctuary) {
                            Button(onClick = onSanctuary, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                                Text("🌑 " + stringResource(R.string.skill_sanctuary), fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = onRestart) { Text(stringResource(R.string.body_restart), color = GREY, fontSize = 18.sp) }
    }
}
