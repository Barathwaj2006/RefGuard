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
        "refund", "bonus", "claim", "gift", "credited", "receive"
    )

    private val UTILITY_KEYWORDS = listOf(
        "electricity", "power", "disconnection", "disconnected", "cutoff",
        "bill", "discom", "utility", "urgent", "update"
    )

    private val UPI_URI_REGEX = Regex("upi://pay\\?[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
    private val VPA_REGEX = Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}")

    fun decode(content: String): DecodedUpiPayload? {
        val trimmed = content.trim()

        // 1. Direct UPI URI
        val directUpi = trimmed.startsWith("upi://pay", ignoreCase = true)
        // 2. Embedded UPI URI inside text
        val embeddedUpiMatch = UPI_URI_REGEX.find(trimmed)
        // 3. Direct or embedded VPA
        val isDirectVpa = trimmed.matches(Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$"))
        val embeddedVpaMatch = VPA_REGEX.find(trimmed)

        val uriToParse = when {
            directUpi -> trimmed
            embeddedUpiMatch != null -> embeddedUpiMatch.value
            else -> null
        }

        if (uriToParse == null && !isDirectVpa && embeddedVpaMatch == null) {
            return null
        }

        var payeeVpa: String? = null
        var payeeName: String? = null
        var amount: Double? = null
        var currency = "INR"
        var note: String? = null
        var mc: String? = null
        var tr: String? = null

        if (uriToParse != null) {
            try {
                val query = uriToParse.substringAfter("?", "")
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
                // Graceful fallback
            }
        } else if (isDirectVpa) {
            payeeVpa = trimmed
        } else if (embeddedVpaMatch != null) {
            payeeVpa = embeddedVpaMatch.value
        }

        val textToInspect = ((note ?: "") + " " + (payeeName ?: "") + " " + trimmed).lowercase()
        val hasRewardClaim = REWARD_KEYWORDS.any { textToInspect.contains(it) }
        val hasUtilityUrgency = UTILITY_KEYWORDS.any { textToInspect.contains(it) }
        val isCollectOrDebit = (payeeVpa != null)

        // Inversion: user promised reward/refund OR panicked into paying unverified utility
        val hasIntentInversion = (hasRewardClaim || hasUtilityUrgency) && isCollectOrDebit &&
                !(payeeVpa?.contains("swiggy", true) == true || payeeVpa?.contains("zomato", true) == true)

        val statedIntent = when {
            hasRewardClaim -> "Claim ₹${amount?.toInt() ?: "cash"} reward / cashback"
            hasUtilityUrgency -> "Resolve electricity bill / prevent power disconnection"
            textToInspect.contains("task") || textToInspect.contains("job") -> "Activate daily part-time task earnings"
            textToInspect.contains("parcel") || textToInspect.contains("delivery") -> "Pay ₹5 parcel redelivery fee"
            textToInspect.contains("support") || textToInspect.contains("refund") -> "Receive ₹${amount?.toInt() ?: ""} failed transaction refund"
            else -> "Standard payment to merchant/recipient"
        }

        return DecodedUpiPayload(
            rawUri = uriToParse ?: trimmed,
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
