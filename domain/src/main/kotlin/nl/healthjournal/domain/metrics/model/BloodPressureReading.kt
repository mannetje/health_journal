package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.command.*

data class BloodPressureReading(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null
) {
    init {
        require(systolic in MIN_SYSTOLIC..MAX_SYSTOLIC) {
            "Systolic pressure must be between $MIN_SYSTOLIC and $MAX_SYSTOLIC mmHg, got: $systolic"
        }
        require(diastolic in MIN_DIASTOLIC..MAX_DIASTOLIC) {
            "Diastolic pressure must be between $MIN_DIASTOLIC and $MAX_DIASTOLIC mmHg, got: $diastolic"
        }
        require(systolic > diastolic) {
            "Systolic pressure ($systolic mmHg) must be strictly greater than diastolic pressure ($diastolic mmHg)"
        }
        pulse?.let {
            require(it in MIN_PULSE..MAX_PULSE) {
                "Pulse must be between $MIN_PULSE and $MAX_PULSE bpm, got: $it"
            }
        }
    }

    companion object {
        /** Checked factory: the first broken rule comes back as a typed error instead of an exception. */
        fun of(systolic: Int, diastolic: Int, pulse: Int? = null): DomainResult<BloodPressureReading> = validated(
            rangeError(ValidationField.SYSTOLIC, systolic, MIN_SYSTOLIC, MAX_SYSTOLIC),
            rangeError(ValidationField.DIASTOLIC, diastolic, MIN_DIASTOLIC, MAX_DIASTOLIC),
            if (systolic > diastolic) null
            else DomainError.Invalid(ValidationField.SYSTOLIC, ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC),
            pulse?.let { pulseError(it) }
        ) { BloodPressureReading(systolic, diastolic, pulse) }

        /** Null when [pulse] is a valid heart rate in bpm. */
        fun pulseError(pulse: Int): DomainError.Invalid? = rangeError(ValidationField.PULSE, pulse, MIN_PULSE, MAX_PULSE)

        const val MIN_SYSTOLIC = 40
        const val MAX_SYSTOLIC = 300
        const val MIN_DIASTOLIC = 20
        const val MAX_DIASTOLIC = 200
        const val MIN_PULSE = 30
        const val MAX_PULSE = 250
    }
}
