package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level privacy checks. They read the files directly, so they run as plain unit tests. */
class PrivacyChecksTest {

    private val moduleRoots = listOf("src/main", "../data/src/main", "../domain/src/main")

    @Test
    fun manifestDoesNotRequestInternet() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse("INTERNET must stay out of the manifest", manifest.contains("android.permission.INTERNET"))
    }

    @Test
    fun noLoggingInProductionSources() {
        val offenders = moduleRoots.flatMap { root ->
            File(root).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }.filter { file ->
            file.readLines().any { line ->
                val code = line.trim()
                !code.startsWith("//") && !code.startsWith("*") &&
                    (code.contains("android.util.Log") || Regex("""\bLog\.[dviwe]\(""").containsMatchIn(code) ||
                        Regex("""\bprintln\(""").containsMatchIn(code) || code.contains("printStackTrace"))
            }
        }
        assertTrue("Logging can leak names and doses: ${offenders.map { it.path }}", offenders.isEmpty())
    }
}
