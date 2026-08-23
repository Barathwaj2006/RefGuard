package com.refguard.platform.decoder

import org.junit.Assert.*
import org.junit.Test

class UpiIntentDecoderTest {

    @Test
    fun decode_validUpiPayUri_extractsAllFields() {
        val uri = "upi://pay?pa=merchant@okaxis&pn=SuperStore&am=500.00&cu=INR&tn=Order%20Payment"
        val result = UpiIntentDecoder.decode(uri)

        assertNotNull(result)
        assertEquals("merchant@okaxis", result?.payeeVpa)
        assertEquals("SuperStore", result?.payeeName)
        assertEquals(500.00, result?.amount ?: 0.0, 0.001)
        assertEquals("INR", result?.currency)
        assertEquals("Order Payment", result?.transactionNote)
        assertTrue(result?.isCollectOrDebit == true)
        assertFalse(result?.hasIntentInversion == true)
    }

    @Test
    fun decode_inversionUriWithRewardKeywords_detectsInversion() {
        val uri = "upi://pay?pa=scammer.loot@paytm&pn=LotteryReward&am=2500&tn=Cashback%20Claim"
        val result = UpiIntentDecoder.decode(uri)

        assertNotNull(result)
        assertEquals("scammer.loot@paytm", result?.payeeVpa)
        assertEquals(2500.0, result?.amount ?: 0.0, 0.001)
        assertTrue(result?.hasIntentInversion == true)
    }

    @Test
    fun decode_plainVpa_extractsVpa() {
        val vpa = "support.refund@okhdfc"
        val result = UpiIntentDecoder.decode(vpa)

        assertNotNull(result)
        assertEquals("support.refund@okhdfc", result?.payeeVpa)
        assertTrue(result?.hasIntentInversion == true) // contains 'refund' keyword
    }

    @Test
    fun decode_nonUpiText_returnsNull() {
        val text = "Hello world, this is a plain message"
        val result = UpiIntentDecoder.decode(text)
        assertNull(result)
    }
}
