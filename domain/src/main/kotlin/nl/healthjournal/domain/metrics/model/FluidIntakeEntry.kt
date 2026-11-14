package nl.healthjournal.domain.metrics.model

import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Instant

/**
 * One drink: a tap on a quick-add button or a custom amount. Every tap is its own entry. The drink is a label only and there is
 * no assessment; the app records what is entered (ADR 0030).
 */
data class FluidIntakeEntry(
    val id: MeasurementId,
    val profileId: ProfileId,
    val timestamp: Instant,
    val drink: FluidDrink,
    val amount: FluidMl,
    val comment: EntryComment? = null
)
