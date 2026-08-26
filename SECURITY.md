# SECURITY.md — Privacy & Security Boundaries (RefGuard)

## Security Philosophy
RefGuard operates on an **advisory-only** model. It evaluates threats and provides actionable intelligence to the user before they execute a payment. RefGuard does not have the capability to execute, block, or reverse transactions at the banking layer. Our goal is to break the psychological manipulation chain through timely intervention.

## Threat Model
RefGuard is designed to detect:
- **Payment-Intent Mismatches:** Attackers tricking users into authorizing a UPI debit under the guise of receiving money.
- **Deceptive Referrals & Tasks:** Ponzi schemes, advance-fee frauds, and fake job offers propagating via SMS/WhatsApp.
- **Malicious QR Codes:** Tampered or deceptive QR codes designed to initiate unauthorized upi://pay intents.
- **Impersonation:** Lures attempting to impersonate legitimate entities (e.g., customer support, delivery services).

## What RefGuard Does NOT Guarantee
- **RefGuard does NOT prevent all scams.** It is an additional layer of defense, not a silver bullet.
- **RefGuard does NOT intercept bank transactions.** It analyzes the intent *before* the UPI app is launched.
- **RefGuard cannot recover lost funds.** If a transaction is completed, users must contact their bank and report to 1930.

## Privacy Model
- **Zero Credential Collection:** RefGuard never requests, stores, or logs UPI PINs, passwords, OTPs, CVVs, or bank credentials.
- **Explicit Ingress:** RefGuard only scans content explicitly provided by the user (via the camera, clipboard paste, or share sheet). It does not employ background accessibility monitoring.
- **Local Edge Priority:** The primary classifier (LocalEdgeClassifier) runs entirely on-device, meaning sensitive SMS or URLs are analyzed without ever leaving the phone.
- **Evidence Sanitization:** When evidence is displayed in the Investigation screen or Threat Lab, known sensitive patterns (like partial PANs or phone numbers) are masked or stripped from the risk analysis logs.

## Offline Security
RefGuard features a robust offline fallback. The LocalEdgeClassifier contains an embedded, calibrated logistic feature ensemble and a pre-loaded threat domain list. If the device has no internet connection, it can still detect high-risk structural and lexical anomalies, ensuring users are protected in low-connectivity scenarios.

## Input Validation
All incoming intents and API requests are subject to strict JSON schema and structural validation.
- The UpiIntentDecoder strictly parses upi://pay schemas, ignoring extraneous or malformed parameters.
- Backend API endpoints enforce strict length, type, and character set limits on incoming analysis requests to prevent injection attacks.

## API Security
- **CORS:** The backend API restricts cross-origin requests to trusted integration domains.
- **Rate Limiting:** IP-based rate limiting is applied to the public /scan and /threats endpoints to prevent abuse and scraping.
- **Timeouts:** Strict timeouts prevent resource exhaustion from malformed or maliciously slow client requests.

## Logging
- **Structured Logging:** The application logs structural anomalies (e.g., parsing failures) and threat metrics without logging the raw sensitive payload.
- No PII (Personally Identifiable Information) is persisted in server-side logs. 

## Secrets
- RefGuard relies on environment variables for all internal secrets (e.g., API keys, release keystore passwords).
- The CI/CD pipeline injects secrets securely at build time. There are no hardcoded keys in the repository.

## Dependency Security
- Dependencies are managed via Gradle (Android) and npm (Backend).
- We utilize automated dependency scanning (via GitHub Dependabot) to monitor and patch known vulnerabilities in third-party libraries.

## Reporting a Vulnerability
If you discover a security vulnerability in RefGuard, please do NOT file a public issue.
Instead, responsibly disclose it by emailing the project maintainers directly or submitting a draft security advisory via GitHub. We will acknowledge receipt within 48 hours.

## Known Limitations
- The edge model relies on a static vocabulary of threat tokens. While it generalizes well, sophisticated adversaries may evade it by using entirely novel vocabulary until the model is updated.
- Optical Character Recognition (OCR) for screenshots is dependent on the device's ML Kit capabilities and image quality, which may occasionally result in false negatives.
