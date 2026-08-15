# RefGuard Threat Model

## 1. Attacker Goals
Threat actors utilizing the financial ecosystem generally aim to:
- Trick users into authorizing outbound transactions (Refund scams, advance-fee fraud).
- Steal sensitive credentials (UPI PINs, OTPs, CVVs) via phishing links.
- Intercept legitimate payments by replacing QRs or VPAs (Man-in-the-Middle).

## 2. Attack Surfaces
- **SMS/WhatsApp:** Primary vector for social engineering and phishing URLs.
- **Physical QR Codes:** Malicious stickers placed over legitimate merchant codes.
- **Clipboard/Share Intent:** Hijacking or injecting malicious VPAs when a user copies payment details.
- **Community Reporting:** Adversaries submitting false reports to poison the threat intelligence database and block legitimate users.

## 3. Privacy Risks & Mitigation
RefGuard analyzes personal messages and transaction intents. This presents a high privacy risk.
- **Mitigation:** RefGuard strictly operates on a "No Credentials" policy. The `ScanRequest` validator actively rejects payloads containing patterns resembling PINs, passwords, or CVVs.
- The `source_context` is used only for contextual AI modeling and is not stored tied to a permanent user identity.

## 4. False Positives & False Negatives
- **False Positives:** Flagging a legitimate transaction as a scam (e.g., inside jokes among friends containing scam keywords).
  - *Impact:* User friction.
  - *Mitigation:* Advisory-only decisions (`WARN_CAUTION`) with `override_allowed: true` empower the user to proceed anyway.
- **False Negatives:** Missing a sophisticated zero-day scam.
  - *Impact:* Financial loss.
  - *Mitigation:* The Community Reporting (`/api/v1/report`) allows fast crowd-sourced updates to the Threat Intelligence engine.

## 5. Why RefGuard Does NOT Control Payments
RefGuard provides an **advisory** `ProtectionDecision`. It is mathematically and legally dangerous for a third-party security layer to forcefully block or reverse financial transactions.
- **Liability:** Blocking a legitimate, time-critical payment (e.g., medical emergency) causes severe harm.
- **Ecosystem Boundaries:** UPI apps and banks are the authoritative executors. RefGuard acts as the intelligent advisor on the edge, empowering the user with the context needed to make the final, safe decision.
