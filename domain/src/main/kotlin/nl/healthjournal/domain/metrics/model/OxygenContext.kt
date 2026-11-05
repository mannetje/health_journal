package nl.healthjournal.domain.metrics.model

/** A note by the person about the moment of the reading. Never derived and never interpreted. */
enum class OxygenContext {
    AT_REST,
    AFTER_ACTIVITY,
    ASLEEP_OR_JUST_WOKE,
    OTHER;

    companion object {
        /** Null for an unknown or absent code, so a code from a later version reads as "no context". */
        fun fromCode(code: String?): OxygenContext? = entries.firstOrNull { it.name == code }
    }
}
