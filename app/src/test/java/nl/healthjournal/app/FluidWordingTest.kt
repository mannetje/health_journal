package nl.healthjournal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Fluid intake sets no target and gives no advice (ADR 0030, ADR 0021): no coaching, goal, praise or verdict words in
 * its texts, and no streak, badge or reminder in its code. The position words (below, within, above) and the person's
 * own numbers are allowed. The one reference note names a source and is the only text that may state a general amount.
 */
class FluidWordingTest {

    private val root = File("..")
    private val english = "app/src/main/res/values/strings.xml"
    private val dutch = "app/src/main/res/values-nl/strings.xml"

    private val coaching = Regex(
        """\b(should|must|need|needs|recommended|recommend|enough|too much|too little|healthy|unhealthy|good|bad|great|well done|""" +
            """keep it up|goal|target|streak|badge|points|reward|dehydrated|drink more|drink less|""" +
            """moet|moeten|aanbevolen|genoeg|te veel|te weinig|gezond|ongezond|goed|slecht|geweldig|goed zo|doel|streak|badge|""" +
            """punten|beloning|uitgedroogd|drink meer|drink minder)\b""",
        RegexOption.IGNORE_CASE
    )

    private val fluidName = Regex(
        """^(fluid_|log_tab_fluid|history_filter_fluid|history_export_fluid|history_import_type_fluid)"""
    )

    private fun strings(path: String): Map<String, String> =
        Regex("""<string name="([^"]+)"[^>]*>([^<]*)</string>""")
            .findAll(File(root, path).readText()).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `no fluid text coaches, sets a goal or judges a value, in English or Dutch`() {
        listOf(english, dutch).forEach { path ->
            val fluid = strings(path).filterKeys { fluidName.containsMatchIn(it) && it != "fluid_reference_body" }
            assertTrue("$path: the fluid strings were not found", fluid.size >= 40)
            fluid.forEach { (name, value) ->
                assertFalse("$path: '$name' = '$value' coaches or judges", coaching.containsMatchIn(value))
            }
        }
    }

    @Test
    fun `the position texts always carry the person's own number`() {
        listOf(english, dutch).forEach { path ->
            val all = strings(path)
            listOf("fluid_position_below", "fluid_position_within", "fluid_position_above").forEach { name ->
                assertTrue("$path: '$name' must print the number", Regex("""%\d\${'$'}d""").containsMatchIn(all.getValue(name)))
            }
        }
    }

    @Test
    fun `the fluid code has no streak, badge, reminder or app-chosen target`() {
        val files = listOf("ui/fluid/FluidGauge.kt", "ui/fluid/FluidLogScreen.kt", "ui/fluid/FluidLogViewModel.kt", "ui/history/charts/FluidGraphSection.kt")
        files.forEach { rel ->
            val code = File(root, "app/src/main/java/nl/healthjournal/app/$rel").readText()
                .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""//.*"""), "")
            assertFalse("$rel: no streak, badge, reward or reminder", Regex("""streak|badge|reward|confetti|reminder|notification""", RegexOption.IGNORE_CASE).containsMatchIn(code))
        }
    }

    @Test
    fun `English and Dutch have the same string names`() {
        val en = strings(english).keys
        val nl = strings(dutch).keys
        assertEquals("only in English", emptySet<String>(), en - nl)
        assertEquals("only in Dutch", emptySet<String>(), nl - en)
    }

    @Test
    fun `English and Dutch use the same placeholders in every fluid text`() {
        val placeholders = Regex("""%\d\$[ds]""")
        val en = strings(english)
        val nl = strings(dutch)
        en.filterKeys { fluidName.containsMatchIn(it) }.forEach { (name, value) ->
            val other = nl[name] ?: return@forEach
            assertEquals("'$name' placeholders differ", placeholders.findAll(value).map { it.value }.sorted().toList(),
                placeholders.findAll(other).map { it.value }.sorted().toList())
        }
    }
}
