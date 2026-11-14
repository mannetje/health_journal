package nl.healthjournal.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FluidGaugePreferenceTest {

    @Test
    fun `every style reads back from its own name`() {
        FluidGaugeStyle.entries.forEach { assertEquals(it, FluidGaugePreference.parse(it.name)) }
    }

    @Test
    fun `a missing, empty, unknown or wrongly cased value reads as the glass`() {
        listOf(null, "", "BOTTLE", "woman", " MAN").forEach {
            assertEquals(FluidGaugeStyle.GLASS, FluidGaugePreference.parse(it))
        }
    }
}
