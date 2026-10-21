package nl.healthjournal.domain.model.backup

import java.text.Normalizer

/** Rules for the passphrase that protects a backup. The app stores it nowhere. */
object BackupPassphrase {
    const val MIN_LENGTH = 8

    enum class Check { OK, TOO_SHORT, MISMATCH }

    /** Length is counted in characters of the normalised form, and both entries must be identical. */
    fun check(passphrase: CharArray, confirmation: CharArray): Check = when {
        normalize(passphrase).length < MIN_LENGTH -> Check.TOO_SHORT
        !passphrase.contentEquals(confirmation) -> Check.MISMATCH
        else -> Check.OK
    }

    /** NFKC, so the same words typed on another keyboard or platform derive the same key. */
    fun normalize(passphrase: CharArray): String = Normalizer.normalize(String(passphrase), Normalizer.Form.NFKC)

    fun toUtf8(passphrase: CharArray): ByteArray = normalize(passphrase).toByteArray(Charsets.UTF_8)
}
