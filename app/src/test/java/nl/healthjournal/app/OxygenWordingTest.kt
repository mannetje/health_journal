package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Oxygen saturation is a plain metric with no assessment (ADR 0026): no judging words in its texts and no
 * bands, limit lines or category in its code. Lowest, highest and average are figures, not judgements.
 */
class OxygenWordingTest {

    private val root = File("..")

    private val judging = Regex(
        """\b(low|high|normal|abnormal|healthy|unhealthy|good|bad|safe|unsafe|dangerous|critical|warning|""" +
            """laag|hoog|normaal|gezond|goed|slecht|veilig|gevaarlijk|kritiek|waarschuwing)\b""",
        RegexOption.IGNORE_CASE
    )

    private fun oxygenStrings(path: String): List<Pair<String, String>> =
        Regex("""<string name="((?:log_(?:tab_)?(?:msg_)?oxygen|oxygen_|history_(?:filter_)?oxygen|history_export_oxygen|history_import_type_oxygen|trend_oxygen)[^"]*)"[^>]*>([^<]*)</string>""")
            .findAll(File(root, path).readText()).map { it.groupValues[1] to it.groupValues[2] }.toList()

    @Test
    fun `no oxygen text judges a value, in English or Dutch`() {
        listOf("app/src/main/res/values/strings.xml", "app/src/main/res/values-nl/strings.xml").forEach { path ->
            val strings = oxygenStrings(path)
            assertTrue("$path: the oxygen strings were not found", strings.size >= 25)
            strings.forEach { (name, value) ->
                assertFalse("$path: '$name' = '$value' judges a value", judging.containsMatchIn(value))
            }
        }
    }

    @Test
    fun `the oxygen screens and charts carry no band, limit line or category`() {
        listOf(
            "app/src/main/java/nl/healthjournal/app/ui/history/charts/OxygenSaturationSections.kt",
            "app/src/main/java/nl/healthjournal/app/ui/logging/OxygenFields.kt"
        ).forEach { path ->
            val code = File(root, path).readText()
            assertFalse("$path: no category", Regex("""Category|Nhg""").containsMatchIn(code))
            assertFalse("$path: no bands or limit lines", Regex("""band|limit\s*line|threshold|referenceLine""", RegexOption.IGNORE_CASE)
                .containsMatchIn(code.replace(Regex("""/\*\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")))
        }
    }
}
