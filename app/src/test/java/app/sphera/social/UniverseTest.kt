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
    fun distanceGrowsUntilTwiceTheFrequency() {
        val now = 1_000 * day
        assertEquals(0f, distance(contact(0, 7), now), 0.001f)
        assertEquals(0.5f, distance(contact(7, 7), now))
        assertEquals(1f, distance(contact(14, 7), now), 0.001f)
        assertEquals(1f, distance(contact(100, 7), now), 0.001f)
        assertEquals(3, daysSince(contact(3, 30), now))
    }
}
