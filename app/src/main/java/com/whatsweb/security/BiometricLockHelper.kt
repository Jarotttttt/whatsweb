package com.whatsweb.security

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
        if (lastBackgroundTime <= 0L) return true
        val elapsed = currentTime - lastBackgroundTime
        return elapsed > timeoutMs
    }
}
