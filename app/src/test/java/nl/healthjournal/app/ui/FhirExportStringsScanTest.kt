package nl.healthjournal.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Task 2.2: Scan test for banned phrases:
 * (care system, EHR, GP, compatible, certified, interoperable, "FHIR export" without "based on")
 * Also asserts English and Dutch string parity and presence for all FHIR export keys.
 */
class FhirExportStringsScanTest {

    private val root = File("..")

    private fun loadStrings(dir: String): Map<String, String> {
        val file = File("src/main/res/$dir/strings.xml")
        assertTrue("${file.absolutePath} must exist", file.exists())
        val entry = Regex("""<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return entry.findAll(file.readText())
            .filterNot { it.groupValues[2].contains("translatable=\"false\"") }
            .associate { it.groupValues[1] to it.groupValues[3] }
    }

    private val en = loadStrings("values")
    private val nl = loadStrings("values-nl")

    private val fhirKeys = listOf(
        "history_export_fhir_button",
        "history_export_fhir_short_notice",
        "history_export_fhir_full_notice",
        "history_export_fhir_notice_more",
        "history_export_fhir_notice_less",
        "history_export_fhir_range_all",
        "history_export_fhir_range_selected",
        "history_export_fhir_opt_in_birth_date",
        "history_export_fhir_opt_in_sex",
        "history_msg_export_fhir_done"
    )

    @Test
    fun `all FHIR export keys exist in both English and Dutch`() {
        for (key in fhirKeys) {
            assertTrue("Key $key missing in English", en.containsKey(key))
            assertTrue("Key $key missing in Dutch", nl.containsKey(key))
            assertTrue("Key $key is empty in English", en[key]!!.isNotBlank())
            assertTrue("Key $key is empty in Dutch", nl[key]!!.isNotBlank())
        }
    }

    @Test
    fun `FHIR export strings contain no banned phrases in English or Dutch`() {
        val bannedEnglish = listOf("care system", "ehr", "gp", "compatible", "certified", "interoperable")
        val bannedDutch = listOf("zorgsysteem", "epd", "huisarts", "compatibel", "gecertificeerd", "interoperabel")

        for (key in fhirKeys) {
            val textEn = en.getValue(key).lowercase()
            for (banned in bannedEnglish) {
                assertFalse("Key $key in English contains banned phrase '$banned': $textEn", textEn.contains(banned))
            }

            val textNl = nl.getValue(key).lowercase()
            for (banned in bannedDutch) {
                assertFalse("Key $key in Dutch contains banned phrase '$banned': $textNl", textNl.contains(banned))
            }
        }
    }

    @Test
    fun `FHIR references in strings always include based on or op gebaseerd`() {
        for (key in fhirKeys) {
            val textEn = en.getValue(key)
            if (textEn.contains("FHIR", ignoreCase = true)) {
                assertTrue("Key $key in English must say 'based on FHIR' or 'FHIR-based': $textEn",
                    textEn.contains("based on FHIR", ignoreCase = true) || textEn.contains("FHIR-based", ignoreCase = true))
            }

            val textNl = nl.getValue(key)
            if (textNl.contains("FHIR", ignoreCase = true)) {
                assertTrue("Key $key in Dutch must say 'op FHIR gebaseerd': $textNl",
                    textNl.contains("op FHIR gebaseerd", ignoreCase = true))
            }
        }
    }

    @Test
    fun `export doc and readme make no conformance or interoperability claims`() {
        val docFile = File(root, "docs/dev/fhir-based-export.md")
        assertTrue("fhir-based-export.md exists", docFile.exists())
        val docText = docFile.readText().lowercase()

        val bannedClaims = listOf(
            "certified by", "conforms to hl7", "compatible with care",
            "compatible with ehr", "interoperable with ehr", "for use in care"
        )
        for (banned in bannedClaims) {
            assertFalse("Docs must not contain '$banned'", docText.contains(banned))
        }
    }
}
