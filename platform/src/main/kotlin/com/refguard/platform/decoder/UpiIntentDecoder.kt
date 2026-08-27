package com.refguard.platform.decoder

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class DecodedUpiPayload(
    val rawUri: String,
    val payeeVpa: String?,
    val payeeName: String?,
    val amount: Double?,
    val currency: String,
    val transactionNote: String?,
    val merchantCode: String?,
    val transactionRef: String?,
    val isCollectOrDebit: Boolean,
    val statedIntentSummary: String?,
    val hasIntentInversion: Boolean
)

object UpiIntentDecoder {

    private val REWARD_KEYWORDS = listOf(
        "reward", "cashback", "prize", "won", "winner", "lottery",
        "refund", "bonus", "claim", "gift", "electricity", "bill",
        "subsidy", "scratch card", "credited", "receive", "congratulations",
        "deposit", "reimbursement", "voucher", "free", "overpayment"
    )

    private val URGENCY_KEYWORDS = listOf(
        "urgent", "immediately", "block", "suspended", "disconnect",
        "expire", "today", "penalty", "warning", "police", "legal action"
    )

    private val CREDENTIAL_KEYWORDS = listOf(
        "pin", "otp", "mpin", "password", "cvv", "card number"
    )

    fun decode(content: String): DecodedUpiPayload? {
        val trimmed = content.trim()
        val isUpiUri = trimmed.startsWith("upi://pay", ignoreCase = true)
        val isVpa = trimmed.matches(Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$"))

        if (!isUpiUri && !isVpa) {
            return null
        }

        var payeeVpa: String? = null
        var payeeName: String? = null
        var amount: Double? = null
        var currency = "INR"
        var note: String? = null
        var mc: String? = null
        var tr: String? = null

        if (isUpiUri) {
            try {
                val query = trimmed.substringAfter("?", "")
                val params = query.split("&")
                for (param in params) {
                    val parts = param.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].lowercase()
                        val value = try {
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                        } catch (e: Exception) {
                            parts[1]
                        }
                        when (key) {
                            "pa" -> payeeVpa = value
                            "pn" -> payeeName = value
                            "am" -> amount = value.toDoubleOrNull()
                            "cu" -> currency = value
                            "tn" -> note = value
                            "mc" -> mc = value
                            "tr" -> tr = value
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback graceful parsing
            }
        } else {
            payeeVpa = trimmed
        }

        val textToInspect = ((note ?: "") + " " + (payeeName ?: "") + " " + trimmed).lowercase()
        val hasRewardClaim = REWARD_KEYWORDS.any { textToInspect.contains(it) }
        val isCollectOrDebit = isUpiUri || isVpa
        val hasIntentInversion = hasRewardClaim && isCollectOrDebit

        val statedIntent = when {
            hasRewardClaim -> "Claim prize/reward or receive incoming refund"
            URGENCY_KEYWORDS.any { textToInspect.contains(it) } -> "Urgent utility bill resolution or account unblock"
            CREDENTIAL_KEYWORDS.any { textToInspect.contains(it) } -> "Credential verification or security sync"
            else -> "Standard payment to merchant or individual contact"
        }

        return DecodedUpiPayload(
            rawUri = trimmed,
            payeeVpa = payeeVpa,
            payeeName = payeeName,
            amount = amount,
            currency = currency,
            transactionNote = note,
            merchantCode = mc,
            transactionRef = tr,
            isCollectOrDebit = isCollectOrDebit,
            statedIntentSummary = statedIntent,
            hasIntentInversion = hasIntentInversion
        )
    }
}
