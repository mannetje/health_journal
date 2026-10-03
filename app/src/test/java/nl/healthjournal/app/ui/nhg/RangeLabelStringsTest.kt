package nl.healthjournal.app.ui.nhg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the range label strings of both languages straight from the resource files. */
class RangeLabelStringsTest {

    private fun load(dir: String): Map<String, String> {
        val file = listOf("src/main/res/$dir/strings.xml", "app/src/main/res/$dir/strings.xml")
            .map(::File).first { it.exists() }
        val entry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return entry.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
            .filterKeys { it.startsWith("nhg_") }
    }

    private val en = load("values")
    private val nl = load("values-nl")

    @Test
    fun `both languages define the same label keys`() {
        assertTrue(en.isNotEmpty())
        assertEquals(en.keys, nl.keys)
    }

    @Test
    fun `placeholders match between languages`() {
        val placeholder = Regex("""%\d\$[sd]""")
        for (key in en.keys) {
            assertEquals(key, placeholder.findAll(en.getValue(key)).map { it.value }.toSet(),
                placeholder.findAll(nl.getValue(key)).map { it.value }.toSet())
        }
    }

    @Test
    fun `no label names a condition`() {
        val forbidden = listOf("diabet", "hypertens", "hypotens", "hypoglyc", "hyperglyc", "pre-diabet")
        for ((lang, strings) in listOf("en" to en, "nl" to nl)) {
            for ((key, text) in strings) {
                forbidden.forEach { assertFalse("$lang $key: $text", text.lowercase().contains(it)) }
            }
        }
    }

    @Test
    fun `blood pressure and BMI labels read name dot range`() {
        for (strings in listOf(en, nl)) {
            strings.filterKeys { it.startsWith("nhg_bp_") || it.startsWith("nhg_bmi_") }
                .forEach { (key, text) -> assertTrue("$key: $text", text.contains(" · ")) }
        }
    }

    @Test
    fun `blood pressure limits are the same in both languages`() {
        for (strings in listOf(en, nl)) {
            assertTrue(strings.getValue("nhg_bp_normal").contains("140/90"))
            assertTrue(strings.getValue("nhg_bp_high").contains("140/90"))
            assertTrue(strings.getValue("nhg_bp_seriously_raised").contains("180/110"))
        }
    }
}
