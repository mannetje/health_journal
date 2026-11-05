package nl.healthjournal.app.command

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds
import java.time.Clock
import nl.healthjournal.domain.backup.command.CreateBackupCommand
import nl.healthjournal.domain.backup.command.CreateBackupHandler
import nl.healthjournal.domain.backup.command.OpenBackupHandler
import nl.healthjournal.domain.backup.command.OpenBackupQuery
import nl.healthjournal.domain.backup.command.RestoreBackupCommand
import nl.healthjournal.domain.backup.command.RestoreBackupHandler
import nl.healthjournal.domain.shared.command.CommandHandler
import nl.healthjournal.domain.shared.command.DeleteEntryCommand
import nl.healthjournal.domain.shared.command.DeleteEntryHandler
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.EntryGateway
import nl.healthjournal.domain.shared.command.ExportCsvHandler
import nl.healthjournal.domain.shared.command.ExportCsvQuery
import nl.healthjournal.domain.shared.command.GetHistoryHandler
import nl.healthjournal.domain.shared.command.GetHistoryQuery
import nl.healthjournal.domain.shared.command.ImportCsvCommand
import nl.healthjournal.domain.shared.command.ImportCsvHandler
import nl.healthjournal.domain.shared.command.ObserveQueryHandler
import nl.healthjournal.domain.shared.command.PortEntryGateway
import nl.healthjournal.domain.shared.command.QueryHandler
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
import nl.healthjournal.domain.medication.command.GetMedicationHandler
import nl.healthjournal.domain.medication.command.GetMedicationQuery
import nl.healthjournal.domain.medication.command.ListMedicationsHandler
import nl.healthjournal.domain.medication.command.ListMedicationsQuery
import nl.healthjournal.domain.medication.command.ArchiveMedicationCommand
import nl.healthjournal.domain.medication.command.ArchiveMedicationHandler
import nl.healthjournal.domain.medication.command.ChangeMedicationScheduleCommand
import nl.healthjournal.domain.medication.command.ChangeMedicationScheduleHandler
import nl.healthjournal.domain.medication.command.DeleteMedicationCommand
import nl.healthjournal.domain.medication.command.DeleteMedicationHandler
import nl.healthjournal.domain.medication.command.GetAdherenceHandler
import nl.healthjournal.domain.medication.command.GetAdherenceQuery
import nl.healthjournal.domain.medication.command.GetPillboxDayHandler
import nl.healthjournal.domain.medication.command.GetPillboxDayQuery
import nl.healthjournal.domain.medication.command.RecordIntakeCommand
import nl.healthjournal.domain.medication.command.RecordIntakeHandler
import nl.healthjournal.domain.medication.command.RecordSlotIntakesCommand
import nl.healthjournal.domain.medication.command.RecordSlotIntakesHandler
import nl.healthjournal.domain.medication.command.SaveMedicationCommand
import nl.healthjournal.domain.medication.command.SaveMedicationHandler
import nl.healthjournal.domain.medication.command.TakeAllForSlotCommand
import nl.healthjournal.domain.medication.command.TakeAllForSlotHandler
import nl.healthjournal.domain.migraine.command.AddMigraineTagCommand
import nl.healthjournal.domain.migraine.command.AddMigraineTagHandler
import nl.healthjournal.domain.migraine.command.ArchiveMigraineTagCommand
import nl.healthjournal.domain.migraine.command.ArchiveMigraineTagHandler
import nl.healthjournal.domain.migraine.command.EndMigraineCommand
import nl.healthjournal.domain.migraine.command.EndMigraineHandler
import nl.healthjournal.domain.migraine.command.ObserveMigraineHistoryHandler
import nl.healthjournal.domain.migraine.command.ObserveMigraineHistoryQuery
import nl.healthjournal.domain.migraine.command.ObserveMigraineTagsHandler
import nl.healthjournal.domain.migraine.command.ObserveMigraineTagsQuery
import nl.healthjournal.domain.migraine.command.RecordMigraineCommand
import nl.healthjournal.domain.migraine.command.RecordMigraineHandler
import nl.healthjournal.domain.migraine.command.RenameMigraineTagCommand
import nl.healthjournal.domain.migraine.command.RenameMigraineTagHandler
import nl.healthjournal.domain.migraine.command.UpdateMigraineCommand
import nl.healthjournal.domain.migraine.command.UpdateMigraineHandler
import nl.healthjournal.domain.metrics.port.ActivityRepositoryPort
import nl.healthjournal.domain.backup.port.BackupCodecPort
import nl.healthjournal.domain.backup.port.BackupSnapshotPort
import nl.healthjournal.domain.metrics.port.BloodPressureRepositoryPort
import nl.healthjournal.domain.metrics.port.DataExportPort
import nl.healthjournal.domain.metrics.port.DataImportPort
import nl.healthjournal.domain.metrics.port.GlucoseRepositoryPort
import nl.healthjournal.domain.medication.port.MedicationRepositoryPort
import nl.healthjournal.domain.migraine.port.MigraineExportPort
import nl.healthjournal.domain.migraine.port.MigraineImportPort
import nl.healthjournal.domain.migraine.port.MigraineRepositoryPort
import nl.healthjournal.domain.metrics.port.OxygenSaturationRepositoryPort
import nl.healthjournal.domain.profile.port.ProfileRepositoryPort
import nl.healthjournal.domain.metrics.port.WaistCircumferenceRepositoryPort
import nl.healthjournal.domain.metrics.port.WeightRepositoryPort
import nl.healthjournal.domain.profile.command.GetActiveProfileHandler
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.profile.command.SaveProfileCommand
import nl.healthjournal.domain.profile.command.SaveProfileHandler
import nl.healthjournal.domain.migraine.usecase.AddMigraineTagUseCase
import nl.healthjournal.domain.medication.usecase.ArchiveMedicationUseCase
import nl.healthjournal.domain.migraine.usecase.ArchiveMigraineTagUseCase
import nl.healthjournal.domain.medication.usecase.ChangeMedicationScheduleUseCase
import nl.healthjournal.domain.backup.usecase.CreateBackupUseCase
import nl.healthjournal.domain.medication.usecase.DeleteMedicationUseCase
import nl.healthjournal.domain.medication.usecase.GetAdherenceUseCase
import nl.healthjournal.domain.metrics.usecase.GetHealthHistoryUseCase
import nl.healthjournal.domain.medication.usecase.GetPillboxDayUseCase
import nl.healthjournal.domain.medication.usecase.RecordIntakeUseCase
import nl.healthjournal.domain.medication.usecase.RecordSlotIntakesUseCase
import nl.healthjournal.domain.migraine.usecase.RenameMigraineTagUseCase
import nl.healthjournal.domain.backup.usecase.RestoreBackupUseCase
import nl.healthjournal.domain.medication.usecase.SaveMedicationUseCase
import nl.healthjournal.domain.medication.usecase.TakeAllForSlotUseCase

