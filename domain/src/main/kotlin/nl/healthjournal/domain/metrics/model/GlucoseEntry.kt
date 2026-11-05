package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant
import nl.healthjournal.domain.shared.model.EntryComment

data class GlucoseEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val glucose: GlucoseLevel,
    val context: GlucoseContext,
    val category: NhgGlucoseCategory,
    val comment: EntryComment? = null
)
