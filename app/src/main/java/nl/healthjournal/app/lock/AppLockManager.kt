package nl.healthjournal.app.lock

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.healthjournal.app.settings.AppLockPreference
import nl.healthjournal.domain.model.lock.LockState
import nl.healthjournal.domain.model.lock.LockTimeout
import nl.healthjournal.domain.port.secondary.AppLockPort

/**
 * Android adapter for [AppLockPort].
 *
 * Manages the observable lock state, timeout calculation when the app is backgrounded,
 * and integration with the system's credential availability (PIN, pattern, password, or biometrics).
 */
class AppLockManager(
    private val context: Context,
    private val preference: AppLockPreference = AppLockPreference(context)
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
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun onForegrounded() {
        if (!isEnabled) {
            _lockState.value = LockState.Unlocked
            return
        }

        val lastBackground = backgroundTimestamp
        if (lastBackground != null) {
            val elapsedSeconds = (System.currentTimeMillis() - lastBackground) / 1000
            if (elapsedSeconds >= timeout.seconds) {
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