/** Declares the handler maps, so they exist (empty) even before a handler of that kind is registered. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommandMapsModule {
    @Multibinds abstract fun commandHandlers(): Map<Class<*>, @JvmSuppressWildcards CommandHandler<*, *>>
    @Multibinds abstract fun queryHandlers(): Map<Class<*>, @JvmSuppressWildcards QueryHandler<*, *>>
    @Multibinds abstract fun observeHandlers(): Map<Class<*>, @JvmSuppressWildcards ObserveQueryHandler<*, *>>

    @Binds abstract fun dispatcher(impl: DefaultDispatcher): Dispatcher
}

/** One `@Provides @IntoMap` per handler: the key is the command class, the body is the constructor call. */
@Module
@InstallIn(SingletonComponent::class)
object CommandModule {

    @Provides
    fun entryGateway(
        weight: WeightRepositoryPort,
        bloodPressure: BloodPressureRepositoryPort,
        glucose: GlucoseRepositoryPort,
        activity: ActivityRepositoryPort,
        waist: WaistCircumferenceRepositoryPort,
        oxygen: OxygenSaturationRepositoryPort,
        migraine: MigraineRepositoryPort
    ): EntryGateway = PortEntryGateway(weight, bloodPressure, glucose, activity, waist, oxygen, migraine)

    @Provides @IntoMap @ClassKey(DeleteEntryCommand::class)
    fun deleteEntry(gateway: EntryGateway): CommandHandler<*, *> = DeleteEntryHandler(gateway)

    @Provides @IntoMap @ClassKey(RecordWeightCommand::class)
    fun recordWeight(weight: WeightRepositoryPort, profiles: ProfileRepositoryPort, clock: Clock): CommandHandler<*, *> = RecordWeightHandler(weight, profiles, clock)

    @Provides @IntoMap @ClassKey(UpdateWeightCommand::class)
    fun updateWeight(weight: WeightRepositoryPort, profiles: ProfileRepositoryPort): CommandHandler<*, *> = UpdateWeightHandler(weight, profiles)

    @Provides @IntoMap @ClassKey(RecordBloodPressureCommand::class)
    fun recordBloodPressure(bloodPressure: BloodPressureRepositoryPort, clock: Clock): CommandHandler<*, *> = RecordBloodPressureHandler(bloodPressure, clock)

