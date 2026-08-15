package com.refguard.platform.providers

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest

/**
 * Handles explicit user-initiated clipboard paste actions.
 */
class ClipboardProvider(
    private val clipboardContentFetcher: suspend () -> String?
) : IngressChannel {
    override val sourceChannel: String = "CLIPBOARD_PASTE"

    override suspend fun capture(): IngressResult {
        val content = clipboardContentFetcher()
        if (content.isNullOrBlank()) {
            return IngressResult.Failure(IngressError.EmptyContent)
        }

        val type = if (content.startsWith("http")) ContentType.URL else ContentType.TEXT

        val request = ScanRequest(
            contentType = type,
            contentValue = content,
            sourceContext = "com.android.clipboard",
            timestamp = java.time.Instant.now().toString()
        )
        return IngressResult.Success(request)
    }
}
