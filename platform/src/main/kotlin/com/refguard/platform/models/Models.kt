package com.refguard.platform.models

/**
 * Normalized content payload that maps directly to the scan-request.json contract.
 */
data class ScanRequest(
    val contentType: ContentType,
    val contentValue: String,
    val sourceContext: String,
    val timestamp: String
)

enum class ContentType {
    TEXT,
    URL,
    UPI_VPA,
    IMAGE,
    QR,
    SHARE_INTENT,
    CLIPBOARD,
    MANUAL
}

/**
 * Explicit error states.
 */
sealed class IngressError {
    object PermissionDenied : IngressError()
    object EmptyContent : IngressError()
    object UnsupportedContent : IngressError()
    object HardwareUnavailable : IngressError()
    data class MalformedContent(val reason: String) : IngressError()
}

sealed class IngressResult {
    data class Success(val request: ScanRequest) : IngressResult()
    data class SuccessOffline(val request: ScanRequest) : IngressResult()
    data class Failure(val error: IngressError) : IngressResult()
}
