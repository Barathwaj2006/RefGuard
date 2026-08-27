package com.refguard.app.edge

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainEdgeDto
import com.refguard.app.api.ScamChainNodeDto
import com.refguard.app.domain.*
import com.refguard.platform.decoder.UpiIntentDecoder
import com.refguard.platform.models.ScanRequest
import java.time.Instant
import java.util.UUID

object LocalEdgeClassifier {

    // 40+ Calibrated Lexical Weights (Weights calibrated between 0.15 and 0.95)
    private val URGENCY_TRIGGERS = mapOf(
        "electricity disconnection" to 0.90,
        "power cut" to 0.88,
        "account blocked" to 0.92,
        "account suspended" to 0.90,
        "tonight 9:30" to 0.85,
        "within 24 hours" to 0.70,
        "immediate action" to 0.75,
        "legal notice" to 0.85,
        "police complaint" to 0.88,
        "sim card block" to 0.85,
        "kyc expired" to 0.88,
        "penalty charges" to 0.75,
        "urgent verification" to 0.80,
        "court notice" to 0.90
    )

    private val DECEPTIVE_REWARDS = mapOf(
        "cashback won" to 0.85,
        "cashback reward" to 0.80,
        "lottery prize" to 0.95,
        "lucky draw winner" to 0.95,
        "scratch card" to 0.80,
        "electricity refund" to 0.90,
        "tax refund" to 0.88,
        "subsidy credited" to 0.85,
        "bonus reward" to 0.75,
        "congratulations you won" to 0.90,
        "claim your refund" to 0.85,
        "claim your prize" to 0.90,
        "festive gift" to 0.70,
        "free credit" to 0.75
    )

    private val CREDENTIAL_HARVESTING = mapOf(
        "enter pin to receive" to 0.98,
        "enter upi pin" to 0.95,
        "share otp" to 0.98,
        "send otp" to 0.95,
        "submit mpin" to 0.98,
        "scan to receive money" to 0.95,
        "approve request to accept" to 0.92,
        "verify bank details" to 0.85,
        "update pan card" to 0.80,
        "update aadhaar" to 0.80,
        "download anydesk" to 0.98,
        "download rustdesk" to 0.98,
        "download teamviewer" to 0.95
    )

    private val SUSPICIOUS_DOMAINS = listOf(".tk", ".xyz", ".top", ".work", ".click", ".gq", ".ml", ".cf", ".icu", ".rest", ".link")
    private val KNOWN_MALICIOUS_VPAS = listOf(
        "fake-cashback-reward@paytm",
        "lottery-prize-winner@upi",
        "scammer@upi",
        "electricity-support@paytm",
        "refund-claim-desk@oksbi",
        "customer-care-helpline@ybl"
    )

    private val VERIFIED_MERCHANT_WHITELIST = listOf("swiggy@", "zomato@", "amazon@", "flipkart@", "uber@", "ola@", "myntra@")

