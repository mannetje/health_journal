package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartHereTest {
    @Test
    fun `the ladder is breathing for 1 minute, breathing for 3 minutes and a short timer`() {
        val steps = StartHere.steps
        assertEquals(3, steps.size)
        assertEquals(BreathingDuration.ONE_MINUTE, (steps[0] as StartHereStep.Breathing).duration)
        assertEquals(BreathingDuration.THREE_MINUTES, (steps[1] as StartHereStep.Breathing).duration)
        assertTrue(steps[2] is StartHereStep.Timer)
    }

    @Test
    fun `the breathing steps use patterns that exist and hold no breath`() {
        for (step in StartHere.steps.filterIsInstance<StartHereStep.Breathing>()) {
            val pattern = BreathingPatterns.byId(step.patternId)
            assertNotNull(pattern)
            assertFalse(pattern!!.holdsBreath)
        }
    }

    @Test
    fun `the timer step is a valid meditation timer`() {
        val timer = StartHere.steps.filterIsInstance<StartHereStep.Timer>().single()
        assertEquals(5, MeditationTimer(timer.durationMinutes).durationMinutes)
    }
}
