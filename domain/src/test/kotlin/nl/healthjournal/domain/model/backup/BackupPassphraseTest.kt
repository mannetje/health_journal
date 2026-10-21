package nl.healthjournal.domain.model.backup

import nl.healthjournal.domain.model.backup.BackupPassphrase.Check
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPassphraseTest {

    private fun check(a: String, b: String = a) = BackupPassphrase.check(a.toCharArray(), b.toCharArray())

    @Test
    fun `eight characters is accepted`() = assertEquals(Check.OK, check("abcdefgh"))

    @Test
    fun `seven characters is too short`() = assertEquals(Check.TOO_SHORT, check("abcdefg"))

    @Test
    fun `words with spaces count every character`() = assertEquals(Check.OK, check("a b c d e"))

    @Test
    fun `two different entries mismatch`() = assertEquals(Check.MISMATCH, check("correct horse", "correct hors"))

    @Test
    fun `too short wins over mismatch`() = assertEquals(Check.TOO_SHORT, check("abc", "abd"))

    @Test
    fun `length is counted after normalisation`() {
        // Eight fullwidth letters normalise to eight ASCII letters; a decomposed e-acute is one character after NFKC.
        assertEquals(Check.OK, check("ａｂｃｄｅｆｇｈ"))
        assertEquals(Check.TOO_SHORT, check("abcdefǵ".substring(0, 7)))
    }

    @Test
    fun `normalisation makes composed and decomposed forms equal`() {
        val composed = "café café".toCharArray()
        val decomposed = "café café".toCharArray()
        assertArrayEquals(BackupPassphrase.toUtf8(composed), BackupPassphrase.toUtf8(decomposed))
    }
}
