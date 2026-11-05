package nl.healthjournal.app.command

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.domain.backup.command.OpenBackupHandler
import nl.healthjournal.domain.backup.command.OpenBackupQuery
import nl.healthjournal.domain.backup.command.RestoreBackupCommand
import nl.healthjournal.domain.backup.command.RestoreBackupHandler
import nl.healthjournal.domain.command.CommandHandler
import nl.healthjournal.domain.command.Dispatcher
import nl.healthjournal.domain.command.QueryHandler
import nl.healthjournal.domain.port.secondary.BackupCodecPort
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import javax.inject.Inject

/**
 * The dispatcher of the lost-key screen: only open and restore, and the restore merges into a fresh database,
 * because the normal one cannot be read. It lives here so no ViewModel has to build ports and use cases.
 */
class RecoveryDispatcher @Inject constructor(
    codec: BackupCodecPort,
    @ApplicationContext context: Context
) : Dispatcher by recoveryDispatcher(RestoreBackupUseCase(FreshDatabaseBackupSnapshot(context), codec))

private fun recoveryDispatcher(restore: RestoreBackupUseCase): Dispatcher = DefaultDispatcher(
    commands = mapOf<Class<*>, CommandHandler<*, *>>(RestoreBackupCommand::class.java to RestoreBackupHandler(restore)),
    queries = mapOf<Class<*>, QueryHandler<*, *>>(OpenBackupQuery::class.java to OpenBackupHandler(restore)),
    observers = emptyMap()
)
