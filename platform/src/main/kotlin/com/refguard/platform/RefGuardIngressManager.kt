package com.refguard.platform

import com.refguard.platform.ingress.IngressChannel
import com.refguard.platform.models.IngressResult

/**
 * Orchestrator for handling RefGuard ingress events.
 */
class RefGuardIngressManager(
    private val isNetworkAvailable: () -> Boolean
) {
    /**
     * Dispatches an ingress channel action.
     * Allows capture when offline, returning SuccessOffline to allow queuing.
     */
    suspend fun processIngress(channel: IngressChannel): IngressResult {
        val result = channel.capture()
        
        if (result is IngressResult.Success && !isNetworkAvailable()) {
            return IngressResult.SuccessOffline(result.request)
        }
        
        return result
    }
}
