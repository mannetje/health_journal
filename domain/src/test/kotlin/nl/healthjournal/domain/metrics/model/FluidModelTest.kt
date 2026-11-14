package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class FluidModelTest {
    private val profile = ProfileId.generate()

    private fun entry(drink: FluidDrink, ml: Int, at: String = "2026-11-14T08:00:00Z") = FluidIntakeEntry(
        MeasurementId.generate(), profile, Instant.parse(at), drink, FluidMl.of(ml).getOrNull()!!
    )

    @Test
    fun `unknown or missing drink code reads as other`() {
        assertEquals(FluidDrink.OTHER, FluidDrink.fromCode(null))
        assertEquals(FluidDrink.OTHER, FluidDrink.fromCode("beer"))
        assertEquals(FluidDrink.COFFEE, FluidDrink.fromCode(" Coffee "))
    }

    @Test
    fun `portions start at the documented values and can be changed`() {
        val portions = DrinkPortions()
        assertEquals(250, portions.of(FluidDrink.WATER))
        assertEquals(125, portions.of(FluidDrink.COFFEE))
        assertEquals(300, portions.with(FluidDrink.WATER, 300).of(FluidDrink.WATER))
    }

    @Test
    fun `drink totals are plain ml largest first then fixed order`() {
        val totals = FluidDrinkTotals.of(
            listOf(entry(FluidDrink.TEA, 125), entry(FluidDrink.WATER, 250), entry(FluidDrink.COFFEE, 125), entry(FluidDrink.WATER, 250))
        )
        assertEquals(listOf(FluidDrink.WATER, FluidDrink.COFFEE, FluidDrink.TEA), totals.map { it.drink })
        assertEquals(500, totals.first().ml)
    }

    @Test
    fun `day totals group by local day`() {
        val days = FluidDayTotals.of(
            listOf(entry(FluidDrink.WATER, 250, "2026-11-14T23:30:00Z"), entry(FluidDrink.TEA, 125, "2026-11-15T00:10:00Z")),
            ZoneOffset.UTC
        )
        assertEquals(listOf(250, 125), days.map { it.ml })
    }

    @Test
    fun `limits reject a minimum above the maximum and accept one or none`() {
        assertFalse(FluidLimits.of(2000, 1500).isSuccess)
        assertTrue(FluidLimits.of(1400, null).isSuccess)
        assertTrue(FluidLimits.of(null, null).isSuccess)
        assertFalse(FluidLimits.of(50, null).isSuccess)
    }

    @Test
    fun `position is none without limits`() {
        assertEquals(FluidPosition.NONE, fluidPosition(900, FluidLimits.NONE))
        assertEquals(FluidPosition.BELOW_MIN, fluidPosition(900, FluidLimits(minMl = 1400)))
        assertEquals(FluidPosition.ABOVE_MAX, fluidPosition(2100, FluidLimits(maxMl = 2000)))
        assertEquals(FluidPosition.WITHIN, fluidPosition(1500, FluidLimits(1400, 2000)))
    }

    @Test
    fun `statistics are null without logged days`() {
        assertNull(FluidStatistics.of(emptyList()))
    }
}
