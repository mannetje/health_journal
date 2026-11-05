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
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.port.secondary.ActivityRepositoryPort
import java.time.Instant

/** Records an activity session. */
data class RecordActivityCommand(
    val profileId: ProfileId,
    val startTime: Instant,
    val endTime: Instant,
    val distanceInMeters: Double,
    val id: MeasurementId = MeasurementId.generate(),
    val comment: String? = null
) : Command<ActivitySession>

/** Replaces an existing activity session (same id and profile). */
data class UpdateActivityCommand(
    val id: MeasurementId,
    val profileId: ProfileId,
    val startTime: Instant,
    val endTime: Instant,
    val distanceInMeters: Double,
    val comment: String? = null
) : Command<ActivitySession>

private fun buildActivity(
    id: MeasurementId,
    profileId: ProfileId,
    start: Instant,
    end: Instant,
    distance: Double,
    comment: String?
): DomainResult<ActivitySession> {
    val note = EntryComment.of(comment).getOrElse { return DomainResult.failure(it) }
    return ActivitySession.of(id, profileId, start, end, distance, note)
}

class RecordActivityHandler(
    private val repository: ActivityRepositoryPort
) : CommandHandler<RecordActivityCommand, ActivitySession> {
    override suspend fun handle(command: RecordActivityCommand): DomainResult<ActivitySession> {
        val session = buildActivity(
            command.id, command.profileId, command.startTime, command.endTime, command.distanceInMeters, command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching { repository.saveActivity(session); session }
    }
}

class UpdateActivityHandler(
    private val repository: ActivityRepositoryPort
) : CommandHandler<UpdateActivityCommand, ActivitySession> {
    override suspend fun handle(command: UpdateActivityCommand): DomainResult<ActivitySession> {
        val session = buildActivity(
            command.id, command.profileId, command.startTime, command.endTime, command.distanceInMeters, command.comment
        ).getOrElse { return DomainResult.failure(it) }
        return domainCatching {
            if (repository.updateActivity(session)) DomainResult.success(session)
            else DomainResult.failure(DomainError.NotFound(EntryKind.ACTIVITY.name, command.id.toString()))
        }.flatMap { it }
    }
}
