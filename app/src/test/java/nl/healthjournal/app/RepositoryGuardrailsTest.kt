package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that keep facts stated in more than one place in step: the schema version, the migration chain, the
 * ADR index, the release artifact and the starting values the policy fixes. They read the files directly, so
 * they run as plain unit tests (the working directory is `app/`). A failure names the file that fell behind.
 */
class RepositoryGuardrailsTest {

    private val root = File("..")
    private fun file(path: String) = File(root, path)
    private fun text(path: String) = file(path).readText()
    private fun firstGroup(path: String, pattern: String): String =
        requireNotNull(Regex(pattern).find(text(path))) { "$path: no match for $pattern" }.groupValues[1]

    private val databaseFile = "data/src/main/java/nl/healthjournal/data/local/HealthJournalDatabase.kt"
    private val schemaDir = file("data/schemas/nl.healthjournal.data.local.HealthJournalDatabase")

    // --- Schema version and migration chain ---

    private val schemaVersion: Int get() = firstGroup(databaseFile, """version = (\d+),""").toInt()

    @Test
    fun exportedSchemasCoverEveryVersionFromOneToTheCurrentOne() {
        val exported = schemaDir.listFiles().orEmpty().map { it.nameWithoutExtension.toInt() }.sorted()
        assertEquals("one exported schema per version, none missing", (1..schemaVersion).toList(), exported)
    }

    @Test
    fun theMigrationListCoversEveryVersionStepInOrder() {
        val declared = Regex("""val (MIGRATION_\d+_\d+) = object : Migration\((\d+), (\d+)\)""")
            .findAll(text(databaseFile)).toList()
        declared.forEach {
            val (name, from, to) = it.destructured
            assertEquals("$name must be named after the versions it migrates", "MIGRATION_${from}_$to", name)
            assertEquals("$name must move up exactly one version", from.toInt() + 1, to.toInt())
        }
        val expected = (1 until schemaVersion).map { "MIGRATION_${it}_${it + 1}" }
        assertEquals("every version step has one migration", expected, declared.map { it.groupValues[1] })

        val registered = firstGroup(databaseFile, """\.addMigrations\(([^)]*)\)""").split(",").map { it.trim() }
        assertEquals("create() registers every migration, in order", expected, registered)
    }

    @Test
    fun testsAndDocsNameTheCurrentSchemaVersion() {
        val version = schemaVersion
        assertEquals(
            "MigrationChainTest.CURRENT_VERSION",
            version,
            firstGroup("data/src/test/java/nl/healthjournal/data/local/MigrationChainTest.kt", """CURRENT_VERSION = (\d+)""").toInt()
        )
        assertEquals(
            "DatabaseKeyProviderTest expected version",
            version,
            firstGroup("data/src/androidTest/java/nl/healthjournal/data/local/DatabaseKeyProviderTest.kt", """, (\d+), version1""").toInt()
        )
        val howTo = "docs/dev/how-to/change-the-database.md"
        assertEquals("$howTo: the version stated as today's", version, firstGroup(howTo, """at version (\d+) today""").toInt())
        assertTrue("$howTo: the next migration is Migration($version, ${version + 1})", text(howTo).contains("`Migration($version, ${version + 1})`"))
        assertTrue("$howTo: the registration example lists every migration", text(howTo).contains(
            ".addMigrations(" + (1 until version).joinToString(", ") { "MIGRATION_${it}_${it + 1}" } + ")"
        ))
    }

    // --- ADR index ---

    private val adrFiles: List<File>
        get() = file("docs/adr").listFiles().orEmpty().filter { it.extension == "md" }.sortedBy { it.name }

    @Test
    fun adrNumbersAreContinuousAndMatchTheirHeadings() {
        assertEquals("ADR numbers have no gap", (1..adrFiles.size).toList(), adrFiles.map { it.name.take(4).toInt() })
        adrFiles.forEach {
            val number = it.name.take(4).toInt()
            assertTrue("${it.name}: first line is '# $number. Title'", it.readLines().first().startsWith("# $number. "))
            assertTrue("${it.name}: has a Status line", it.readLines().any { line -> line.startsWith("- **Status:**") })
        }
    }

    @Test
    fun readmeIndexListsEveryAdrOnce() {
        val readme = text("README.md")
        adrFiles.forEach {
            val number = it.name.take(4)
            val links = Regex("""- \[ADR $number: [^\]]+\]\(docs/adr/${Regex.escape(it.name)}\)""").findAll(readme).count()
            assertEquals("README.md: ADR index entry for ${it.name}", 1, links)
        }
    }

    // --- Release artifact ---

    @Test
    fun releaseUploadsOnlyTheDebugApkAndItsChecksum() {
        val workflow = text(".github/workflows/android.yml")
        val uploaded = workflow.substringAfter("files: |").substringBefore("draft:").lines().map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals("the release attaches two files", 2, uploaded.size)
        assertTrue(uploaded[0], uploaded[0].endsWith("-debug.apk"))
        assertTrue(uploaded[1], uploaded[1].endsWith("-debug.apk.sha256"))
        assertFalse("no app bundle is built or published", Regex("""bundleRelease|assembleRelease|\.aab""").containsMatchIn(workflow))
    }

    @Test
    fun releaseDocsNameTheSameArtifact() {
        assertTrue("releases.md names the debug APK", text("docs/dev/releases.md").contains("health-journal-vX.Y.Z-debug.apk"))
        assertTrue("the workflow uses the fixed debug key of ADR 0008", file("docs/adr/0008-fixed-debug-signing-key.md").exists())
    }

    // --- Policy-sensitive defaults (ADR 0033) ---

    private val loggingViewModel = "app/src/main/java/nl/healthjournal/app/ui/logging/LoggingViewModel.kt"

    @Test
    fun pickerStartingPositionsAreTheOnesTheAdrNames() {
        val starts = Regex("""private const val START_\w+ = ([\d.]+)""").findAll(text(loggingViewModel)).map { it.groupValues[1] }.toList()
        assertEquals(listOf("75.0", "120", "80", "70", "90.0"), starts)
        val adr = text("docs/adr/0033-picker-starts-from-own-entries-only.md")
        listOf("weight 75.0 kg", "blood pressure 120/80", "pulse 70 bpm", "waist 90 cm").forEach {
            assertTrue("ADR 0033 names the starting position '$it'", adr.contains(it))
        }
        assertTrue("glucose has no starting position", text(loggingViewModel).contains("""val glucoseInput: String = """""))
    }

    @Test
    fun pickersNeverStartFromTheProfileOrAnIdeal() {
        val source = text(loggingViewModel)
        assertFalse("a starting value must not depend on the person's sex", Regex("""\bSex\b""").containsMatchIn(source))
        assertFalse("a starting value must not depend on height or an ideal", Regex("""heightCm|ideal""", RegexOption.IGNORE_CASE).containsMatchIn(source))
    }
}
