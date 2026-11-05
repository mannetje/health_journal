package nl.healthjournal.app.lock

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.healthjournal.app.settings.AppLockPreference
import nl.healthjournal.domain.lock.model.LockState
import nl.healthjournal.domain.lock.model.LockTimeout
import nl.healthjournal.domain.lock.port.AppLockPort

/**
 * Android adapter for [AppLockPort].
 *
 * Manages the observable lock state, timeout calculation when the app is backgrounded,
 * and integration with the system's credential availability (PIN, pattern, password, or biometrics).
 */
class AppLockManager(
    private val context: Context,
    private val preference: AppLockPreference = AppLockPreference(context),
    // Monotonic: changing the wall clock must not stretch the timeout.
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime
) : AppLockPort {

    private val _lockState = MutableStateFlow<LockState>(
        if (preference.enabled && isDeviceSecure()) LockState.Locked else LockState.Unlocked
    )
    override val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private var backgroundTimestamp: Long? = null

    override val isAvailable: Boolean
        get() = isDeviceSecure()

    override var isEnabled: Boolean
        get() = preference.enabled && isDeviceSecure()
        set(value) {
            preference.enabled = value
            if (!value || !isDeviceSecure()) {
                _lockState.value = LockState.Unlocked
                backgroundTimestamp = null
            } else if (_lockState.value == LockState.Unlocked) {
                // If newly enabled, start unlocked in the current session
            }
        }

    override var timeout: LockTimeout
        get() = preference.timeout
        set(value) {
            preference.timeout = value
        }

    override fun lock() {
        if (isEnabled) {
            _lockState.value = LockState.Locked
        }
    }

    override fun onAuthenticated() {
        _lockState.value = LockState.Unlocked
        backgroundTimestamp = null
    }

    override fun onBackgrounded() {
        if (isEnabled) {
            backgroundTimestamp = elapsedRealtime()
        }
    }

    fun onForegrounded() {
        if (!isEnabled) {
            _lockState.value = LockState.Unlocked
            return
        }

        val lastBackground = backgroundTimestamp
        // Consumed: a resume that follows no new backgrounding (a configuration change, a system dialog)
        // must not be measured against an old timestamp.
        backgroundTimestamp = null
        if (lastBackground != null) {
            if (timeout.hasElapsed(elapsedRealtime() - lastBackground)) {
                _lockState.value = LockState.TimedOut
            }
        }
    }

    private fun isDeviceSecure(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val biometricManager = BiometricManager.from(context)

        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        val isBiometricOrCredentialAvailable = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS

        return keyguardManager?.isDeviceSecure == true || isBiometricOrCredentialAvailable
    }
}
