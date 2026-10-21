package nl.healthjournal.data.local

/** What [HealthJournalDatabase.create] found when it opened the database at app start. */
sealed interface EncryptionState {
    /** The database is encrypted with SQLCipher. */
    data object Active : EncryptionState

    /** The database is still plain SQLite. [reason] says why; the app retries at the next start. */
    data class NotActive(val reason: String) : EncryptionState

    /** The database is encrypted but its key is gone, so the data cannot be read. */
    data object KeyLost : EncryptionState
}

/** Process-wide result of the last database open, read by the UI. Holds no health data. */
object DatabaseEncryptionStatus {
    @Volatile
    var state: EncryptionState = EncryptionState.Active
        internal set
}

/** The wrapped database passphrase can no longer be unwrapped (Keystore key missing or invalidated). */
class DatabaseKeyLostException(message: String, cause: Throwable? = null) : Exception(message, cause)
