package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedExerciseCatalogueTest {
    private val catalogue = GuidedExerciseCatalogue.exercises

    private fun exercise(id: String, language: ContentLanguage) = GuidedExercise(
        id = id,
        audioLanguage = language,
        durationSeconds = 120,
        title = LocalizedText.of(english = "English $id", dutch = "Nederlands $id"),
        description = LocalizedText.of(english = "d"),
        rawResourceName = "res_" + id.replace('-', '_'),
        credit = catalogue.first().credit,
    )

    @Test
    fun `the first version has the three Free Mindfulness Project exercises`() {
        assertEquals(
            listOf("three-step-breathing-space", "mindfulness-of-sounds", "mountain-meditation"),
            catalogue.map { it.id },
        )
        assertEquals(listOf(214, 182, 492), catalogue.map { it.durationSeconds })
        assertEquals(
            listOf("fmp_three_step_breathing_space", "fmp_mindfulness_of_sounds", "fmp_mountain_meditation"),
            catalogue.map { it.rawResourceName },
        )
    }

    @Test
    fun `ids and resource names are unique`() {
        assertEquals(catalogue.size, catalogue.map { it.id }.toSet().size)
        assertEquals(catalogue.size, catalogue.map { it.rawResourceName }.toSet().size)
    }

    @Test
    fun `every exercise names title, author, source and licence with its version and links`() {
        for (e in catalogue) {
            assertEquals("Peter Morgan", e.credit.author)
            assertEquals("Free Mindfulness Project", e.credit.sourceName)
            assertTrue(e.credit.licenceName.contains("3.0"))
            assertTrue(e.credit.licenceName.contains("NonCommercial-ShareAlike"))
            assertEquals("https://creativecommons.org/licenses/by-nc-sa/3.0/", e.credit.licenceUrl)
            assertEquals("https://www.freemindfulness.org/download", e.credit.sourceUrl)
            assertTrue(e.credit.originalTitle.isNotBlank())
        }
    }

    @Test
    fun `every title and description exists in English and Dutch`() {
        for (e in catalogue) {
            assertTrue(e.title.forLanguage(ContentLanguage.DUTCH) != e.title.forLanguage(ContentLanguage.ENGLISH))
            assertTrue(e.description.forLanguage(ContentLanguage.DUTCH) != e.description.forLanguage(ContentLanguage.ENGLISH))
        }
    }

    @Test
    fun `the recordings are English`() {
        assertTrue(catalogue.all { it.audioLanguage == ContentLanguage.ENGLISH })
    }

    @Test
    fun `with a Dutch app the English recordings are still listed and none is left out`() {
        val ordered = GuidedExerciseCatalogue.orderedFor(ContentLanguage.DUTCH)
        assertEquals(catalogue, ordered)
        assertTrue(ordered.none { it.audioMatches(ContentLanguage.DUTCH) })
    }

    @Test
    fun `exercises in the app language come first, each group keeps the catalogue order`() {
        val items = listOf(
            exercise("en1", ContentLanguage.ENGLISH),
            exercise("nl1", ContentLanguage.DUTCH),
            exercise("en2", ContentLanguage.ENGLISH),
            exercise("nl2", ContentLanguage.DUTCH),
        )
        assertEquals(listOf("nl1", "nl2", "en1", "en2"), GuidedExerciseCatalogue.orderedFor(ContentLanguage.DUTCH, items).map { it.id })
        assertEquals(listOf("en1", "en2", "nl1", "nl2"), GuidedExerciseCatalogue.orderedFor(ContentLanguage.ENGLISH, items).map { it.id })
    }

    @Test
    fun `a Dutch recording is just another entry`() {
        val withDutch = catalogue + exercise("nl-new", ContentLanguage.DUTCH)
        val ordered = GuidedExerciseCatalogue.orderedFor(ContentLanguage.DUTCH, withDutch)
        assertEquals("nl-new", ordered.first().id)
        assertEquals(withDutch.size, ordered.size)
    }

    @Test
    fun `text falls back to English when the language has none`() {
        val text = LocalizedText.of(english = "Hello")
        assertEquals("Hello", text.forLanguage(ContentLanguage.DUTCH))
        assertEquals("Hello", text.forLanguage(ContentLanguage.ENGLISH))
    }

    @Test
    fun `language tags map to languages with English as the fallback`() {
        assertEquals(ContentLanguage.DUTCH, ContentLanguage.fromTag("nl"))
        assertEquals(ContentLanguage.DUTCH, ContentLanguage.fromTag("nl-NL"))
        assertEquals(ContentLanguage.ENGLISH, ContentLanguage.fromTag("en-GB"))
        assertEquals(ContentLanguage.ENGLISH, ContentLanguage.fromTag("de"))
        assertEquals(ContentLanguage.ENGLISH, ContentLanguage.fromTag(""))
    }

    @Test
    fun `an exercise can be found by id`() {
        assertEquals("mountain-meditation", GuidedExerciseCatalogue.byId("mountain-meditation")?.id)
        assertNull(GuidedExerciseCatalogue.byId("nope"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `English text is required`() {
        LocalizedText(mapOf(ContentLanguage.DUTCH to "alleen Nederlands"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a resource name must be lowercase`() {
        GuidedExercise(
            "x", ContentLanguage.ENGLISH, 10, LocalizedText.of("t"), LocalizedText.of("d"), "Bad-Name", catalogue.first().credit,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `credit links must be https`() {
        ContentCredit("t", "a", "s", "http://example.org", "l", "https://example.org")
    }
}
