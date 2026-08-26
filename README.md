# RefGuard

**Stop the scam before you pay.**

[![Build and Publish Release APK](https://github.com/Barathwaj2006/RefGuard/actions/workflows/release.yml/badge.svg)](https://github.com/Barathwaj2006/RefGuard/actions/workflows/release.yml)
[![RefGuard Full-System CI](https://github.com/Barathwaj2006/RefGuard/actions/workflows/ci.yml/badge.svg)](https://github.com/Barathwaj2006/RefGuard/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Barathwaj2006/RefGuard?label=Release)](https://github.com/Barathwaj2006/RefGuard/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

## The Problem
Social engineering payment fraud, particularly via UPI and SMS, is accelerating. Scammers manipulate victims into authorizing payments under the guise of refunds, lottery wins, or urgent disconnections. Traditional bank security kicks in *after* the payment intent is formed.

## The Solution
RefGuard is an intent-level pre-execution protection system. It acts as a shield before you enter your UPI PIN. By analyzing the contextual promise (e.g., "You won ₹5000") against the actual technical protocol (e.g., "UPI Collect Request for ₹5000"), RefGuard catches the deception at the psychological and protocol level.

## Why RefGuard Is Different
- **Payment-Intent Mismatch Analysis:** The core innovation. It detects when a user is promised money but the underlying technical link requests a debit.
- **Evidence-Backed Verdicts:** We don't just say "Scam." We highlight the exact reasons, showing the extracted features and matching threat intelligence.
- **ScamChain:** Reconstructs the multi-step directed graph of the attack (Message → Referral → Shortlink → UPI Collect).
- **Offline Edge Analysis:** Uses a lightweight LocalEdgeClassifier on the device. It detects threats even without an internet connection.
- **Human-Readable Explanations:** Explains the threat in simple terms, e.g., "Entering a UPI PIN NEVER receives money."
- **Incident Response (1930):** Direct integration with the national cybercrime reporting helpline (1930) for post-incident action.
- **Threat Lab & Threat Radar:** Internal tools to inspect payloads, view the logistic feature ensemble weights, and understand *why* the model flagged a threat.

## How It Works
INPUT (QR, UPI URI, SMS) → EXTRACTION (VPA, Amount, Note) → ANALYSIS (NLP & Protocol Check) → INTENT CHECK (Promise vs. Action) → EVIDENCE (Extracted Tokens) → VERDICT (Score 0-100) → ACTION (Block / Warn / Allow)

## Product Experience
- **Home:** Dashboard showing recent scans, threat radar, and quick scan actions.
- **Scan (QR Scanner):** CameraX and ML Kit integration for real-time QR code threat detection.
- **Analyze:** Manual input for suspicious links, SMS, or UPI VPAs.
- **Result:** The verdict screen. Red for critical threat, yellow for warning, green for safe.
- **Investigation & ScamChain:** Deep dive into the evidence, showing the timeline of the attack and extracted risk factors.
- **Threat Lab:** A developer/analyst interface to test the adversarial classifier and view logistic token weights.
- **Threat Radar:** Geographic/Categorical breakdown of ongoing threats.
- **Settings:** Adjust the shield sensitivity and offline fallback mechanisms.
- **Incident Response:** Immediate guidance on what to do if you've already been scammed (calling 1930).

## Judge Demo
1. **Open RefGuard** and go to the **Analyze** tab.
2. **Enter a suspicious scenario:** A UPI collect request claiming a refund (upi://pay?pa=scammer@ybl&pn=Refund&am=5000&cu=INR).
3. **Analyze it:** Click analyze.
4. **Show Verdict:** The screen immediately blocks the action with a Critical Threat warning.
5. **Open Investigation:** Click on "Investigation" to view the ScamChain and evidence.
6. **Demonstrate Threat Lab:** Navigate to Threat Lab to see the underlying logistic model weights for terms like "refund" and "cashback".
7. **Demonstrate Safe Merchant:** Scan a legitimate merchant QR (e.g., Swiggy/Zomato). It passes with a low score.
8. **Demonstrate Offline Capability:** Disable internet and run a scan. The LocalEdgeClassifier still catches known scam keywords and intent mismatches.
9. **Show 1930 Response:** Navigate to the Incident Response screen.

## Architecture
`
┌─────────────────┐     ┌──────────────────────┐     ┌────────────────────────┐
│  1. INGRESS     │────▶│ 2. PROTOCOL & NLP    │────▶│ 3. EDGE LOGISTIC       │
│ • Intent URI    │     │    DECODER           │     │    FEATURE ENSEMBLE    │
│ • QR Bitmaps    │     │ • UPI Scheme Parser  │     │ • 40+ Token Weights    │
│ • SMS Payload   │     │ • Target VPA / Debit │     │ • Prior Log-Odds Model │
└─────────────────┘     └──────────────────────┘     └───────────┬────────────┘
                                                                 │
                                                                 ▼
┌─────────────────┐     ┌──────────────────────┐     ┌────────────────────────┐
│  6. PROTECTIVE  │◀────│ 5. RISK SCORING &    │◀────│ 4. INTENT INVERSION    │
│     INTERVENTION│     │    CONFIDENCE HEDGE  │     │    MISMATCH REASONER   │
│ • Block / Warn  │     │ • Sigmoid (0-100 pts)│     │ • Promised: ₹5,000 Win │
│ • ScamChain UI  │     │ • Confidence % Margin│     │ • Reality: Debit ₹5,000│
└─────────────────┘     └──────────────────────┘     └────────────────────────┘
`

## Technology Stack
- **Android App:** Kotlin, Jetpack Compose (Material 3), CameraX, ML Kit, Coroutines.
- **Backend API:** Node.js, Express (used for cloud intelligence/demo integration).
- **Edge AI:** Custom LocalEdgeClassifier (Kotlin) implementing a calibrated logistic feature ensemble.
- **CI/CD:** GitHub Actions (Automated release pipeline and integration tests).

## Security & Privacy
See our comprehensive [SECURITY.md](SECURITY.md) for details.
- No credential collection.
- Local edge processing for sensitive payloads.
- Explicit user-initiated scans.

## Installation
The final hackathon APK is available in the [Releases](https://github.com/Barathwaj2006/RefGuard/releases/tag/hackathon-build).
Download pp-release.apk and install it on any Android device (Android 8.0+).

## Development
`ash
# Clone the repository
git clone https://github.com/Barathwaj2006/RefGuard.git
cd RefGuard/android_native

# Build Debug APK
./gradlew :app:assembleDebug

# Run Tests
./gradlew testDebugUnitTest
`

## Current Status
- **Implemented:** Full native Android UI (Compose), Offline Edge Classifier, QR Scanner, ScamChain Investigation, Threat Lab, UPI Intent Parsing, Node.js Backend API integration, GitHub Release Pipeline.
- **Experimental:** Live Threat Radar syncing with remote API.
- **Planned:** iOS App, Browser Extension.

## License
MIT License.
