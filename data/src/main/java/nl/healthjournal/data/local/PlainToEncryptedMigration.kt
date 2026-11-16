package nl.healthjournal.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import nl.healthjournal.data.local.HealthJournalDatabase.Companion.DATABASE_NAME
import java.io.File
import java.io.FileInputStream

/**
 * Performs a one-time migration from an unencrypted SQLite database to an SQLCipher-encrypted database.
 *
 * Requirements:
 * - Checks whether the database file exists and is plain SQLite (magic header "SQLite format 3\0").
 * - If not plain SQLite (or doesn't exist), skips migration.
 * - Collects row counts per table and database version from the plain database.
 * - Exports the plain database to a temporary encrypted file using sqlcipher_export.
 * - Verifies that the encrypted database has the matching version and row counts.
 * - Keeps the plain file untouched until verification passes.
 * - Only upon successful verification, moves the encrypted database into place. The plain files are
 *   set aside under [BACKUP_SUFFIX] until the swap is complete, so a crash or failure in between
 *   never loses data: a failed swap puts them back at once, a crash leaves them for the next [migrate].
 * - On any failure, removes temporary files, keeps the plain database intact, and returns [MigrationResult.Failed].
 */
class PlainToEncryptedMigration(
    private val context: Context,
    private val keyProvider: DatabaseKeyProvider,
    private val exportStep: (plainPath: String, encPath: String, passphrase: ByteArray, plainVersion: Int) -> Unit = ::defaultExportStep,
    private val verifyStep: (encPath: String, passphrase: ByteArray, expectedCounts: Map<String, Long>, expectedVersion: Int) -> Unit = ::defaultVerifyStep,
    private val rename: (from: File, to: File) -> Boolean = File::renameTo,
    private val delete: (File) -> Boolean = File::delete,
) {

    sealed class MigrationResult {
        data object Success : MigrationResult()
        data object AlreadyEncrypted : MigrationResult()

        /**
         * The encrypted database is in place and verified, but a set-aside plain file could not be deleted.
         * The next [migrate] retries the deletion. Not [Success]: plain health data is still on disk.
         */
        data class CleanupIncomplete(val leftovers: List<String>) : MigrationResult()
        data class Failed(val reason: String) : MigrationResult()
    }

    fun migrate(): MigrationResult {
        val plainFile = context.getDatabasePath(DATABASE_NAME)
        recoverInterruptedSwap(plainFile)
        if (!plainFile.exists()) {
            return MigrationResult.AlreadyEncrypted
        }

        if (!isPlainSqlite(plainFile)) {
            return MigrationResult.AlreadyEncrypted
        }

        val tempEncFile = File(plainFile.parentFile, TEMP_ENC_NAME)
        // Clean up any stale temp file from earlier crashed runs
        cleanupFiles(tempEncFile)

        val plainVersion: Int
        val plainCounts: Map<String, Long>
        try {
            val plainDb = SQLiteDatabase.openDatabase(plainFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            plainDb.use { db ->
                plainVersion = db.version
                plainCounts = collectRowCounts(db)
            }
        } catch (e: Exception) {
            val msg = "Failed to inspect plain database: ${e.message}"
            return MigrationResult.Failed(msg)
        }

        val passphrase: ByteArray
        try {
            passphrase = obtainPassphrase()
        } catch (e: Exception) {
            val msg = "Failed to obtain passphrase from DatabaseKeyProvider: ${e.message}"
            return MigrationResult.Failed(msg)
        }

        try {
            exportStep(plainFile.absolutePath, tempEncFile.absolutePath, passphrase, plainVersion)
            verifyStep(tempEncFile.absolutePath, passphrase, plainCounts, plainVersion)

            // Verification succeeded! Now swap the files; the plain ones stay recoverable until the end.
            val leftovers = swapIn(plainFile, tempEncFile)

            return if (leftovers.isEmpty()) MigrationResult.Success else MigrationResult.CleanupIncomplete(leftovers)
        } catch (e: Exception) {
            val msg = "Migration failed: ${e.message}"
            cleanupFiles(tempEncFile)
            return MigrationResult.Failed(msg)
        } finally {
            passphrase.fill(0)
        }
    }

    /**
     * Sets the plain files aside, moves the encrypted file in, and only then drops the set-aside files.
     * When any step fails, every file already set aside is put back before the failure is reported. When a file
     * cannot be put back the failure names it, and [isSetAside] keeps the app from opening a database over it.
     * Returns the set-aside files that could not be deleted at the end (empty when the cleanup is complete).
     */
    private fun swapIn(plainFile: File, tempEncFile: File): List<String> {
        try {
            setPlainFilesAside(plainFile)
            check(rename(tempEncFile, plainFile)) { "Could not move the encrypted database into place" }
        } catch (e: Exception) {
            val notRestored = restoreBackups(plainFile)
            if (notRestored.isNotEmpty()) {
                throw IllegalStateException("${e.message}; could not restore ${notRestored.joinToString()}", e)
            }
            throw e
        }
        return deleteBackups(plainFile)
    }

    private fun setPlainFilesAside(plainFile: File) {
        val dir = plainFile.parentFile ?: return
        for (suffix in DATABASE_SUFFIXES) {
            val original = File(dir, DATABASE_NAME + suffix)
            if (original.exists()) {
                check(rename(original, backupOf(original))) { "Could not set aside ${original.name}" }
            }
        }
    }

    /**
     * A crash during [swapIn] leaves set-aside files. When the encrypted database made it into place
     * the swap had finished and they are dropped; otherwise they are put back so the migration retries.
     */
    private fun recoverInterruptedSwap(plainFile: File) {
        val dir = plainFile.parentFile ?: return
        if (DATABASE_SUFFIXES.none { backupOf(File(dir, DATABASE_NAME + it)).exists() }) return
        if (plainFile.exists() && !isPlainSqlite(plainFile)) deleteBackups(plainFile) else restoreBackups(plainFile)
    }

    /** Puts every set-aside file back. Returns the names that could not be restored (empty when all were). */
    private fun restoreBackups(plainFile: File): List<String> {
        val dir = plainFile.parentFile ?: return emptyList()
        val failed = mutableListOf<String>()
        for (suffix in DATABASE_SUFFIXES) {
            val original = File(dir, DATABASE_NAME + suffix)
            val backup = backupOf(original)
            if (!backup.exists()) continue
            val clear = !original.exists() || delete(original)
            if (!clear || !rename(backup, original)) failed.add(backup.name)
        }
        return failed
    }

    /** Deletes the set-aside files. Returns the names that could not be deleted (empty when all were). */
    private fun deleteBackups(plainFile: File): List<String> {
        val dir = plainFile.parentFile ?: return emptyList()
        val failed = mutableListOf<String>()
        for (suffix in DATABASE_SUFFIXES) {
            val backup = backupOf(File(dir, DATABASE_NAME + suffix))
            if (backup.exists() && !delete(backup)) failed.add(backup.name)
        }
        return failed
    }

    private fun backupOf(file: File) = File(file.parentFile, file.name + BACKUP_SUFFIX)

    /**
     * The database is still plain here, so stale key material from an earlier install carries no
     * data: when it cannot be unwrapped it is discarded and a fresh key is created.
     */
    private fun obtainPassphrase(): ByteArray = try {
        keyProvider.getOrCreatePassphrase()
    } catch (_: DatabaseKeyLostException) {
        keyProvider.discardKey()
        keyProvider.getOrCreatePassphrase()
    }

    private fun collectRowCounts(db: SQLiteDatabase): Map<String, Long> {
        val counts = mutableMapOf<String, Long>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
            null
        ).use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            for (table in tables) {
                db.rawQuery("SELECT COUNT(*) FROM `$table`", null).use { countCursor ->
                    if (countCursor.moveToFirst()) {
                        counts[table] = countCursor.getLong(0)
                    }
                }
            }
        }
        return counts
    }

    private fun cleanupFiles(baseFile: File) {
        val parent = baseFile.parentFile ?: return
        val name = baseFile.name
        for (suffix in DATABASE_SUFFIXES) {
            val f = File(parent, name + suffix)
            if (f.exists()) {
                f.delete()
            }
        }
    }

    companion object {
        const val TEMP_ENC_NAME = "health_journal_enc.db"
        const val BACKUP_SUFFIX = ".pre-encryption"

        private val DATABASE_SUFFIXES = listOf("", "-journal", "-wal", "-shm")

        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        /**
         * True when a set-aside plain file is still there and the database is not the finished encrypted one:
         * the database file is missing, or it is plain while a set-aside sidecar (-wal, -journal, -shm) was not
         * restored. Nothing may open or create a database then. Leftovers beside a finished encrypted database
         * are only a cleanup to retry, so they do not count.
         */
        fun isSetAside(databaseFile: File): Boolean {
            val dir = databaseFile.parentFile
            val anyAside = DATABASE_SUFFIXES.any { File(dir, databaseFile.name + it + BACKUP_SUFFIX).exists() }
            if (!anyAside) return false
            return !databaseFile.exists() || isPlainSqlite(databaseFile)
        }

        fun isPlainSqlite(file: File): Boolean {
            if (!file.exists() || file.length() < 16) return false
            val header = ByteArray(16)
            return try {
                FileInputStream(file).use { it.read(header) }
                header.contentEquals(SQLITE_HEADER)
            } catch (_: Exception) {
                false
            }
        }
    }
}

