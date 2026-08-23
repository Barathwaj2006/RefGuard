package com.refguard.platform

import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.providers.ClipboardProvider
import com.refguard.platform.providers.ManualInputProvider
import com.refguard.platform.providers.QRScannerProvider
import com.refguard.platform.providers.ScreenshotProvider
import com.refguard.platform.providers.ShareSheetProvider
// In a real android project we would use JUnit/MockK. Here we simulate the tests.

fun main() {
    var passed = 0
    var failed = 0

    suspend fun runTest(name: String, block: suspend () -> Boolean) {
        try {
            if (block()) {
                println("PASS: $name")
                passed++
            } else {
                println("FAIL: $name - Condition not met")
                failed++
            }
        } catch (e: Exception) {
            println("FAIL: $name - Exception: $e")
            failed++
        }
    }

    suspend fun runAllTests() {
        val manager = RefGuardIngressManager(isNetworkAvailable = { true })
        val offlineManager = RefGuardIngressManager(isNetworkAvailable = { false })

        // 1. Share Sheet text
        runTest("Share Sheet text") {
            val provider = ShareSheetProvider("Hello world", "com.whatsapp")
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.TEXT
        }

        // 2. Share Sheet URL
        runTest("Share Sheet URL") {
            val provider = ShareSheetProvider("http://example.com", "com.chrome")
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.URL
        }

        // 3. Clipboard text
        runTest("Clipboard text") {
            val provider = ClipboardProvider { "pasted text" }
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.TEXT
        }

        // 4. Manual URL
        runTest("Manual URL") {
            val provider = ManualInputProvider("http://test.com")
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.URL
        }

        // 5. Manual UPI VPA
        runTest("Manual UPI VPA") {
            val provider = ManualInputProvider("user@bank", isUpi = true)
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.UPI_VPA
        }

        // 6. QR scan success
        runTest("QR scan success") {
            val provider = QRScannerProvider(true, true) { "qr_data_123" }
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.QR
        }

        // 7. QR scan failure (malformed)
        runTest("QR scan failure (malformed)") {
            val provider = QRScannerProvider(true, true) { "ab" }
            val res = manager.processIngress(provider)
            res is IngressResult.Failure && res.error is IngressError.MalformedContent
        }

        // 8. Camera permission denied
        runTest("Camera permission denied") {
            val provider = QRScannerProvider(false, true) { "data" }
            val res = manager.processIngress(provider)
            res is IngressResult.Failure && res.error is IngressError.PermissionDenied
        }

        // 9. Screenshot import
        runTest("Screenshot import") {
            val provider = ScreenshotProvider { "base64data" }
            val res = manager.processIngress(provider)
            res is IngressResult.Success && res.request.contentType == ContentType.IMAGE
        }

        // 10. Invalid screenshot (oversized)
        runTest("Invalid screenshot") {
            val provider = ScreenshotProvider { "a".repeat(6_000_000) }
            val res = manager.processIngress(provider)
            res is IngressResult.Failure && res.error is IngressError.UnsupportedContent
        }

        // 11. Offline state
        runTest("Offline state") {
            val provider = ManualInputProvider("test")
            val res = offlineManager.processIngress(provider)
            res is IngressResult.Failure && res.error is IngressError.Offline
        }

        // 12. Empty input
        runTest("Empty input") {
            val provider = ManualInputProvider("")
            val res = manager.processIngress(provider)
            res is IngressResult.Failure && res.error is IngressError.EmptyContent
        }

        // 13. Source Channel Correctness
        runTest("Source Channel Check") {
            val provider = ShareSheetProvider("t", "p")
            provider.sourceChannel == "ANDROID_SHARE_SHEET"
        }

        println("\nTests complete: $passed passed, $failed failed")
        if (failed > 0) kotlin.system.exitProcess(1)
    }

    // Since this is a simple script runner for demonstration:
    kotlinx.coroutines.runBlocking {
        runAllTests()
    }
}
