package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant
import nl.healthjournal.domain.shared.model.EntryComment

data class BloodPressureEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val reading: BloodPressureReading,
    val category: NhgBloodPressureCategory,
    val comment: EntryComment? = null
)
