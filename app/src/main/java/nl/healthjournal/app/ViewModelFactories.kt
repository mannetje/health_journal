package nl.healthjournal.app

import android.content.ContentResolver
import android.content.Context
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.backup.ContentResolverBackupFileIo
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.domain.usecase.RestoreBackupUseCase

// What each ViewModel is built from. They take the Application's objects only, so no ViewModel can capture the Activity.

internal fun HealthJournalApp.profileViewModelFactory() = ProfileViewModel.Factory(
    profileRepository,
    createProfileUseCase,
    metricDisplayOrderPreference.order,
    metricDisplayOrderPreference::save
)

internal fun HealthJournalApp.loggingViewModelFactory() = LoggingViewModel.Factory(
    profileRepository,
    recordWeightUseCase,
    recordBloodPressureUseCase,
    recordGlucoseUseCase,
    recordActivityUseCase,
    recordWaistCircumferenceUseCase,
    healthLogRepository,
    units = { unitPreference.resolve() },
    metricOrder = metricDisplayOrderPreference.order
)

internal fun HealthJournalApp.migraineViewModelFactory() = MigraineLogViewModel.Factory(profileRepository, migraineRepository)

internal fun HealthJournalApp.historyViewModelFactory() = HistoryViewModel.Factory(
    profileRepository,
    getHealthHistoryUseCase,
    migraineRepository,
    dataExportAdapter,
    dataImportAdapter,
    migraineExportAdapter,
    migraineImportAdapter,
    entryUseCases,
    metricDisplayOrderPreference.order
)

internal fun HealthJournalApp.medicationViewModelFactory() =
    MedicationViewModel.Factory(profileRepository, medicationRepository, medicationUseCases, reminders)

internal fun HealthJournalApp.backupViewModelFactory(contentResolver: ContentResolver) = BackupViewModel.Factory(
    create = createBackupUseCase,
    restore = restoreBackupUseCase,
    io = ContentResolverBackupFileIo(contentResolver),
    preferences = { backupPreferences.collect() },
    applyPreferences = { backupPreferences.apply(it) },
    appVersion = BuildConfig.VERSION_NAME
)

/** Restore on the lost-key screen: the merge deletes the unreadable data first, into a fresh database. */
internal fun HealthJournalApp.recoveryBackupViewModelFactory(context: Context) = BackupViewModel.Factory(
    create = null,
    restore = RestoreBackupUseCase(FreshDatabaseBackupSnapshot(context), backupCodec),
    io = ContentResolverBackupFileIo(context.contentResolver),
    preferences = { null },
    applyPreferences = { backupPreferences.apply(it) },
    appVersion = BuildConfig.VERSION_NAME
)
