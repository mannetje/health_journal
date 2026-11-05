package nl.healthjournal.domain.model.metrics

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import java.time.Instant
import nl.healthjournal.domain.model.common.EntryComment

data class GlucoseEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val glucose: GlucoseLevel,
    val context: GlucoseContext,
    val category: NhgGlucoseCategory,
    val comment: EntryComment? = null
)
