package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.command.*

/**
 * The person's own daily minimum and maximum in ml. Either, both or neither may be set; the app never supplies a
 * number (ADR 0030). There is deliberately no default value anywhere in this type.
 */
data class FluidLimits(val minMl: Int? = null, val maxMl: Int? = null) {
    init {
        minMl?.let { require(it in MIN..MAX) { "Minimum must be between $MIN and $MAX ml, got: $it" } }
        maxMl?.let { require(it in MIN..MAX) { "Maximum must be between $MIN and $MAX ml, got: $it" } }
        if (minMl != null && maxMl != null) require(minMl <= maxMl) { "Minimum must not be above the maximum" }
    }

    val isSet: Boolean get() = minMl != null || maxMl != null

    companion object {
        val NONE = FluidLimits()

        fun of(minMl: Int?, maxMl: Int?): DomainResult<FluidLimits> = validated(
            minMl?.let { rangeError(ValidationField.FLUID_MIN, it, MIN, MAX) },
            maxMl?.let { rangeError(ValidationField.FLUID_MAX, it, MIN, MAX) },
            if (minMl != null && maxMl != null && minMl > maxMl) {
                DomainError.Invalid(ValidationField.FLUID_MIN, ValidationRule.MIN_ABOVE_MAX, listOf(maxMl))
            } else null
        ) { FluidLimits(minMl, maxMl) }

        const val MIN = 100
        const val MAX = 10000
    }
}

/** Where a day's total stands against the person's own numbers. Not an assessment: no thresholds of the app. */
enum class FluidPosition { NONE, BELOW_MIN, WITHIN, ABOVE_MAX }

/** Only the numbers that are set take part. A total equal to a number is within it. */
fun fluidPosition(totalMl: Int, limits: FluidLimits): FluidPosition = when {
    !limits.isSet -> FluidPosition.NONE
    limits.minMl != null && totalMl < limits.minMl -> FluidPosition.BELOW_MIN
    limits.maxMl != null && totalMl > limits.maxMl -> FluidPosition.ABOVE_MAX
    else -> FluidPosition.WITHIN
}
