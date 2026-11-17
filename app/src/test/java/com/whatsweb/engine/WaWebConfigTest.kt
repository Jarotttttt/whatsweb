package com.whatsweb.engine

import org.junit.Assert.*
import org.junit.Test

class WaWebConfigTest {
    @Test
    fun verifyUrlIsOfficialWhatsAppWeb() {
        assertEquals("https://web.whatsapp.com", WaWebConfig.WHATSAPP_WEB_URL)
    }

    @Test
    fun verifyUserAgentIsDesktopChrome() {
        val ua = WaWebConfig.DESKTOP_USER_AGENT
        assertTrue("Must contain Windows NT", ua.contains("Windows NT 10.0"))
        assertTrue("Must contain Chrome", ua.contains("Chrome/"))
        assertFalse("Must not contain Mobile", ua.contains("Mobile"))
        assertFalse("Must not contain Android", ua.contains("Android"))
    }

    @Test
    fun verifyDesktopUserAgentValidationHelper() {
        assertTrue(WaWebConfig.isDesktopUserAgentValid())
        assertTrue(WaWebConfig.isDesktopUserAgentValid(WaWebConfig.DESKTOP_USER_AGENT))
        assertFalse(WaWebConfig.isDesktopUserAgentValid("Mozilla/5.0 (Linux; Android 10; Mobile)"))
        assertFalse(WaWebConfig.isDesktopUserAgentValid("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"))
    }
}
