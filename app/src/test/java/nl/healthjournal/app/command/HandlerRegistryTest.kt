package nl.healthjournal.app.command

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every Command, Query and ObserveQuery in `:domain` has a handler registered in CommandModule (read from source). */
class HandlerRegistryTest {
    private val root = File("..")
    private val domain = File(root, "domain/src/main/kotlin")
    private val module = File(root, "app/src/main/java/nl/healthjournal/app/command/CommandModule.kt").readText()

    private fun requests(kind: String): List<String> {
        val declaration = Regex("""(?:data )?(?:class|object) (\w+)(?:\((?:[^()]|\([^()]*\))*\))?\s*:\s*$kind<""")
        return domain.walkTopDown().filter { it.extension == "kt" }
            .flatMap { declaration.findAll(it.readText()).map { m -> m.groupValues[1] } }.sorted().toList()
    }

    private fun all() = listOf("Command", "Query", "ObserveQuery").flatMap { requests(it) }.toSet()

    private fun registered(): Set<String> =
        Regex("""@ClassKey\((\w+)::class\)""").findAll(module).map { it.groupValues[1] }.toSet()

    @Test fun `every command and query has a registered handler`() {
        assertEquals("requests without a handler", emptySet<String>(), all() - registered())
    }

    @Test fun `no handler is registered for a request that does not exist`() {
        assertEquals(emptySet<String>(), registered() - all())
    }
}
