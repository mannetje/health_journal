package nl.healthjournal.domain.model.mindfulness

/** Where a breathing exercise is at a given moment. */
sealed interface BreathingState {
    /** The quiet moment before the first phase. */
    data class Ready(val secondsLeft: Int) : BreathingState

    /**
     * Inside a phase. [phaseProgress] runs from 0 to 1 within the phase. [fill] is how large or open the visual
     * is, from 0 (small, closed) to 1 (large, open); it moves in a straight line, the screen adds the easing.
     */
    data class Running(
        val phaseIndex: Int,
        val phase: BreathingPhase,
        val cycleIndex: Int,
        val secondsLeft: Int,
        val phaseProgress: Float,
        val fill: Float,
    ) : BreathingState

    /** The chosen duration has passed. */
    data object Done : BreathingState
}

/**
 * Turns elapsed time into a [BreathingState]. It has no clock and no Android types: the caller passes the
 * milliseconds since the start button was pressed, which keeps it testable and free of any session record.
 */
class BreathingTimer(
    val pattern: BreathingPattern,
    val duration: BreathingDuration,
    val readyMillis: Long = READY_MILLIS,
) {
    private val fillRanges: List<Pair<Float, Float>> = computeFillRanges(pattern)

    /** Ready moment plus the chosen duration. */
    val totalMillis: Long get() = readyMillis + duration.millis

    fun stateAt(elapsedMillis: Long): BreathingState {
        val elapsed = elapsedMillis.coerceAtLeast(0)
        if (elapsed < readyMillis) return BreathingState.Ready(ceilSeconds(readyMillis - elapsed))

        val running = elapsed - readyMillis
        if (running >= duration.millis) return BreathingState.Done

        val cycleIndex = (running / pattern.cycleMillis).toInt()
        var inPhase = running % pattern.cycleMillis
        var index = 0
        while (inPhase >= pattern.phases[index].millis) {
            inPhase -= pattern.phases[index].millis
            index++
        }

        val phase = pattern.phases[index]
        val progress = inPhase.toFloat() / phase.millis
        val (fillFrom, fillTo) = fillRanges[index]
        return BreathingState.Running(
            phaseIndex = index,
            phase = phase,
            cycleIndex = cycleIndex,
            secondsLeft = ceilSeconds(phase.millis - inPhase),
            phaseProgress = progress,
            fill = fillFrom + (fillTo - fillFrom) * progress,
        )
    }

    companion object {
        const val READY_MILLIS = 3000L

        /** How far the first inhale of the physiological sigh fills the visual before the second one tops it up. */
        const val SIGH_FIRST_FILL = 0.75f

        private fun ceilSeconds(millis: Long): Int = ((millis + 999) / 1000).toInt()

        private fun computeFillRanges(pattern: BreathingPattern): List<Pair<Float, Float>> {
            var level = 0f
            return pattern.phases.mapIndexed { index, phase ->
                val start = level
                val end = when (phase.kind) {
                    BreathingPhaseKind.INHALE ->
                        if (pattern.phases.getOrNull(index + 1)?.kind == BreathingPhaseKind.INHALE_AGAIN) SIGH_FIRST_FILL else 1f
                    BreathingPhaseKind.INHALE_AGAIN -> 1f
                    BreathingPhaseKind.EXHALE -> 0f
                    BreathingPhaseKind.HOLD, BreathingPhaseKind.HOLD_EMPTY -> level
                }
                level = end
                Pair(start, end)
            }
        }
    }
}
