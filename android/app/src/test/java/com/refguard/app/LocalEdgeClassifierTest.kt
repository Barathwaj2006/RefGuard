package com.refguard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalEdgeClassifierTest {

    private lateinit var classifier: LocalEdgeClassifier

    @Before
    fun setUp() {
        classifier = LocalEdgeClassifier()
    }

    // A1
    @Test
    fun testFakeViralReferral_A1() {
        val input = "🎉 You got ₹500! Claim your referral bonus now: bit.ly/xR7pQz2"
        val result = classifier.classifyText(input)
        assertTrue("Expected score >= 40 for shortlink + spam keywords", result.score >= 40)
        assertTrue(result.verdict == Verdict.REQUIRE_CONFIRMATION || result.verdict == Verdict.DISCOURAGE)
    }

    // A2
    @Test
    fun testFakeViralReferral_A2() {
        val input = "Your friend Priya invited you to earn ₹200 cashback. Download: paytm-rewards.in/claim"
        val result = classifier.classifyText(input)
        assertTrue("Expected DISCOURAGE", result.verdict == Verdict.DISCOURAGE)
    }

    // A4
    @Test
    fun testLegitReferral_A4() {
        val input = "Hey, check out this app my college uses for attendance: college-attend.edu.in"
        val result = classifier.classifyText(input)
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    // B1
    @Test
    fun testTamperedQrCode_B1() {
        val input = "upi://pay?pa=scammer123@oksbi&pn=Refund&am=1&cu=INR"
        val result = classifier.classifyUpiUri(input)
        assertEquals(Verdict.DISCOURAGE, result.verdict)
        assertTrue(result.contributingFactors.contains("known_scam_vpa +80"))
    }

    // B2
    @Test
    fun testLegitQrCode_B2() {
        val input = "upi://pay?pa=merchant.legit@okhdfcbank&pn=Cafe%20Coffee%20Day&am=250&cu=INR"
        val result = classifier.classifyUpiUri(input)
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    // C5
    @Test
    fun testFearKyc_C5() {
        val input = "Your bank account will be blocked. Update KYC: sbi-kyc-update.info"
        val result = classifier.classifyText(input)
        assertEquals(Verdict.DISCOURAGE, result.verdict)
        assertTrue(result.contributingFactors.contains("fear_kyc_language +35"))
    }

    // E1
    @Test
    fun testPaymentIntentMismatch_E1() {
        // Message claims receiving money but link asks to pay
        val input = "you're receiving ₹500, please pay to verify"
        val result = classifier.classifyText(input)
        assertEquals(Verdict.DISCOURAGE, result.verdict)
        assertTrue(result.contributingFactors.contains("payment_intent_mismatch +50"))
    }
}
