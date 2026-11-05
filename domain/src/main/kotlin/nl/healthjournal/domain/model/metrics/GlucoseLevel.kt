package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.command.*
import java.math.BigDecimal
import java.math.RoundingMode

data class GlucoseLevel(val valueInMmolL: BigDecimal) {
    init {
        require(valueInMmolL >= MIN_GLUCOSE && valueInMmolL <= MAX_GLUCOSE) {
            "Glucose level $valueInMmolL mmol/L is outside accepted physiological range ($MIN_GLUCOSE - $MAX_GLUCOSE mmol/L)"
        }
    }

    constructor(doubleVal: Double) : this(BigDecimal.valueOf(doubleVal).setScale(2, RoundingMode.HALF_UP))

    companion object {
        fun of(valueInMmolL: BigDecimal): DomainResult<GlucoseLevel> = validated(
            rangeError(ValidationField.GLUCOSE, valueInMmolL, MIN_GLUCOSE, MAX_GLUCOSE)
        ) { GlucoseLevel(valueInMmolL) }

        /** Checked variant of [fromMgDl]: the converted value must lie inside the accepted range. */
        fun ofMgDl(valueInMgDl: BigDecimal): DomainResult<GlucoseLevel> =
            of(valueInMgDl.multiply(MG_DL_TO_MMOL_FACTOR).setScale(2, RoundingMode.HALF_UP))

        val MIN_GLUCOSE: BigDecimal = BigDecimal("0.5")
        val MAX_GLUCOSE: BigDecimal = BigDecimal("55.0")
        private val MG_DL_TO_MMOL_FACTOR: BigDecimal = BigDecimal("0.0555")

        /**
         * Converts blood glucose measured in mg/dL to canonical mmol/L.
         */
        fun fromMgDl(valueInMgDl: BigDecimal): GlucoseLevel {
            val converted = valueInMgDl.multiply(MG_DL_TO_MMOL_FACTOR)
                .setScale(2, RoundingMode.HALF_UP)
            return GlucoseLevel(converted)
        }

        fun fromMgDl(valueInMgDl: Double): GlucoseLevel =
            fromMgDl(BigDecimal.valueOf(valueInMgDl))
    }
}
