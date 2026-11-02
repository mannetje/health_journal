package nl.healthjournal.app.ui.backup

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import nl.healthjournal.app.BuildConfig
import nl.healthjournal.app.settings.BackupPreferenceBridge
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import javax.inject.Inject

/**
 * Restore on the lost-key screen: the merge deletes the unreadable data first, into a fresh database. A type of
 * its own, so the Activity holds it next to [BackupViewModel] without the two sharing one instance.
 */
@HiltViewModel
class RecoveryBackupViewModel @Inject constructor(
    codec: BackupCodecPort,
    @ApplicationContext context: Context,
    backupPreferences: BackupPreferenceBridge
) : BackupViewModel(
    create = null,
    restore = RestoreBackupUseCase(FreshDatabaseBackupSnapshot(context), codec),
    io = ContentResolverBackupFileIo(context.contentResolver),
    preferences = { null },
    applyPreferences = { backupPreferences.apply(it) },
    appVersion = BuildConfig.VERSION_NAME
)
