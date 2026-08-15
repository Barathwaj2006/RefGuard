package com.refguard.platform.providers

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest

class QRScannerProvider(
    private val hasCameraPermission: Boolean,
    private val isCameraAvailable: Boolean,
    private val scanAction: suspend () -> String?
) : IngressChannel {
    override val sourceChannel: String = "CAMERA_QR_SCAN"

    override suspend fun capture(): IngressResult {
        if (!hasCameraPermission) return IngressResult.Failure(IngressError.PermissionDenied)
        if (!isCameraAvailable) return IngressResult.Failure(IngressError.HardwareUnavailable)

        val qrContent = scanAction()
        if (qrContent.isNullOrBlank()) {
            return IngressResult.Failure(IngressError.EmptyContent)
        }

        // Very basic validation simulation
        if (qrContent.length < 3) {
            return IngressResult.Failure(IngressError.MalformedContent("Content too short"))
        }

        val request = ScanRequest(
            contentType = ContentType.QR,
            contentValue = qrContent,
            sourceContext = "com.android.camera",
            timestamp = java.time.Instant.now().toString()
        )
        return IngressResult.Success(request)
    }
}
