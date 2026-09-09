package nl.healthjournal.domain.model.metrics

import java.math.BigDecimal

data class WeightKg(val value: BigDecimal) {
    init {
        require(value >= MIN_WEIGHT_KG && value <= MAX_WEIGHT_KG) {
            "Weight must be between $MIN_WEIGHT_KG and $MAX_WEIGHT_KG kg, got: $value"
        }
    }

    constructor(doubleVal: Double) : this(BigDecimal.valueOf(doubleVal))

    companion object {
        val MIN_WEIGHT_KG: BigDecimal = BigDecimal("1.0")
        val MAX_WEIGHT_KG: BigDecimal = BigDecimal("700.0")
    }
}
