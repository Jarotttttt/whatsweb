package com.whatsweb.engine

import android.webkit.PermissionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaWebChromeClientTest {

    @Test
    fun testProgressCallbackInvoked() {
        var recordedProgress = -1
        val client = WaWebChromeClient(
            onProgressChanged = { progress ->
                recordedProgress = progress
            }
        )

        client.onProgressChanged(null, 45)
        assertEquals(45, recordedProgress)

        client.onProgressChanged(null, 100)
        assertEquals(100, recordedProgress)
    }

    @Test
    fun testFileChooserCallbackInvoked() {
        var fileChooserInvoked = false
        val client = WaWebChromeClient(
            onFileChooser = { _, _ ->
                fileChooserInvoked = true
                true
            }
        )

        val handled = client.onShowFileChooser(null, null, null)
        assertTrue(fileChooserInvoked)
        assertTrue(handled)
    }

    @Test
    fun testPermissionRequestCallbackInvoked() {
        var permissionInvoked = false
        val client = WaWebChromeClient(
            onPermissionRequest = {
                permissionInvoked = true
            }
        )

        client.onPermissionRequest(null)
        assertTrue(permissionInvoked)
    }

    @Test
    fun testMediaCaptureResourceDetection() {
        assertTrue(
            WaWebChromeClient.hasMediaCaptureResource(
                arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            )
        )
        assertTrue(
            WaWebChromeClient.hasMediaCaptureResource(
                arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            )
        )
        assertTrue(
            WaWebChromeClient.hasMediaCaptureResource(
                arrayOf(
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE
                )
            )
        )
        assertFalse(
            WaWebChromeClient.hasMediaCaptureResource(
                arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
            )
        )
        assertFalse(WaWebChromeClient.hasMediaCaptureResource(null))
        assertFalse(WaWebChromeClient.hasMediaCaptureResource(emptyArray()))
    }
}
