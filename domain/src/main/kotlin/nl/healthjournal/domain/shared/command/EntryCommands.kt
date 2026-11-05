package nl.healthjournal.domain.shared.command

import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort

/** The kinds of entry the journal stores. */
enum class EntryKind { WEIGHT, BLOOD_PRESSURE, GLUCOSE, ACTIVITY, WAIST_CIRCUMFERENCE, OXYGEN_SATURATION, MIGRAINE }

/** Permanently deletes one entry. Fails with [DomainError.NotFound] when the id is unknown. */
data class DeleteEntryCommand(val kind: EntryKind, val id: MeasurementId) : Command<Unit>

/** Entry-level operations over every store, so a handler depends on one thing instead of seven ports. */
interface EntryGateway {
    /** True when an entry was deleted, false when the id is unknown. */
    suspend fun delete(kind: EntryKind, id: MeasurementId): Boolean
}

class PortEntryGateway(
    private val weight: WeightRepositoryPort,
    private val bloodPressure: BloodPressureRepositoryPort,
    private val glucose: GlucoseRepositoryPort,
    private val activity: ActivityRepositoryPort,
    private val waist: WaistCircumferenceRepositoryPort,
    private val oxygen: OxygenSaturationRepositoryPort,
    private val migraine: MigraineRepositoryPort
) : EntryGateway {
    override suspend fun delete(kind: EntryKind, id: MeasurementId): Boolean = when (kind) {
        EntryKind.WEIGHT -> weight.deleteWeight(id)
        EntryKind.BLOOD_PRESSURE -> bloodPressure.deleteBloodPressure(id)
        EntryKind.GLUCOSE -> glucose.deleteGlucose(id)
        EntryKind.ACTIVITY -> activity.deleteActivity(id)
        EntryKind.WAIST_CIRCUMFERENCE -> waist.deleteWaistCircumference(id)
        EntryKind.OXYGEN_SATURATION -> oxygen.deleteOxygenSaturation(id)
        EntryKind.MIGRAINE -> migraine.deleteMigraine(id)
    }
}

class DeleteEntryHandler(private val gateway: EntryGateway) : CommandHandler<DeleteEntryCommand, Unit> {
    override suspend fun handle(command: DeleteEntryCommand): DomainResult<Unit> =
        domainCatching { gateway.delete(command.kind, command.id) }.flatMap { deleted ->
            if (deleted) DomainResult.success(Unit)
            else DomainResult.failure(DomainError.NotFound(command.kind.name, command.id.toString()))
        }
}