    @Provides @IntoMap @ClassKey(UpdateBloodPressureCommand::class)
    fun updateBloodPressure(bloodPressure: BloodPressureRepositoryPort): CommandHandler<*, *> = UpdateBloodPressureHandler(bloodPressure)

    @Provides @IntoMap @ClassKey(RecordGlucoseCommand::class)
    fun recordGlucose(glucose: GlucoseRepositoryPort, clock: Clock): CommandHandler<*, *> = RecordGlucoseHandler(glucose, clock)

    @Provides @IntoMap @ClassKey(UpdateGlucoseCommand::class)
    fun updateGlucose(glucose: GlucoseRepositoryPort): CommandHandler<*, *> = UpdateGlucoseHandler(glucose)

    @Provides @IntoMap @ClassKey(RecordActivityCommand::class)
    fun recordActivity(activity: ActivityRepositoryPort): CommandHandler<*, *> = RecordActivityHandler(activity)

    @Provides @IntoMap @ClassKey(UpdateActivityCommand::class)
    fun updateActivity(activity: ActivityRepositoryPort): CommandHandler<*, *> = UpdateActivityHandler(activity)

    @Provides @IntoMap @ClassKey(RecordWaistCircumferenceCommand::class)
    fun recordWaistCircumference(waist: WaistCircumferenceRepositoryPort, profiles: ProfileRepositoryPort, clock: Clock): CommandHandler<*, *> = RecordWaistCircumferenceHandler(waist, profiles, clock)

    @Provides @IntoMap @ClassKey(UpdateWaistCircumferenceCommand::class)
    fun updateWaistCircumference(waist: WaistCircumferenceRepositoryPort, profiles: ProfileRepositoryPort): CommandHandler<*, *> = UpdateWaistCircumferenceHandler(waist, profiles)

    @Provides @IntoMap @ClassKey(RecordOxygenSaturationCommand::class)
    fun recordOxygenSaturation(oxygen: OxygenSaturationRepositoryPort, clock: Clock): CommandHandler<*, *> = RecordOxygenSaturationHandler(oxygen, clock)

    @Provides @IntoMap @ClassKey(UpdateOxygenSaturationCommand::class)
    fun updateOxygenSaturation(oxygen: OxygenSaturationRepositoryPort): CommandHandler<*, *> = UpdateOxygenSaturationHandler(oxygen)

    @Provides @IntoMap @ClassKey(ExportCsvQuery::class)
    fun exportCsv(export: DataExportPort, migraine: MigraineExportPort): QueryHandler<*, *> =
        ExportCsvHandler(export, migraine)

    @Provides @IntoMap @ClassKey(ImportCsvCommand::class)
    fun importCsv(import: DataImportPort, migraine: MigraineImportPort): CommandHandler<*, *> =
        ImportCsvHandler(import, migraine)

    @Provides @IntoMap @ClassKey(GetActiveProfileQuery::class)
    fun getActiveProfile(profiles: ProfileRepositoryPort): QueryHandler<*, *> = GetActiveProfileHandler(profiles)

    @Provides @IntoMap @ClassKey(SaveProfileCommand::class)
    fun saveProfile(profiles: ProfileRepositoryPort, clock: Clock): CommandHandler<*, *> =
        SaveProfileHandler(profiles, clock)

    @Provides @IntoMap @ClassKey(GetHistoryQuery::class)
    fun getHistory(health: GetHealthHistoryUseCase, migraines: MigraineRepositoryPort): QueryHandler<*, *> =
        GetHistoryHandler(health, migraines)

    @Provides @IntoMap @ClassKey(ObserveMigraineHistoryQuery::class)
    fun observeMigraineHistory(migraines: MigraineRepositoryPort): ObserveQueryHandler<*, *> =
        ObserveMigraineHistoryHandler(migraines)

    @Provides @IntoMap @ClassKey(ObserveMigraineTagsQuery::class)
    fun observeMigraineTags(migraines: MigraineRepositoryPort): ObserveQueryHandler<*, *> =
        ObserveMigraineTagsHandler(migraines)

    // ---- migraine ----

    @Provides @IntoMap @ClassKey(RecordMigraineCommand::class)
    fun recordMigraine(migraines: MigraineRepositoryPort): CommandHandler<*, *> = RecordMigraineHandler(migraines)

    @Provides @IntoMap @ClassKey(UpdateMigraineCommand::class)
    fun updateMigraine(migraines: MigraineRepositoryPort): CommandHandler<*, *> = UpdateMigraineHandler(migraines)

    @Provides @IntoMap @ClassKey(EndMigraineCommand::class)
    fun endMigraine(migraines: MigraineRepositoryPort, clock: Clock): CommandHandler<*, *> =
        EndMigraineHandler(migraines, clock)

