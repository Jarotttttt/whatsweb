package com.whatsweb.engine

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient keeping navigation inside WhatsApp Web,
 * launching external intents (tel:, mailto:, external URLs), and handling loading & error states.
 */
open class WaWebViewClient(
    var onPageLoadingState: ((isLoading: Boolean) -> Unit)? = null,
    var onError: ((errorCode: Int, description: String, failingUrl: String?) -> Unit)? = null,
    var onUrlOverride: ((url: String) -> Boolean)? = null
) : WebViewClient() {

    companion object {
        private const val TAG = "WaWebViewClient"

        /**
         * Checks whether the given URL is part of WhatsApp Web and should remain inside the WebView.
         */
        fun isAllowedWhatsAppUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val lower = url.trim().lowercase()
            if (lower.startsWith("blob:") || lower.startsWith("data:") || lower.startsWith("about:")) {
                return true
            }
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                return false
            }
            return try {
                val uri = java.net.URI(lower)
                val host = uri.host ?: return false
                host == "whatsapp.com" ||
                        host.endsWith(".whatsapp.com") ||
                        host == "whatsapp.net" ||
                        host.endsWith(".whatsapp.net")
            } catch (e: Exception) {
                lower.startsWith("https://web.whatsapp.com") ||
                        lower.startsWith("http://web.whatsapp.com")
            }
        }

        /**
         * Checks whether the scheme is an external intent scheme like tel, mailto, or sms.
         */
        fun isExternalIntentScheme(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val lower = url.trim().lowercase()
            return lower.startsWith("tel:") ||
                    lower.startsWith("mailto:") ||
                    lower.startsWith("sms:") ||
                    lower.startsWith("geo:")
        }

        /**
         * JavaScript injected into WhatsApp Web to stabilize touch interactions and
         * prevent ghost clicks on detached DOM nodes (caused by React re-renders)
         * from triggering WhatsApp Web's click-outside dismissal of the emoji/sticker drawer.
         */
        const val DOM_STABILIZER_SCRIPT = """
            (function() {
                if (window.__waTouchStabilized) return;
                window.__waTouchStabilized = true;

                // Stop ghost clicks on detached nodes from triggering click-outside dismissals
                document.addEventListener('click', function(e) {
                    if (e.target && !document.body.contains(e.target)) {
                        e.stopPropagation();
                    }
                }, true);

                document.addEventListener('pointerdown', function(e) {
                    if (e.target && !document.body.contains(e.target)) {
                        e.stopPropagation();
                    }
                }, true);
            })();
        """

        /**
         * Injects a desktop viewport meta tag to instruct Blink to render the page at
         * 1024px layout width and scale it down to fit the full device screen width,
         * matching the "Desktop site" behavior in mobile browsers (no horizontal panning).
         */
        const val VIEWPORT_FIT_SCRIPT = """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.setAttribute('content', 'width=1024, user-scalable=yes');
            })();
        """
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageLoadingState?.invoke(true)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        if (url != null && isAllowedWhatsAppUrl(url)) {
            view?.evaluateJavascript(VIEWPORT_FIT_SCRIPT, null)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoadingState?.invoke(false)
        if (url != null && isAllowedWhatsAppUrl(url)) {
            view?.evaluateJavascript(VIEWPORT_FIT_SCRIPT, null)
            view?.evaluateJavascript(DOM_STABILIZER_SCRIPT, null)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        Log.w(TAG, "WebView render process gone. Crashed: ${detail?.didCrash()}")
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (!request.isForMainFrame && !isExternalIntentScheme(url)) {
            return false
        }
        return handleUrlLoading(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrlLoading(view, url)
    }

    /**
     * Determines whether to load the URL in the WebView or delegate to external intent.
     * Returns true if URL handling was overridden (handled externally or intercepted),
     * false to let WebView load it.
     */
    fun handleUrlLoading(view: WebView?, url: String): Boolean {
        if (onUrlOverride?.invoke(url) == true) {
            return true
        }

        // WhatsApp Web internal navigation stays in WebView
        if (isAllowedWhatsAppUrl(url)) {
            return false
        }

        // External protocols (tel:, mailto:, sms:, etc.) or external web links launched externally
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            view?.context?.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity found to handle url: $url", e)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching intent for url: $url", e)
            return true
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error?.errorCode ?: ERROR_UNKNOWN
            } else {
                ERROR_UNKNOWN
            }
            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error?.description?.toString() ?: "Connection error"
            } else {
                "Connection error"
            }
            val failingUrl = request.url?.toString()
            notifyError(view, errorCode, description, failingUrl)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            notifyError(view, errorCode, description ?: "Connection error", failingUrl)
        }
    }

    private fun notifyError(
        view: WebView?,
        errorCode: Int,
        description: String,
        failingUrl: String?
    ) {
        onPageLoadingState?.invoke(false)
        if (onError != null) {
            onError?.invoke(errorCode, description, failingUrl)
        } else {
            displayOfflineRetryOverlay(view, description)
        }
    }

    private fun displayOfflineRetryOverlay(view: WebView?, errorDescription: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 90vh;
                        margin: 0;
                        padding: 24px;
                        text-align: center;
                        background-color: #f0f2f5;
                        color: #3b4a54;
                    }
                    h2 { margin-bottom: 8px; color: #111b21; font-size: 20px; }
                    p { margin-top: 0; margin-bottom: 24px; color: #667781; font-size: 14px; line-height: 1.4; }
                    button {
                        background-color: #00a884;
                        color: white;
                        border: none;
                        padding: 12px 28px;
                        font-size: 15px;
                        font-weight: 600;
                        border-radius: 24px;
                        cursor: pointer;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    button:active { background-color: #008069; }
                </style>
            </head>
            <body>
                <h2>Unable to connect</h2>
                <p>$errorDescription</p>
                <button onclick="location.href='${WaWebConfig.WHATSAPP_WEB_URL}'">Retry</button>
            </body>
            </html>
        """.trimIndent()
        view?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
