# RefGuard

**Stop the scam before you pay.**

[![RefGuard Hackathon Release](https://github.com/Barathwaj2006/RefGuard/actions/workflows/release.yml/badge.svg)](https://github.com/Barathwaj2006/RefGuard/actions/workflows/release.yml)
[![RefGuard Full-System CI](https://github.com/Barathwaj2006/RefGuard/actions/workflows/ci.yml/badge.svg)](https://github.com/Barathwaj2006/RefGuard/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Barathwaj2006/RefGuard?label=Hackathon%20Build)](https://github.com/Barathwaj2006/RefGuard/releases/tag/hackathon-build)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

---

## Download

### Latest Hackathon Release APK

Directly installable on any modern Android device or emulator (Android 8.0+ / API 26+).

- 📦 **Release APK:** [Download `app-release.apk`](https://github.com/Barathwaj2006/RefGuard/releases/download/hackathon-build/app-release.apk)
- 🏷️ **Release Tag:** `hackathon-build`
- 🛡️ **Package / Application ID:** `com.aistudio.refguard.upiprot`
- 🔢 **Version:** `1.0.0` (Version Code `1`)
- 📏 **Size:** `24.2 MB` (Release optimized with R8)
- 🔐 **SHA-256:** `905f1207ab2306278b98221283d9baa5eadeb28e72925208b4e641c501ce95c7`

---

## The Problem

Social engineering payment fraud across UPI (Unified Payments Interface), SMS lures, and QR codes is causing massive financial harm:
- Scammers manipulate victims into believing they are **receiving** funds (lottery winnings, electricity refund, cashback rewards, OLX payment).
- In reality, the victim is prompted to authorize a **UPI Collect request** or scan an **outbound debit QR code**.
- Because users are mentally primed for an inbound reward, they enter their UPI PIN—which **only ever debits funds** from their bank account.
- Banking fraud systems operate *after* payment authorization, when the money has already left the account.

## The Solution

**RefGuard** is an **intent-level pre-execution protection system** for Android that intercepts the scam *before* the user opens their UPI banking app or enters their PIN. 

By comparing the **stated psychological promise** (e.g. *"Claim ₹5,000 electricity subsidy"*) with the **underlying technical transaction payload** (e.g. `upi://pay?pa=scammer@ybl&am=5000`), RefGuard identifies payment-intent inversions and adversarial deception on-device.

```
┌────────────────────────────────────────────────────────┐
│                        PROMISE                         │
│       "You will RECEIVE ₹5,000 electricity refund"     │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                    ACTUAL PROTOCOL                     │
│    upi://pay?pa=scammer@ybl&pn=Refund&am=5000&cu=INR   │
│             (Debit ₹5,000 from YOUR account)           │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                 RefGuard Intent Engine                 │
│  [!] Intent Inversion Detected: Inbound vs Outbound    │
│  [!] High-Risk VPA Pattern & Social Engineering Token  │
│  ────────────────────────────────────────────────────  │
│  VERDICT: CRITICAL THREAT (Score 96/100) — BLOCKED     │
└────────────────────────────────────────────────────────┘
```

---

## Core Innovation: Payment-Intent Inversion Detection

RefGuard solves the fundamental cognitive vulnerability in UPI transactions:

1. **Protocol & Scheme Extraction**: Decodes standard and non-standard `upi://pay` schemas, extracting Virtual Payment Addresses (VPA), payees, amounts, transaction notes (`tn`), and transaction references (`tr`).
2. **Intent Inversion Reasoner**: Analyzes stated contextual promises against actual transaction directions. Entering a UPI PIN **never** receives money; RefGuard flags any inbound promise tied to an outbound debit intent as a **CRITICAL THREAT**.
3. **LocalEdgeClassifier**: Lightweight on-device logistic feature ensemble evaluating 40+ lexical token weights, urgency triggers, PIN harvesting indicators, phishing domains, and payee structural patterns without requiring internet connectivity.
4. **ScamChain Directed Threat Graph**: Maps out the adversary's multi-step attack chain (Lure Message → Referral Link → Domain Cloak → Malicious Payment Intent).
5. **Incident Response System**: Provides immediate guided actions, advisory sharing, and 1-tap dialing to the **National Cybercrime Reporting Helpline (1930)**.

---

## Features (Verified in Source Code)

- **Input Ingress Channels**:
  - **Manual Analysis**: Direct text / URI / SMS input analyzer.
  - **CameraX QR Scanner**: Real-time high-speed QR barcode scanning powered by Google ML Kit.
  - **Android System Sharesheet Target**: Seamlessly handles text, single images, or multiple screenshots shared from any messaging app (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`).
  - **Clipboard Ingress**: 1-tap automatic clipboard payload ingestion.
  - **Quick Settings Tile**: Android Quick Settings tile (`RefGuardTileService`) for instant scam checking during active calls or chats.
- **Analysis Engine**:
  - **UPI URI Parser**: Validates `upi://pay` syntax, payee VPA, merchant handles, amounts, and currency.
  - **Payment Direction Analysis**: Differentiates outbound debit, inbound credit lures, and collect requests.
  - **Risk Scoring (0–100)**: Calibrated risk levels (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  - **Threat Explanations**: Dual explanations (plain-language for non-technical users and technical risk evidence for security analysts).
  - **Text-to-Speech (TTS)**: Built-in audio readout for accessible warning alerts.
- **Security & Privacy**:
  - **Zero Credential Collection**: Never asks for, inspects, or stores UPI PINs, passwords, or bank OTPs.
  - **Offline Edge Mode**: Complete local classification fallback via `LocalEdgeClassifier` and persistent `OfflineScanQueue`.
  - **Investigation History**: Local on-device history storage via `InvestigationHistoryManager`.
  - **Threat Lab**: Interactive security playground to inspect token feature weights and test adversarial payloads.
  - **Threat Radar**: Live categorization and threat landscape overview.

---

## Technical APK Analysis: 40 MB Debug vs 24 MB Release

| Build Variant | APK Size | DEX Architecture | R8 Minification | Native Libraries & Models |
| :--- | :--- | :--- | :--- | :--- |
| **Debug APK** | **42.1 MB** (40.16 MiB) | MultiDex (~65 MB uncompressed DEX bytecode across 16 dex shards) | Disabled | Full ML Kit Barcode native libs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) + TFLite models |
| **Release APK** | **24.2 MB** (23.12 MiB) | Single DEX (3.75 MB compressed DEX) | **Enabled (R8 optimization & dead-code elimination)** | **Identical native libraries and TFLite models 100% preserved** |

The 24 MB release build preserves all functionality, ML Kit models, and native binaries. The size reduction is purely due to production-grade R8 compiler dead-code elimination and single-dex optimization.

---

## Judge Demo Walkthrough

1. **Launch RefGuard**: Open the app to view the **Home Dashboard** with threat statistics and quick actions.
2. **Open Analyze Screen**: Tap **Analyze** from the navigation bar.
3. **Test Payment Mismatch Scenario**:
   - Enter payload: `Claim your ₹5,000 electricity subsidy refund now: upi://pay?pa=discom.billing@okhdfcbank.fraud.co&am=5000&pn=Electricity_Refund`
   - Tap **Analyze Threat**.
4. **Inspect Verdict Screen**:
   - Immediate **CRITICAL THREAT (Score 94-98/100)** banner.
   - Plain-language warning: *"Entering your UPI PIN will DEBIT ₹5,000, not receive it."*
   - Audio alert readout via speaker icon.
5. **Open Investigation**:
   - Tap **Investigation Details** to inspect the **ScamChain Directed Graph** linking the SMS lure to the destination VPA.
   - Review the cryptographic evidence and token weights.
6. **Test Offline Edge Engine**:
   - Enable Airplane Mode on the device.
   - Scan or analyze any UPI link.
   - RefGuard's `LocalEdgeClassifier` executes entirely on-device and provides an offline verdict badge.
7. **Incident Response**:
   - Tap **1930 Cybercrime Helpline** to view immediate step-by-step incident containment guidance and 1-tap call launcher.
8. **Explore Threat Lab**:
   - Navigate to **Threat Lab** to inspect live logistic model weights for terms like `refund`, `lottery`, `kyc_expire`, and `pin_required`.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           INGRESS LAYER                                 │
│  CameraX QR Scanner │ Sharesheet Target │ Clipboard │ Manual Input      │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    UPI INTENT & PROTOCOL DECODER                        │
│   • VPA Validation (RFC / NPCI Specs)    • Amount & Direction Parsing   │
│   • Merchant Parameter Sanitization      • Source Context Extraction    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    INTENT INVERSION & EDGE CLASSIFIER                   │
│   • Inbound Promise vs Outbound Debit Inversion Logic                   │
│   • Calibrated Logistic Feature Ensemble (40+ Threat Token Weights)     │
│   • Phishing Domain & Suspicious TLD Regex Detectors                   │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION & RESPONSE                          │
│   • Material 3 Threat Verdict UI         • ScamChain Graph Visualizer   │
│   • TTS Voice Guidance Alert             • 1930 Incident Response Flow  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

- **Platform:** Android 8.0+ (API Level 26–35)
- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (Material Design 3), Navigation Compose
- **Vision & Scanning:** Google ML Kit Barcode Scanning, CameraX
- **Concurrency & Flow:** Kotlin Coroutines, StateFlow, LiveData
- **Networking:** Retrofit 2, OkHttp 3, Gson
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog (`libs.versions.toml`)
- **CI/CD:** GitHub Actions (Automated release & test workflows)

---

## Building and Testing Locally

```bash
# Clone the repository
git clone https://github.com/Barathwaj2006/RefGuard.git
cd RefGuard

# Run unit tests across all modules (:app and :platform)
./gradlew test

# Assemble Debug APK (42 MB with MultiDex)
./gradlew :app:assembleDebug

# Assemble Production-Optimized Release APK (24 MB with R8)
./gradlew :app:assembleRelease
```

Generated APK paths:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## License

This project is licensed under the MIT License.
