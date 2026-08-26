package com.refguard.app.edge

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainNodeDto
import com.refguard.app.domain.*
import com.refguard.platform.decoder.UpiIntentDecoder
import com.refguard.platform.models.ScanRequest
import java.time.Instant
import java.util.UUID

object LocalEdgeClassifier {

    private val SUSPICIOUS_DOMAINS = listOf(".tk", ".xyz", ".top", ".work", ".click", ".gq", ".ml", ".cf")
    private val KNOWN_MALICIOUS_VPAS = listOf("fake-cashback-reward@paytm", "lottery-prize-winner@upi", "scammer@upi")

    fun classify(request: ScanRequest): ScanResult {
        val text = request.contentValue.lowercase()
        val upiPayload = UpiIntentDecoder.decode(request.contentValue)
        val payeeVpa: String? = upiPayload?.payeeVpa
        val amount: Double? = upiPayload?.amount
        val isDebit: Boolean = upiPayload?.isCollectOrDebit == true
        val statedIntent: String? = upiPayload?.statedIntentSummary

        val scanId = "local_" + UUID.randomUUID().toString().take(8)
        val timestamp = Instant.now().toString()

        val hasOtpSolicitation = (text.contains("otp") || text.contains("pin")) && 
                                (text.contains("share") || text.contains("enter") || text.contains("send") || text.contains("verify"))
        val hasSuspiciousTld = SUSPICIOUS_DOMAINS.any { text.contains(it) }
        val isKnownBlacklist = payeeVpa?.let { vpa -> KNOWN_MALICIOUS_VPAS.any { vpa.contains(it, ignoreCase = true) } } ?: false
        val isMerchantWhitelist = payeeVpa?.let { vpa -> listOf("swiggy@", "zomato@", "amazon@", "flipkart@").any { vpa.contains(it, ignoreCase = true) } } ?: false

        val isMismatch = upiPayload?.hasIntentInversion == true

        val riskLevel: RiskLevel
        val riskScore: Int
        val signals = mutableListOf<String>()

        when {
            isKnownBlacklist -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 95
                signals.add("local_threat_blacklist_match")
            }
            isMismatch -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 92
                signals.add("payment_intent_inversion")
                signals.add("deceptive_reward_trigger")
            }
            hasOtpSolicitation -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 85
                signals.add("credential_otp_harvesting")
            }
            hasSuspiciousTld -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 78
                signals.add("suspicious_tld_domain")
            }
            isMerchantWhitelist -> {
                riskLevel = RiskLevel.SAFE
                riskScore = 5
                signals.add("verified_merchant_whitelist")
            }
            upiPayload != null -> {
                riskLevel = RiskLevel.WARNING
                riskScore = 40
                signals.add("unverified_payment_request")
            }
            else -> {
                riskLevel = RiskLevel.SAFE
                riskScore = 10
            }
        }

        val protectionAction = when (riskLevel) {
            RiskLevel.CRITICAL -> ProtectionAction.DISCOURAGE_PROCEED
            RiskLevel.HIGH -> ProtectionAction.REQUIRE_CONFIRMATION
            RiskLevel.WARNING -> ProtectionAction.WARN_CAUTION
            else -> ProtectionAction.ALLOW
        }

        val summary = when (riskLevel) {
            RiskLevel.CRITICAL -> if (isMismatch) "Critical Payment-Intent Mismatch Detected" else "Known Fraud Signature Identified"
            RiskLevel.HIGH -> "High Risk Phishing Pattern Detected"
            RiskLevel.WARNING -> "Unverified Payment / Link"
            else -> "No Threats Detected"
        }

        val instruction = when (riskLevel) {
            RiskLevel.CRITICAL -> "DO NOT enter your UPI PIN. This will debit money from your account."
            RiskLevel.HIGH -> "Do not share OTPs, click suspicious links, or send funds."
            RiskLevel.WARNING -> "Verify sender identity before proceeding."
            else -> "Proceed with normal caution."
        }

        val whyItMatters = when (riskLevel) {
            RiskLevel.CRITICAL -> if (isMismatch) "They claimed you would receive money/rewards, but this request asks you to send ₹. Entering UPI PIN sends money." else "Matches verified threat database patterns."
            RiskLevel.HIGH -> "Content contains deceptive urgency or credential solicitation."
            RiskLevel.WARNING -> "Unknown payee or destination link."
            else -> "No threat patterns detected."
        }

        val nodes = mutableListOf<ScamChainNodeDto>()
        nodes.add(
            ScamChainNodeDto(
                node_id = "node_ingress",
                node_type = "MESSAGE",
                state = "OBSERVED",
                confidence = 1.0,
                provenance = "LOCAL_EDGE_CLASSIFIER",
                entity_reference = "Ingress Content",
                evidence_references = listOf("ev_local_orig")
            )
        )
        if (payeeVpa != null) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_upi",
                    node_type = "UPI_REQUEST",
                    state = "OBSERVED",
                    confidence = 0.95,
                    provenance = "UPI_INTENT_DECODER",
                    entity_reference = payeeVpa,
                    evidence_references = listOf("ev_vpa")
                )
            )
        }
        if (isDebit) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_debit",
                    node_type = "PAYMENT_ACTION",
                    state = "INFERRED",
                    confidence = 0.95,
                    provenance = "UPI_INTENT_DECODER",
                    entity_reference = "Outgoing Debit ₹$amount",
                    evidence_references = listOf("ev_vpa")
                )
            )
        }

        val evidence = mutableListOf<EvidenceItemDto>()
        evidence.add(
            EvidenceItemDto(
                evidence_id = "ev_local_orig",
                evidence_type = "ORIGINAL_CONTENT",
                data = request.contentValue.take(120),
                explanation = "Local input payload for edge analysis",
                source_category = "LOCAL_EDGE_CLASSIFIER"
            )
        )
        if (payeeVpa != null) {
            evidence.add(
                EvidenceItemDto(
                    evidence_id = "ev_vpa",
                    evidence_type = "UPI_IDENTIFIER",
                    data = payeeVpa,
                    explanation = "Extracted target VPA from payment intent",
                    source_category = "UPI_INTENT_DECODER"
                )
            )
        }

        return ScanResult(
            scanId = scanId,
            timestamp = timestamp,
            riskLevel = riskLevel,
            riskScore = riskScore,
            riskConfidence = 0.88,
            signals = signals,
            humanExplanation = whyItMatters,
            recommendedAction = instruction,
            protectionAction = protectionAction,
            detectedSummary = summary,
            whyItMatters = whyItMatters,
            userInstruction = instruction,
            mismatchStatus = if (isMismatch) MismatchStatus.DETECTED else MismatchStatus.NOT_DETECTED,
            statedIntent = statedIntent,
            actualPaymentAction = if (isDebit) "OUTBOUND_DEBIT_COLLECT" else "NONE",
            paymentDirection = if (isDebit) "OUTBOUND_DEBIT" else "NONE",
            mismatchAmount = amount,
            recipientVpa = payeeVpa,
            scamChainNodes = nodes,
            scamChainEdges = emptyList(),
            evidenceItems = evidence,
            isLocalEdgeResult = true
        )
    }
}
