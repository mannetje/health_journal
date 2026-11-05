package nl.healthjournal.app.command

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `DefaultDispatcher` casts a handler to the type of the request it is asked about, and the compiler cannot check
 * that cast. This test checks it instead, from source, for every `@ClassKey` binding in `CommandModule`:
 * the key is the request the handler declares, the handler kind matches the kind of request (command, query or
 * observe query), and the handler's response type is the response type the request declares.
 * A wrong binding then fails the build, not a user's save.
 */
class HandlerWiringTest {
    private val root = File("..")
    private val domainSources = File(root, "domain/src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.map { it.readText() }.toList()
    private val module = File(root, "app/src/main/java/nl/healthjournal/app/command/CommandModule.kt").readText()

    private val kinds = mapOf("CommandHandler" to "Command", "QueryHandler" to "Query", "ObserveQueryHandler" to "ObserveQuery")

    /** Anything up to the supertype colon, but never across another class, function or body, so one match cannot swallow its neighbour. */
    private val untilNextDeclaration = """(?:(?!\bclass\b|\bobject\b|\bfun\b|\{)[\s\S])*?"""

    private class Binding(val key: String, val mapKind: String, val handler: String)
    private class Handler(val kind: String, val request: String, val response: String)
    private class Request(val kind: String, val response: String)

    /** The text between the `<` at [open] and its matching `>`, without spaces. */
    private fun typeArguments(text: String, open: Int): String {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '<' -> depth++
                '>' -> if (--depth == 0) return text.substring(open + 1, i).filterNot { it.isWhitespace() }
            }
        }
        error("unbalanced type arguments at: ${text.substring(open, minOf(text.length, open + 60))}")
    }

    /** Splits `A,List<B,C>` at the top-level commas only. */
    private fun split(arguments: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        arguments.forEachIndexed { i, c ->
            when (c) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) { parts += arguments.substring(start, i); start = i + 1 }
            }
        }
        return parts + arguments.substring(start)
    }

    private val bindings: List<Binding> = Regex("""@ClassKey\((\w+)::class\)\s*fun \w+\([^{=]*\)\s*:\s*(\w+)<\*,\s*\*>\s*(?:=|\{[^}]*?\breturn)\s*(\w+)\(""")
        .findAll(module).map { Binding(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }.toList()

    private val handlers: Map<String, Handler> = buildMap {
        val declaration = Regex("""class (\w+)\b$untilNextDeclaration(?:\)|\b)\s*:\s*(CommandHandler|QueryHandler|ObserveQueryHandler)<""")
        for (text in domainSources) for (m in declaration.findAll(text)) {
            val (request, response) = split(typeArguments(text, m.range.last)).let { it[0] to it.drop(1).joinToString(",") }
            put(m.groupValues[1], Handler(m.groupValues[2], request, response))
        }
    }

    private val requests: Map<String, Request> = buildMap {
        val declaration = Regex("""(?:class|object) (\w+)\b$untilNextDeclaration(?:\)|\b)\s*:\s*(Command|Query|ObserveQuery)<""")
        for (text in domainSources) for (m in declaration.findAll(text)) {
            put(m.groupValues[1], Request(m.groupValues[2], typeArguments(text, m.range.last)))
        }
    }

    @Test fun `every ClassKey binding in CommandModule is read by this test`() {
        val declared = Regex("""@ClassKey\((\w+)::class\)""").findAll(module).map { it.groupValues[1] }.toList()
        assertTrue("found no bindings: the pattern no longer matches CommandModule", bindings.isNotEmpty())
        assertEquals("bindings this test could not parse (a new shape needs a new pattern)", declared - bindings.map { it.key }.toSet(), emptyList<String>())
    }

    @Test fun `each binding names a handler class that exists`() {
        val unknown = bindings.filter { it.handler !in handlers }.map { "${it.key} -> ${it.handler}" }
        assertEquals("handler classes not found in :domain", emptyList<String>(), unknown)
    }

    @Test fun `each binding maps the request its handler declares`() {
        val wrong = bindings.mapNotNull { b -> handlers[b.handler]?.takeIf { it.request != b.key }?.let { "${b.key} is bound to ${b.handler}, which handles ${it.request}" } }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test fun `each binding sits in the map of its handler kind and request kind`() {
        val wrong = bindings.mapNotNull { b ->
            val handler = handlers[b.handler] ?: return@mapNotNull null
            val request = requests[b.key] ?: return@mapNotNull "${b.key} is not a Command, Query or ObserveQuery in :domain"
            when {
                handler.kind != b.mapKind -> "${b.handler} is a ${handler.kind} but ${b.key} is bound as ${b.mapKind}"
                kinds[handler.kind] != request.kind -> "${b.key} is a ${request.kind} but ${b.handler} is a ${handler.kind}"
                else -> null
            }
        }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test fun `each handler returns the response type its request declares`() {
        val wrong = bindings.mapNotNull { b ->
            val handler = handlers[b.handler] ?: return@mapNotNull null
            val request = requests[b.key] ?: return@mapNotNull null
            if (handler.response == request.response) null
            else "${b.key} declares ${request.response} but ${b.handler} returns ${handler.response}"
        }
        assertEquals(emptyList<String>(), wrong)
    }
}
