package nl.healthjournal.domain.model

import nl.healthjournal.domain.model.metrics.EntryComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryCommentTest {

    @Test
    fun `blank or missing text is no comment`() {
        assertNull(EntryComment.ofOrNull(null))
        assertNull(EntryComment.ofOrNull(""))
        assertNull(EntryComment.ofOrNull("   \n\t "))
    }

    @Test
    fun `text is trimmed`() {
        assertEquals("after lunch", EntryComment.ofOrNull("  after lunch \t")?.text)
    }

    @Test
    fun `line breaks become a single space`() {
        assertEquals("one two", EntryComment.ofOrNull("one\ntwo")?.text)
        assertEquals("one two", EntryComment.ofOrNull("one \r\n\r\n two")?.text)
    }

    @Test
    fun `exactly 200 characters is accepted and 201 is rejected`() {
        assertEquals(200, EntryComment.ofOrNull("a".repeat(200))?.text?.length)
        assertThrows(IllegalArgumentException::class.java) { EntryComment.ofOrNull("a".repeat(201)) }
    }

    @Test
    fun `the limit counts the trimmed text`() {
        assertEquals(200, EntryComment.ofOrNull("  " + "a".repeat(200) + "  ")?.text?.length)
    }

    @Test
    fun `constructor rejects text that is not normalised`() {
        assertThrows(IllegalArgumentException::class.java) { EntryComment(" padded ") }
        assertThrows(IllegalArgumentException::class.java) { EntryComment("a\nb") }
        assertThrows(IllegalArgumentException::class.java) { EntryComment("") }
    }
}
