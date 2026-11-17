package com.whatsweb.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopModeHelperTest {

    private val sampleMobileUa = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    @Test
    fun verifyConstants() {
        assertEquals("whatsweb_prefs", DesktopModeHelper.PREFS_NAME)
        assertEquals("pref_desktop_mode", DesktopModeHelper.PREF_KEY_DESKTOP_MODE)
        assertTrue(DesktopModeHelper.DEFAULT_DESKTOP_MODE)
    }

    @Test
    fun resolveUserAgent_whenDesktopTrue_returnsDesktopUserAgent() {
        val result = DesktopModeHelper.resolveUserAgent(
            isDesktop = true,
            defaultMobileUa = sampleMobileUa
        )
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopTrueAndMobileUaBlank_returnsDesktopUserAgent() {
        val result = DesktopModeHelper.resolveUserAgent(
            isDesktop = true,
            defaultMobileUa = ""
        )
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopFalseAndMobileUaPresent_returnsMobileUa() {
        val result = DesktopModeHelper.resolveUserAgent(
            isDesktop = false,
            defaultMobileUa = sampleMobileUa
        )
        assertEquals(sampleMobileUa, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopFalseAndMobileUaEmpty_returnsDesktopUserAgentFallback() {
        val result = DesktopModeHelper.resolveUserAgent(
            isDesktop = false,
            defaultMobileUa = ""
        )
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }

    @Test
    fun resolveUserAgent_whenDesktopFalseAndMobileUaWhitespace_returnsDesktopUserAgentFallback() {
        val result = DesktopModeHelper.resolveUserAgent(
            isDesktop = false,
            defaultMobileUa = "   \n\t  "
        )
        assertEquals(WaWebConfig.DESKTOP_USER_AGENT, result)
    }
}
