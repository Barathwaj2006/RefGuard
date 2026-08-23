package com.refguard.platform.ingress

import com.refguard.platform.models.IngressResult

/**
 * Base interface for all RefGuard user-initiated ingress channels.
 */
interface IngressChannel {
    /**
     * The normalized source of the content (e.g. ANDROID_SHARE_SHEET, CLIPBOARD_PASTE)
     */
    val sourceChannel: String

    /**
     * Executes the capture mechanism and normalizes the output.
     */
    suspend fun capture(): IngressResult
}
