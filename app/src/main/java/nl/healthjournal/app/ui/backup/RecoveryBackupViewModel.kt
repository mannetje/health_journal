package nl.healthjournal.app.ui.backup

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import nl.healthjournal.app.BuildConfig
import nl.healthjournal.app.command.RecoveryDispatcher
import nl.healthjournal.app.settings.BackupPreferenceBridge
import javax.inject.Inject

/**
 * Restore on the lost-key screen: the merge deletes the unreadable data first, into a fresh database. A type of
 * its own, so the Activity holds it next to [BackupViewModel] without the two sharing one instance.
 */
@HiltViewModel
class RecoveryBackupViewModel @Inject constructor(
    dispatcher: RecoveryDispatcher,
    @ApplicationContext context: Context,
    backupPreferences: BackupPreferenceBridge
) : BackupViewModel(
    dispatcher = dispatcher,
    canCreate = false,
    io = ContentResolverBackupFileIo(context.contentResolver),
    preferences = { null },
    applyPreferences = { backupPreferences.apply(it) },
    appVersion = BuildConfig.VERSION_NAME
)
