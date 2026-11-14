package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.command.*

/** A whole number of millilitres drunk in one entry. */
@JvmInline
value class FluidMl(val value: Int) {
    init {
        require(value in MIN..MAX) { "Fluid intake must be between $MIN and $MAX ml, got: $value" }
    }

    companion object {
        fun of(value: Int): DomainResult<FluidMl> = validated(
            rangeError(ValidationField.FLUID, value, MIN, MAX)
        ) { FluidMl(value) }

        const val MIN = 10
        const val MAX = 5000
    }
}
