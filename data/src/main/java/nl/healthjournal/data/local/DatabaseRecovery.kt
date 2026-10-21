package nl.healthjournal.data.local

import android.content.Context
import java.io.File

/** Last-resort recovery for [EncryptionState.KeyLost]: removes the unreadable database and its key material. */
object DatabaseRecovery {

    /** Deletes the encrypted database (and its journal files) and the key files. This cannot be undone. */
    fun deleteUnreadableData(context: Context) {
        val dbFile = context.getDatabasePath(HealthJournalDatabase.DATABASE_NAME)
        for (suffix in listOf("", "-journal", "-wal", "-shm")) {
            File(dbFile.parentFile, HealthJournalDatabase.DATABASE_NAME + suffix).delete()
        }
        DatabaseKeyProvider(context).discardKey()
    }
}
