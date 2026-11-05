package nl.healthjournal.domain.migraine.command

import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.shared.command.getOrElse
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.migraine.model.MigraineEntry
import nl.healthjournal.domain.migraine.model.MigraineTagId
import nl.healthjournal.domain.migraine.model.MigraineTagKind
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import nl.healthjournal.domain.migraine.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.migraine.usecase.AddTagResult
import nl.healthjournal.domain.migraine.usecase.ArchiveMigraineTagUseCase
import nl.healthjournal.domain.migraine.usecase.RenameMigraineTagUseCase
import nl.healthjournal.domain.migraine.usecase.RenameTagResult
import java.time.Clock
import java.time.Instant

private const val MIGRAINE = "MIGRAINE"
private const val MIGRAINE_TAG = "MIGRAINE_TAG"

/** Saves a new migraine. */
data class RecordMigraineCommand(val entry: MigraineEntry) : Command<MigraineEntry>

/** Replaces a migraine (same id and profile). Fails with NotFound when the id is unknown. */
data class UpdateMigraineCommand(val entry: MigraineEntry) : Command<MigraineEntry>

/**
 * Sets the end of an ongoing migraine ([end] null means now). Fails with NotFound for an unknown id, and with
 * END_NOT_AFTER_START when it already has an end or [end] is before the start.
 */
data class EndMigraineCommand(val id: MeasurementId, val end: Instant? = null) : Command<MigraineEntry>

/** Adds a tag, or returns the existing one with the same label. A full list is an outcome, not an error. */
data class AddMigraineTagCommand(
    val profileId: ProfileId,
    val kind: MigraineTagKind,
    val label: String
) : Command<AddTagResult>

/** Renames a tag. An unknown tag or a taken label is an outcome of [RenameTagResult]. */
data class RenameMigraineTagCommand(val id: MigraineTagId, val label: String) : Command<RenameTagResult>

/** Hides a tag from the picker; entries that used it keep it. Fails with NotFound for an unknown id. */
data class ArchiveMigraineTagCommand(val id: MigraineTagId) : Command<Unit>

private fun blankLabel() = DomainResult.failure(DomainError.Invalid(ValidationField.NAME, ValidationRule.REQUIRED))

class RecordMigraineHandler(
    private val repository: MigraineRepositoryPort
) : CommandHandler<RecordMigraineCommand, MigraineEntry> {
    override suspend fun handle(command: RecordMigraineCommand): DomainResult<MigraineEntry> =
        domainCatching { command.entry.also { repository.saveMigraine(it) } }
}

class UpdateMigraineHandler(
    private val repository: MigraineRepositoryPort
) : CommandHandler<UpdateMigraineCommand, MigraineEntry> {
    override suspend fun handle(command: UpdateMigraineCommand): DomainResult<MigraineEntry> =
        domainCatching { repository.updateMigraine(command.entry) }.flatMap { updated ->
            if (updated) DomainResult.success(command.entry)
            else DomainResult.failure(DomainError.NotFound(MIGRAINE, command.entry.id.toString()))
        }
}

class EndMigraineHandler(
    private val repository: MigraineRepositoryPort,
    private val clock: Clock
) : CommandHandler<EndMigraineCommand, MigraineEntry> {
    override suspend fun handle(command: EndMigraineCommand): DomainResult<MigraineEntry> {
        val notFound = DomainError.NotFound(MIGRAINE, command.id.toString())
        val entry = domainCatching { repository.getMigraine(command.id) }.getOrElse { return DomainResult.failure(it) }
            ?: return DomainResult.failure(notFound)
        val ended = entry.endedAt(command.end ?: clock.instant())
            ?: return DomainResult.failure(DomainError.Invalid(ValidationField.TIME_RANGE, ValidationRule.END_NOT_AFTER_START))
        return domainCatching { repository.updateMigraine(ended) }.flatMap { updated ->
            if (updated) DomainResult.success(ended) else DomainResult.failure(notFound)
        }
    }
}

class AddMigraineTagHandler(
    private val add: AddMigraineTagUseCase
) : CommandHandler<AddMigraineTagCommand, AddTagResult> {
    override suspend fun handle(command: AddMigraineTagCommand): DomainResult<AddTagResult> =
        if (command.label.isBlank()) blankLabel()
        else domainCatching { add(command.profileId, command.kind, command.label) }
}

class RenameMigraineTagHandler(
    private val rename: RenameMigraineTagUseCase
) : CommandHandler<RenameMigraineTagCommand, RenameTagResult> {
    override suspend fun handle(command: RenameMigraineTagCommand): DomainResult<RenameTagResult> =
        if (command.label.isBlank()) blankLabel()
        else domainCatching { rename(command.id, command.label) }
}

class ArchiveMigraineTagHandler(
    private val archive: ArchiveMigraineTagUseCase
) : CommandHandler<ArchiveMigraineTagCommand, Unit> {
    override suspend fun handle(command: ArchiveMigraineTagCommand): DomainResult<Unit> =
        domainCatching { archive(command.id) }.flatMap { archived ->
            if (archived) DomainResult.success(Unit)
            else DomainResult.failure(DomainError.NotFound(MIGRAINE_TAG, command.id.toString()))
        }
}
