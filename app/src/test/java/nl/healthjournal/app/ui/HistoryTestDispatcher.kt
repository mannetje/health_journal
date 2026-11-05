package nl.healthjournal.app.ui

import nl.healthjournal.app.command.DefaultDispatcher
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.DeleteEntryCommand
import nl.healthjournal.domain.command.DeleteEntryHandler
import nl.healthjournal.domain.command.Dispatcher
import nl.healthjournal.domain.command.ExportCsvHandler
import nl.healthjournal.domain.command.ExportCsvQuery
import nl.healthjournal.domain.command.GetHistoryHandler
import nl.healthjournal.domain.command.GetHistoryQuery
import nl.healthjournal.domain.command.ImportCsvCommand
import nl.healthjournal.domain.command.ImportCsvHandler
import nl.healthjournal.domain.command.PortEntryGateway
import nl.healthjournal.domain.command.QueryHandler
import nl.healthjournal.domain.metrics.command.RecordActivityCommand
import nl.healthjournal.domain.metrics.command.RecordActivityHandler
import nl.healthjournal.domain.metrics.command.RecordBloodPressureCommand
import nl.healthjournal.domain.metrics.command.RecordBloodPressureHandler
import nl.healthjournal.domain.metrics.command.RecordGlucoseCommand
import nl.healthjournal.domain.metrics.command.RecordGlucoseHandler
import nl.healthjournal.domain.metrics.command.RecordOxygenSaturationCommand
import nl.healthjournal.domain.metrics.command.RecordOxygenSaturationHandler
import nl.healthjournal.domain.metrics.command.RecordWaistCircumferenceCommand
import nl.healthjournal.domain.metrics.command.RecordWaistCircumferenceHandler
import nl.healthjournal.domain.metrics.command.RecordWeightCommand
import nl.healthjournal.domain.metrics.command.RecordWeightHandler
import nl.healthjournal.domain.metrics.command.UpdateActivityCommand
import nl.healthjournal.domain.metrics.command.UpdateActivityHandler
import nl.healthjournal.domain.metrics.command.UpdateBloodPressureCommand
import nl.healthjournal.domain.metrics.command.UpdateBloodPressureHandler
import nl.healthjournal.domain.metrics.command.UpdateGlucoseCommand
import nl.healthjournal.domain.metrics.command.UpdateGlucoseHandler
import nl.healthjournal.domain.metrics.command.UpdateOxygenSaturationCommand
import nl.healthjournal.domain.metrics.command.UpdateOxygenSaturationHandler
import nl.healthjournal.domain.metrics.command.UpdateWaistCircumferenceCommand
import nl.healthjournal.domain.metrics.command.UpdateWaistCircumferenceHandler
import nl.healthjournal.domain.metrics.command.UpdateWeightCommand
import nl.healthjournal.domain.metrics.command.UpdateWeightHandler
import nl.healthjournal.domain.migraine.command.EndMigraineCommand
import nl.healthjournal.domain.migraine.command.EndMigraineHandler
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineImportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.profile.command.GetActiveProfileHandler
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import nl.healthjournal.domain.testing.healthHistoryUseCase
import java.time.Clock

/** The real record, update, delete and history handlers wired over fakes, behind the real [DefaultDispatcher], for ViewModel tests. */
internal fun historyDispatcher(
    profiles: ProfileRepositoryPort,
    health: FakeHealthLogRepository,
    migraines: MigraineRepositoryPort,
    export: DataExportPort,
    import: DataImportPort,
    migraineExport: MigraineExportPort,
    migraineImport: MigraineImportPort,
    clock: Clock = Clock.systemUTC()
): Dispatcher = DefaultDispatcher(
    commands = mapOf<Class<*>, CommandHandler<*, *>>(
        RecordActivityCommand::class.java to RecordActivityHandler(health),
        RecordBloodPressureCommand::class.java to RecordBloodPressureHandler(health, clock),
        RecordGlucoseCommand::class.java to RecordGlucoseHandler(health, clock),
        RecordOxygenSaturationCommand::class.java to RecordOxygenSaturationHandler(health, clock),
        RecordWaistCircumferenceCommand::class.java to RecordWaistCircumferenceHandler(health, profiles, clock),
        RecordWeightCommand::class.java to RecordWeightHandler(health, profiles, clock),
        UpdateWeightCommand::class.java to UpdateWeightHandler(health, profiles),
        UpdateBloodPressureCommand::class.java to UpdateBloodPressureHandler(health),
        UpdateGlucoseCommand::class.java to UpdateGlucoseHandler(health),
        UpdateActivityCommand::class.java to UpdateActivityHandler(health),
        UpdateWaistCircumferenceCommand::class.java to UpdateWaistCircumferenceHandler(health, profiles),
        UpdateOxygenSaturationCommand::class.java to UpdateOxygenSaturationHandler(health),
        DeleteEntryCommand::class.java to DeleteEntryHandler(PortEntryGateway(health, health, health, health, health, health, migraines)),
        EndMigraineCommand::class.java to EndMigraineHandler(migraines, clock),
        ImportCsvCommand::class.java to ImportCsvHandler(import, migraineImport)
    ),
    queries = mapOf<Class<*>, QueryHandler<*, *>>(
        GetActiveProfileQuery::class.java to GetActiveProfileHandler(profiles),
        GetHistoryQuery::class.java to GetHistoryHandler(health.healthHistoryUseCase(), migraines),
        ExportCsvQuery::class.java to ExportCsvHandler(export, migraineExport)
    ),
    observers = emptyMap()
)
