package nl.healthjournal.domain.port.secondary

import kotlinx.coroutines.flow.StateFlow
import nl.healthjournal.domain.model.lock.LockState
import nl.healthjournal.domain.model.lock.LockTimeout

/**
 * Secondary port for the opt-in app lock.
 *
 * The domain drives all lock-state transitions through this port; the platform adapter
 * (in :app) holds the actual [StateFlow] and talks to BiometricPrompt. The setting
 * (enabled/disabled, timeout) is stored in the existing SharedPreferences by the adapter.
 *
 * Lifecycle contract:
 *  - [lock] transitions any state → [LockState.Locked].
 *  - [onAuthenticated] transitions any state → [LockState.Unlocked].
 *  - [onBackgrounded] starts the timeout clock; if [isEnabled] and the elapsed time
 *    exceeds [timeout], the adapter emits [LockState.TimedOut] automatically.
 *  - If [isEnabled] is false the adapter always emits [LockState.Unlocked] and ignores
 *    [lock] and [onBackgrounded].
 *  - If the device has no enrolled credential [isAvailable] returns false; the setting
 *    must be shown as unavailable and [isEnabled] must be kept false.
 */
interface AppLockPort {

    /** Current lock state, hot and always available. */
    val lockState: StateFlow<LockState>

    /** Whether the lock feature is supported on this device (enrolled screen-lock present). */
    val isAvailable: Boolean

    /** Whether the user has enabled the lock setting. */
    var isEnabled: Boolean

    /** How long the app may be backgrounded before [LockState.TimedOut] is emitted. */
    var timeout: LockTimeout

    /**
     * Immediately transitions to [LockState.Locked].
     * No-op when [isEnabled] is false.
     */
    fun lock()

    /**
     * Transitions to [LockState.Unlocked] after the platform confirms successful authentication.
     * Must only be called by the platform adapter upon a confirmed BiometricPrompt success.
     */
    fun onAuthenticated()

    /**
     * Called when the app moves to the background. The adapter starts the timeout clock and
     * emits [LockState.TimedOut] when the chosen [timeout] has elapsed and [isEnabled] is true.
     */
    fun onBackgrounded()
}
