package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every string exists in English and Dutch, with the same placeholders. The per-feature string tests cover their
 * own keys; this one covers the rest, the reminder and adherence texts included, so a new key cannot ship in one
 * language only. Strings marked `translatable="false"` are left out.
 */
class StringParityTest {

    private fun load(dir: String): Map<String, String> {
        // Unit tests run with the module directory as working directory.
        val file = File("src/main/res/$dir/strings.xml")
        assertTrue("${file.absolutePath} must exist", file.exists())
        val entry = Regex("""<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return entry.findAll(file.readText())
            .filterNot { it.groupValues[2].contains("translatable=\"false\"") }
            .associate { it.groupValues[1] to it.groupValues[3] }
    }

    private val en = load("values")
    private val nl = load("values-nl")

    @Test
    fun `both languages define the same string keys`() {
        assertTrue(en.isNotEmpty())
        assertEquals("only in English: ${en.keys - nl.keys}", emptySet<String>(), en.keys - nl.keys)
        assertEquals("only in Dutch: ${nl.keys - en.keys}", emptySet<String>(), nl.keys - en.keys)
    }

    @Test
    fun `the reminder and adherence texts exist in both languages`() {
        for (prefix in listOf("reminder_", "adherence_")) {
            val inEnglish = en.keys.filter { it.startsWith(prefix) }.toSet()
            assertTrue("no $prefix strings found, has the prefix changed?", inEnglish.isNotEmpty())
            assertEquals(prefix, inEnglish, nl.keys.filter { it.startsWith(prefix) }.toSet())
        }
    }

    @Test
    fun `placeholders match between languages`() {
        val placeholder = Regex("""%(\d\$)?[sdf]|%\d*\.?\d*[sdf]""")
        val mismatches = en.keys.intersect(nl.keys).filter { key ->
            placeholder.findAll(en.getValue(key)).map { it.value }.sorted().toList() !=
                placeholder.findAll(nl.getValue(key)).map { it.value }.sorted().toList()
        }
        assertEquals("placeholders differ between English and Dutch for $mismatches", emptyList<String>(), mismatches)
    }

    @Test
    fun `no Dutch string is empty`() {
        val empty = nl.filterValues { it.isBlank() }.keys.filter { en.getValue(it).isNotBlank() }
        assertEquals("empty Dutch strings: $empty", emptyList<String>(), empty)
    }
}
