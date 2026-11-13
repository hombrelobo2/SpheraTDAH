package app.sphera.social

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Contact(
    val id: Long,
    val name: String,
    val phone: String,
    val photo: String?, // ruta a la copia de la foto dentro de la app
    val frequencyDays: Int,
    val lastContact: Long,
    val alias: String? = null, // nombre propio para el planeta; `name` sigue siendo el de la agenda
)

/** Nombre del planeta: el alias o, si no hay, nombre + inicial del apellido ("Unai B."). */
fun displayName(c: Contact): String {
    c.alias?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    val parts = c.name.trim().split(Regex("\\s+"))
    return if (parts.size > 1) "${parts[0]} ${parts[1].first()}." else parts[0]
}

/** Contactos del universo, guardados solo en el móvil (contacts.json). Se usa desde el hilo principal. */
object Store {
    private var file: File? = null
    val contacts = mutableStateListOf<Contact>()

    fun init(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "contacts.json")
        file = f
        if (!f.exists()) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                contacts += Contact(
                    o.getLong("id"), o.getString("name"), o.getString("phone"),
                    o.optString("photo").ifEmpty { null }, o.getInt("frequencyDays"), o.getLong("lastContact"),
                    o.optString("alias").ifEmpty { null },
                )
            }
        } catch (e: Exception) {
            // Archivo dañado: se aparta en vez de sobrescribirlo, para no perder los datos.
            f.renameTo(File(f.parentFile, "contacts.json.bad-${System.currentTimeMillis()}"))
            contacts.clear()
        }
    }

    fun upsert(c: Contact) {
        val i = contacts.indexOfFirst { it.id == c.id }
        if (i >= 0) contacts[i] = c else contacts += c
        save()
    }

    fun remove(c: Contact) {
        contacts.removeAll { it.id == c.id }
        c.photo?.let { File(it).delete() }
        save()
    }

    /** Cambia un contacto partiendo de su versión guardada más reciente. */
    fun update(id: Long, change: (Contact) -> Contact) {
        val i = contacts.indexOfFirst { it.id == id }
        if (i >= 0) {
            contacts[i] = change(contacts[i])
            save()
        }
    }

    /** Registra un contacto con la persona, solo si es más reciente que el último conocido. */
    fun touch(id: Long, time: Long) {
        val i = contacts.indexOfFirst { it.id == id }
        if (i >= 0 && time > contacts[i].lastContact) {
            contacts[i] = contacts[i].copy(lastContact = time)
            save()
        }
    }

    private fun save() {
        val f = file ?: return
        val arr = JSONArray()
        contacts.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone)
                    .put("photo", it.photo ?: "").put("frequencyDays", it.frequencyDays).put("lastContact", it.lastContact)
                    .put("alias", it.alias ?: ""),
            )
        }
        val tmp = File(f.parentFile, "contacts.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }
}
