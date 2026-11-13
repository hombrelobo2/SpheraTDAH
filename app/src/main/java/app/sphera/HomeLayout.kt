package app.sphera

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

/** Una casilla del inicio, con el color de su módulo. [icon]: icono dibujado en lugar del emoji. */
class Skill(
    val key: String, val emoji: String, @StringRes val label: Int, val target: String, val color: Color,
    @DrawableRes val icon: Int? = null,
)

/** El icono de un módulo: su dibujo si lo tiene, o su emoji. */
@Composable
fun SkillIcon(skill: Skill, size: TextUnit = 24.sp, modifier: Modifier = Modifier) {
    if (skill.icon != null) {
        Image(painterResource(skill.icon), null, modifier.size((size.value * 1.15f).dp))
    } else {
        Text(skill.emoji, fontSize = size, modifier = modifier)
    }
}

fun skill(key: String) = SKILLS.first { it.key == key }

val SKILLS = listOf(
    Skill("social", "🪐", R.string.skill_social, Routes.SOCIAL, Palette.social),
    Skill("sanctuary", "🌙", R.string.skill_sanctuary, Routes.SANCTUARY, Palette.sanctuary, R.drawable.ic_sanctuary),
    Skill("return", "📍", R.string.skill_return, Routes.RETURN_FORM, Palette.returnPoint),
    Skill("rabbit", "🐇", R.string.skill_rabbit, Routes.RABBIT, Palette.rabbit),
    Skill("steps", "👣", R.string.skill_steps, Routes.STEPS, Palette.steps),
    Skill("body", "🫀", R.string.skill_body, Routes.BODY, Palette.body),
    Skill("cards", "🗣️", R.string.skill_cards, Routes.CARDS, Palette.cards),
    Skill("gate", "🚪", R.string.skill_gate, Routes.GATE, Palette.gate),
)

/**
 * Orden y visibilidad de las casillas del inicio, elegidos en Ajustes.
 * Ocultar una casilla no apaga el módulo: sus avisos, widgets y enlaces siguen funcionando.
 */
object HomeLayout {
    val order = mutableStateListOf<String>()
    val hidden = mutableStateListOf<String>()
    private var loaded = false

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("home", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        val saved = p.getString("order", "").orEmpty().split(',').filter { k -> SKILLS.any { it.key == k } }
        // Las casillas nuevas (de versiones futuras) se añaden al final.
        order.addAll(saved + SKILLS.map { it.key }.filter { it !in saved })
        hidden.addAll(p.getString("hidden", "").orEmpty().split(',').filter { it.isNotBlank() })
    }

    private fun save(ctx: Context) = prefs(ctx).edit()
        .putString("order", order.joinToString(","))
        .putString("hidden", hidden.joinToString(","))
        .apply()

    fun ordered(): List<Skill> = order.mapNotNull { k -> SKILLS.firstOrNull { it.key == k } }
    fun visible(): List<Skill> = ordered().filter { it.key !in hidden }

    fun move(ctx: Context, key: String, delta: Int) {
        val i = order.indexOf(key)
        val j = i + delta
        if (i < 0 || j !in order.indices) return
        order[i] = order[j].also { order[j] = key }
        save(ctx)
    }

    /** Siempre queda al menos una casilla visible. */
    fun setVisible(ctx: Context, key: String, visible: Boolean) {
        if (visible) hidden.remove(key)
        else if (key !in hidden && visible().size > 1) hidden.add(key)
        save(ctx)
    }
}
