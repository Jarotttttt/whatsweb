package com.whatsweb.engine

object WaWebConfig {
    const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"
    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    fun isDesktopUserAgentValid(ua: String = DESKTOP_USER_AGENT): Boolean {
        return ua.contains("Windows NT 10.0") &&
                ua.contains("Chrome/") &&
                !ua.contains("Mobile") &&
                !ua.contains("Android")
    }
}
