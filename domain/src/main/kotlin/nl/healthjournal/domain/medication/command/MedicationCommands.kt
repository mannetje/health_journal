package nl.healthjournal.domain.medication.command

import nl.healthjournal.domain.command.Command
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.Query
import nl.healthjournal.domain.command.QueryHandler
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.command.domainCatching
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.Adherence
import nl.healthjournal.domain.model.medication.AdherenceReport
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillboxDay
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.usecase.ArchiveMedicationUseCase
import nl.healthjournal.domain.usecase.ChangeMedicationScheduleUseCase
import nl.healthjournal.domain.usecase.DeleteMedicationUseCase
import nl.healthjournal.domain.usecase.GetAdherenceUseCase
import nl.healthjournal.domain.usecase.GetPillboxDayUseCase
import nl.healthjournal.domain.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.usecase.SaveMedicationUseCase
import nl.healthjournal.domain.usecase.TakeAllForSlotUseCase
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

private const val KIND = "MEDICATION"

/**
 * Null when every id exists, otherwise the failure to return: [DomainError.NotFound] for the first unknown id,
 * [DomainError.Storage] when the lookup itself fails. The use cases below would throw instead.
 */
private suspend fun MedicationRepositoryPort.firstProblem(ids: Collection<MedicationId>): DomainResult<Nothing>? {
    for (id in ids.distinct()) {
        val found = domainCatching { getMedication(id) != null }
        if (found is DomainResult.Failure) return found
        if (found.getOrNull() != true) return DomainResult.failure(DomainError.NotFound(KIND, id.toString()))
    }
    return null
}

// ---- queries ----------------------------------------------------------------------------------------------

/** One day of planned and recorded intakes. */
data class GetPillboxDayQuery(val profileId: ProfileId, val date: LocalDate) : Query<PillboxDay>

/** Adherence over the last [days] days; only the ranges in [Adherence.RANGES_IN_DAYS] are accepted. */
data class GetAdherenceQuery(val profileId: ProfileId, val days: Int) : Query<AdherenceReport>

/** The medications of one profile, archived ones included when asked, in the order the repository returns them. */
data class ListMedicationsQuery(val profileId: ProfileId, val includeArchived: Boolean = true) : Query<List<Medication>>

/** One medication, or null when the id is unknown. */
data class GetMedicationQuery(val id: MedicationId) : Query<Medication?>

class ListMedicationsHandler(
    private val repository: MedicationRepositoryPort
) : QueryHandler<ListMedicationsQuery, List<Medication>> {
    override suspend fun handle(query: ListMedicationsQuery): DomainResult<List<Medication>> =
        domainCatching { repository.getMedications(query.profileId, query.includeArchived) }
}

class GetMedicationHandler(
    private val repository: MedicationRepositoryPort
) : QueryHandler<GetMedicationQuery, Medication?> {
    override suspend fun handle(query: GetMedicationQuery): DomainResult<Medication?> =
        domainCatching { repository.getMedication(query.id) }
}

class GetPillboxDayHandler(
    private val getDay: GetPillboxDayUseCase
) : QueryHandler<GetPillboxDayQuery, PillboxDay> {
    override suspend fun handle(query: GetPillboxDayQuery): DomainResult<PillboxDay> =
        domainCatching { getDay(query.profileId, query.date) }
}

class GetAdherenceHandler(
    private val getAdherence: GetAdherenceUseCase
) : QueryHandler<GetAdherenceQuery, AdherenceReport> {
    override suspend fun handle(query: GetAdherenceQuery): DomainResult<AdherenceReport> {
        if (query.days !in Adherence.RANGES_IN_DAYS) {
            return DomainResult.failure(
                DomainError.Invalid(ValidationField.TIME_RANGE, ValidationRule.OUT_OF_RANGE, Adherence.RANGES_IN_DAYS.toList())
            )
        }
        return domainCatching { getAdherence(query.profileId, query.days) }
    }
}

// ---- commands ---------------------------------------------------------------------------------------------

/** Creates a medication, or edits the details of [existingId]; an edit keeps schedules and archived state. */
data class SaveMedicationCommand(
    val profileId: ProfileId,
    val name: String,
    val form: MedicationForm?,
    val strengthAmount: BigDecimal?,
    val strengthUnit: StrengthUnit?,
    val amountPerIntake: BigDecimal,
    val doseUnit: DoseUnit,
    val appearance: PillAppearance?,
    val schedule: Schedule,
    val comment: String? = null,
    val effectiveFrom: LocalDate? = null,
    val existingId: MedicationId? = null
) : Command<Medication>

/** Adds a schedule version from [effectiveFrom] (default: today). */
data class ChangeMedicationScheduleCommand(
    val id: MedicationId,
    val schedule: Schedule,
    val effectiveFrom: LocalDate? = null
) : Command<Medication>

