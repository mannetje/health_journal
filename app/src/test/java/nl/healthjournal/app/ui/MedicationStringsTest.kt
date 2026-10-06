package nl.healthjournal.app.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Reads the string resource files directly, so it runs as a plain unit test. */
class MedicationStringsTest {

    private class Entries(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(dir: String): Entries {
        val file = File("src/main/res/$dir/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val name = el.getAttribute("name")
            if (!name.startsWith("medication_") && !name.startsWith("nav_pillbox") &&
                !name.startsWith("nav_title_pillbox") && name != "profile_medication_notice_button"
            ) continue
            when (el.tagName) {
                "string" -> strings[name] = el.textContent
                "plurals" -> {
                    val items = el.getElementsByTagName("item")
                    plurals[name] = (0 until items.length).associate {
                        val item = items.item(it) as Element
                        item.getAttribute("quantity") to item.textContent
                    }
                }
            }
        }
        return Entries(strings, plurals)
    }

    private val en = load("values")
    private val nl = load("values-nl")

    @Test
    fun `english and dutch have the same medication keys`() {
        assertTrue(en.strings.isNotEmpty())
        assertEquals(en.strings.keys, nl.strings.keys)
        assertEquals(en.plurals.keys, nl.plurals.keys)
    }

    @Test
    fun `every plural has one and other in both languages`() {
        for ((name, items) in en.plurals + nl.plurals) {
            assertTrue("$name needs one and other", items.keys.containsAll(listOf("one", "other")))
        }
    }

    @Test
    fun `placeholders match between languages`() {
        val placeholder = Regex("""%\d\$[sd]""")
        for ((name, text) in en.strings) {
            assertEquals(
                name,
                placeholder.findAll(text).map { it.value }.sorted().toList(),
                placeholder.findAll(nl.strings.getValue(name)).map { it.value }.sorted().toList(),
            )
        }
    }

    @Test
    fun `counted doses read as expected`() {
        fun plural(e: Entries, name: String, q: String) = e.plurals.getValue(name).getValue(q)
        assertEquals("%1\$s tablet", plural(en, "medication_dose_tablets", "one"))
        assertEquals("%1\$s tablets", plural(en, "medication_dose_tablets", "other"))
        assertEquals("%1\$s tablet", plural(nl, "medication_dose_tablets", "one"))
        assertEquals("%1\$s tabletten", plural(nl, "medication_dose_tablets", "other"))
        assertEquals("%1\$s puff", plural(en, "medication_dose_puffs", "one"))
        assertEquals("%1\$s pufjes", plural(nl, "medication_dose_puffs", "other"))
    }

    @Test
    fun `dutch uses IE and microgram`() {
        assertEquals("IE", nl.strings.getValue("medication_unit_iu"))
        assertEquals("microgram", nl.strings.getValue("medication_unit_mcg"))
        assertEquals("IE/ml", nl.strings.getValue("medication_strength_iu_per_ml"))
        assertEquals("IU", en.strings.getValue("medication_unit_iu"))
        assertEquals("mcg", en.strings.getValue("medication_unit_mcg"))
    }
}
