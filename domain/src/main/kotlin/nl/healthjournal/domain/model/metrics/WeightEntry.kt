package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import java.math.BigDecimal
import java.time.Instant

data class WeightEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val weight: WeightKg,
    val bmi: BigDecimal? = null
)
