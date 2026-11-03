package nl.healthjournal.domain.model.metrics

/** A whole percent of oxygen saturation, as read from a pulse oximeter. */
@JvmInline
value class OxygenSaturationPercent(val value: Int) {
    init {
        require(value in MIN..MAX) { "Oxygen saturation must be between $MIN and $MAX percent, got: $value" }
    }

    companion object {
        const val MIN = 50
        const val MAX = 100
    }
}
