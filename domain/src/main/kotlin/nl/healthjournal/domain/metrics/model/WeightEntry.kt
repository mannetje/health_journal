package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.math.BigDecimal
import java.time.Instant
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.WeightKg

data class WeightEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val weight: WeightKg,
    val bmi: BigDecimal? = null,
    val comment: EntryComment? = null
)
