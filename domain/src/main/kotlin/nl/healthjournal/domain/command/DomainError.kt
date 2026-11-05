package nl.healthjournal.domain.command

/** What a failed domain operation reports. A code and parameters only; the UI chooses the words. */
sealed interface DomainError {
    /** A value broke a rule. [params] holds the numbers a message needs, such as the bounds. */
    data class Invalid(
        val field: ValidationField,
        val rule: ValidationRule,
        val params: List<Any> = emptyList()
    ) : DomainError

    /** No entry of [kind] has this [id]. */
    data class NotFound(val kind: String, val id: String) : DomainError

    /** A backup file could not be opened. */
    data class Backup(val problem: BackupProblem) : DomainError

    /** The storage layer failed. [cause] is for logs, not for the user. */
    data class Storage(val cause: Throwable? = null) : DomainError
}

/** The input a [ValidationRule] was broken on. */
enum class ValidationField {
    SYSTOLIC, DIASTOLIC, PULSE, GLUCOSE, WEIGHT, WAIST, OXYGEN_SATURATION, DISTANCE, TIME_RANGE, COMMENT, NAME, HEIGHT, DATE_OF_BIRTH, PASSPHRASE
}

enum class ValidationRule {
    /** The value is missing. */
    REQUIRED,

    /** The value is outside its bounds; params are the minimum and the maximum. */
    OUT_OF_RANGE,

    /** Systolic must be strictly above diastolic. */
    SYSTOLIC_NOT_ABOVE_DIASTOLIC,

    /** The end must be after the start. */
    END_NOT_AFTER_START,

    /** The text is longer than allowed; params hold the maximum length. */
    TOO_LONG,

    /** The text is shorter than allowed; params hold the minimum length. */
    TOO_SHORT,

    /** Two entries that must be equal are not (a passphrase and its confirmation). */
    MISMATCH
}

/** Why a backup could not be opened. The two passphrase causes cannot be told apart, by design. */
enum class BackupProblem { NOT_A_BACKUP, UNSUPPORTED, WRONG_PASSPHRASE_OR_CORRUPT, MALFORMED }
