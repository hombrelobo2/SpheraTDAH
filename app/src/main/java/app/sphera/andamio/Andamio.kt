package app.sphera.andamio

/**
 * Motor del Andamio: interpolación de cadenas, sin IA ni servidores.
 * Las plantillas viven en strings.xml (array "andamio_templates") con el formato
 * "Escenario|Tono|Texto con {contexto}, {necesidad} y {accion}". Añadir una línea ahí = nueva plantilla o escenario.
 */
object Andamio {
    data class Template(val scenario: String, val tone: String, val text: String)

    fun parse(lines: Array<String>): List<Template> = lines.mapNotNull { line ->
        val parts = line.split('|', limit = 3)
        if (parts.size == 3) Template(parts[0].trim(), parts[1].trim(), parts[2].trim()) else null
    }

    fun fill(t: Template, context: String, need: String, action: String) = t.text
        .replace("{contexto}", clean(context, "que "))
        .replace("{necesidad}", clean(need, "necesito ", "que "))
        .replace("{accion}", clean(action, "que "))

    // Palabras con las que suelen empezar las respuestas; si la frase empieza por una, va en minúscula
    // para que encaje en mitad de la plantilla. Si empieza por otra (un nombre propio, por ejemplo), se respeta.
    private val COMMON_STARTS = setOf(
        "me", "mi", "mis", "te", "tu", "tus", "se", "su", "sus", "nos", "os", "le", "les", "lo", "la", "las", "los",
        "el", "un", "una", "unos", "unas", "que", "no", "sí", "si", "ya", "he", "has", "ha", "hemos", "han",
        "tengo", "tienes", "tiene", "estoy", "estás", "está", "voy", "vas", "va", "quiero", "quieres", "necesito",
        "puedo", "puedes", "hoy", "mañana", "ayer", "esta", "este", "eso", "esto", "hay", "ir", "quedarme",
        "descansar", "estar", "hacer", "ser", "decir", "dejar", "cancelar", "aplazar", "posponer",
    )

    fun clean(raw: String, vararg dropPrefixes: String): String {
        var s = raw.trim().trimEnd('.', '!', ' ')
        val first = s.substringBefore(' ').lowercase()
        if (first in COMMON_STARTS) s = s.replaceFirstChar { it.lowercase() }
        for (p in dropPrefixes) if (s.startsWith(p)) s = s.removePrefix(p).trim()
        return s
    }
}
