package nl.healthjournal.data.backup

import android.content.Context
import nl.healthjournal.data.local.DatabaseRecovery
import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.backup.MergeResult
import nl.healthjournal.domain.port.secondary.BackupSnapshotPort

/**
 * The lost-key restore target: the current data cannot be read, so [merge] first removes it together with the
 * key material, then creates a fresh key and database and imports into that. The caller has already decrypted
 * and validated the backup in memory, so nothing is deleted for a file that cannot be read. The process is
 * expected to restart afterwards.
 */
class FreshDatabaseBackupSnapshot(private val context: Context) : BackupSnapshotPort {

    override suspend fun snapshot(): List<BackupProfile> = emptyList()

    override suspend fun merge(profiles: List<BackupProfile>): MergeResult {
        DatabaseRecovery.deleteUnreadableData(context)
        val fresh = HealthJournalDatabase.create(context)
        try {
            return RoomBackupSnapshot(fresh).merge(profiles)
        } finally {
            fresh.close()
        }
    }
}
