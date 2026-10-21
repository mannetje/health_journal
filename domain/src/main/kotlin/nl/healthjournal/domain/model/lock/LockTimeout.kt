package nl.healthjournal.domain.model.lock

/**
 * How long the app waits after going to the background before requiring authentication again.
 * [IMMEDIATELY] triggers the prompt as soon as the app is no longer in the foreground.
 */
enum class LockTimeout(val seconds: Int) {
    IMMEDIATELY(0),
    ONE_MINUTE(60),
    FIVE_MINUTES(300);

    /**
     * True when the app has been in the background long enough to require authentication.
     * A negative duration (a clock that ran backwards) counts as elapsed, so the lock never stays open.
     */
    fun hasElapsed(backgroundMillis: Long): Boolean =
        backgroundMillis < 0 || backgroundMillis / 1000 >= seconds

    companion object {
        fun fromSeconds(seconds: Int): LockTimeout =
            entries.firstOrNull { it.seconds == seconds } ?: IMMEDIATELY
    }
}
