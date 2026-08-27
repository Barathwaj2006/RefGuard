# SECURITY.md — Privacy & Security Architecture (RefGuard)

## Security Philosophy & Boundary of Operation
RefGuard operates strictly on a **pre-execution advisory model**:
- RefGuard evaluates transaction payloads, URIs, QR codes, and psychological lures **before** the user authorizes a transaction or opens a UPI payment application.
- **Banking Layer Isolation:** RefGuard does not execute, intercept, modify, or reverse transactions at the NPCI/banking layer. It operates at the user awareness and presentation level to break social engineering manipulation before user authorization.

---

## Threat Model & Scope
RefGuard is engineered to identify:
1. **Payment-Intent Inversions**: Adversaries enticing users with incoming reward/subsidy/refund claims while presenting an outbound `upi://pay` debit payload or collect request.
2. **Deceptive Referrals & Tasks**: Phishing links, fake task invitations, and advance-fee schemes embedded in SMS or instant messaging texts.
3. **Malicious / Tampered QR Codes**: QR codes embedding manipulated parameters, unexpected target VPAs, or concealed URL redirects.
4. **Credential & PIN Harvesting Lures**: Requests urging users to enter a UPI PIN, OTP, or CVV to "verify" or "receive" payments.

---

## What RefGuard Does NOT Guarantee
- **Not a Banking Interceptor:** RefGuard does not replace bank-side transaction monitoring or hardware token security.
- **No Transaction Cancellation:** Once a transaction is authorized by the user in their UPI app with their secret UPI PIN, funds transfer is irrevocable through RefGuard; the user must immediately contact their bank and dial the National Cybercrime Helpline at **1930**.

---

## Privacy & Data Protection Architecture

### 1. Zero Credential Collection
RefGuard never asks for, captures, logs, or transmits:
- UPI PINs
- Netbanking passwords
- Debit/Credit card CVVs / Expiry dates
- SMS One-Time Passwords (OTPs)

### 2. Explicit User-Initiated Ingress
RefGuard analyzes data only upon explicit user action:
- Direct input into the **Analyze** text interface
- Active camera viewfinder targeting during **QR Scanning**
- User-directed tap of the **Android Share Sheet** target (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`)
- 1-tap **Clipboard Ingestion** initiated by the user

RefGuard does not utilize continuous background accessibility scraping, keeping battery and privacy impact minimal.

### 3. Local Edge Processing & Data Retention
- The primary classifier (`LocalEdgeClassifier`) executes completely on-device without network dependency.
- Stored history (`InvestigationHistoryManager`) and offline queues (`OfflineScanQueue`) reside entirely in on-device private app storage (`Context.MODE_PRIVATE`) and are never sent to third-party tracking services.

---

## Offline Security Architecture
When internet connectivity is absent:
- `LocalEdgeClassifier` leverages an embedded 40+ token logistic weight matrix, heuristic syntax parsers, and threat pattern detectors.
- Requests failing network transmission are safely queued locally in `OfflineScanQueue` without credentials or sensitive identifiers.

---

## Input Validation & Robustness
- **UPI Scheme Decoder (`UpiIntentDecoder`)**: Strictly validates `upi://pay` URI specifications, stripping unexpected control characters and discarding malformed query components.
- **Network Request Validation**: API data transfer objects use strict schemas, immutable Kotlin data classes, and type-safe enums (`ContentType`, `RiskSeverity`).
- **Network Security Configuration**: Android Network Security Config (`res/xml/network_security_config.xml`) disables cleartext HTTP traffic in production release builds.

---

## Secrets & Dependency Management
- Keystores and private signing credentials are parameterized through environment variables (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and not hardcoded into Git.
- Third-party dependencies are pinned in Gradle Version Catalog (`gradle/libs.versions.toml`).

---

## Responsible Vulnerability Disclosure
If you discover a security vulnerability in RefGuard, please report it responsibly by contacting the maintainers directly or opening a private GitHub Security Advisory. We acknowledge all reports within 48 hours.
