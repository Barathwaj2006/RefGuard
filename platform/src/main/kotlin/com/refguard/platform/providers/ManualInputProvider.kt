package com.refguard.platform.providers

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest

class ManualInputProvider(
    private val input: String,
    private val isUpi: Boolean = false
) : IngressChannel {
    override val sourceChannel: String = "MANUAL_INPUT"

    override suspend fun capture(): IngressResult {
        if (input.isBlank()) {
            return IngressResult.Failure(IngressError.EmptyContent)
        }

        val type = when {
            isUpi -> ContentType.UPI_VPA
            input.startsWith("http") -> ContentType.URL
            else -> ContentType.TEXT
        }

        val request = ScanRequest(
            contentType = type,
            contentValue = input,
            sourceContext = "com.refguard.manual",
            timestamp = java.time.Instant.now().toString()
        )
        return IngressResult.Success(request)
    }
}
