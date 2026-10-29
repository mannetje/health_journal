package nl.healthjournal.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogLinkTest {

    private val base = "https://github.com/mannetje/health_journal/blob/master/CHANGELOG.md"

    @Test
    fun anchorScrollsToTheVersionSection() {
        assertEquals("$base#163---2026-10-28", changelogUrl("163---2026-10-28"))
    }

    @Test
    fun blankAnchorOpensTheTopOfTheFile() {
        assertEquals(base, changelogUrl(""))
    }
}
