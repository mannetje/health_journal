package nl.healthjournal.domain.model.mindfulness

/** One step of the Start here list. It only carries the settings to open the exercise with; nothing tracks it. */
sealed interface StartHereStep {
    data class Breathing(val patternId: BreathingPatternId, val duration: BreathingDuration) : StartHereStep

    data class Timer(val durationMinutes: Int) : StartHereStep
}

/** A fixed, short ladder that begins with patterns that hold no breath. */
object StartHere {
    val steps: List<StartHereStep> = listOf(
        StartHereStep.Breathing(BreathingPatternId.BELLY, BreathingDuration.ONE_MINUTE),
        StartHereStep.Breathing(BreathingPatternId.BELLY, BreathingDuration.THREE_MINUTES),
        StartHereStep.Timer(durationMinutes = 5),
    )
}
