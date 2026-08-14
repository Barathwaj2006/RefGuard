package com.refguard.platform

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult

/**
 * Orchestrator for handling RefGuard ingress events.
 */
class RefGuardIngressManager(
    private val isNetworkAvailable: () -> Boolean
) {
    /**
     * Dispatches an ingress channel action.
     * Prevents analysis dispatch if device is offline.
     */
    suspend fun processIngress(channel: IngressChannel): IngressResult {
        if (!isNetworkAvailable()) {
            return IngressResult.Failure(IngressError.Offline)
        }

        return channel.capture()
    }
}
