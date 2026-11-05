package nl.healthjournal.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.reminder.ReminderCoordinator
import nl.healthjournal.app.settings.BackupPreferenceBridge
import nl.healthjournal.app.settings.MetricDisplayOrderPreference
import nl.healthjournal.app.settings.ReminderPreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.data.DataModule
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort
import nl.healthjournal.domain.port.secondary.DataExportPort
import nl.healthjournal.domain.port.secondary.DataImportPort
import nl.healthjournal.domain.port.secondary.MedicationRepositoryPort
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineImportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort
import nl.healthjournal.domain.port.secondary.ProfileRepositoryPort
import javax.inject.Inject

/**
 * The Hilt application. The object graph lives in `di/`; the properties below still expose it to the code that has not
 * moved to injection yet (ViewModel factories, receivers, Activity wiring).
 */
@HiltAndroidApp
class HealthJournalApp : Application() {

    @Inject lateinit var dataModule: DataModule

    val profileRepository: ProfileRepositoryPort
        get() = dataModule.profileRepository

    val medicationRepository: MedicationRepositoryPort
        get() = dataModule.medicationRepository

    val migraineRepository: MigraineRepositoryPort
        get() = dataModule.migraineRepository

    val dataExportAdapter: DataExportPort
        get() = dataModule.dataExportPort

    val dataImportAdapter: DataImportPort
        get() = dataModule.dataImportPort

    val migraineExportAdapter: MigraineExportPort
        get() = dataModule.migraineExportPort

    val migraineImportAdapter: MigraineImportPort
        get() = dataModule.migraineImportPort

    val backupCodec: BackupCodecPort
        get() = dataModule.backupCodec

    val backupSnapshot: BackupSnapshotPort
        get() = dataModule.backupSnapshotPort

    // Preferences

    @Inject lateinit var metricDisplayOrderPreference: MetricDisplayOrderPreference
    @Inject lateinit var reminderPreference: ReminderPreference
    @Inject lateinit var unitPreference: UnitPreference
    @Inject lateinit var backupPreferences: BackupPreferenceBridge

    /** Owned by the process, not the Activity: the lock must survive an Activity being recreated. */
    @Inject lateinit var appLock: AppLockManager
    @Inject lateinit var reminders: ReminderCoordinator

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: HealthJournalApp
            private set
    }
}
