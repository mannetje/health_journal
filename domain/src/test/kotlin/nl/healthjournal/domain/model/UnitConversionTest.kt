package nl.healthjournal.domain.model

import nl.healthjournal.domain.model.common.UnitConversion
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {

    @Test
    fun `pounds and kilograms convert both ways`() {
        assertEquals(74.8427, UnitConversion.lbToKg(165.0), 0.001)
        assertEquals(165.0, UnitConversion.kgToLb(UnitConversion.lbToKg(165.0)), 1e-9)
    }

    @Test
    fun `miles and kilometers convert both ways`() {
        assertEquals(8.04672, UnitConversion.milesToKm(5.0), 1e-9)
        assertEquals(5.0, UnitConversion.kmToMiles(UnitConversion.milesToKm(5.0)), 1e-9)
    }

    @Test
    fun `glucose uses the same factor as mg per dL input`() {
        assertEquals(5.55, UnitConversion.mgDlToMmol(100.0), 1e-9)
        assertEquals(100.0, UnitConversion.mmolToMgDl(5.55), 1e-9)
    }

    @Test
    fun `height converts to feet and inches and back`() {
        assertEquals(5 to 10, UnitConversion.cmToFeetInches(178))
        assertEquals(6 to 0, UnitConversion.cmToFeetInches(183))
        assertEquals(178, UnitConversion.feetInchesToCm(5, 10))
    }

    @Test
    fun `stored kilograms are rounded to two decimals`() {
        assertEquals("74.84", UnitConversion.toStoredKg(UnitConversion.lbToKg(165.0)).toPlainString())
    }
}
