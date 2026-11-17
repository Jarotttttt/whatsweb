package com.whatsweb.engine

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient handling file picking, WebRTC camera/audio permissions,
 * and page loading progress updates.
 */
open class WaWebChromeClient(
    var onProgressChanged: ((progress: Int) -> Unit)? = null,
    var onFileChooser: ((filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?) -> Boolean)? = null,
    var onPermissionRequest: ((request: PermissionRequest?) -> Unit)? = null,
    var onPermissionRequestCanceled: ((request: PermissionRequest?) -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        val handled = onFileChooser?.invoke(filePathCallback, fileChooserParams)
        return handled ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (onPermissionRequest != null) {
            onPermissionRequest?.invoke(request)
        } else {
            super.onPermissionRequest(request)
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        if (onPermissionRequestCanceled != null) {
            onPermissionRequestCanceled?.invoke(request)
        } else {
            super.onPermissionRequestCanceled(request)
        }
    }

    companion object {
        /**
         * Checks whether the given resource array contains audio or video capture resources.
         */
        fun hasMediaCaptureResource(resources: Array<String>?): Boolean {
            if (resources == null) return false
            return resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) ||
                    resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        }

        /**
         * Checks whether the permission request contains WebRTC audio or video capture resources.
         */
        fun isMediaCaptureRequest(request: PermissionRequest?): Boolean {
            return hasMediaCaptureResource(request?.resources)
        }
    }
}
