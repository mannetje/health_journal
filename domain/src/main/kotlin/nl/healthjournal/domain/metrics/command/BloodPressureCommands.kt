package nl.healthjournal.domain.metrics.command

import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.EntryKind
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.shared.command.getOrElse
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.BloodPressureEntry
import nl.healthjournal.domain.metrics.model.BloodPressureReading
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import java.time.Clock
import java.time.Instant

/** Records a blood pressure. A null [timestamp] means now. */
data class RecordBloodPressureCommand(
    val profileId: ProfileId,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<BloodPressureEntry>

/** Replaces an existing blood pressure entry (same id and profile). */
data class UpdateBloodPressureCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Instant,
    val pulse: Int? = null,
    val comment: String? = null
) : Command<BloodPressureEntry>

private fun buildBloodPressure(
    id: MeasurementId,
    profileId: ProfileId,
    systolic: Int,
    diastolic: Int,
    pulse: Int?,
    timestamp: Instant,
    comment: String?
): DomainResult<BloodPressureEntry> {
    val reading = BloodPressureReading.of(systolic, diastolic, pulse).getOrElse { return DomainResult.failure(it) }
    val note = EntryComment.of(comment).getOrElse { return DomainResult.failure(it) }
    return DomainResult.success(
        BloodPressureEntry(id, profileId, timestamp, reading, NhgBloodPressureCategory.classify(reading), note)
    )
}

class RecordBloodPressureHandler(
    private val repository: BloodPressureRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordBloodPressureCommand, BloodPressureEntry> {
    override suspend fun handle(command: RecordBloodPressureCommand): DomainResult<BloodPressureEntry> {
        val entry = buildBloodPressure(
            command.id, command.profileId, command.systolic, command.diastolic, command.pulse,
            command.timestamp ?: clock.instant(), command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveBloodPressure(entry); entry }
    }
}

class UpdateBloodPressureHandler(
    private val repository: BloodPressureRepositoryPort
) : CommandHandler<UpdateBloodPressureCommand, BloodPressureEntry> {
    override suspend fun handle(command: UpdateBloodPressureCommand): DomainResult<BloodPressureEntry> {
        val entry = buildBloodPressure(
            command.id, command.profileId, command.systolic, command.diastolic, command.pulse,
            command.timestamp, command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            if (repository.updateBloodPressure(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.BLOOD_PRESSURE.name, command.id.toString()))
        }.flatMap { it }
    }
}
