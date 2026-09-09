package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import java.time.Instant

data class BloodPressureEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val reading: BloodPressureReading,
    val category: NhgBloodPressureCategory
)
