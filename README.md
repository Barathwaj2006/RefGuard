<<<<<<< HEAD
﻿# RefGuard — Universal UPI & Referral Scam Protection Platform

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
*Executes all 8 end-to-end integration flows plus strict schema validation for all 9 contract payload examples.*

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
├── integration/             # Lead Integration & Runtime Wiring
│   ├── src/                 # Pipeline, Extraction, Intelligence, Mismatch, Risk, Graph Engines
│   ├── demo/                # Interactive Demo Web Application
│   └── tests/               # End-to-End test suite & Contract conformance tests
├── tests/android-platform/  # Android ingress platform unit test suite
├── .github/workflows/       # Automated CI workflow
└── package.json             # Root runner scripts
```
=======
# RefGuard
An evidence‑based, privacy‑first documentation for the RefGuard hackathon project.

One-line description
RefGuard is an ambient, privacy-first AI layer that analyzes submitted message payloads and UPI payment-intent payloads (when available), extracts structured signals, and returns an advisory, human-readable risk verdict and evidence. This repository contains the hackathon proof-of-concept and related contracts and demos across branches.

Status (verified snapshot)
- Repository: Barathwaj2006/RefGuard (id: 1333821641)
- Default branch: main
- Verified branches (present in the repository):
  - main
  - refguard/android-platform
  - refguard/backend-10777323175845688980
  - refguard/contracts
  - refguard/integration
  - refguard/mvp-integration
- Notes: the main branch initially contained a minimal README. Several branches contain component code and documentation (see section "Where to look").

Why this README is evidence-based
- I inspected the repository branches and files before authoring this document and only state facts that are present in the repository. I do not assume runtime behavior from branch names. Implemented vs contract vs demo vs future work sections below are explicitly labeled.

Contents (judge-oriented)
- Problem → Solution → Key implemented capabilities → Architecture → Demo flow → Technology → Testing → Setup

## The Problem
Social‑engineering payment fraud (UPI/QR/peer transfers, referral scams) relies on persuasion, spoofing and missing contextual signals. Payment flows often lack the conversation context that distinguishes a legitimate payment from a scam. Judges want a solution that (a) makes decisions explainable, (b) preserves privacy, and (c) is verifiable in a short demo.

## The Solution
RefGuard provides an advisory layer that accepts submitted message/intent payloads from a client (Android or web demo), extracts payment and contextual signals, evaluates risk using deterministic rules plus lightweight contextual intelligence, and returns an explainable advisory (human-readable verdict and evidence). RefGuard is advisory-only: it does not perform or reverse payments.

## Key implemented capabilities (verified)
- Repository-level facts (verified): multiple branches hold different components (android, backend, contracts, integration, mvp-integration). Several documentation files exist on the backend branch (ARCHITECTURE.md, README.md, DEMO.md, SAMPLE_PAYLOADS.md, THREAT_MODEL.md).

- Implemented (verified by inspecting repository branches and files):
  - Backend API & documentation: README.md, ARCHITECTURE.md, DEMO.md, SAMPLE_PAYLOADS.md, THREAT_MODEL.md are present in branch refguard/backend-10777323175845688980.
  - MVP/integration artifacts and project layout: branch refguard/mvp-integration contains android/, backend/, contracts/, docs/, integration/, package.json, tests, and a branch README.
  - Contracts: refguard/contracts branch contains a README and a contracts/ directory (schemas and contract artifacts likely present there).

- Contract-defined (exists as schema/artifacts in repository branches):
  - JSON/OpenAPI/contract artifacts are present in contracts/ directories across branches (explicit contract files should be inspected in refguard/contracts and integration branches).

- Demo (present / partially present):
  - The backend branch includes DEMO.md and SAMPLE_PAYLOADS.md — these are explicit, verifiable demo artifacts for the hackathon demo flow.

- NOT implemented / NOT claimed here (explicitly excluded):
  - I do NOT claim SMS interception, automatic payment blocking, production ML pipelines, benchmarked accuracy, or any action that would initiate or reverse payments. Those features are not proven by branch names alone and are not asserted unless explicit code in a branch demonstrates them.

## How RefGuard is intended to work (high level, mapped to repo)
- Client (Android/web demo) collects or receives a message/UPI intent and packages a ScanRequest.
- The ScanRequest is validated against contracts (contracts/). Backend API (backend/) performs extraction and produces an evidence pack and advisory response.
- Demo scripts and sample payloads (DEMO.md, SAMPLE_PAYLOADS.md) show how to exercise the API for judge verification.

Where to look (verified locations)
- Backend documentation & artifacts: branch refguard/backend-10777323175845688980 — includes ARCHITECTURE.md, README.md, DEMO.md, SAMPLE_PAYLOADS.md, THREAT_MODEL.md and a backend/ directory.
- MVP integration: branch refguard/mvp-integration — contains android/, backend/, contracts/, docs/, integration/, package.json, tests, and README.md.
- Contracts: branch refguard/contracts — contains README.md and contracts/ directory.

## Demo flow for judges (evidence‑only)
1. Checkout the mvp-integration branch locally (contains demo client and backend layout):
   git checkout refguard/mvp-integration
2. Open DEMO.md in the backend branch for the sequence of requests and sample payloads (refguard/backend-10777323175845688980/DEMO.md).
3. Run the backend (if package.json / start scripts exist) and use SAMPLE_PAYLOADS.md to POST canonical ScanRequests to /api/v1/scan as described in the backend README.
4. Observe the returned ScanResponse and the evidence pack printed by the backend demo; validate that the response is advisory-only.

## Verified Technology stack (from backend and package files)
- Backend: Node.js / TypeScript (package.json present in mvp-integration and backend branches; backend README references Node.js, Express, TypeScript, Ajv)
- Tests: Jest + Supertest references appear in backend README.md
- Contracts: JSON Schema / OpenAPI references in backend README and contracts directories

## Verified Testing & Build notes
- Backend README in refguard/backend-10777323175845688980 documents npm install, npm run test, and npm run dev. These are documented steps and can be run to validate the backend locally.
- mvp-integration branch contains a tests/ directory (presence verified). Exact test coverage and pass/fail must be run locally to verify.

## How judges can verify the project quickly
- Clone and checkout the backend branch, run npm install and npm run test (backend branch documents these steps).
- Use SAMPLE_PAYLOADS.md to POST requests to the running backend and show the advisory responses.
- Optionally checkout mvp-integration to inspect the demo client layout and package.json.

## Roadmap (explicitly labeled)
- Implemented / present in repo: backend docs + demo scripts, MVP integration layout, contracts directory and README.
- Future (not present / not claimed unless code exists): production ML pipelines, automated payment blocking, background SMS/notification interception services.

---

*(This README and the accompanying ARCHITECTURE.md and SECURITY.md are documentation-only changes and do not alter application code or contracts.)
>>>>>>> 36a6bd6 (docs: polish RefGuard GitHub presentation)
