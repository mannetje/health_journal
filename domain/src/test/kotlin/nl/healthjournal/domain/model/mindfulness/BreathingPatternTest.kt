package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingPatternTest {
    private fun BreathingPattern.seconds() = phases.map { it.seconds }

    private fun BreathingPattern.kinds() = phases.map { it.kind }

    @Test
    fun `box breathing is inhale 4, hold 4, exhale 4, hold 4`() {
        val p = BreathingPatterns.box
        assertEquals(listOf(4, 4, 4, 4), p.seconds())
        assertEquals(
            listOf(BreathingPhaseKind.INHALE, BreathingPhaseKind.HOLD, BreathingPhaseKind.EXHALE, BreathingPhaseKind.HOLD_EMPTY),
            p.kinds(),
        )
        assertEquals(16_000L, p.cycleMillis)
    }

    @Test
    fun `4-7-8 is inhale 4, hold 7, exhale 8`() {
        val p = BreathingPatterns.fourSevenEight
        assertEquals(listOf(4, 7, 8), p.seconds())
        assertEquals(listOf(BreathingPhaseKind.INHALE, BreathingPhaseKind.HOLD, BreathingPhaseKind.EXHALE), p.kinds())
        assertEquals(19_000L, p.cycleMillis)
    }

    @Test
    fun `physiological sigh is inhale 2, a second inhale 1, exhale 6`() {
        val p = BreathingPatterns.physiologicalSigh
        assertEquals(listOf(2, 1, 6), p.seconds())
        assertEquals(
            listOf(BreathingPhaseKind.INHALE, BreathingPhaseKind.INHALE_AGAIN, BreathingPhaseKind.EXHALE),
            p.kinds(),
        )
    }

    @Test
    fun `belly breathing is inhale 4 and exhale 6`() {
        val p = BreathingPatterns.belly
        assertEquals(listOf(4, 6), p.seconds())
        assertEquals(10_000L, p.cycleMillis)
    }

    @Test
    fun `only the patterns with a hold ask for the breath hold reminder`() {
        assertTrue(BreathingPatterns.box.holdsBreath)
        assertTrue(BreathingPatterns.fourSevenEight.holdsBreath)
        assertFalse(BreathingPatterns.physiologicalSigh.holdsBreath)
        assertFalse(BreathingPatterns.belly.holdsBreath)
    }

    @Test
    fun `built-in patterns are listed in the order of the Breathe section`() {
        assertEquals(
            listOf(
                BreathingPatternId.BOX,
                BreathingPatternId.FOUR_SEVEN_EIGHT,
                BreathingPatternId.PHYSIOLOGICAL_SIGH,
                BreathingPatternId.BELLY,
            ),
            BreathingPatterns.builtIn.map { it.id },
        )
        assertEquals(BreathingPatterns.box, BreathingPatterns.byId(BreathingPatternId.BOX))
        assertNull(BreathingPatterns.byId(BreathingPatternId.CUSTOM))
    }

    @Test
    fun `durations are 1, 3 and 5 minutes`() {
        assertEquals(listOf(60, 180, 300), BreathingDuration.entries.map { it.seconds })
        assertEquals(180_000L, BreathingDuration.THREE_MINUTES.millis)
    }

    @Test
    fun `a custom pattern with both holds has four phases`() {
        val p = BreathingPatterns.custom(CustomBreathingCounts(3, 2, 5, 1))
        assertEquals(BreathingPatternId.CUSTOM, p.id)
        assertEquals(listOf(3, 2, 5, 1), p.seconds())
        assertEquals(
            listOf(BreathingPhaseKind.INHALE, BreathingPhaseKind.HOLD, BreathingPhaseKind.EXHALE, BreathingPhaseKind.HOLD_EMPTY),
            p.kinds(),
        )
        assertTrue(p.holdsBreath)
    }

    @Test
    fun `a custom pattern with holds of 0 leaves them out`() {
        val p = BreathingPatterns.custom(CustomBreathingCounts(4, 0, 6, 0))
        assertEquals(listOf(BreathingPhaseKind.INHALE, BreathingPhaseKind.EXHALE), p.kinds())
        assertFalse(p.holdsBreath)
    }

    @Test
    fun `the default custom counts are valid`() {
        assertTrue(CustomBreathingCounts.validate(4, 0, 6, 0).isValid)
        assertEquals(CustomBreathingCounts.DEFAULT, CustomBreathingCounts.of(4, 0, 6, 0))
    }

    @Test
    fun `empty fields are reported per field`() {
        val check = CustomBreathingCounts.validate(null, 0, null, 0)
        assertEquals(CountError.EMPTY, check.inhale)
        assertNull(check.holdAfterInhale)
        assertEquals(CountError.EMPTY, check.exhale)
        assertNull(check.holdAfterExhale)
        assertFalse(check.isValid)
        assertNull(CustomBreathingCounts.of(null, 0, null, 0))
    }

    @Test
    fun `inhale and exhale of 0 are rejected, a hold of 0 is accepted`() {
        val check = CustomBreathingCounts.validate(0, 0, 0, 0)
        assertEquals(CountError.OUT_OF_RANGE, check.inhale)
        assertNull(check.holdAfterInhale)
        assertEquals(CountError.OUT_OF_RANGE, check.exhale)
        assertNull(check.holdAfterExhale)
    }

    @Test
    fun `the limits are 1 to 20 for breaths and 0 to 20 for holds`() {
        val max = CustomBreathingCounts.MAX_SECONDS
        assertTrue(CustomBreathingCounts.validate(1, 0, 1, 0).isValid)
        assertTrue(CustomBreathingCounts.validate(max, max, max, max).isValid)
        val over = CustomBreathingCounts.validate(max + 1, max + 1, max + 1, max + 1)
        assertEquals(CountError.OUT_OF_RANGE, over.inhale)
        assertEquals(CountError.OUT_OF_RANGE, over.holdAfterInhale)
        assertEquals(CountError.OUT_OF_RANGE, over.exhale)
        assertEquals(CountError.OUT_OF_RANGE, over.holdAfterExhale)
        assertEquals(CountError.OUT_OF_RANGE, CustomBreathingCounts.validate(4, -1, 6, 0).holdAfterInhale)
    }

    @Test
    fun `of returns the counts when valid`() {
        assertNotNull(CustomBreathingCounts.of(4, 4, 4, 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `counts out of range cannot be constructed`() {
        CustomBreathingCounts(0, 0, 6, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a phase of 0 seconds cannot be constructed`() {
        BreathingPhase(BreathingPhaseKind.INHALE, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a pattern without phases cannot be constructed`() {
        BreathingPattern(BreathingPatternId.CUSTOM, emptyList())
    }
}
