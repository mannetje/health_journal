package nl.healthjournal.domain.model.metrics

@JvmInline
value class HeightCm(val value: Int) {
    init {
        require(value in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            "Height must be between $MIN_HEIGHT_CM and $MAX_HEIGHT_CM cm, got: $value"
        }
    }

    companion object {
        const val MIN_HEIGHT_CM = 50
        const val MAX_HEIGHT_CM = 300
    }
}
