package nl.healthjournal.app

import nl.healthjournal.data.local.HealthJournalDatabase
import nl.healthjournal.data.local.PlainToEncryptedMigration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The database is kept out of Android's cloud backup and device transfer. The plain-to-encrypted migration
 * briefly (or, after a crash, for longer) leaves plain copies under other names, and its temporary encrypted
 * file; those must be excluded too, or a plain health database could be uploaded.
 */
class BackupRulesTest {

    private val sidecars = listOf("", "-journal", "-wal", "-shm")

    private val mustBeExcluded: Set<String> = buildSet {
        for (suffix in sidecars) {
            add(HealthJournalDatabase.DATABASE_NAME + suffix)
            add(HealthJournalDatabase.DATABASE_NAME + suffix + PlainToEncryptedMigration.BACKUP_SUFFIX)
            add(PlainToEncryptedMigration.TEMP_ENC_NAME + suffix)
        }
    }

    private fun resource(name: String): String {
        // Unit tests run with the module directory as working directory.
        val file = File("src/main/res/xml/$name")
        assertTrue("${file.absolutePath} must exist", file.exists())
        return file.readText()
    }

    private fun excludedDatabaseFiles(xml: String): Set<String> =
        Regex("""<exclude\s+domain="database"\s+path="([^"]+)"\s*/>""").findAll(xml).map { it.groupValues[1] }.toSet()

    @Test
    fun full_backup_rules_exclude_every_database_file_the_migration_can_leave() {
        assertEquals(emptySet<String>(), mustBeExcluded - excludedDatabaseFiles(resource("backup_rules.xml")))
    }

    @Test
    fun cloud_backup_and_device_transfer_exclude_every_database_file_the_migration_can_leave() {
        val xml = resource("data_extraction_rules.xml")
        for (section in listOf("cloud-backup", "device-transfer")) {
            val body = xml.substringAfter("<$section>").substringBefore("</$section>")
            assertEquals(section, emptySet<String>(), mustBeExcluded - excludedDatabaseFiles(body))
        }
    }
}
