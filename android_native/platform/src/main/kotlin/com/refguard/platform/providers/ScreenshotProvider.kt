package com.refguard.platform.providers

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest

class ScreenshotProvider(
    private val base64ImageFetcher: suspend () -> String?
) : IngressChannel {
    override val sourceChannel: String = "SCREENSHOT_IMPORT"

    override suspend fun capture(): IngressResult {
        val imageContent = base64ImageFetcher()
        if (imageContent.isNullOrBlank()) {
            return IngressResult.Failure(IngressError.EmptyContent)
        }

        // Basic mock oversized check
        if (imageContent.length > 5_000_000) {
            return IngressResult.Failure(IngressError.UnsupportedContent)
        }

        val request = ScanRequest(
            contentType = ContentType.IMAGE,
            contentValue = imageContent,
            sourceContext = "com.android.gallery",
            timestamp = java.time.Instant.now().toString()
        )
        return IngressResult.Success(request)
    }
}
