# RefGuard — Universal UPI & Referral Scam Protection Platform

**RefGuard** is an ambient, AI-driven digital-payment and referral scam protection platform designed to stop financial fraud before the user authorizes payment or shares sensitive information.

Given a suspicious referral, payment interaction, SMS message, URL, UPI VPA, QR code, or screenshot, RefGuard automatically:
1. Ingests and normalizes the content across Android & Web ingress channels.
2. Extracts payment vectors, URLs, referral codes, and psychological manipulation triggers.
3. Performs real-time threat intelligence and local community scam matching.
4. Identifies **Payment-Intent Mismatches** (e.g. user believes they are receiving a prize/refund, but the underlying action triggers an outbound UPI debit).
5. Reconstructs the multi-step **Scam Chain** directed graph (Message → Referral → Shortlink → Landing Page → UPI Collect → Debit).
6. Compiles a traceable **Evidence Pack** with unique verifiable evidence IDs.
7. Computes explainable risk scores and delivers clear, unambiguous **Protective Action Guidance**.

---

## ⚡ The Golden Loop

```
[ USER CONTENT INGRESS ] 
(Share Sheet / QR / Screenshot / Clipboard / Manual Input)
          ↓
[ EXTRACTION & NORMALIZATION ] 
(UPI Pay URI, VPAs, URLs, Referral Codes, Amounts, Urgency Triggers)
          ↓
[ THREAT INTELLIGENCE & MISMATCH ANALYSIS ]
(Domain Reputation, Phishing Patterns, Stated Intent vs Actual Debit Action)
          ↓
[ GRAPH RECONSTRUCTION & EVIDENCE PACKING ]
(Scam Chain DAG, Traceable Evidence Item Registry)
          ↓
[ RISK ASSESSMENT & PROTECTIVE ACTION ]
(0-100 Score, Severity Level, Plain-Language Advisory, Direct User Instructions)
```

---

## 🚀 Quick Start & Running the Demo

### Prerequisites
- **Node.js**: v18+ (tested on v24)
- **Java / Android SDK** (optional for Android platform tests): JDK 17+

### 1. Run the Full Integration & Contract Test Suite
```bash
npm test
```

### 2. Launch the Interactive Demo Web UI & API Server
```bash
npm run demo
# or
npm start
```
Navigate to **`http://localhost:3000`** in your browser to interactively test all core flows (Fake Referrals, QR Scams, Payment Mismatch Traps, High-Risk VPAs, Screenshot OCR, and Clean Merchant Payments).

### 3. Run Android Platform Module Tests
```bash
cd android
./gradlew test
```

---

## 🛡️ Core MVP Flows Verified

| Flow | Input Type | Detection Mechanism | Protection Decision |
| :--- | :--- | :--- | :--- |
| **A. Fake Viral Referral** | URL / Text | Malicious TLD heuristic + Viral referral pattern | `REQUIRE_CONFIRMATION` / `DISCOURAGE_PROCEED` |
| **B. Tampered QR Code** | QR (`upi://pay`) | Known fraudulent VPA + Collect request analysis | `DISCOURAGE_PROCEED` (CRITICAL) |
| **C. Screenshot / Image OCR** | IMAGE (Base64) | OCR entity parsing + Advance-fee task scam detection | `REQUIRE_CONFIRMATION` (HIGH) |
| **D. High-Risk UPI VPA** | UPI_VPA | Community registry & deceptive keyword heuristic | `DISCOURAGE_PROCEED` (CRITICAL) |
| **E. Payment-Intent Mismatch** | Text / UPI | Semantic contrast: Stated Credit vs Actual Outbound Debit | `DISCOURAGE_PROCEED` ("DO NOT enter UPI PIN") |
| **F. Legitimate Merchant** | QR / VPA | Curated whitelist + Standard payment protocol | `ALLOW` (LOW Risk, Score < 30) |
| **G. Malformed / Invalid Input**| Any | Strict JSON Schema validation | `400 Bad Request` with structured error |
| **H. Safe Offline State** | Ingress | Local caching & community registry ingestion | Queued as `SuccessOffline` |

---

## 🔒 Privacy & Security First
- **Zero Credential Collection**: RefGuard never requests, stores, or logs UPI PINs, passwords, OTPs, CVVs, or bank credentials.
- **Explicit User Initiation**: Platform ingestion only operates upon explicit user action (Share Sheet click, Clipboard paste button, Camera QR scan, or Image import). No background monitoring or accessibility service abuse.

---

## 📂 Repository Architecture

```
RefGuard/
├── contracts/               # Authoritative Contract Layer (FROZEN API v1.0)
│   ├── api.yaml             # OpenAPI 3.0.3 specification
│   ├── schemas/             # 11 JSON Schema definitions (ScanRequest, ScanResponse, etc.)
│   └── examples/            # 9 Golden contract fixture examples
├── android/                 # Android Platform Ingestion Module
│   ├── platform/            # Library module (Ingress channels, permissions, offline queueing)
│   └── build.gradle.kts     # Root Android Gradle configuration
├── backend/                 # Core Detection Engine & API Service
│   ├── src/                 # Express app, Analyzer, Extractor pipelines, Threat Store
│   └── tests/               # Backend Jest test suites
├── integration/             # Web Demo & Integration UI
│   ├── demo/public/         # Frontend web application (Scanner, Evidence, Incident Recovery, Intel)
│   └── tests/               # Integration tests
└── package.json             # Root runner scripts
```
