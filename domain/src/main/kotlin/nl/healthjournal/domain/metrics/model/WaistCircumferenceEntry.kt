package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant
import nl.healthjournal.domain.shared.model.EntryComment

data class WaistCircumferenceEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val waist: WaistCircumferenceCm,
    val category: NhgWaistCircumferenceCategory? = null,
    val comment: EntryComment? = null
)
