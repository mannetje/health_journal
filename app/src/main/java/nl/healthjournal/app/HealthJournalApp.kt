package nl.healthjournal.app

import android.app.Application
import nl.healthjournal.app.ui.history.EntryUseCases
import nl.healthjournal.app.ui.medication.MedicationUseCases
import nl.healthjournal.data.DataModule
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.HealthLogRepositoryPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import nl.healthjournal.domain.usecase.*

class HealthJournalApp : Application() {

    private lateinit var dataModule: DataModule

    val profileRepository: ProfileRepositoryPort
        get() = dataModule.profileRepository

    val healthLogRepository: HealthLogRepositoryPort
        get() = dataModule.healthLogRepository

    val medicationRepository: MedicationRepositoryPort
        get() = dataModule.medicationRepository

    val dataExportAdapter: DataExportPort
        get() = dataModule.dataExportPort

    val dataImportAdapter: DataImportPort
        get() = dataModule.dataImportPort

    // Use cases
    lateinit var createProfileUseCase: CreateProfileUseCase
        private set
    lateinit var recordWeightUseCase: RecordWeightUseCase
        private set
    lateinit var recordBloodPressureUseCase: RecordBloodPressureUseCase
        private set
    lateinit var recordGlucoseUseCase: RecordGlucoseUseCase
        private set
    lateinit var recordActivityUseCase: RecordActivityUseCase
        private set
    lateinit var recordWaistCircumferenceUseCase: RecordWaistCircumferenceUseCase
        private set
    lateinit var entryUseCases: EntryUseCases
        private set
    lateinit var getHealthHistoryUseCase: GetHealthHistoryUseCase
        private set
    lateinit var medicationUseCases: MedicationUseCases
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        dataModule = DataModule(this)

        createProfileUseCase = CreateProfileUseCase(profileRepository)
        recordWeightUseCase = RecordWeightUseCase(healthLogRepository, profileRepository)
        recordBloodPressureUseCase = RecordBloodPressureUseCase(healthLogRepository)
        recordGlucoseUseCase = RecordGlucoseUseCase(healthLogRepository)
        recordActivityUseCase = RecordActivityUseCase(healthLogRepository)
        recordWaistCircumferenceUseCase = RecordWaistCircumferenceUseCase(healthLogRepository, profileRepository)
        entryUseCases = EntryUseCases(
            updateWeight = UpdateWeightUseCase(healthLogRepository, profileRepository),
            updateBloodPressure = UpdateBloodPressureUseCase(healthLogRepository),
            updateGlucose = UpdateGlucoseUseCase(healthLogRepository),
            updateActivity = UpdateActivityUseCase(healthLogRepository),
            updateWaistCircumference = UpdateWaistCircumferenceUseCase(healthLogRepository, profileRepository),
            deleteWeight = DeleteWeightUseCase(healthLogRepository),
            deleteBloodPressure = DeleteBloodPressureUseCase(healthLogRepository),
            deleteGlucose = DeleteGlucoseUseCase(healthLogRepository),
            deleteActivity = DeleteActivityUseCase(healthLogRepository),
            deleteWaistCircumference = DeleteWaistCircumferenceUseCase(healthLogRepository)
        )
        getHealthHistoryUseCase = GetHealthHistoryUseCase(healthLogRepository)
        val recordIntake = RecordIntakeUseCase(medicationRepository)
        medicationUseCases = MedicationUseCases(
            getDay = GetPillboxDayUseCase(medicationRepository),
            recordIntake = recordIntake,
            recordSlot = RecordSlotIntakesUseCase(recordIntake),
            save = SaveMedicationUseCase(medicationRepository),
            changeSchedule = ChangeMedicationScheduleUseCase(medicationRepository),
            archive = ArchiveMedicationUseCase(medicationRepository),
            delete = DeleteMedicationUseCase(medicationRepository)
        )
    }

    companion object {
        lateinit var instance: HealthJournalApp
            private set
    }
}
