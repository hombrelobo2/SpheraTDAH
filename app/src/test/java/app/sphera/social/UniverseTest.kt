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
    fun distanceGrowsUntilTwiceTheFrequency() {
        val now = 1_000 * day
        assertEquals(0f, distance(contact(0, 7), now), 0.001f)
        assertEquals(0.5f, distance(contact(7, 7), now))
        assertEquals(1f, distance(contact(14, 7), now), 0.001f)
        assertEquals(1f, distance(contact(100, 7), now), 0.001f)
        assertEquals(3, daysSince(contact(3, 30), now))
    }
}