    @Provides @IntoMap @ClassKey(AddMigraineTagCommand::class)
    fun addMigraineTag(migraines: MigraineRepositoryPort): CommandHandler<*, *> =
        AddMigraineTagHandler(AddMigraineTagUseCase(migraines))

    @Provides @IntoMap @ClassKey(RenameMigraineTagCommand::class)
    fun renameMigraineTag(migraines: MigraineRepositoryPort): CommandHandler<*, *> =
        RenameMigraineTagHandler(RenameMigraineTagUseCase(migraines))

    @Provides @IntoMap @ClassKey(ArchiveMigraineTagCommand::class)
    fun archiveMigraineTag(migraines: MigraineRepositoryPort): CommandHandler<*, *> =
        ArchiveMigraineTagHandler(ArchiveMigraineTagUseCase(migraines))

    // ---- medication ----
    // The pillbox and adherence read "now" in the person's own time zone, so they use the system-default
    // clock; the recorded instants (taken at) come from the app clock.

    @Provides @IntoMap @ClassKey(ListMedicationsQuery::class)
    fun listMedications(medication: MedicationRepositoryPort): QueryHandler<*, *> = ListMedicationsHandler(medication)

    @Provides @IntoMap @ClassKey(GetMedicationQuery::class)
    fun getMedication(medication: MedicationRepositoryPort): QueryHandler<*, *> = GetMedicationHandler(medication)

    @Provides @IntoMap @ClassKey(GetPillboxDayQuery::class)
    fun getPillboxDay(medication: MedicationRepositoryPort): QueryHandler<*, *> =
        GetPillboxDayHandler(GetPillboxDayUseCase(medication, Clock.systemDefaultZone()))

    @Provides @IntoMap @ClassKey(GetAdherenceQuery::class)
    fun getAdherence(medication: MedicationRepositoryPort): QueryHandler<*, *> =
        GetAdherenceHandler(GetAdherenceUseCase(medication, Clock.systemDefaultZone()))

    @Provides @IntoMap @ClassKey(SaveMedicationCommand::class)
    fun saveMedication(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> =
        SaveMedicationHandler(SaveMedicationUseCase(medication), localClock(clock))

    @Provides @IntoMap @ClassKey(ChangeMedicationScheduleCommand::class)
    fun changeMedicationSchedule(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> =
        ChangeMedicationScheduleHandler(medication, ChangeMedicationScheduleUseCase(medication), localClock(clock))

    @Provides @IntoMap @ClassKey(ArchiveMedicationCommand::class)
    fun archiveMedication(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> =
        ArchiveMedicationHandler(medication, ArchiveMedicationUseCase(medication), localClock(clock))

    @Provides @IntoMap @ClassKey(DeleteMedicationCommand::class)
    fun deleteMedication(medication: MedicationRepositoryPort): CommandHandler<*, *> =
        DeleteMedicationHandler(medication, DeleteMedicationUseCase(medication))

    @Provides @IntoMap @ClassKey(RecordIntakeCommand::class)
    fun recordIntake(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> =
        RecordIntakeHandler(medication, RecordIntakeUseCase(medication, clock))

    @Provides @IntoMap @ClassKey(RecordSlotIntakesCommand::class)
    fun recordSlotIntakes(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> =
        RecordSlotIntakesHandler(medication, RecordSlotIntakesUseCase(RecordIntakeUseCase(medication, clock), clock))

    @Provides @IntoMap @ClassKey(TakeAllForSlotCommand::class)
    fun takeAllForSlot(medication: MedicationRepositoryPort, clock: Clock): CommandHandler<*, *> {
        val local = Clock.systemDefaultZone()
        val recordSlot = RecordSlotIntakesUseCase(RecordIntakeUseCase(medication, clock), clock)
        return TakeAllForSlotHandler(TakeAllForSlotUseCase(GetPillboxDayUseCase(medication, local), recordSlot, local))
    }

    // ---- backup ----

    @Provides @IntoMap @ClassKey(CreateBackupCommand::class)
    fun createBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort): CommandHandler<*, *> =
        CreateBackupHandler(CreateBackupUseCase(snapshot, codec))

    @Provides @IntoMap @ClassKey(OpenBackupQuery::class)
    fun openBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort): QueryHandler<*, *> =
        OpenBackupHandler(RestoreBackupUseCase(snapshot, codec))

    @Provides @IntoMap @ClassKey(RestoreBackupCommand::class)
    fun restoreBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort): CommandHandler<*, *> =
        RestoreBackupHandler(RestoreBackupUseCase(snapshot, codec))

    /** A date "today" is the person's today, not UTC's: same instant source, local zone. */
    private fun localClock(clock: Clock): Clock = clock.withZone(Clock.systemDefaultZone().zone)
}
