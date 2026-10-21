package nl.healthjournal.app.lock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import nl.healthjournal.app.R

/**
 * Handles device authentication using [BiometricPrompt] with biometrics and device credential
 * (PIN, pattern, password) allowed, with API 26+ KeyguardManager fallback.
 */
class BiometricAuthHelper(
    private val activity: FragmentActivity,
    private val onAuthenticated: () -> Unit
) {
    private val executor = ContextCompat.getMainExecutor(activity)

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            val action = pendingAction
            pendingAction = null
            if (action != null) action() else onAuthenticated()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Stays locked on error or user cancel; a pending security change is dropped
            pendingAction = null
        }
    }

    private val biometricPrompt = BiometricPrompt(activity, executor, authenticationCallback)

    /** Set while a confirmation for a security change is pending; replaces the normal unlock callback. */
    private var pendingAction: (() -> Unit)? = null

    /** Asks for the device credential and runs [action] only when it succeeds (e.g. before turning the lock off). */
    fun confirm(
        action: () -> Unit,
        fallbackLauncher: ActivityResultLauncher<Intent>? = null
    ) {
        pendingAction = action
        authenticate(
            fallbackLauncher,
            title = activity.getString(R.string.lock_confirm_title),
            subtitle = activity.getString(R.string.lock_confirm_subtitle)
        )
    }

    /** Runs and clears the pending confirmation (used by the keyguard fallback); false when none is pending. */
    fun runPendingAction(): Boolean {
        val action = pendingAction ?: return false
        pendingAction = null
        action()
        return true
    }

    fun authenticate(
        fallbackLauncher: ActivityResultLauncher<Intent>? = null,
        title: String = activity.getString(R.string.lock_prompt_title),
        subtitle: String = activity.getString(R.string.lock_prompt_subtitle)
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            @Suppress("DEPRECATION")
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
            if (intent != null && fallbackLauncher != null) {
                fallbackLauncher.launch(intent)
            }
        }
    }
}