private fun defaultExportStep(
    plainPath: String,
    encPath: String,
    passphrase: ByteArray,
    plainVersion: Int
) {
    System.loadLibrary("sqlcipher")
    val enc = net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
        encPath,
        passphrase,
        null,
        null,
        null,
    )
    try {
        enc.rawExecSQL("ATTACH DATABASE '$plainPath' AS plaintext KEY ''")
        enc.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
        enc.version = plainVersion
        enc.rawExecSQL("DETACH DATABASE plaintext")
    } finally {
        enc.close()
    }
}

private fun defaultVerifyStep(
    encPath: String,
    passphrase: ByteArray,
    expectedCounts: Map<String, Long>,
    expectedVersion: Int
) {
    System.loadLibrary("sqlcipher")
    val enc = net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
        encPath,
        passphrase,
        null,
        null,
        null,
    )
    try {
        if (enc.version != expectedVersion) {
            throw IllegalStateException("Database version mismatch: expected $expectedVersion but got ${enc.version}")
        }
        for ((table, expectedCount) in expectedCounts) {
            enc.rawQuery("SELECT COUNT(*) FROM `$table`", null).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalStateException("Table $table not found in encrypted database")
                }
                val actual = cursor.getLong(0)
                if (actual != expectedCount) {
                    throw IllegalStateException("Row count mismatch for $table: expected $expectedCount but got $actual")
                }
            }
        }
    } finally {
        enc.close()
    }
}
