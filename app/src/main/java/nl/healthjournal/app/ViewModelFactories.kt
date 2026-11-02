package nl.healthjournal.app

import android.content.ContentResolver
import android.content.Context
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.backup.ContentResolverBackupFileIo
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.domain.usecase.RestoreBackupUseCase

// What each ViewModel is built from. They take the Application's objects only, so no ViewModel can capture the Activity.

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
