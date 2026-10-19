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
            onAuthenticated()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Stays locked on error or user cancel
        }
    }

    private val biometricPrompt = BiometricPrompt(activity, executor, authenticationCallback)

    fun authenticate(fallbackLauncher: ActivityResultLauncher<Intent>? = null) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.lock_prompt_title))
            .setSubtitle(activity.getString(R.string.lock_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            @Suppress("DEPRECATION")
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.lock_prompt_title),
                activity.getString(R.string.lock_prompt_subtitle)
            )
            if (intent != null && fallbackLauncher != null) {
                fallbackLauncher.launch(intent)
            }
        }
    }
}
