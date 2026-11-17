package com.whatsweb.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaWebViewClientTest {

    @Test
    fun testIsAllowedWhatsAppUrl() {
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("https://web.whatsapp.com"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("https://web.whatsapp.com/send?phone=123"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("https://whatsapp.com"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("https://faq.whatsapp.com"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("https://media.whatsapp.net/resource"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("blob:https://web.whatsapp.com/uuid"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("data:image/png;base64,iVBORw0KGgoAAAANS"))
        assertTrue(WaWebViewClient.isAllowedWhatsAppUrl("about:blank"))

        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl("https://google.com"))
        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl("https://facebook.com"))
        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl("tel:+1234567890"))
        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl("mailto:support@example.com"))
        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl(""))
        assertFalse(WaWebViewClient.isAllowedWhatsAppUrl(null))
    }

    @Test
    fun testIsExternalIntentScheme() {
        assertTrue(WaWebViewClient.isExternalIntentScheme("tel:+1234567890"))
        assertTrue(WaWebViewClient.isExternalIntentScheme("mailto:someone@example.com"))
        assertTrue(WaWebViewClient.isExternalIntentScheme("sms:+1234567890"))
        assertTrue(WaWebViewClient.isExternalIntentScheme("geo:0,0?q=London"))

        assertFalse(WaWebViewClient.isExternalIntentScheme("https://web.whatsapp.com"))
        assertFalse(WaWebViewClient.isExternalIntentScheme("http://google.com"))
        assertFalse(WaWebViewClient.isExternalIntentScheme(""))
        assertFalse(WaWebViewClient.isExternalIntentScheme(null))
    }

    @Test
    fun testPageLoadingStateTransitions() {
        val states = mutableListOf<Boolean>()
        val client = WaWebViewClient(
            onPageLoadingState = { isLoading ->
                states.add(isLoading)
            }
        )

        client.onPageStarted(null, "https://web.whatsapp.com", null)
        client.onPageFinished(null, "https://web.whatsapp.com")

        assertEquals(listOf(true, false), states)
    }

    @Test
    fun testUrlOverrideHook() {
        var interceptedUrl: String? = null
        val client = WaWebViewClient(
            onUrlOverride = { url ->
                interceptedUrl = url
                true
            }
        )

        val handled = client.handleUrlLoading(null, "https://custom.hook.com")
        assertTrue(handled)
        assertEquals("https://custom.hook.com", interceptedUrl)
    }

    @Test
    fun testInternalUrlNotOverridden() {
        val client = WaWebViewClient()
        // WhatsApp Web URL should NOT be overridden (returns false so WebView loads it)
        val handled = client.handleUrlLoading(null, "https://web.whatsapp.com")
        assertFalse(handled)
    }

    @Test
    fun testDomStabilizerScriptContent() {
        val script = WaWebViewClient.DOM_STABILIZER_SCRIPT
        assertTrue(script.contains("document.body.contains"))
        assertTrue(script.contains("stopPropagation"))
        assertTrue(script.contains("click"))
    }
}
