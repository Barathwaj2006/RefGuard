package com.refguard.app

import com.refguard.app.api.*
import com.refguard.app.domain.*
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.ScanRequest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for domain model mappers.
 * These run as plain JVM unit tests (no Android runtime required).
 */
class DomainMappersTest {

    // ──────────────────────────────────────────────
    // Contract → Domain mapping
    // ──────────────────────────────────────────────

    private fun makeScanResponse(
        severity: String = "LOW",
        action: String = "ALLOW",
        mismatchStatus: String? = null
    ) = ScanResponseDto(
        scan_id = "scan_test_001",
        timestamp = "2026-01-01T00:00:00Z",
        risk_assessment = RiskAssessmentDto(
            risk_score = 5,
            risk_severity = severity,
            confidence = 0.95,
            signals = listOf("known_safe_upi"),
            evidence_references = null,
            human_explanation = "This VPA is verified and has no scam history.",
            recommended_action = "Safe to proceed."
        ),
        protection_decision = ProtectionDecisionDto(
            action = action,
            detected_summary = "No threats detected.",
            why_it_matters = "Keeping your money safe.",
            user_instruction = "You can proceed safely."
        ),
        payment_intent_mismatch = mismatchStatus?.let {
            PaymentIntentMismatchDto(
                status = it,
                stated_intent = "pay merchant",
                actual_payment_action = "transfer to unknown",
                payment_direction = "OUTBOUND_DEBIT",
                amount = 500.0,
                recipient_vpa = "scammer@upi",
                confidence = 0.91,
                provenance = "EXTRACTED",
                evidence = listOf("ev_001")
            )
        },
        scam_chain = null,
        evidence_pack = null
    )

    @Test
    fun `LOW severity maps to SAFE risk level`() {
        val result = makeScanResponse(severity = "LOW").toDomain()
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `MEDIUM severity maps to WARNING risk level`() {
        val result = makeScanResponse(severity = "MEDIUM").toDomain()
        assertEquals(RiskLevel.WARNING, result.riskLevel)
    }

    @Test
    fun `HIGH severity maps to HIGH risk level`() {
        val result = makeScanResponse(severity = "HIGH").toDomain()
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    @Test
    fun `CRITICAL severity maps to CRITICAL risk level`() {
        val result = makeScanResponse(severity = "CRITICAL").toDomain()
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun `unknown severity maps to UNKNOWN risk level`() {
        val result = makeScanResponse(severity = "SOMETHING_ELSE").toDomain()
        assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
    }

    @Test
    fun `ALLOW action maps correctly`() {
        val result = makeScanResponse(action = "ALLOW").toDomain()
        assertEquals(ProtectionAction.ALLOW, result.protectionAction)
    }

    @Test
    fun `REQUIRE_CONFIRMATION action maps correctly`() {
        val result = makeScanResponse(action = "REQUIRE_CONFIRMATION").toDomain()
        assertEquals(ProtectionAction.REQUIRE_CONFIRMATION, result.protectionAction)
    }

    @Test
    fun `DISCOURAGE_PROCEED action maps correctly`() {
        val result = makeScanResponse(action = "DISCOURAGE_PROCEED").toDomain()
        assertEquals(ProtectionAction.DISCOURAGE_PROCEED, result.protectionAction)
    }

    @Test
    fun `DETECTED mismatch maps correctly`() {
        val result = makeScanResponse(mismatchStatus = "DETECTED").toDomain()
        assertEquals(MismatchStatus.DETECTED, result.mismatchStatus)
        assertEquals("scammer@upi", result.recipientVpa)
        assertEquals(500.0, result.mismatchAmount)
    }

    @Test
    fun `NOT_OBSERVED mismatch maps correctly`() {
        val result = makeScanResponse(mismatchStatus = "NOT_OBSERVED").toDomain()
        assertEquals(MismatchStatus.NOT_OBSERVED, result.mismatchStatus)
    }

    @Test
    fun `null mismatch gives null status`() {
        val result = makeScanResponse(mismatchStatus = null).toDomain()
        assertNull(result.mismatchStatus)
        assertNull(result.recipientVpa)
    }

    @Test
    fun `scan id and timestamp are preserved`() {
        val result = makeScanResponse().toDomain()
        assertEquals("scan_test_001", result.scanId)
        assertEquals("2026-01-01T00:00:00Z", result.timestamp)
    }

    // ──────────────────────────────────────────────
    // Platform ScanRequest → API DTO
    // ──────────────────────────────────────────────

    @Test
    fun `TEXT ContentType maps to correct contract string`() {
        val request = ScanRequest(ContentType.TEXT, "hello", "ctx", "2026-01-01T00:00:00Z")
        val dto = request.toDto()
        assertEquals("TEXT", dto.content_type)
        assertEquals("hello", dto.content_value)
        assertEquals("ctx", dto.source_context)
    }

    @Test
    fun `UPI_VPA ContentType maps correctly`() {
        val request = ScanRequest(ContentType.UPI_VPA, "user@bank", "manual", "2026-01-01T00:00:00Z")
        val dto = request.toDto()
        assertEquals("UPI_VPA", dto.content_type)
    }

    @Test
    fun `QR ContentType maps correctly`() {
        val request = ScanRequest(ContentType.QR, "qrdata", "camera", "2026-01-01T00:00:00Z")
        val dto = request.toDto()
        assertEquals("QR", dto.content_type)
    }

    @Test
    fun `IMAGE ContentType maps correctly`() {
        val request = ScanRequest(ContentType.IMAGE, "base64==", "gallery", "2026-01-01T00:00:00Z")
        val dto = request.toDto()
        assertEquals("IMAGE", dto.content_type)
    }

    @Test
    fun `blank source context becomes null in DTO`() {
        val request = ScanRequest(ContentType.TEXT, "hello", "", "2026-01-01T00:00:00Z")
        val dto = request.toDto()
        assertNull(dto.source_context)
    }
}
