package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import java.time.Instant
import nl.healthjournal.domain.model.common.EntryComment

data class BloodPressureEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val reading: BloodPressureReading,
    val category: NhgBloodPressureCategory,
    val comment: EntryComment? = null
)
