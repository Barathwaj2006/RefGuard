# RefGuard Sample Payloads

This document provides realistic JSON payload examples for testing RefGuard's API v1.

## 1. Safe P2P Payment (Legitimate)
**Scenario:** A user is paying a friend for shared expenses.

```json
{
  "content_type": "TEXT",
  "content_value": "Hey, here is the 1500 for the concert tickets. Pay to amit.sharma@okaxis",
  "source_context": "com.whatsapp",
  "timestamp": "2023-10-25T14:30:00Z"
}
```
**Expected Outcome:** `risk_severity: "LOW"`, `action: "ALLOW"`

---

## 2. The Refund Scam (Payment Intent Mismatch)
**Scenario:** A scammer tricks a user into paying a fee to receive a larger sum.

```json
{
  "content_type": "TEXT",
  "content_value": "Congratulations! You won the Diwali lottery of ₹50,000. Send ₹50 processing fee to claim@upi immediately.",
  "source_context": "com.android.mms",
  "timestamp": "2023-10-25T15:00:00Z"
}
```
**Expected Outcome:**
- `PaymentIntentMismatch: { status: "DETECTED", stated_intent: "Receive money", payment_direction: "OUTBOUND_DEBIT" }`
- `risk_severity: "CRITICAL"`
- `action: "DISCOURAGE_PROCEED"`

---

## 3. The KYC Update Scam
**Scenario:** A fake bank alert urging the user to click a malicious link to avoid account suspension.

```json
{
  "content_type": "URL",
  "content_value": "http://update-kyc-hdfc-secure-verify.com/login",
  "source_context": "com.android.mms",
  "timestamp": "2023-10-25T16:15:00Z"
}
```
**Expected Outcome:** `risk_severity: "HIGH"`, `action: "WARN_CAUTION"` (or `DISCOURAGE_PROCEED` if the domain is a known threat).

---

## 4. QR Code Scam (Physical Context)
**Scenario:** A user scans a QR code at a local store that has been pasted over with a scammer's code.

```json
{
  "content_type": "QR",
  "content_value": "upi://pay?pa=scammer.123@ybl&pn=Fake+Store&am=500.00",
  "source_context": "com.google.android.apps.nbu.paisa.user",
  "timestamp": "2023-10-25T17:45:00Z"
}
```
*Note: Detection here relies on community reports or anomaly detection on the VPA (`scammer.123@ybl`) compared to the expected store location.*

---

## 5. Malformed Input (Testing Error Handling)
**Scenario:** A bad client implementation sends an invalid `content_type`.

```json
{
  "content_type": "PDF",
  "content_value": "base64encodedata...",
  "timestamp": "2023-10-25T18:00:00Z"
}
```
**Expected Outcome:** `400 Bad Request`, `error_code: "INVALID_REQUEST"` (Enum validation failure).

## 6. Safe Message (No Intent Detected)
**Scenario:** A general conversation with no financial context.

```json
{
  "content_type": "TEXT",
  "content_value": "Are we still on for the movie tonight at 8?",
  "source_context": "com.whatsapp",
  "timestamp": "2023-10-25T19:00:00Z"
}
```
**Expected Outcome:** `risk_severity: "LOW"`, `action: "ALLOW"`

---

## 7. Malicious UPI Identifier
**Scenario:** A user attempts to pay a VPA that is blacklisted in threat intelligence databases.

```json
{
  "content_type": "UPI_VPA",
  "content_value": "urgent-support-axis@okaxis",
  "source_context": "com.android.clipboard",
  "timestamp": "2023-10-25T19:30:00Z"
}
```
**Expected Outcome:** `risk_severity: "HIGH"`, `action: "DISCOURAGE_PROCEED"`

---

## 8. Ambiguous Input (Contextual Inference Required)
**Scenario:** A message that sounds urgent but lacks a direct call to action or link immediately. Scammers often use these to test the waters.

```json
{
  "content_type": "TEXT",
  "content_value": "Your package could not be delivered due to unpaid customs fee. Reply YES to proceed.",
  "source_context": "com.android.mms",
  "timestamp": "2023-10-25T20:00:00Z"
}
```
**Expected Outcome:** `risk_severity: "MEDIUM"`, `action: "WARN_CAUTION"` (System flags social engineering tactics like urgency and authority, but waits for the payment intent).
