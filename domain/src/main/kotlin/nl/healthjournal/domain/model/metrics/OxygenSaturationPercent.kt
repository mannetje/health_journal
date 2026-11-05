package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.command.*

/** A whole percent of oxygen saturation, as read from a pulse oximeter. */
@JvmInline
value class OxygenSaturationPercent(val value: Int) {
    init {
        require(value in MIN..MAX) { "Oxygen saturation must be between $MIN and $MAX percent, got: $value" }
    }

    companion object {
        fun of(value: Int): DomainResult<OxygenSaturationPercent> = validated(
            rangeError(ValidationField.OXYGEN_SATURATION, value, MIN, MAX)
        ) { OxygenSaturationPercent(value) }

        const val MIN = 50
        const val MAX = 100
    }
}
