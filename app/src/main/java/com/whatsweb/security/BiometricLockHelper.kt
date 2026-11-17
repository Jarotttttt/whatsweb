package com.whatsweb.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

object BiometricLockHelper {
    const val DEFAULT_TIMEOUT_MS: Long = 60_000L
    const val PREF_BIOMETRIC_ENABLED = "biometric_lock_enabled"
    const val PREFS_NAME = "whatsweb_security_prefs"

    fun shouldLock(
        enabled: Boolean,
        lastBackgroundTime: Long,
        currentTime: Long,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Boolean {
        if (!enabled) return false
        if (lastBackgroundTime <= 0L) return false
        val elapsed = currentTime - lastBackgroundTime
        return elapsed > timeoutMs
    }

    /**
     * Checks if biometric authentication or device credential (PIN/pattern/password)
     * is available on this device across all Android versions, with special handling for Android 10 (API 29).
     */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        // Device credentials (PIN, pattern, password) check
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true

        // Strong & Weak biometric check
        val biometricStatus = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        val hasBiometrics = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS

        // Legacy check for Android 10 (API 29) and lower
        @Suppress("DEPRECATION")
        val hasLegacyBiometrics = try {
            biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }

        return hasBiometrics || hasLegacyBiometrics || isDeviceSecure
    }
}
