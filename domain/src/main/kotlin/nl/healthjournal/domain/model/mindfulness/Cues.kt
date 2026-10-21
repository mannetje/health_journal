package nl.healthjournal.domain.model.mindfulness

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Which cues the user wants during an exercise. Each is optional and off until chosen; [tones] covers the
 * bells of the timer and the soft tones of the breathing exercise.
 */
data class CueChoices(
    val tones: Boolean = false,
    val haptics: Boolean = false,
    val speech: Boolean = false,
    /** The name of the speech voice the user picked; null means the best offline voice of the language. */
    val voiceName: String? = null,
) {
    val any: Boolean get() = tones || haptics || speech
}

/**
 * Makes the sounds in code, so the app needs no audio files: mono 16-bit samples at [SAMPLE_RATE]. The bells are a
 * few soft partials with a short attack and a long decay; the breathing tones are plain sines with a gentle
 * envelope. Everything stays well below full scale because the user's own volume decides how loud it is.
 */
object ToneSynth {
    const val SAMPLE_RATE = 22_050

    /** How long the end bell rings; the timer waits this long before it releases the audio. */
    const val END_BELL_MILLIS = 3_500L

    private const val PEAK = 0.6
    private const val FADE_OUT_SECONDS = 0.02

    fun bell(kind: BellKind): ShortArray = when (kind) {
        BellKind.START -> bellSound(frequency = 528.0, seconds = 3.0, decaySeconds = 1.1)
        BellKind.INTERVAL -> bellSound(frequency = 660.0, seconds = 2.0, decaySeconds = 0.7)
        BellKind.END -> bellSound(frequency = 396.0, seconds = END_BELL_MILLIS / 1000.0, decaySeconds = 1.4)
    }

    fun phaseTone(kind: BreathingPhaseKind): ShortArray = when (kind) {
        BreathingPhaseKind.INHALE -> softTone(392.0)
        BreathingPhaseKind.INHALE_AGAIN -> softTone(440.0)
        BreathingPhaseKind.HOLD -> softTone(330.0)
        BreathingPhaseKind.EXHALE -> softTone(294.0)
        BreathingPhaseKind.HOLD_EMPTY -> softTone(262.0)
    }

    private fun bellSound(frequency: Double, seconds: Double, decaySeconds: Double): ShortArray {
        // Partial ratio and level; the odd ratios give the bell its soft, slightly metallic colour.
        val partials = listOf(1.0 to 1.0, 2.0 to 0.5, 2.76 to 0.25, 5.4 to 0.1)
        return render(seconds) { t ->
            val envelope = (1 - exp(-t / ATTACK_SECONDS)) * exp(-t / decaySeconds)
            envelope * partials.sumOf { (ratio, level) -> level * sin(2 * PI * frequency * ratio * t) }
        }
    }

    private fun softTone(frequency: Double): ShortArray {
        val seconds = 0.5
        return render(seconds) { t ->
            val envelope = sin(PI * t / seconds).let { it * it }
            envelope * sin(2 * PI * frequency * t) * 0.5
        }
    }

    private fun render(seconds: Double, wave: (Double) -> Double): ShortArray {
        val count = (seconds * SAMPLE_RATE).toInt()
        val raw = DoubleArray(count) { wave(it.toDouble() / SAMPLE_RATE) }
        val top = raw.maxOf { kotlin.math.abs(it) }.takeIf { it > 0.0 } ?: 1.0
        val fadeStart = count - (FADE_OUT_SECONDS * SAMPLE_RATE).toInt()
        return ShortArray(count) { i ->
            val fade = if (i >= fadeStart) (count - 1 - i).toDouble() / (count - fadeStart) else 1.0
            (raw[i] / top * PEAK * fade * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private const val ATTACK_SECONDS = 0.005
}
