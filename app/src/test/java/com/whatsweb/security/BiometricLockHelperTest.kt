package com.whatsweb.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricLockHelperTest {

    @Test
    fun shouldNotLockWhenDisabled() {
        val shouldLock = BiometricLockHelper.shouldLock(
            enabled = false,
            lastBackgroundTime = 1000L,
            currentTime = 100000L
        )
        assertFalse(shouldLock)
    }

    @Test
    fun shouldLockOnColdStartWhenEnabled() {
        val shouldLock = BiometricLockHelper.shouldLock(
            enabled = true,
            lastBackgroundTime = 0L,
            currentTime = 100000L
        )
        assertTrue(shouldLock)
    }

    @Test
    fun shouldNotLockWithinOneMinuteTimeout() {
        val lastBackgroundTime = 100_000L
        val currentTime = lastBackgroundTime + 45_000L // 45 seconds elapsed
        val shouldLock = BiometricLockHelper.shouldLock(
            enabled = true,
            lastBackgroundTime = lastBackgroundTime,
            currentTime = currentTime
        )
        assertFalse(shouldLock)
    }

    @Test
    fun shouldLockAfterOneMinuteTimeoutExceeded() {
        val lastBackgroundTime = 100_000L
        val currentTime = lastBackgroundTime + 61_000L // 61 seconds elapsed
        val shouldLock = BiometricLockHelper.shouldLock(
            enabled = true,
            lastBackgroundTime = lastBackgroundTime,
            currentTime = currentTime
        )
        assertTrue(shouldLock)
    }
}
