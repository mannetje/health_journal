package nl.healthjournal.domain.model.mindfulness

/**
 * What the lungs do in a phase. The visual grows or opens on an inhale, stays still on a hold and
 * shrinks or closes on an exhale. [INHALE_AGAIN] is the second, short inhale of the physiological sigh.
 */
enum class BreathingPhaseKind { INHALE, INHALE_AGAIN, HOLD, EXHALE, HOLD_EMPTY }

/** One phase of a breathing cycle, in whole seconds. */
data class BreathingPhase(val kind: BreathingPhaseKind, val seconds: Int) {
    init {
        require(seconds >= 1) { "A phase lasts at least 1 second, got: $seconds" }
    }

    val millis: Long get() = seconds * 1000L
}

/** Identifies a pattern. The app maps it to its name and description; [CUSTOM] carries the user's own counts. */
enum class BreathingPatternId { BOX, FOUR_SEVEN_EIGHT, PHYSIOLOGICAL_SIGH, BELLY, CUSTOM }

/** A repeating cycle of phases. Plain data: the timer and the screens derive everything from it. */
data class BreathingPattern(val id: BreathingPatternId, val phases: List<BreathingPhase>) {
    init {
        require(phases.isNotEmpty()) { "A pattern needs at least one phase" }
    }

    val cycleMillis: Long = phases.sumOf { it.millis }

    /** True when the pattern holds the breath, after an inhale or after an exhale. The app shows a reminder for these. */
    val holdsBreath: Boolean =
        phases.any { it.kind == BreathingPhaseKind.HOLD || it.kind == BreathingPhaseKind.HOLD_EMPTY }
}

/** The durations the user can choose for a breathing exercise. */
enum class BreathingDuration(val seconds: Int) {
    ONE_MINUTE(60),
    THREE_MINUTES(180),
    FIVE_MINUTES(300);

    val millis: Long get() = seconds * 1000L
}

/** The built-in patterns plus the builder for the user's own. */
object BreathingPatterns {
    val box = BreathingPattern(
        BreathingPatternId.BOX,
        listOf(
            phase(BreathingPhaseKind.INHALE, 4),
            phase(BreathingPhaseKind.HOLD, 4),
            phase(BreathingPhaseKind.EXHALE, 4),
            phase(BreathingPhaseKind.HOLD_EMPTY, 4),
        ),
    )

    val fourSevenEight = BreathingPattern(
        BreathingPatternId.FOUR_SEVEN_EIGHT,
        listOf(
            phase(BreathingPhaseKind.INHALE, 4),
            phase(BreathingPhaseKind.HOLD, 7),
            phase(BreathingPhaseKind.EXHALE, 8),
        ),
    )

    val physiologicalSigh = BreathingPattern(
        BreathingPatternId.PHYSIOLOGICAL_SIGH,
        listOf(
            phase(BreathingPhaseKind.INHALE, 2),
            phase(BreathingPhaseKind.INHALE_AGAIN, 1),
            phase(BreathingPhaseKind.EXHALE, 6),
        ),
    )

    val belly = BreathingPattern(
        BreathingPatternId.BELLY,
        listOf(
            phase(BreathingPhaseKind.INHALE, 4),
            phase(BreathingPhaseKind.EXHALE, 6),
        ),
    )

    /** In the order the Breathe section lists them. The custom pattern is built from [CustomBreathingCounts]. */
    val builtIn: List<BreathingPattern> = listOf(box, fourSevenEight, physiologicalSigh, belly)

    /** Inhale, optional hold, exhale, optional hold. A count of 0 leaves that hold out. */
    fun custom(counts: CustomBreathingCounts): BreathingPattern = BreathingPattern(
        BreathingPatternId.CUSTOM,
        buildList {
            add(phase(BreathingPhaseKind.INHALE, counts.inhale))
            if (counts.holdAfterInhale > 0) add(phase(BreathingPhaseKind.HOLD, counts.holdAfterInhale))
            add(phase(BreathingPhaseKind.EXHALE, counts.exhale))
            if (counts.holdAfterExhale > 0) add(phase(BreathingPhaseKind.HOLD_EMPTY, counts.holdAfterExhale))
        },
    )

    fun byId(id: BreathingPatternId): BreathingPattern? = builtIn.firstOrNull { it.id == id }

    private fun phase(kind: BreathingPhaseKind, seconds: Int) = BreathingPhase(kind, seconds)
}

/** Why a count the user typed is not accepted. */
enum class CountError { EMPTY, OUT_OF_RANGE }

/** The result of checking the four fields of the custom pattern: one optional error per field. */
data class CustomCountsCheck(
    val inhale: CountError?,
    val holdAfterInhale: CountError?,
    val exhale: CountError?,
    val holdAfterExhale: CountError?,
) {
    val isValid: Boolean get() = inhale == null && holdAfterInhale == null && exhale == null && holdAfterExhale == null
}

/**
 * The counts of a custom pattern, in whole seconds. Inhale and exhale are 1 to [MAX_SECONDS];
 * a hold is 0 (left out) to [MAX_SECONDS]. Every field has to be filled in.
 */
data class CustomBreathingCounts(
    val inhale: Int,
    val holdAfterInhale: Int,
    val exhale: Int,
    val holdAfterExhale: Int,
) {
    init {
        require(validate(inhale, holdAfterInhale, exhale, holdAfterExhale).isValid) {
            "Counts out of range: $inhale, $holdAfterInhale, $exhale, $holdAfterExhale"
        }
    }

    companion object {
        const val MAX_SECONDS = 20

        val DEFAULT = CustomBreathingCounts(inhale = 4, holdAfterInhale = 0, exhale = 6, holdAfterExhale = 0)

        /** Checks what the user typed. A null count is an empty field. */
        fun validate(
            inhale: Int?,
            holdAfterInhale: Int?,
            exhale: Int?,
            holdAfterExhale: Int?,
        ): CustomCountsCheck = CustomCountsCheck(
            inhale = error(inhale, minimum = 1),
            holdAfterInhale = error(holdAfterInhale, minimum = 0),
            exhale = error(exhale, minimum = 1),
            holdAfterExhale = error(holdAfterExhale, minimum = 0),
        )

        /** The counts when all four fields are valid, otherwise null. */
        fun of(inhale: Int?, holdAfterInhale: Int?, exhale: Int?, holdAfterExhale: Int?): CustomBreathingCounts? =
            if (validate(inhale, holdAfterInhale, exhale, holdAfterExhale).isValid) {
                CustomBreathingCounts(inhale!!, holdAfterInhale!!, exhale!!, holdAfterExhale!!)
            } else {
                null
            }

        private fun error(value: Int?, minimum: Int): CountError? = when {
            value == null -> CountError.EMPTY
            value < minimum || value > MAX_SECONDS -> CountError.OUT_OF_RANGE
            else -> null
        }
    }
}
