package nl.healthjournal.app.settings

import nl.healthjournal.domain.metrics.model.DrinkPortions
import nl.healthjournal.domain.metrics.model.FluidDrink
import org.junit.Assert.assertEquals
import org.junit.Test

class DrinkPortionsPreferenceTest {

    @Test
    fun `nothing stored reads as the starting portions`() {
        listOf(null, "", "garbage").forEach {
            val portions = DrinkPortionsPreference.parse(it)
            FluidDrink.entries.forEach { drink -> assertEquals(DrinkPortions.DEFAULTS.getValue(drink), portions.of(drink)) }
        }
    }

    @Test
    fun `saved portions survive encoding and parsing`() {
        val saved = DrinkPortions().with(FluidDrink.WATER, 330).with(FluidDrink.TEA, 180)
        val back = DrinkPortionsPreference.parse(DrinkPortionsPreference.encode(saved))
        assertEquals(330, back.of(FluidDrink.WATER))
        assertEquals(180, back.of(FluidDrink.TEA))
        assertEquals(125, back.of(FluidDrink.COFFEE))
    }

    @Test
    fun `an unknown drink, a non-number or an out-of-range piece is ignored and the rest is kept`() {
        val back = DrinkPortionsPreference.parse("water=300;kombucha=200;tea=abc;coffee=9;milk=501;juice=175")
        assertEquals(300, back.of(FluidDrink.WATER))
        assertEquals(125, back.of(FluidDrink.TEA))
        assertEquals(125, back.of(FluidDrink.COFFEE))
        assertEquals(200, back.of(FluidDrink.MILK))
        assertEquals(175, back.of(FluidDrink.JUICE))
    }
}
