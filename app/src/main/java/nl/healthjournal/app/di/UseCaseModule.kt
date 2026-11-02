package nl.healthjournal.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.healthjournal.app.ui.history.EntryUseCases
import nl.healthjournal.app.ui.medication.MedicationUseCases
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.*

/** The domain has no DI annotations, so each use case is built here from its ports. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun createProfile(profile: ProfileRepositoryPort) = CreateProfileUseCase(profile)

    @Provides
    fun recordWeight(log: HealthLogRepositoryPort, profile: ProfileRepositoryPort) =
        RecordWeightUseCase(log, profile)

    @Provides
    fun recordBloodPressure(log: HealthLogRepositoryPort) = RecordBloodPressureUseCase(log)

    @Provides
    fun recordGlucose(log: HealthLogRepositoryPort) = RecordGlucoseUseCase(log)

    @Provides
    fun recordActivity(log: HealthLogRepositoryPort) = RecordActivityUseCase(log)

    @Provides
    fun recordWaistCircumference(log: HealthLogRepositoryPort, profile: ProfileRepositoryPort) =
        RecordWaistCircumferenceUseCase(log, profile)

    @Provides
    fun entryUseCases(
        log: HealthLogRepositoryPort,
        profile: ProfileRepositoryPort,
        migraine: MigraineRepositoryPort
    ) = EntryUseCases(
        updateWeight = UpdateWeightUseCase(log, profile),
        updateBloodPressure = UpdateBloodPressureUseCase(log),
        updateGlucose = UpdateGlucoseUseCase(log),
        updateActivity = UpdateActivityUseCase(log),
        updateWaistCircumference = UpdateWaistCircumferenceUseCase(log, profile),
        deleteWeight = DeleteWeightUseCase(log),
        deleteBloodPressure = DeleteBloodPressureUseCase(log),
        deleteGlucose = DeleteGlucoseUseCase(log),
        deleteActivity = DeleteActivityUseCase(log),
        deleteWaistCircumference = DeleteWaistCircumferenceUseCase(log),
        deleteMigraine = DeleteMigraineUseCase(migraine),
        endMigraine = EndMigraineUseCase(migraine)
    )

    @Provides
    fun getHealthHistory(log: HealthLogRepositoryPort) = GetHealthHistoryUseCase(log)

    @Provides
    fun medicationUseCases(medication: MedicationRepositoryPort): MedicationUseCases {
        val recordIntake = RecordIntakeUseCase(medication)
        return MedicationUseCases(
            getDay = GetPillboxDayUseCase(medication),
            recordIntake = recordIntake,
            recordSlot = RecordSlotIntakesUseCase(recordIntake),
            save = SaveMedicationUseCase(medication),
            changeSchedule = ChangeMedicationScheduleUseCase(medication),
            archive = ArchiveMedicationUseCase(medication),
            delete = DeleteMedicationUseCase(medication),
            getAdherence = GetAdherenceUseCase(medication)
        )
    }

    @Provides
    fun createBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort) = CreateBackupUseCase(snapshot, codec)

    @Provides
    fun restoreBackup(snapshot: BackupSnapshotPort, codec: BackupCodecPort) = RestoreBackupUseCase(snapshot, codec)
}
