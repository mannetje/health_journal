package nl.healthjournal.domain.model.mindfulness

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneSynthTest {
    private fun peak(samples: ShortArray) = samples.maxOf { abs(it.toInt()) }

    @Test
    fun `every bell and tone has sound and stays below full scale`() {
        val sounds = BellKind.entries.map { ToneSynth.bell(it) } + BreathingPhaseKind.entries.map { ToneSynth.phaseTone(it) }
        sounds.forEach { samples ->
            assertTrue(samples.isNotEmpty())
            assertTrue("audible", peak(samples) > 1000)
            assertTrue("soft", peak(samples) < Short.MAX_VALUE * 0.7)
        }
    }

    @Test
    fun `sounds begin and end at silence so they do not click`() {
        val sounds = BellKind.entries.map { ToneSynth.bell(it) } + BreathingPhaseKind.entries.map { ToneSynth.phaseTone(it) }
        sounds.forEach { samples ->
            assertTrue(abs(samples.first().toInt()) < 200)
            assertTrue(abs(samples.last().toInt()) < 200)
        }
    }

    @Test
    fun `the end bell rings for the announced time`() {
        val samples = ToneSynth.bell(BellKind.END)
        assertEquals(ToneSynth.END_BELL_MILLIS, samples.size * 1000L / ToneSynth.SAMPLE_RATE)
    }

    @Test
    fun `a bell dies away`() {
        val samples = ToneSynth.bell(BellKind.START)
        val early = peak(samples.copyOfRange(0, samples.size / 4))
        val late = peak(samples.copyOfRange(samples.size * 3 / 4, samples.size))
        assertTrue(late < early / 3)
    }

    @Test
    fun `the three bells and five breathing tones sound different`() {
        val bells = BellKind.entries.map { ToneSynth.bell(it).toList() }
        assertEquals(bells.size, bells.toSet().size)
        val tones = BreathingPhaseKind.entries.map { ToneSynth.phaseTone(it).toList() }
        assertEquals(tones.size, tones.toSet().size)
    }

    @Test
    fun `the same sound is made every time`() {
        assertTrue(ToneSynth.bell(BellKind.INTERVAL).contentEquals(ToneSynth.bell(BellKind.INTERVAL)))
    }

    @Test
    fun `cues are off until chosen`() {
        assertFalse(CueChoices().any)
        assertTrue(CueChoices(haptics = true).any)
    }
}
