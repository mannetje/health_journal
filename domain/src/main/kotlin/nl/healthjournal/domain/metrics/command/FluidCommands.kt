package nl.healthjournal.domain.metrics.command

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.healthjournal.domain.metrics.model.FluidDrink
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry
import nl.healthjournal.domain.metrics.model.FluidLimits
import nl.healthjournal.domain.metrics.model.FluidMl
import nl.healthjournal.domain.metrics.port.FluidRepositoryPort
import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.EntryKind
import nl.healthjournal.domain.shared.command.ObserveQuery
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.shared.command.getOrElse
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import java.time.Clock
import java.time.Instant

/** Records one drink. A null [timestamp] means now. Quick-add taps pass no comment. */
data class RecordFluidIntakeCommand(
    val profileId: ProfileId,
    val drink: FluidDrink,
    val ml: Int,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<FluidIntakeEntry>

/** Replaces an existing fluid intake entry (same id and profile). */
data class UpdateFluidIntakeCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val drink: FluidDrink,
    val ml: Int,
    val timestamp: Instant,
    val comment: String? = null
) : Command<FluidIntakeEntry>

/** Sets, changes or clears the person's own minimum and maximum; null clears one. Never touches entries. */
data class SetFluidLimitsCommand(
    val profileId: ProfileId,
    val minMl: Int?,
    val maxMl: Int?
) : Command<FluidLimits>

/** The person's own minimum and maximum; emits again when they change. */
data class ObserveFluidLimitsQuery(val profileId: ProfileId) : ObserveQuery<FluidLimits>

private fun buildFluid(
    id: MeasurementId,
    profileId: ProfileId,
    drink: FluidDrink,
    ml: Int,
    timestamp: Instant,
    comment: String?
): DomainResult<FluidIntakeEntry> {
    val amount = FluidMl.of(ml).getOrElse { return DomainResult.failure(it) }
    val note = EntryComment.of(comment).getOrElse { return DomainResult.failure(it) }
    return DomainResult.success(FluidIntakeEntry(id, profileId, timestamp, drink, amount, note))
}

class RecordFluidIntakeHandler(
    private val repository: FluidRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordFluidIntakeCommand, FluidIntakeEntry> {
    override suspend fun handle(command: RecordFluidIntakeCommand): DomainResult<FluidIntakeEntry> {
        val entry = buildFluid(
            command.id, command.profileId, command.drink, command.ml, command.timestamp ?: clock.instant(), command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveFluidIntake(entry); entry }
    }
}

class UpdateFluidIntakeHandler(
    private val repository: FluidRepositoryPort
) : CommandHandler<UpdateFluidIntakeCommand, FluidIntakeEntry> {
    override suspend fun handle(command: UpdateFluidIntakeCommand): DomainResult<FluidIntakeEntry> {
        val entry = buildFluid(command.id, command.profileId, command.drink, command.ml, command.timestamp, command.comment)
            .getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            if (repository.updateFluidIntake(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.FLUID_INTAKE.name, command.id.toString()))
        }.flatMap { it }
    }
}

class SetFluidLimitsHandler(
    private val repository: FluidRepositoryPort
) : CommandHandler<SetFluidLimitsCommand, FluidLimits> {
    override suspend fun handle(command: SetFluidLimitsCommand): DomainResult<FluidLimits> {
        val limits = FluidLimits.of(command.minMl, command.maxMl).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveFluidLimits(command.profileId, limits); limits }
    }
}

class ObserveFluidLimitsHandler(
    private val repository: FluidRepositoryPort
) : ObserveQueryHandler<ObserveFluidLimitsQuery, FluidLimits> {
    override fun observe(query: ObserveFluidLimitsQuery): Flow<DomainResult<FluidLimits>> =
        repository.observeFluidLimits(query.profileId).map { DomainResult.success(it) }
}
