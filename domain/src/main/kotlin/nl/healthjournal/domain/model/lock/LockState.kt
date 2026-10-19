package nl.healthjournal.domain.model.lock

/**
 * The observable lock state of the app.
 *
 * - [Unlocked] — the user has authenticated (or the lock is disabled) and can see health data.
 * - [Locked] — the lock is enabled and the user must authenticate; no health data is composed.
 * - [TimedOut] — the app returned from the background after the chosen timeout; authentication
 *   is required before data is shown.
 */
sealed class LockState {
    data object Unlocked : LockState()
    data object Locked : LockState()
    data object TimedOut : LockState()

    /** True when the user cannot see health data and must authenticate. */
    val requiresAuthentication: Boolean
        get() = this is Locked || this is TimedOut
}
