package app.sphera.andamio

import org.junit.Assert.assertEquals
import org.junit.Test

class AndamioTest {
    private val t = Andamio.Template("General", "Corta", "Sobre lo de que {contexto}: {accion}, necesito {necesidad}.")

    @Test
    fun fillsTheHolesAndSmoothsTheAnswers() {
        assertEquals(
            "Sobre lo de que me has invitado a tu cumpleaños mañana: no voy a ir, necesito quedarme en mi casa en silencio.",
            Andamio.fill(t, "Me has invitado a tu cumpleaños mañana.", "Necesito quedarme en mi casa en silencio", "Que no voy a ir"),
        )
    }

    @Test
    fun keepsProperNames() {
        assertEquals("Juan me ha llamado", Andamio.clean("Juan me ha llamado."))
    }

    @Test
    fun parsesTemplateLines() {
        val list = Andamio.parse(arrayOf("Trabajo|Formal|Hola {contexto}", "línea mal formada"))
        assertEquals(listOf(Andamio.Template("Trabajo", "Formal", "Hola {contexto}")), list)
    }
}
