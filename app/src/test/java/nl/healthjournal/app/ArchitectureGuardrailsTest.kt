package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Architecture rules of ADR 0002, 0035 and the application boundary, read from the source files so they run
 * as plain unit tests (the working directory is `app/`). Every rule fails on a new offender. An accepted
 * exception goes in the allowed set of its rule, with the reason next to it.
 */
class ArchitectureGuardrailsTest {

    private val root = File("..")
    private val domainMain = File(root, "domain/src/main/kotlin/nl/healthjournal/domain")
    private val appMain = File(root, "app/src/main/java/nl/healthjournal/app")

    private fun kotlinFiles(dir: File) = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    private fun File.rel() = relativeTo(root).invariantSeparatorsPath

    // --- 3.1 ports and dependency injection ---

    @Test
    fun theCombinedRepositoryPortIsGone() {
        val sources = listOf("domain", "data", "app").flatMap { module ->
            kotlinFiles(File(root, module)).filter { "/build/" !in it.invariantSeparatorsPath && it.name != "ArchitectureGuardrailsTest.kt" }
        }
        val users = sources.filter { "HealthLogRepositoryPort" in it.readText() }.map { it.rel() }
        assertTrue("HealthLogRepositoryPort was split into one port per metric: $users", users.isEmpty())
    }

    @Test
    fun domainAndDataSourcesDoNotUseHiltDaggerOrJavaxInject() {
        val forbidden = Regex("""^import (dagger|javax\.inject|jakarta\.inject)""", RegexOption.MULTILINE)
        val offenders = listOf("domain", "data").flatMap { module ->
            kotlinFiles(File(root, "$module/src/main")).filter { forbidden.containsMatchIn(it.readText()) }
        }.map { it.rel() }
        assertTrue("DI belongs in :app (ADR 0035): $offenders", offenders.isEmpty())
    }

    // --- 3.2 no wall-clock default ---

    @Test
    fun noInstantNowInTheDomainOrAnyClassThatTakesAClock() {
        val offenders = kotlinFiles(domainMain).filter { file ->
            val text = file.readText()
            "Instant.now()" in text && ("Clock" in text || file.invariantSeparatorsPath.contains("/command/"))
        }.map { it.rel() }
        assertTrue("use the injected Clock instead of Instant.now(): $offenders", offenders.isEmpty())
    }

    @Test
    fun commandPackageHasNoWallClockDefault() {
        val offenders = kotlinFiles(File(domainMain, "command")).filter { "Instant.now()" in it.readText() }
        assertTrue(offenders.map { it.rel() }.toString(), offenders.isEmpty())
    }

    // --- 3.3 ViewModels and handlers ---

    private val viewModels get() = kotlinFiles(appMain).filter { it.name.endsWith("ViewModel.kt") }

    private fun offenders(rule: (String) -> Boolean) =
        viewModels.filter { rule(it.readText()) }.map { it.name.removeSuffix(".kt") }.sorted()

    @Test
    fun viewModelsDoNotImportSecondaryPorts() {
        val found = offenders { "import nl.healthjournal.domain.port.secondary." in it }
        assertTrue("a ViewModel talks to the Dispatcher, not to a port: $found", found.isEmpty())
    }

    @Test
    fun viewModelsDoNotUseAUseCaseBundle() {
        val found = offenders { Regex("""\b[A-Z][A-Za-z]*UseCases\b""").containsMatchIn(it) }
        assertTrue("a ViewModel dispatches commands and queries, it has no use case bundle: $found", found.isEmpty())
    }

    /** The only catches left in a ViewModel are around Android APIs, and they name a narrower type. */
    @Test
    fun viewModelsDoNotCatchExceptionAroundADispatch() {
        val found = offenders { Regex("""catch \((\w+): (Exception|Throwable)\)""").containsMatchIn(it) }
        assertTrue("a failed dispatch is a DomainResult.Failure, not an exception: $found", found.isEmpty())
    }

    @Test
    fun everyHandlerReturnsADomainResult() {
        val signature = Regex("""override (?:suspend )?fun (handle|observe)\([^)]*\)\s*:\s*([^={\r\n]+)""")
        val offenders = kotlinFiles(domainMain).flatMap { file ->
            signature.findAll(file.readText()).map { it.groupValues[2].trim() to file.rel() }.toList()
        }.filterNot { (type, _) -> type.startsWith("DomainResult<") || type.startsWith("Flow<DomainResult<") }
        assertTrue("handlers return DomainResult (or a Flow of it): $offenders", offenders.isEmpty())
    }

    // --- 3.4 context map of `domain/model` ---

    private val shared = "common"

    /** Contexts that may import `profile` (it is upstream of them). */
    private val downstreamOfProfile = setOf("metrics", "medication", "migraine")

    private fun allowed(from: String, to: String): Boolean = when {
        from == to -> true
        to == shared -> true
        from == "backup" -> true
        to == "profile" && from in downstreamOfProfile -> true
        else -> false
    }

    /** Edge `from -> to` -> why it is still there. Empty since task 3.5; an accepted exception is added here with its reason. */
    private val knownCouplings: Map<String, String> = emptyMap()

    private fun contextEdges(): Map<String, List<String>> {
        val modelDir = File(domainMain, "model")
        val reference = Regex("""nl\.healthjournal\.domain\.(model\.(\w+)|usecase|port)\.""")
        val edges = sortedMapOf<String, MutableList<String>>()
        for (file in kotlinFiles(modelDir)) {
            val from = file.relativeTo(modelDir).invariantSeparatorsPath.substringBefore('/')
            for (match in reference.findAll(file.readText())) {
                val to = match.groupValues[2].ifEmpty { match.groupValues[1] }
                if (!allowed(from, to)) edges.getOrPut("$from -> $to") { mutableListOf() }.add(file.name)
            }
        }
        return edges.mapValues { it.value.distinct().sorted() }
    }

    @Test
    fun domainModelContextsOnlyImportWhatTheContextMapAllows() {
        val found = contextEdges()
        val unexpected = found.keys - knownCouplings.keys
        assertEquals("forbidden context edges (file names: ${unexpected.map { found[it] }})", emptySet<String>(), unexpected)
        assertEquals("these couplings are fixed, remove them from the list", emptySet<String>(), knownCouplings.keys - found.keys)
    }

    @Test
    fun modelNeverImportsUseCasesOrPorts() {
        val edges = contextEdges().keys.filter { it.endsWith("-> usecase") || it.endsWith("-> port") }
        assertFalse("model must not depend on use cases or ports: $edges", edges.isNotEmpty())
    }
}
