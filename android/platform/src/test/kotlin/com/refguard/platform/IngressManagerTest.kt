package com.refguard.platform

import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.providers.ClipboardProvider
import com.refguard.platform.providers.ManualInputProvider
import com.refguard.platform.providers.QRScannerProvider
import com.refguard.platform.providers.ScreenshotProvider
import com.refguard.platform.providers.ShareSheetProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IngressManagerTest {

    private val onlineManager = RefGuardIngressManager(isNetworkAvailable = { true })
    private val offlineManager = RefGuardIngressManager(isNetworkAvailable = { false })

    @Test
    fun testShareSheetText() = runTest {
        val provider = ShareSheetProvider("Hello world", "com.whatsapp")
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.TEXT, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testShareSheetUrl() = runTest {
        val provider = ShareSheetProvider("http://example.com", "com.chrome")
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.URL, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testClipboardText() = runTest {
        val provider = ClipboardProvider { "pasted text" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.TEXT, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testManualUrl() = runTest {
        val provider = ManualInputProvider("http://test.com")
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.URL, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testManualUpiVpa() = runTest {
        val provider = ManualInputProvider("user@bank", isUpi = true)
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.UPI_VPA, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testQrScanSuccess() = runTest {
        val provider = QRScannerProvider(hasCameraPermission = true, isCameraAvailable = true) { "qr_data_123" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.QR, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testQrScanFailureMalformed() = runTest {
        val provider = QRScannerProvider(hasCameraPermission = true, isCameraAvailable = true) { "ab" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Failure)
        assertTrue((res as IngressResult.Failure).error is IngressError.MalformedContent)
    }

    @Test
    fun testCameraPermissionDenied() = runTest {
        val provider = QRScannerProvider(hasCameraPermission = false, isCameraAvailable = true) { "data" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Failure)
        assertTrue((res as IngressResult.Failure).error is IngressError.PermissionDenied)
    }

    @Test
    fun testScreenshotImport() = runTest {
        val provider = ScreenshotProvider { "base64data" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.IMAGE, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testInvalidScreenshot() = runTest {
        val provider = ScreenshotProvider { "a".repeat(6_000_000) }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Failure)
        assertTrue((res as IngressResult.Failure).error is IngressError.UnsupportedContent)
    }

    @Test
    fun testOfflineStateAllowsCapture() = runTest {
        val provider = ManualInputProvider("test")
        val res = offlineManager.processIngress(provider)
        assertTrue(res is IngressResult.SuccessOffline)
        assertEquals(ContentType.TEXT, (res as IngressResult.SuccessOffline).request.contentType)
    }

    @Test
    fun testEmptyInput() = runTest {
        val provider = ManualInputProvider("")
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Failure)
        assertTrue((res as IngressResult.Failure).error is IngressError.EmptyContent)
    }

    @Test
    fun testShareSheetMultipleImages() = runTest {
        val provider = ScreenshotProvider { "base64_multishot_data" }
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.IMAGE, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testShareSheetUpiPayUrl() = runTest {
        val provider = ShareSheetProvider("upi://pay?pa=scammer@upi&am=5000&tn=Prize", "com.whatsapp")
        val res = onlineManager.processIngress(provider)
        assertTrue(res is IngressResult.Success)
        assertEquals(ContentType.URL, (res as IngressResult.Success).request.contentType)
    }

    @Test
    fun testSourceChannelCorrectness() {
        val provider = ShareSheetProvider("t", "p")
        assertEquals("ANDROID_SHARE_SHEET", provider.sourceChannel)
    }
}
