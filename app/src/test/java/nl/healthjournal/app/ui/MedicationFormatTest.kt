package nl.healthjournal.app.ui

import java.math.BigDecimal
import java.util.Locale
import nl.healthjournal.app.ui.medication.formatAmount
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MedicationFormatTest {
    private lateinit var saved: Locale

    @Before
    fun saveLocale() {
        saved = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(saved)
    }

    @Test
    fun `dutch uses a decimal comma and english a point`() {
        Locale.setDefault(Locale.forLanguageTag("nl-NL"))
        assertEquals("1,5", formatAmount(BigDecimal("1.5")))
        Locale.setDefault(Locale.US)
        assertEquals("1.5", formatAmount(BigDecimal("1.5")))
    }

    @Test
    fun `the stored amount is not changed by the display language`() {
        val stored = BigDecimal("0.25")
        Locale.setDefault(Locale.forLanguageTag("nl-NL"))
        formatAmount(stored)
        Locale.setDefault(Locale.US)
        assertEquals("0.25", formatAmount(stored))
        assertEquals(BigDecimal("0.25"), stored)
    }

    @Test
    fun `whole amounts have no trailing zeros`() {
        Locale.setDefault(Locale.US)
        assertEquals("2", formatAmount(BigDecimal("2.0")))
    }
}
