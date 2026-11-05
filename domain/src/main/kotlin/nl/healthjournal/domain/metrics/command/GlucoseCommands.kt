package nl.healthjournal.domain.metrics.command

import nl.healthjournal.domain.shared.command.Command
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.EntryKind
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.shared.command.domainCatching
import nl.healthjournal.domain.shared.command.getOrElse
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.GlucoseEntry
import nl.healthjournal.domain.metrics.model.GlucoseLevel
import nl.healthjournal.domain.metrics.model.NhgGlucoseCategory
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

/** Records a glucose value given in mmol/L or in mg/dL (one of the two). A null [timestamp] means now. */
data class RecordGlucoseCommand(
    val profileId: ProfileId,
    val context: GlucoseContext,
    val valueInMmolL: BigDecimal? = null,
    val valueInMgDl: BigDecimal? = null,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<GlucoseEntry>

/** Replaces an existing glucose entry (same id and profile). */
data class UpdateGlucoseCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val context: GlucoseContext,
    val valueInMmolL: BigDecimal,
    val timestamp: Instant,
    val comment: String? = null
) : Command<GlucoseEntry>

private fun buildGlucose(
    id: MeasurementId,
    profileId: ProfileId,
    context: GlucoseContext,
    mmolL: BigDecimal?,
    mgDl: BigDecimal?,
    timestamp: Instant,
    comment: String?
): DomainResult<GlucoseEntry> {
    val level = when {
        mmolL != null -> GlucoseLevel.of(mmolL)
        mgDl != null -> GlucoseLevel.ofMgDl(mgDl)
        else -> DomainResult.failure(DomainError.Invalid(ValidationField.GLUCOSE, ValidationRule.REQUIRED))
    }.getOrElse { return DomainResult.failure(it) }
    val note = EntryComment.of(comment).getOrElse { return DomainResult.failure(it) }
    return DomainResult.success(
        GlucoseEntry(id, profileId, timestamp, level, context, NhgGlucoseCategory.classify(level, context), note)
    )
}

class RecordGlucoseHandler(
    private val repository: GlucoseRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordGlucoseCommand, GlucoseEntry> {
    override suspend fun handle(command: RecordGlucoseCommand): DomainResult<GlucoseEntry> {
        val entry = buildGlucose(
            command.id, command.profileId, command.context, command.valueInMmolL, command.valueInMgDl,
            command.timestamp ?: clock.instant(), command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveGlucose(entry); entry }
    }
}

class UpdateGlucoseHandler(
    private val repository: GlucoseRepositoryPort
) : CommandHandler<UpdateGlucoseCommand, GlucoseEntry> {
    override suspend fun handle(command: UpdateGlucoseCommand): DomainResult<GlucoseEntry> {
        val entry = buildGlucose(
            command.id, command.profileId, command.context, command.valueInMmolL, null,
            command.timestamp, command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            if (repository.updateGlucose(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.GLUCOSE.name, command.id.toString()))
        }.flatMap { it }
    }
}
