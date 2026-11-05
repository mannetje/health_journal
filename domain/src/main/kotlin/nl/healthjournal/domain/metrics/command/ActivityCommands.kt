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
import nl.healthjournal.domain.metrics.model.ActivitySession
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
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
