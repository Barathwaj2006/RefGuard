package com.refguard.platform.models

enum class ContentType {
    TEXT,
    QR_CODE,
    QR,
    URL,
    UPI_URI,
    SMS_TEXT,
    IMAGE,
    CLIPBOARD,
    NOTIFICATION,
    MANUAL_TEXT
}

data class ScanRequest(
    val contentType: ContentType,
    val contentValue: String,
    val sourceContext: String = "MANUAL_SCAN",
    val timestamp: String = java.time.Instant.now().toString()
)

sealed class IngressResult {
    data class Success(val request: ScanRequest) : IngressResult()
    data class SuccessOffline(val request: ScanRequest) : IngressResult()
    data class Failure(val error: IngressError) : IngressResult()
}

sealed class IngressError {
    object EmptyContent : IngressError()
    object PermissionDenied : IngressError()
    object HardwareUnavailable : IngressError()
    data class InvalidPayload(val message: String) : IngressError()
    data class NoData(val message: String = "No data found") : IngressError()
    data class Unknown(val cause: Throwable) : IngressError()
}