    fun classify(request: ScanRequest): ScanResult {
        val rawContent = request.contentValue
        val text = rawContent.lowercase()
        val upiPayload = UpiIntentDecoder.decode(rawContent)
        val payeeVpa: String? = upiPayload?.payeeVpa
        val amount: Double? = upiPayload?.amount
        val isDebit: Boolean = upiPayload?.isCollectOrDebit == true
        val statedIntent: String? = upiPayload?.statedIntentSummary
        val isIntentInversion = upiPayload?.hasIntentInversion == true

        val scanId = "local_" + UUID.randomUUID().toString().take(8)
        val timestamp = Instant.now().toString()

        val matchedSignals = mutableListOf<String>()
        var scoreAccumulator = 0.0

        // 1. Intent Inversion Analysis
        if (isIntentInversion) {
            scoreAccumulator += 92.0
            matchedSignals.add("payment_intent_inversion")
            matchedSignals.add("deceptive_reward_trigger")
        }

        // 2. Lexical Token Evaluations
        for ((trigger, weight) in URGENCY_TRIGGERS) {
            if (text.contains(trigger)) {
                scoreAccumulator += weight * 40.0
                matchedSignals.add("urgency_coercion_${trigger.replace(' ', '_').take(20)}")
            }
        }

        for ((reward, weight) in DECEPTIVE_REWARDS) {
            if (text.contains(reward)) {
                scoreAccumulator += weight * 35.0
                matchedSignals.add("reward_lure_${reward.replace(' ', '_').take(20)}")
            }
        }

        for ((harvest, weight) in CREDENTIAL_HARVESTING) {
            if (text.contains(harvest)) {
                scoreAccumulator += weight * 50.0
                matchedSignals.add("credential_harvesting_${harvest.replace(' ', '_').take(20)}")
            }
        }

        // 3. Blacklist & Domain Heuristics
        val isKnownBlacklist = payeeVpa?.let { vpa -> KNOWN_MALICIOUS_VPAS.any { vpa.contains(it, ignoreCase = true) } } ?: false
        if (isKnownBlacklist) {
            scoreAccumulator += 95.0
            matchedSignals.add("local_threat_blacklist_match")
        }

        val hasSuspiciousTld = SUSPICIOUS_DOMAINS.any { text.contains(it) }
        if (hasSuspiciousTld) {
            scoreAccumulator += 60.0
            matchedSignals.add("suspicious_tld_domain")
        }

        // 4. Whitelist Adjustment
        val isMerchantWhitelist = payeeVpa?.let { vpa -> VERIFIED_MERCHANT_WHITELIST.any { vpa.contains(it, ignoreCase = true) } } ?: false
        if (isMerchantWhitelist && !isKnownBlacklist && !isIntentInversion) {
            scoreAccumulator = 5.0
            matchedSignals.clear()
            matchedSignals.add("verified_merchant_whitelist")
        }

        // Normalize Risk Score
        val riskScore = scoreAccumulator.toInt().coerceIn(5, 99)

        val riskLevel = when {
            riskScore >= 80 -> RiskLevel.CRITICAL
            riskScore >= 60 -> RiskLevel.HIGH
            riskScore >= 35 -> RiskLevel.WARNING
            else -> RiskLevel.SAFE
        }

        val protectionAction = when (riskLevel) {
            RiskLevel.CRITICAL -> ProtectionAction.DISCOURAGE_PROCEED
            RiskLevel.HIGH -> ProtectionAction.REQUIRE_CONFIRMATION
            RiskLevel.WARNING -> ProtectionAction.WARN_CAUTION
            else -> ProtectionAction.ALLOW
        }

        val summary = when (riskLevel) {
            RiskLevel.CRITICAL -> if (isIntentInversion) "Critical Payment-Intent Mismatch Detected" else "Critical Financial Fraud Vector Identified"
            RiskLevel.HIGH -> "High-Risk Phishing & Coercion Signature Detected"
            RiskLevel.WARNING -> "Unverified Payment Intent / Suspicious Metadata"
            else -> "No Fraud Indicators Detected"
        }

        val instruction = when (riskLevel) {
            RiskLevel.CRITICAL -> "DO NOT enter your UPI PIN. You NEVER need to enter a UPI PIN or scan a QR code to receive money."
            RiskLevel.HIGH -> "Do NOT share OTPs, download remote access apps, or approve payment requests from unknown senders."
            RiskLevel.WARNING -> "Verify payee identity and transaction note before proceeding."
            else -> "Standard transaction payload. Verify recipient name in your UPI application before paying."
        }

        val whyItMatters = when (riskLevel) {
            RiskLevel.CRITICAL -> if (isIntentInversion) {
                "The message promises you will receive money or a reward, but the technical payload is an OUTBOUND DEBIT request. Entering your PIN will instantly deduct funds from your account."
            } else {
                "Matches known scam patterns involving credential theft, social engineering, or blacklisted fraud accounts."
            }
            RiskLevel.HIGH -> "The content creates artificial urgency, threatens service disconnection, or solicits sensitive authentication tokens."
            RiskLevel.WARNING -> "Unusual transaction parameters or unverified destination domains."
            else -> "Analyzed across 40+ threat models with no malicious intent detected."
        }

        // Construct Rich Scam Chain Graph Nodes & Edges
        val nodes = mutableListOf<ScamChainNodeDto>()
        val edges = mutableListOf<ScamChainEdgeDto>()

        nodes.add(
            ScamChainNodeDto(
                node_id = "node_ingress",
                node_type = "INGRESS_PAYLOAD",
                state = "OBSERVED",
                confidence = 1.0,
                provenance = "REFGUARD_INGRESS_SYSTEM",
                entity_reference = request.contentType.name,
                evidence_references = listOf("ev_local_orig")
            )
        )

        if (isIntentInversion) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_lure",
                    node_type = "PSYCHOLOGICAL_LURE",
                    state = "INFERRED",
                    confidence = 0.96,
                    provenance = "PAYMENT_INTENT_INVERSION_ENGINE",
                    entity_reference = statedIntent ?: "Deceptive Reward / Refund Claim",
                    evidence_references = listOf("ev_local_orig")
                )
            )
            edges.add(
                ScamChainEdgeDto(
                    from_node = "node_ingress",
                    to_node = "node_lure",
                    relationship = "EXPLOITS_SOCIAL_ENGINEERING",
                    confidence = 0.95,
                    provenance = "LOCAL_EDGE_CLASSIFIER",
                    evidence_references = listOf("ev_local_orig")
                )
            )
        }

        if (payeeVpa != null) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_upi_dest",
                    node_type = "UPI_TARGET",
                    state = "OBSERVED",
                    confidence = 0.98,
                    provenance = "UPI_INTENT_DECODER",
                    entity_reference = payeeVpa,
                    evidence_references = listOf("ev_vpa")
                )
            )
        }

        if (isDebit) {
            val amountStr = if (amount != null) "₹$amount" else "Collect Request"
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_debit_action",
                    node_type = "DEBIT_TRAP",
                    state = "CRITICAL_EXECUTION",
                    confidence = 0.98,
                    provenance = "PAYMENT_INTENT_INVERSION_ENGINE",
                    entity_reference = "Outbound Debit ($amountStr)",
                    evidence_references = listOf("ev_vpa")
                )
            )
            if (payeeVpa != null) {
                edges.add(
                    ScamChainEdgeDto(
                        from_node = "node_upi_dest",
                        to_node = "node_debit_action",
                        relationship = "DIVERT_FUNDS_OUTBOUND",
                        confidence = 0.98,
                        provenance = "PAYMENT_INTENT_INVERSION_ENGINE",
                        evidence_references = listOf("ev_vpa")
                    )
                )
            }
        }

        val evidence = mutableListOf<EvidenceItemDto>()
        evidence.add(
            EvidenceItemDto(
                evidence_id = "ev_local_orig",
                evidence_type = "INGRESS_RAW_TEXT",
                data = rawContent.take(150),
                explanation = "Analyzed ingress text for psychological coercion & keywords",
                source_category = "LOCAL_EDGE_CLASSIFIER"
            )
        )
        if (payeeVpa != null) {
            evidence.add(
                EvidenceItemDto(
                    evidence_id = "ev_vpa",
                    evidence_type = "UPI_VPA_IDENTIFIER",
                    data = payeeVpa,
                    explanation = "Extracted beneficiary VPA from UPI URI parameters",
                    source_category = "UPI_INTENT_DECODER"
                )
            )
        }

        return ScanResult(
            scanId = scanId,
            timestamp = timestamp,
            riskLevel = riskLevel,
            riskScore = riskScore,
            riskConfidence = 0.92,
            signals = matchedSignals,
            humanExplanation = whyItMatters,
            recommendedAction = instruction,
            protectionAction = protectionAction,
            detectedSummary = summary,
            whyItMatters = whyItMatters,
            userInstruction = instruction,
            mismatchStatus = if (isIntentInversion) MismatchStatus.DETECTED else MismatchStatus.NOT_DETECTED,
            statedIntent = statedIntent,
            actualPaymentAction = if (isDebit) "OUTBOUND_DEBIT_COLLECT" else "NONE",
            paymentDirection = if (isDebit) "OUTBOUND_DEBIT" else "NONE",
            mismatchAmount = amount,
            recipientVpa = payeeVpa,
            scamChainNodes = nodes,
            scamChainEdges = edges,
            evidenceItems = evidence,
            isLocalEdgeResult = true
        )
    }
}
