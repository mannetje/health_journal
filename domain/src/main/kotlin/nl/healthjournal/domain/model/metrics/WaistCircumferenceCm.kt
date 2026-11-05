package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.command.*

@JvmInline
value class WaistCircumferenceCm(val value: Double) {
    init {
        require(value in MIN_CM..MAX_CM) {
            "Waist circumference must be between $MIN_CM and $MAX_CM cm, got: $value"
        }
    }

    constructor(intValue: Int) : this(intValue.toDouble())

    companion object {
        fun of(value: Double): DomainResult<WaistCircumferenceCm> = validated(
            rangeError(ValidationField.WAIST, value, MIN_CM, MAX_CM)
        ) { WaistCircumferenceCm(value) }

        const val MIN_CM = 40.0
        const val MAX_CM = 200.0
    }
}
