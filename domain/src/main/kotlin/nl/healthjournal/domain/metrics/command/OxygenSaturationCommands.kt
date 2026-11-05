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
import nl.healthjournal.domain.model.metrics.OxygenContext
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.OxygenSaturationPercent
import nl.healthjournal.domain.model.metrics.OxygenSite
import nl.healthjournal.domain.port.secondary.OxygenSaturationRepositoryPort
import java.time.Clock
import java.time.Instant

/** Records an oxygen saturation reading. A null [timestamp] means now. */
data class RecordOxygenSaturationCommand(
    val profileId: ProfileId,
    val percent: Int,
    val pulse: Int? = null,
    val context: OxygenContext? = null,
    val site: OxygenSite? = null,
    val oxygenGiven: Boolean = false,
    val timestamp: Instant? = null,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<OxygenSaturationEntry>

/** Replaces an existing oxygen saturation entry (same id and profile). */
data class UpdateOxygenSaturationCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val percent: Int,
    val pulse: Int?,
    val context: OxygenContext?,
    val site: OxygenSite?,
    val oxygenGiven: Boolean,
    val timestamp: Instant,
    val comment: String? = null
) : Command<OxygenSaturationEntry>

private fun buildOxygen(
    id: MeasurementId,
    profileId: ProfileId,
    percent: Int,
    pulse: Int?,
    context: OxygenContext?,
    site: OxygenSite?,
    oxygenGiven: Boolean,
    timestamp: Instant,
    comment: String?
): DomainResult<OxygenSaturationEntry> {
    val saturation = OxygenSaturationPercent.of(percent).getOrElse { return DomainResult.failure(it) }
    pulse?.let { p -> OxygenSaturationEntry.pulseError(p)?.let { return DomainResult.failure(it) } }
    val note = EntryComment.of(comment).getOrElse { return DomainResult.failure(it) }
    return DomainResult.success(
        OxygenSaturationEntry(id, profileId, timestamp, saturation, pulse, context, site, oxygenGiven, note)
    )
}

class RecordOxygenSaturationHandler(
    private val repository: OxygenSaturationRepositoryPort,
    private val clock: Clock
) : CommandHandler<RecordOxygenSaturationCommand, OxygenSaturationEntry> {
    override suspend fun handle(command: RecordOxygenSaturationCommand): DomainResult<OxygenSaturationEntry> {
        val entry = buildOxygen(
            command.id, command.profileId, command.percent, command.pulse, command.context, command.site,
            command.oxygenGiven, command.timestamp ?: clock.instant(), command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveOxygenSaturation(entry); entry }
    }
}

class UpdateOxygenSaturationHandler(
    private val repository: OxygenSaturationRepositoryPort
) : CommandHandler<UpdateOxygenSaturationCommand, OxygenSaturationEntry> {
    override suspend fun handle(command: UpdateOxygenSaturationCommand): DomainResult<OxygenSaturationEntry> {
        val entry = buildOxygen(
            command.id, command.profileId, command.percent, command.pulse, command.context, command.site,
            command.oxygenGiven, command.timestamp, command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            if (repository.updateOxygenSaturation(entry)) DomainResult.success(entry)
            else DomainResult.failure(DomainError.NotFound(EntryKind.OXYGEN_SATURATION.name, command.id.toString()))
        }.flatMap { it }
    }
}
