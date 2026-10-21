package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level privacy checks. They read the files directly, so they run as plain unit tests. */
class PrivacyChecksTest {

    private val moduleRoots = listOf("src/main", "../data/src/main", "../domain/src/main")

    @Test
    fun manifestDoesNotRequestInternet() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse("INTERNET must stay out of the manifest", manifest.contains("android.permission.INTERNET"))
    }

    @Test
    fun manifestAsksOnlyForVibrationAmongTheMindfulnessPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue("VIBRATE is used for the optional haptic cues", manifest.contains("android.permission.VIBRATE"))
        assertFalse("Spoken cues use the device voice; no audio recording is needed", manifest.contains("RECORD_AUDIO"))
        assertFalse("Cues never change do-not-disturb", manifest.contains("ACCESS_NOTIFICATION_POLICY"))
    }

    @Test
    fun noLoggingInProductionSources() {
        val offenders = moduleRoots.flatMap { root ->
            File(root).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }.filter { file ->
            // The only allowed logger: it writes technical encryption-failure reasons, never health data.
            file.name != "HealthJournalDatabase.kt"
        }.filter { file ->
            file.readLines().any { line ->
                val code = line.trim()
                !code.startsWith("//") && !code.startsWith("*") &&
                    (code.contains("android.util.Log") || Regex("""\bLog\.[dviwe]\(""").containsMatchIn(code) ||
                        Regex("""\bprintln\(""").containsMatchIn(code) || code.contains("printStackTrace"))
            }
        }
        assertTrue("Logging can leak names and doses: ${offenders.map { it.path }}", offenders.isEmpty())
    }

    @Test
    fun backupRulesExcludeEncryptedDatabaseAndKeyMaterial() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue("Manifest must configure dataExtractionRules", manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue("Manifest must configure fullBackupContent", manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue("Manifest must keep allowBackup=true", manifest.contains("android:allowBackup=\"true\""))

        val dataExtractionRules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue("data_extraction_rules must exclude database from cloud-backup",
            dataExtractionRules.contains("<exclude domain=\"database\" path=\"health_journal.db\""))
        assertTrue("data_extraction_rules must exclude no_backup key material from cloud-backup",
            dataExtractionRules.contains("<exclude domain=\"root\" path=\"no_backup\""))
        assertTrue("data_extraction_rules must exclude database from device-transfer",
            dataExtractionRules.contains("<exclude domain=\"database\" path=\"health_journal.db\""))
        assertTrue("data_extraction_rules must exclude no_backup key material from device-transfer",
            dataExtractionRules.contains("<exclude domain=\"root\" path=\"no_backup\""))

        val backupRules = File("src/main/res/xml/backup_rules.xml").readText()
        assertTrue("backup_rules must exclude database",
            backupRules.contains("<exclude domain=\"database\" path=\"health_journal.db\""))
        assertTrue("backup_rules must exclude no_backup",
            backupRules.contains("<exclude domain=\"root\" path=\"no_backup\""))
    }
}
