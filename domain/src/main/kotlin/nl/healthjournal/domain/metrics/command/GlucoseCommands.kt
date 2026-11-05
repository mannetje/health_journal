package nl.healthjournal.domain.metrics.command

import nl.healthjournal.domain.command.Command
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.EntryKind
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.command.domainCatching
import nl.healthjournal.domain.command.getOrElse
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.metrics.NhgGlucoseCategory
import nl.healthjournal.domain.port.secondary.GlucoseRepositoryPort
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
