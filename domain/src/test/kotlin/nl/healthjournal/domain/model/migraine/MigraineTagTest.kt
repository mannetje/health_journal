package nl.healthjournal.domain.model.migraine

import nl.healthjournal.domain.model.common.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MigraineTagTest {
    private fun tag(label: String, kind: MigraineTagKind = MigraineTagKind.TRIGGER) =
        MigraineTag(MigraineTagId.generate(), ProfileId.generate(), kind, label)

    @Test fun `label of 1 to 30 characters is accepted`() {
        assertEquals("a", tag("a").label)
        assertEquals(30, tag("a".repeat(30)).label.length)
    }

    @Test fun `empty, over-long, untrimmed and pipe labels are refused`() {
        assertThrows(IllegalArgumentException::class.java) { tag("") }
        assertThrows(IllegalArgumentException::class.java) { tag("a".repeat(31)) }
        assertThrows(IllegalArgumentException::class.java) { tag(" lunch") }
        assertThrows(IllegalArgumentException::class.java) { tag("a|b") }
    }

    @Test fun `normalizeLabel trims and joins line breaks`() {
        assertEquals("skipped lunch", MigraineTag.normalizeLabel("  skipped\n lunch "))
    }

    @Test fun `sameLabel ignores case and surrounding spaces`() {
        assertTrue(MigraineTag.sameLabel("Skipped Lunch", " skipped lunch "))
        assertFalse(MigraineTag.sameLabel("rest", "rested"))
    }

    @Test fun `a new tag is not archived`() {
        assertFalse(tag("rest").archived)
    }
}
