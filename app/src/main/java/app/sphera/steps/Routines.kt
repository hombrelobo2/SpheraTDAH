/*
 * Sphera — Paso a paso.
 * Adaptado de Visual Routines (https://github.com/oikumene-works/visual-routines), GPL-3.0:
 * rutinas de pasos cortos, seguidas de una en una, con Hecho / Saltar / Atrás / Pausa y recuerdo de dónde ibas.
 */
package app.sphera.steps

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import app.sphera.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Routine(val id: String, val name: String, val steps: List<String>)

/** Ejemplo incluido: nombre y pasos en strings.xml (arrays "steps_ex_*"), para traducirlos y editarlos sin tocar código. */
class ExampleRoutine(val id: String, @StringRes val name: Int, val steps: Int)

val EXAMPLES = listOf(
    ExampleRoutine("ex-leave-home", R.string.steps_ex_leave_home, R.array.steps_ex_leave_home),
    ExampleRoutine("ex-morning", R.string.steps_ex_morning, R.array.steps_ex_morning),
    ExampleRoutine("ex-start-work", R.string.steps_ex_start_work, R.array.steps_ex_start_work),
    ExampleRoutine("ex-shower", R.string.steps_ex_shower, R.array.steps_ex_shower),
    ExampleRoutine("ex-bedtime", R.string.steps_ex_bedtime, R.array.steps_ex_bedtime),
)

fun ExampleRoutine.load(ctx: Context) = Routine(id, ctx.getString(name), ctx.resources.getStringArray(steps).toList())

/** Paso en el que se dejó una rutina a medias ("Seguir luego"). */
data class Session(val routineId: String, val index: Int)

/** Rutinas propias en routines.json (solo en el móvil) y la rutina a medias. Usar desde el hilo principal. */
object StepsStore {
    private var file: File? = null
    val routines = mutableStateListOf<Routine>()

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.applicationContext.filesDir, "routines.json")
        file = f
        if (!f.exists()) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val steps = o.getJSONArray("steps")
                routines += Routine(o.getString("id"), o.getString("name"), List(steps.length()) { steps.getString(it) })
            }
        } catch (e: Exception) {
            // Archivo dañado: se aparta en vez de sobrescribirlo, para no perder datos.
            f.renameTo(File(f.parentFile, "routines.json.bad-${System.currentTimeMillis()}"))
            routines.clear()
        }
    }

    fun find(ctx: Context, id: String): Routine? =
        routines.firstOrNull { it.id == id } ?: EXAMPLES.firstOrNull { it.id == id }?.load(ctx)

    fun newId() = UUID.randomUUID().toString()

    fun upsert(r: Routine) {
        val i = routines.indexOfFirst { it.id == r.id }
        if (i >= 0) routines[i] = r else routines += r
        save()
    }

    fun delete(id: String) {
        routines.removeAll { it.id == id }
        save()
    }

    private fun save() {
        val f = file ?: return
        val arr = JSONArray()
        routines.forEach { r -> arr.put(JSONObject().put("id", r.id).put("name", r.name).put("steps", JSONArray(r.steps))) }
        val tmp = File(f.parentFile, "routines.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("steps", Context.MODE_PRIVATE)

    fun session(ctx: Context): Session? =
        prefs(ctx).getString("session_routine", null)?.let { Session(it, prefs(ctx).getInt("session_index", 0)) }

    fun saveSession(ctx: Context, s: Session?) {
        val e = prefs(ctx).edit()
        if (s == null) e.remove("session_routine").remove("session_index") else e.putString("session_routine", s.routineId).putInt("session_index", s.index)
        e.apply()
    }
}
