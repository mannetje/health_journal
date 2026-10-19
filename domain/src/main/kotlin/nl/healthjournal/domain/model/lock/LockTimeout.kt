package nl.healthjournal.domain.model.lock

/**
 * How long the app waits after going to the background before requiring authentication again.
 * [IMMEDIATELY] triggers the prompt as soon as the app is no longer in the foreground.
 */
enum class LockTimeout(val seconds: Int) {
    IMMEDIATELY(0),
    ONE_MINUTE(60),
    FIVE_MINUTES(300);

    companion object {
        fun fromSeconds(seconds: Int): LockTimeout =
            entries.firstOrNull { it.seconds == seconds } ?: IMMEDIATELY
    }
}
