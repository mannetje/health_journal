package nl.healthjournal.domain.usecase

import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.metrics.OxygenContext
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.OxygenSaturationPercent
import nl.healthjournal.domain.model.metrics.OxygenSite
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import java.time.Instant

class RecordOxygenSaturationUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        percent: Int,
        pulse: Int? = null,
        context: OxygenContext? = null,
        site: OxygenSite? = null,
        oxygenGiven: Boolean = false,
        timestamp: Instant = Instant.now(),
        measurementId: MeasurementId = MeasurementId.generate(),
        comment: String? = null
    ): OxygenSaturationEntry {
        val entry = OxygenSaturationEntry(
            id = measurementId,
            profileId = profileId,
            timestamp = timestamp,
            saturation = OxygenSaturationPercent(percent),
            pulse = pulse,
            context = context,
            site = site,
            oxygenGiven = oxygenGiven,
            comment = EntryComment.ofOrNull(comment)
        )
        healthLogRepository.saveOxygenSaturation(entry)
        return entry
    }
}

/** Replaces an existing oxygen saturation entry (same id and profile). Returns null when the id is unknown. */
class UpdateOxygenSaturationUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        id: MeasurementId,
        profileId: ProfileId,
        percent: Int,
        pulse: Int?,
        context: OxygenContext?,
        site: OxygenSite?,
        oxygenGiven: Boolean,
        timestamp: Instant,
        comment: String? = null
    ): OxygenSaturationEntry? {
        val entry = OxygenSaturationEntry(
            id = id,
            profileId = profileId,
            timestamp = timestamp,
            saturation = OxygenSaturationPercent(percent),
            pulse = pulse,
            context = context,
            site = site,
            oxygenGiven = oxygenGiven,
            comment = EntryComment.ofOrNull(comment)
        )
        return entry.takeIf { healthLogRepository.updateOxygenSaturation(it) }
    }
}

/** Permanently deletes one oxygen saturation entry by id. Returns false when the id is unknown. */
class DeleteOxygenSaturationUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(id: MeasurementId): Boolean = healthLogRepository.deleteOxygenSaturation(id)
}
