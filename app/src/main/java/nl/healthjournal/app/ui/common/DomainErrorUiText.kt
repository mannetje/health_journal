package nl.healthjournal.app.ui.common

import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.domain.command.BackupProblem
import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule

/**
 * The message for a domain error. The domain carries no display text, so every rule maps to a string
 * resource here; the screen resolves it in the current language. [units] fills the unit symbol of the
 * weight and distance messages. Rules without a specific message fall back to the generic one.
 */
fun DomainError.toUiText(units: DisplayUnits = DisplayUnits.DEFAULT): UiText = when (this) {
    is DomainError.Invalid -> invalidText(units)
    is DomainError.NotFound -> UiText.Res(R.string.history_err_not_found)
    is DomainError.Backup -> UiText.Res(
        when (problem) {
            BackupProblem.NOT_A_BACKUP -> R.string.backup_err_not_backup
            BackupProblem.UNSUPPORTED -> R.string.backup_err_unsupported
            BackupProblem.MALFORMED -> R.string.backup_err_malformed
            BackupProblem.WRONG_PASSPHRASE_OR_CORRUPT -> R.string.backup_err_wrong_passphrase
        }
    )
    is DomainError.Storage -> UiText.Res(R.string.log_err_save_failed)
}

private fun DomainError.Invalid.invalidText(units: DisplayUnits): UiText = when (rule) {
    ValidationRule.END_NOT_AFTER_START -> UiText.Res(R.string.migraine_err_end_before_start)
    ValidationRule.OUT_OF_RANGE -> when (field) {
        ValidationField.SYSTOLIC -> UiText.Res(R.string.log_err_systolic)
        ValidationField.DIASTOLIC -> UiText.Res(R.string.log_err_diastolic)
        ValidationField.GLUCOSE -> UiText.Res(R.string.log_err_glucose)
        ValidationField.WEIGHT -> UiText.Res(R.string.log_err_weight, units.weightSymbol)
        ValidationField.WAIST -> UiText.Res(R.string.log_err_waist)
        ValidationField.DISTANCE -> UiText.Res(R.string.log_err_distance, units.distanceSymbol)
        ValidationField.PULSE,
        ValidationField.OXYGEN_SATURATION,
        ValidationField.TIME_RANGE,
        ValidationField.COMMENT -> UiText.Res(R.string.history_edit_invalid)
        ValidationField.NAME,
        ValidationField.HEIGHT,
        ValidationField.DATE_OF_BIRTH -> UiText.Res(R.string.profile_err_save_failed)
        ValidationField.PASSPHRASE -> UiText.Res(R.string.backup_err_mismatch)
    }
    ValidationRule.TOO_SHORT -> UiText.Res(R.string.backup_err_too_short, params.firstOrNull() ?: 0)
    ValidationRule.MISMATCH -> UiText.Res(R.string.backup_err_mismatch)
    ValidationRule.REQUIRED,
    ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC,
    ValidationRule.TOO_LONG -> UiText.Res(R.string.history_edit_invalid)
}
