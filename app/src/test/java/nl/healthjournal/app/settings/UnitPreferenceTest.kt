package nl.healthjournal.app.settings

import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitPreferenceTest {

    private fun resolve(
        locale: Locale,
        system: UnitSystemChoice = UnitSystemChoice.SYSTEM,
        glucose: GlucoseUnitChoice = GlucoseUnitChoice.SYSTEM
    ) = resolveDisplayUnits(system, glucose, locale)

    @Test
    fun `Netherlands defaults to metric and mmol per L`() {
        val units = resolve(Locale.forLanguageTag("nl-NL"))
        assertEquals(UnitSystem.METRIC, units.system)
        assertEquals(GlucoseUnit.MMOL_PER_L, units.glucose)
    }

    @Test
    fun `United States defaults to imperial and mg per dL`() {
        val units = resolve(Locale.forLanguageTag("en-US"))
        assertEquals(UnitSystem.IMPERIAL, units.system)
        assertEquals(GlucoseUnit.MG_PER_DL, units.glucose)
    }

    @Test
    fun `English with Dutch region stays metric`() {
        assertEquals(UnitSystem.METRIC, resolve(Locale.forLanguageTag("en-NL")).system)
    }

    @Test
    fun `other regions follow their own conventions`() {
        assertEquals(UnitSystem.METRIC, resolve(Locale.forLanguageTag("de-DE")).system)
        assertEquals(GlucoseUnit.MG_PER_DL, resolve(Locale.forLanguageTag("de-DE")).glucose)
        assertEquals(GlucoseUnit.MMOL_PER_L, resolve(Locale.forLanguageTag("en-CA")).glucose)
        assertEquals(GlucoseUnit.MMOL_PER_L, resolve(Locale.forLanguageTag("en-GB")).glucose)
        assertEquals(UnitSystem.IMPERIAL, resolve(Locale.forLanguageTag("en-GB")).system)
    }

    @Test
    fun `explicit choices override the region`() {
        val units = resolve(Locale.forLanguageTag("nl-NL"), UnitSystemChoice.IMPERIAL, GlucoseUnitChoice.MGDL)
        assertEquals(UnitSystem.IMPERIAL, units.system)
        assertEquals(GlucoseUnit.MG_PER_DL, units.glucose)

        val metric = resolve(Locale.forLanguageTag("en-US"), UnitSystemChoice.METRIC, GlucoseUnitChoice.MMOL)
        assertEquals(UnitSystem.METRIC, metric.system)
        assertEquals(GlucoseUnit.MMOL_PER_L, metric.glucose)
    }

    @Test
    fun `symbols match the resolved units`() {
        val imperial = resolve(Locale.forLanguageTag("en-US"))
        assertEquals("lb", imperial.weightSymbol)
        assertEquals("mi", imperial.distanceSymbol)
        assertEquals("mg/dL", imperial.glucoseSymbol)
        val metric = resolve(Locale.forLanguageTag("nl-NL"))
        assertEquals("kg", metric.weightSymbol)
        assertEquals("km", metric.distanceSymbol)
        assertEquals("mmol/L", metric.glucoseSymbol)
    }
}
