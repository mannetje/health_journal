package nl.healthjournal.domain.metrics.command

import nl.healthjournal.domain.command.Command
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.EntryKind
import nl.healthjournal.domain.command.domainCatching
import nl.healthjournal.domain.command.getOrElse
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.metrics.WaistCircumferenceClassifier
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.port.secondary.WaistCircumferenceRepositoryPort
import java.time.Clock
import java.time.Instant

/** Records a waist circumference. A null [timestamp] means now; the NHG category needs the profile's sex. */
data class RecordWaistCircumferenceCommand(
    val profileId: ProfileId,
    val waistCm: Double,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<WaistCircumferenceEntry>

/** Replaces an existing waist circumference entry (same id and profile) and recomputes the category. */
data class UpdateWaistCircumferenceCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val waistCm: Double,
    val timestamp: Instant,
    val comment: String? = null
) : Command<WaistCircumferenceEntry>

private suspend fun buildWaist(
    profiles: ProfileRepositoryPort,
    id: MeasurementId,
    profileId: ProfileId,
    waist: WaistCircumferenceCm,
    timestamp: Instant,
    comment: EntryComment?
) = WaistCircumferenceEntry(
    id = id,
    profileId = profileId,
    timestamp = timestamp,
    waist = waist,
    category = WaistCircumferenceClassifier.classify(profiles.getById(profileId)?.sex, waist),
    comment = comment
)

class RecordWaistCircumferenceHandler(
    private val repository: WaistCircumferenceRepositoryPort,
    private val profiles: ProfileRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordWaistCircumferenceCommand, WaistCircumferenceEntry> {
    override suspend fun handle(command: RecordWaistCircumferenceCommand): DomainResult<WaistCircumferenceEntry> {
        val waist = WaistCircumferenceCm.of(command.waistCm).getOrElse { return DomainResult.failure(it) }
        val comment = EntryComment.of(command.comment).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            val entry = buildWaist(profiles, command.id, command.profileId, waist, command.timestamp ?: clock.instant(), comment)
            repository.saveWaistCircumference(entry)
            entry
        }
    }
}

class UpdateWaistCircumferenceHandler(
    private val repository: WaistCircumferenceRepositoryPort,
    private val profiles: ProfileRepositoryPort
) : CommandHandler<UpdateWaistCircumferenceCommand, WaistCircumferenceEntry> {
    override suspend fun handle(command: UpdateWaistCircumferenceCommand): DomainResult<WaistCircumferenceEntry> {
        val waist = WaistCircumferenceCm.of(command.waistCm).getOrElse { return DomainResult.failure(it) }
        val comment = EntryComment.of(command.comment).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            val entry = buildWaist(profiles, command.id, command.profileId, waist, command.timestamp, comment)
            if (repository.updateWaistCircumference(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.WAIST_CIRCUMFERENCE.name, command.id.toString()))
        }.flatMap { it }
    }
}
