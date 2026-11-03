package nl.healthjournal.domain.model.metrics

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Plain figures over a set of readings: lowest, highest, average (one decimal), count and the count per context.
 * Readings without a context are counted under `null`. Site and the oxygen-given note change nothing here, and no
 * threshold is applied.
 */
data class OxygenStatistics(
    val lowest: Int,
    val highest: Int,
    val average: BigDecimal,
    val count: Int,
    val countPerContext: Map<OxygenContext?, Int>
) {
    companion object {
        /** Null for an empty list. */
        fun of(entries: List<OxygenSaturationEntry>): OxygenStatistics? {
            if (entries.isEmpty()) return null
            val values = entries.map { it.saturation.value }
            return OxygenStatistics(
                lowest = values.min(),
                highest = values.max(),
                average = BigDecimal(values.sum()).divide(BigDecimal(values.size), 1, RoundingMode.HALF_UP),
                count = values.size,
                countPerContext = entries.groupingBy { it.context }.eachCount()
            )
        }
    }
}
