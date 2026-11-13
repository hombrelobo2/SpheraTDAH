package app.sphera.social

import java.util.Calendar

// Reglas del universo, sin interfaz: se usan en la pantalla, en el resumen diario y en los tests.

const val DAY = 86_400_000L

fun isSnoozed(c: Contact, now: Long) = now < c.snoozeUntil

/** Días sin contacto, sin contar el tiempo pospuesto. */
fun daysSince(c: Contact, now: Long): Int {
    var ms = now - c.lastContact
    val start = maxOf(c.snoozeStart, c.lastContact)
    val end = minOf(c.snoozeUntil, now)
    if (end > start) ms -= end - start
    return (ms / DAY).toInt().coerceAtLeast(0)
}

/** 0 = pegado al sol, 1 = órbita exterior (se alcanza al pasar el doble de la frecuencia elegida). */
fun distance(c: Contact, now: Long) = (daysSince(c, now) / (2f * c.frequencyDays)).coerceIn(0f, 1f)

/** Quien ya ha pasado su frecuencia (y no está pospuesto), del más alejado al menos. */
fun drifting(contacts: List<Contact>, now: Long) =
    contacts.filter { !isSnoozed(it, now) && daysSince(it, now) >= it.frequencyDays }
        .sortedByDescending { daysSince(it, now).toFloat() / it.frequencyDays }

fun isBirthday(c: Contact, now: Long): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    return c.birthday == "%02d-%02d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

/** Un único contacto sugerido para hoy: quien cumple años o, si no, el que más se ha pasado de su frecuencia. */
fun todaysPick(contacts: List<Contact>, now: Long) =
    contacts.firstOrNull { isBirthday(it, now) } ?: drifting(contacts, now).firstOrNull()

/** Personas con las que ha habido contacto desde el día 1 de este mes. */
fun talkedThisMonth(contacts: List<Contact>, now: Long): Int {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return contacts.count { it.lastContact >= start }
}

/** Cumpleaños de la agenda ("1990-05-21", "--05-21") a "MM-dd". */
fun parseBirthday(raw: String?): String? =
    raw?.let { Regex("(\\d{1,2})-(\\d{1,2})$").find(it.trim()) }
        ?.let { "%02d-%02d".format(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
