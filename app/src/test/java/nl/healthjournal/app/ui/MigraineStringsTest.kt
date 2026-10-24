package nl.healthjournal.app.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Reads the string resource files directly, so it runs as a plain unit test. */
class MigraineStringsTest {

    private fun load(dir: String): Map<String, String> {
        val file = File("src/main/res/$dir/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.documentElement.childNodes
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) as? Element }
            .filter { it.tagName == "string" }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private val enAll = load("values")
    private val nlAll = load("values-nl")

    private fun isMigraine(name: String) = "migraine" in name

    private val en = enAll.filterKeys(::isMigraine)
    private val nl = nlAll.filterKeys(::isMigraine)

    @Test
    fun `english and dutch have the same migraine keys`() {
        assertTrue(en.isNotEmpty())
        assertEquals(en.keys, nl.keys)
    }

    @Test
    fun `placeholders match between languages`() {
        val placeholder = Regex("""%\d\$[sd]""")
        for ((name, text) in en) {
            assertEquals(
                name,
                placeholder.findAll(text).map { it.value }.sorted().toList(),
                placeholder.findAll(nl.getValue(name)).map { it.value }.sorted().toList()
            )
        }
    }

    @Test
    fun `no text in the app uses the word attack or aanval`() {
        val banned = Regex("attack|aanval", RegexOption.IGNORE_CASE)
        for ((lang, texts) in listOf("en" to enAll, "nl" to nlAll)) {
            for ((key, text) in texts) {
                assertTrue("$lang $key uses a banned word: $text", !banned.containsMatchIn(text))
            }
        }
    }

    @Test
    fun `migraine strings neither advise nor judge a result`() {
        val judging = Regex(
            "improved|effective|worked|should|recommend|consult|urgent|immediately|" +
                "verbeterd|effectief|werkte|aanbevel|raadpleeg|dringend|direct",
            RegexOption.IGNORE_CASE
        )
        for ((lang, texts) in listOf("en" to en, "nl" to nl)) {
            for ((key, text) in texts) {
                assertTrue("$lang $key reads like advice or a verdict: $text", !judging.containsMatchIn(text))
            }
        }
    }

    @Test
    fun `dutch migraine strings address the reader as je`() {
        val formal = Regex("""\b(u|uw|uzelf)\b""", RegexOption.IGNORE_CASE)
        for ((key, text) in nl) {
            assertTrue("$key is formal: $text", !formal.containsMatchIn(text))
        }
    }
}