/** Stops planned intakes from [from] (default: today). */
data class ArchiveMedicationCommand(val id: MedicationId, val from: LocalDate? = null) : Command<Medication>

/** Removes the medication and its recorded intakes. */
data class DeleteMedicationCommand(val id: MedicationId) : Command<Unit>

/** Records the outcome of a planned intake ([planned] set) or an as-needed dose ([planned] null). */
data class RecordIntakeCommand(
    val medicationId: MedicationId,
    val planned: LocalDateTime?,
    val status: IntakeStatus = IntakeStatus.TAKEN,
    val takenAt: Instant? = null,
    val actualAmount: BigDecimal? = null,
    val comment: String? = null
) : Command<Intake>

/** "Taken all" for the medications the screen shows in one slot. */
data class RecordSlotIntakesCommand(
    val medicationIds: List<MedicationId>,
    val planned: LocalDateTime,
    val takenAt: Instant? = null
) : Command<List<Intake>>

/** "Taken all" from a notification: records taken for what is still open in the slot at [slotTime]. */
data class TakeAllForSlotCommand(val profileId: ProfileId, val slotTime: LocalDateTime) : Command<List<Intake>>

class SaveMedicationHandler(
    private val save: SaveMedicationUseCase,
    private val clock: Clock
) : CommandHandler<SaveMedicationCommand, Medication> {
    override suspend fun handle(command: SaveMedicationCommand): DomainResult<Medication> {
        val name = command.name.trim()
        if (name.isEmpty()) {
            return DomainResult.failure(DomainError.Invalid(ValidationField.NAME, ValidationRule.REQUIRED))
        }
        if (name.length > MedicationName.MAX_LENGTH) {
            return DomainResult.failure(
                DomainError.Invalid(ValidationField.NAME, ValidationRule.TOO_LONG, listOf(MedicationName.MAX_LENGTH))
            )
        }
        return domainCatching {
            save(
                command.profileId, name, command.form, command.strengthAmount, command.strengthUnit,
                command.amountPerIntake, command.doseUnit, command.appearance, command.schedule, command.comment,
                command.effectiveFrom ?: LocalDate.now(clock), command.existingId
            )
        }
    }
}

class ChangeMedicationScheduleHandler(
    private val repository: MedicationRepositoryPort,
    private val change: ChangeMedicationScheduleUseCase,
    private val clock: Clock
) : CommandHandler<ChangeMedicationScheduleCommand, Medication> {
    override suspend fun handle(command: ChangeMedicationScheduleCommand): DomainResult<Medication> {
        repository.firstProblem(listOf(command.id))?.let { return it }
        return domainCatching { change(command.id, command.schedule, command.effectiveFrom ?: LocalDate.now(clock)) }
    }
}

class ArchiveMedicationHandler(
    private val repository: MedicationRepositoryPort,
    private val archive: ArchiveMedicationUseCase,
    private val clock: Clock
) : CommandHandler<ArchiveMedicationCommand, Medication> {
    override suspend fun handle(command: ArchiveMedicationCommand): DomainResult<Medication> {
        repository.firstProblem(listOf(command.id))?.let { return it }
        return domainCatching { archive(command.id, command.from ?: LocalDate.now(clock)) }
    }
}

class DeleteMedicationHandler(
    private val repository: MedicationRepositoryPort,
    private val delete: DeleteMedicationUseCase
) : CommandHandler<DeleteMedicationCommand, Unit> {
    override suspend fun handle(command: DeleteMedicationCommand): DomainResult<Unit> {
        repository.firstProblem(listOf(command.id))?.let { return it }
        return domainCatching { delete(command.id) }
    }
}

class RecordIntakeHandler(
    private val repository: MedicationRepositoryPort,
    private val record: RecordIntakeUseCase
) : CommandHandler<RecordIntakeCommand, Intake> {
    override suspend fun handle(command: RecordIntakeCommand): DomainResult<Intake> {
        repository.firstProblem(listOf(command.medicationId))?.let { return it }
        return domainCatching {
            record(
                command.medicationId, command.planned, command.status, command.takenAt, command.actualAmount,
                command.comment
            )
        }
    }
}

class RecordSlotIntakesHandler(
    private val repository: MedicationRepositoryPort,
    private val recordSlot: RecordSlotIntakesUseCase
) : CommandHandler<RecordSlotIntakesCommand, List<Intake>> {
    override suspend fun handle(command: RecordSlotIntakesCommand): DomainResult<List<Intake>> {
        repository.firstProblem(command.medicationIds)?.let { return it }
        return domainCatching { recordSlot(command.medicationIds, command.planned, command.takenAt) }
    }
}

class TakeAllForSlotHandler(
    private val takeAll: TakeAllForSlotUseCase
) : CommandHandler<TakeAllForSlotCommand, List<Intake>> {
    override suspend fun handle(command: TakeAllForSlotCommand): DomainResult<List<Intake>> =
        domainCatching { takeAll(command.profileId, command.slotTime) }
}
