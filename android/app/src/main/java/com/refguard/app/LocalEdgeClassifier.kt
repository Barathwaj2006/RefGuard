package com.refguard.app

import java.net.URI
import java.util.Locale

data class ClassificationResult(
    val score: Int, // 0 to 100
    val verdict: Verdict,
    val contributingFactors: List<String>
)

enum class Verdict {
    ALLOW,
    REQUIRE_CONFIRMATION,
    DISCOURAGE
}

class LocalEdgeClassifier {

    private val maliciousTlds = setOf(".xyz", ".co", ".in", ".info")
    private val spamKeywords = listOf("claim", "bonus", "cashback", "refer", "free", "limited slots", "earn")
    private val urgencyKeywords = listOf("urgent", "last chance", "expires", "minutes", "mins")
    private val fearKeywords = listOf("blocked", "kyc", "update", "account will be")
    private val knownScamVpas = setOf("scammer123@oksbi", "refund.helpdesk@oksbi")
    private val knownWhitelistVpas = setOf("merchant.legit@okhdfcbank", "amazon@apl")

    fun classifyText(text: String): ClassificationResult {
        var score = 0
        val factors = mutableListOf<String>()
        val lowerText = text.lowercase(Locale.getDefault())

        // 1. Check for URL shorteners or suspicious TLDs
        if (lowerText.contains("bit.ly/") || lowerText.contains("t.co/") || lowerText.contains("tinyurl.com/")) {
            score += 30
            factors.add("url_shortener +30")
        }
        
        for (tld in maliciousTlds) {
            // Very naive check for TLDs
            if (lowerText.contains(tld) && !lowerText.contains(".edu.in")) {
                // Ignore .edu.in
                score += 10
                factors.add("suspicious_domain_tld +10")
                break
            }
        }
        
        // 2. Check for spam/greed keywords
        var keywordMatches = 0
        for (keyword in spamKeywords) {
            if (lowerText.contains(keyword)) {
                keywordMatches++
            }
        }
        if (keywordMatches > 0) {
            val addition = keywordMatches * 15
            score += addition
            factors.add("spam_keywords +$addition")
        }

        // 3. Check for urgency
        for (keyword in urgencyKeywords) {
            if (lowerText.contains(keyword)) {
                score += 25
                factors.add("urgency_language +25")
                break
            }
        }

        // 4. Check for fear/KYC patterns
        for (keyword in fearKeywords) {
            if (lowerText.contains(keyword)) {
                score += 35
                factors.add("fear_kyc_language +35")
                break
            }
        }

        // 5. Payment mismatch intent (Text vs Debit)
        // If message says "receive" or "refund" but asks for PIN or has a debit link
        if ((lowerText.contains("receive") || lowerText.contains("refund") || lowerText.contains("கிடைத்தது")) 
            && (lowerText.contains("upi pin") || lowerText.contains("pay") || lowerText.contains("click now"))) {
            score += 50
            factors.add("payment_intent_mismatch +50")
        }

        score = score.coerceIn(0, 100)

        val verdict = when {
            score >= 70 -> Verdict.DISCOURAGE
            score >= 40 -> Verdict.REQUIRE_CONFIRMATION
            else -> Verdict.ALLOW
        }

        return ClassificationResult(score, verdict, factors)
    }

    fun classifyUpiUri(uriString: String, contextText: String = ""): ClassificationResult {
        if (!uriString.startsWith("upi://pay")) {
            return ClassificationResult(0, Verdict.ALLOW, listOf("invalid_format"))
        }

        var score = 0
        val factors = mutableListOf<String>()
        
        // Extract params naively
        val vpaRegex = Regex("pa=([^&]+)")
        val match = vpaRegex.find(uriString)
        val vpa = match?.groupValues?.get(1)?.lowercase(Locale.getDefault())

        if (vpa != null) {
            if (knownScamVpas.contains(vpa)) {
                score += 80
                factors.add("known_scam_vpa +80")
            } else if (knownWhitelistVpas.contains(vpa)) {
                score -= 30 // Safe
                factors.add("whitelisted_merchant -30")
            } else if (vpa.contains("refund") || vpa.contains("cashback") || vpa.contains("support")) {
                score += 50
                factors.add("deceptive_vpa_keyword +50")
            }
        }

        // Check intent mismatch if context is given
        if (contextText.isNotEmpty()) {
            val textRes = classifyText(contextText)
            score += textRes.score
            factors.addAll(textRes.contributingFactors)
            
            // If the URI is a payment request, but context says refund
            if (contextText.lowercase(Locale.getDefault()).contains("refund")) {
                score += 60
                factors.add("context_payment_mismatch +60")
            }
        }

        score = score.coerceIn(0, 100)
        
        val verdict = when {
            score >= 70 -> Verdict.DISCOURAGE
            score >= 40 -> Verdict.REQUIRE_CONFIRMATION
            else -> Verdict.ALLOW
        }

        return ClassificationResult(score, verdict, factors)
    }
}
