package nl.healthjournal.domain.model.backup

import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.profile.Profile
import java.time.Instant

/**
 * Display preferences carried by a backup. Values are the stored tags (`system` means follow the device).
 * Security and privacy settings (app lock, secure screen, lock-screen details) are deliberately not part of this.
 */
data class BackupPreferences(
    val language: String,
    val region: String,
    val unitSystem: String,
    val glucoseUnit: String,
    val theme: String,
    val reminderSnoozeMinutes: Int
)

data class BackupMedication(
    val medication: Medication,
    val intakes: List<Intake>
)

/** Everything that belongs to one profile. */
data class BackupProfile(
    val profile: Profile,
    val isActive: Boolean,
    val weights: List<WeightEntry> = emptyList(),
    val bloodPressures: List<BloodPressureEntry> = emptyList(),
    val glucoses: List<GlucoseEntry> = emptyList(),
    val waistCircumferences: List<WaistCircumferenceEntry> = emptyList(),
    val activities: List<ActivitySession> = emptyList(),
    val medications: List<BackupMedication> = emptyList()
) {
    val entryCount: Int
        get() = weights.size + bloodPressures.size + glucoses.size + waistCircumferences.size + activities.size
    val intakeCount: Int
        get() = medications.sumOf { it.intakes.size }
}

/** The content of a `.hjbackup` file once decrypted: see `docs/backup-format/README.md`. */
data class BackupDocument(
    val createdAt: Instant,
    val appVersion: String?,
    val preferences: BackupPreferences?,
    val profiles: List<BackupProfile>
)

/** What a merge added, and how many items it left alone because they already existed. */
data class MergeResult(
    val profilesAdded: Int,
    val entriesAdded: Int,
    val medicationsAdded: Int,
    val intakesAdded: Int,
    val alreadyPresent: Int
) {
    companion object {
        val NOTHING = MergeResult(0, 0, 0, 0, 0)
    }
}

/** Why a file could not be opened. The messages carry no health data and no passphrase. */
sealed class BackupException(message: String) : Exception(message) {
    /** Not a Health Journal backup (magic bytes missing or file too short). */
    class NotABackup : BackupException("Not a Health Journal backup")

    /** A container, key-derivation or cipher version this app does not know, or parameters outside the allowed limits. */
    class Unsupported(detail: String) : BackupException("Unsupported backup: $detail")

    /** Authentication failed: wrong passphrase, or the file was changed or damaged. The two cannot be told apart. */
    class WrongPassphraseOrCorrupt : BackupException("Wrong passphrase, or the file is damaged")

    /** Decrypted fine but the content does not follow the format. */
    class Malformed(detail: String) : BackupException("Malformed backup: $detail")
}
