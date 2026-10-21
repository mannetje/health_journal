package nl.healthjournal.app.settings

import nl.healthjournal.app.ui.mindfulness.BreathingVisual
import org.junit.Assert.assertEquals
import org.junit.Test

class MindfulnessPreferencesTest {

    @Test
    fun `a stored name is read back`() {
        assertEquals(BreathingVisual.SQUARE, enumOrDefault("SQUARE", BreathingVisual.LOTUS))
    }

    @Test
    fun `missing or unknown names fall back to the default`() {
        assertEquals(BreathingVisual.LOTUS, enumOrDefault(null, BreathingVisual.LOTUS))
        assertEquals(BreathingVisual.LOTUS, enumOrDefault("TRIANGLE", BreathingVisual.LOTUS))
    }
}
