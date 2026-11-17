package com.whatsweb.engine

import android.content.Context

object DesktopModeHelper {
    const val PREFS_NAME = "whatsweb_prefs"
    const val PREF_KEY_DESKTOP_MODE = "pref_desktop_mode"
    const val DEFAULT_DESKTOP_MODE = true

    fun resolveUserAgent(isDesktop: Boolean, defaultMobileUa: String): String {
        return if (isDesktop) {
            WaWebConfig.DESKTOP_USER_AGENT
        } else {
            if (defaultMobileUa.isNotBlank()) {
                defaultMobileUa
            } else {
                WaWebConfig.DESKTOP_USER_AGENT
            }
        }
    }

    fun isDesktopModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_DESKTOP_MODE, DEFAULT_DESKTOP_MODE)
    }

    fun setDesktopModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_DESKTOP_MODE, enabled).apply()
    }
}
