package com.refguard.platform.providers

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest

class ShareSheetProvider(
    private val sharedText: String?,
    private val sharedPackage: String?
) : IngressChannel {
    override val sourceChannel: String = "ANDROID_SHARE_SHEET"

    override suspend fun capture(): IngressResult {
        if (sharedText.isNullOrBlank()) {
            return IngressResult.Failure(IngressError.EmptyContent)
        }
        
        val type = if (sharedText.startsWith("http")) ContentType.URL else ContentType.TEXT
        val context = sharedPackage ?: "unknown_package"

        val request = ScanRequest(
            contentType = type,
            contentValue = sharedText,
            sourceContext = context,
            timestamp = java.time.Instant.now().toString()
        )
        return IngressResult.Success(request)
    }
}
