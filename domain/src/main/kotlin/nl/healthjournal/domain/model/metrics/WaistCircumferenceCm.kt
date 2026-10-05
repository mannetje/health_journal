package nl.healthjournal.domain.model.metrics

@JvmInline
value class WaistCircumferenceCm(val value: Double) {
    init {
        require(value in MIN_CM..MAX_CM) {
            "Waist circumference must be between $MIN_CM and $MAX_CM cm, got: $value"
        }
    }

    constructor(intValue: Int) : this(intValue.toDouble())

    companion object {
        const val MIN_CM = 40.0
        const val MAX_CM = 200.0
    }
}
