package app.sphera.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniverseTest {
    private val day = 86_400_000L
    private fun contact(daysAgo: Int, freq: Int) = Contact(1, "Ana", "600111222", null, freq, 1_000 * day - daysAgo * day)

    @Test
    fun samePhoneIgnoresPrefixAndFormat() {
        assertTrue(Interactions.samePhone("+34 600 11 12 22", "600111222"))
        assertTrue(Interactions.samePhone("0034600111222", "600-111-222"))
        assertFalse(Interactions.samePhone("600111222", "600111223"))
        assertFalse(Interactions.samePhone("123", "123"))
    }

    @Test
    fun displayNameUsesAliasOrSurnameInitial() {
        assertEquals("Unai B.", displayName(contact(0, 7).copy(name = "Unai  Butron Garcia")))
        assertEquals("Ana", displayName(contact(0, 7)))
        assertEquals("Mamá", displayName(contact(0, 7).copy(alias = " Mamá ")))
        assertEquals("Ana", displayName(contact(0, 7).copy(alias = " ")))
    }

    @Test
    fun todaysPickIsTheMostOverdue() {
        val now = 1_000 * day
        val a = contact(10, 30).copy(id = 1) // aún dentro de su frecuencia
        val b = contact(8, 7).copy(id = 2) // 8/7 pasado
        val c = contact(40, 30).copy(id = 3) // 40/30 más pasado
        assertEquals(3L, todaysPick(listOf(a, b, c), now)?.id)
        assertEquals(listOf(3L, 2L), drifting(listOf(a, b, c), now).map { it.id })
        assertEquals(null, todaysPick(listOf(a), now))
    }

    @Test
    fun messagesUseAliasOrDropIt() {
        assertEquals("¡Hola Mamá! ¿Qué tal?", Messages.fill("¡Hola {alias}! ¿Qué tal?", "Mamá"))
        assertEquals("¡Hola! ¿Qué tal?", Messages.fill("¡Hola {alias}! ¿Qué tal?", null))
        assertEquals("¿Quedamos?", Messages.fill("¿Quedamos, {alias}?", " "))
        assertEquals("Sin hueco", Messages.fill("Sin hueco", "Ana"))
    }

    @Test
    fun handlesAreCleaned() {
        assertEquals("ana_g", cleanHandle(" @ana_g "))
        assertEquals("ana_g", cleanHandle("https://t.me/ana_g"))
        assertEquals("ana.g", cleanHandle("https://www.instagram.com/ana.g/"))
        assertEquals(null, cleanHandle("  "))
    }

    @Test
    fun snoozedTimeDoesNotCount() {
        val now = 1_000 * day
        // 20 días sin contacto, de los que 10 estuvo pospuesto (ya terminado): cuentan 10.
        val c = contact(20, 7).copy(snoozeStart = now - 15 * day, snoozeUntil = now - 5 * day)
        assertEquals(10, daysSince(c, now))
        // Pospuesto ahora mismo: congelado en lo que llevaba antes, y fuera del resumen.
        val frozen = contact(20, 7).copy(snoozeStart = now - 5 * day, snoozeUntil = now + 5 * day)
        assertEquals(15, daysSince(frozen, now))
        assertTrue(isSnoozed(frozen, now))
        assertEquals(emptyList<Contact>(), drifting(listOf(frozen), now))
    }

    @Test
    fun birthdaysAreParsedAndComeFirst() {
        assertEquals("05-21", parseBirthday("1990-05-21"))
        assertEquals("05-21", parseBirthday("--05-21"))
        assertEquals(null, parseBirthday(null))
        val now = java.util.Calendar.getInstance().apply { set(2026, 4, 21, 12, 0) }.timeInMillis
        val bday = contact(0, 30).copy(id = 9, birthday = "05-21")
        val late = contact(0, 7).copy(id = 8, lastContact = now - 40 * day)
        assertTrue(isBirthday(bday, now))
        assertEquals(9L, todaysPick(listOf(late, bday), now)?.id)
    }

    @Test
    fun monthCountOnlyCountsThisMonth() {
        val now = java.util.Calendar.getInstance().apply { set(2026, 4, 21, 12, 0) }.timeInMillis
        val thisMonth = contact(0, 7).copy(lastContact = now - 3 * day)
        val lastMonth = contact(0, 7).copy(lastContact = now - 30 * day)
        assertEquals(1, talkedThisMonth(listOf(thisMonth, lastMonth), now))
    }

    @Test
    fun distanceGrowsUntilTwiceTheFrequency() {
        val now = 1_000 * day
        assertEquals(0f, distance(contact(0, 7), now), 0.001f)
        assertEquals(0.5f, distance(contact(7, 7), now))
        assertEquals(1f, distance(contact(14, 7), now), 0.001f)
        assertEquals(1f, distance(contact(100, 7), now), 0.001f)
        assertEquals(3, daysSince(contact(3, 30), now))
    }
}
