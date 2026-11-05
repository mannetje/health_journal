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
import nl.healthjournal.domain.model.common.WeightKg
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.port.secondary.WeightRepositoryPort
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

/** Records a weight. A null [timestamp] means now; the BMI is computed from the profile's height. */
data class RecordWeightCommand(
    val profileId: ProfileId,
    val weightKg: BigDecimal,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<WeightEntry>

/** Replaces an existing weight entry (same id and profile) and recomputes the BMI. */
data class UpdateWeightCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val weightKg: BigDecimal,
    val timestamp: Instant,
    val comment: String? = null
) : Command<WeightEntry>

private suspend fun buildWeight(
    profiles: ProfileRepositoryPort,
    id: MeasurementId,
    profileId: ProfileId,
    weight: WeightKg,
    timestamp: Instant,
    comment: EntryComment?
) = WeightEntry(
    id = id,
    profileId = profileId,
    timestamp = timestamp,
    weight = weight,
    bmi = profiles.getById(profileId)?.calculateBmi(weight)?.bmi,
    comment = comment
)

class RecordWeightHandler(
    private val weights: WeightRepositoryPort,
    private val profiles: ProfileRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordWeightCommand, WeightEntry> {
    override suspend fun handle(command: RecordWeightCommand): DomainResult<WeightEntry> {
        val weight = WeightKg.of(command.weightKg).getOrElse { return DomainResult.failure(it) }
        val comment = EntryComment.of(command.comment).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            val entry = buildWeight(profiles, command.id, command.profileId, weight, command.timestamp ?: clock.instant(), comment)
            weights.saveWeight(entry)
            entry
        }
    }
}

class UpdateWeightHandler(
    private val weights: WeightRepositoryPort,
    private val profiles: ProfileRepositoryPort
) : CommandHandler<UpdateWeightCommand, WeightEntry> {
    override suspend fun handle(command: UpdateWeightCommand): DomainResult<WeightEntry> {
        val weight = WeightKg.of(command.weightKg).getOrElse { return DomainResult.failure(it) }
        val comment = EntryComment.of(command.comment).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            val entry = buildWeight(profiles, command.id, command.profileId, weight, command.timestamp, comment)
            if (weights.updateWeight(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.WEIGHT.name, command.id.toString()))
        }.flatMap { it }
    }
}
