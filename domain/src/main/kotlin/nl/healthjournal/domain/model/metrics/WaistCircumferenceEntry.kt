package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.nhg.NhgWaistCircumferenceCategory
import java.time.Instant

data class WaistCircumferenceEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val waist: WaistCircumferenceCm,
    val category: NhgWaistCircumferenceCategory? = null
)
