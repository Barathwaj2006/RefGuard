# CONTRACT REVIEW REPORT

## Architecture Overview
RefGuard Contract Architecture securely routes scam protection capabilities. Schema constraints enforce data minimization and explicitly track extraction provenance and confidence.

## Scam Chain Derivation
The ScamChain structure represents a contextual graph. It is derived through a synthesis of inputs:
ExtractionResult + ThreatAssessment + RiskAssessment + Contextual Observations → **Context Engine** → ScamChain.
The ScamChain.node_type is a contextual graph classification (e.g. PAYMENT_ACTION, REDIRECT) and does not need to perfectly map to ExtractionResult.entity_type (e.g. URL, UPI_VPA).

## Evidence Resolution
All instances of evidence_references across RiskAssessment, PaymentIntentMismatch, ScamChain, and ScamReport strictly resolve to a valid EvidencePack.items[].evidence_id.

## Schema Inventory & Ownership

1. scan-request.json (Producer: Android Platform | Consumer: API Gateway)
2. scan-response.json (Producer: Risk Engine | Consumer: Android UI)
3. extraction-result.json (Producer: Extraction Engine | Consumer: TI / Risk)
4. threat-assessment.json (Producer: Threat Intelligence | Consumer: Risk Engine)
5. risk-assessment.json (Producer: Risk Engine | Consumer: Protection / UI)
6. payment-intent-mismatch.json (Producer: Risk Engine | Consumer: Protection / UI)
7. protection-decision.json (Producer: Risk Engine | Consumer: Android UI)
8. evidence-pack.json (Producer: Backend | Consumer: Ecosystem)
9. scam-report.json (Producer: Android UI | Consumer: TI / Ecosystem)
10. error-response.json (Producer: Backend | Consumer: Android UI)
11. scam-chain.json (Producer: Context Engine | Consumer: Android UI)

## F01-F27 Traceability Matrix

| ID | Feature Name | Contract(s) | Producer | Consumer | Status | Notes |
|---|---|---|---|---|---|---|
| F01 | Universal Scam Scanner | ScanRequest, ScanResponse | Platform | Backend | SUPPORTED | Core API boundary |
| F02 | UPI QR Intelligence | ScanRequest, ExtractionResult | Platform | Extraction | SUPPORTED | Content-type QR |
| F03 | Screenshot Scam Scanner | ScanRequest | Platform | Backend | SUPPORTED | Content-type IMAGE |
| F04 | WhatsApp Safety Analysis | ScanRequest | Platform | Backend | SUPPORTED | Sourced via source_context |
| F05 | Telegram Safety Analysis | ScanRequest | Platform | Backend | SUPPORTED | Sourced via source_context |
| F06 | SMS Scam Analysis | ScanRequest | Platform | Backend | SUPPORTED | Sourced via source_context |
| F07 | Referral Link Intelligence | ScanRequest, RiskAssessment | Platform | Risk | SUPPORTED | Handles URL scanning |
| F08 | Context-Aware Scam AI | ExtractionResult, ScamChain | Extraction | Context | SUPPORTED | Leverages extracted entities |
| F09 | UPI ID Risk Analyzer | ScanRequest, ThreatAssessment| Platform | TI | SUPPORTED | Sourced via UPI_VPA indicator |
| F10 | Indian Threat Intelligence | ThreatAssessment | TI | Risk | SUPPORTED | Integrates local TI indicators |
| F11 | Official Referral Registry | ThreatAssessment | TI | Risk | SUPPORTED | Maps to CURATED sources |
| F12 | Hybrid Risk Engine | RiskAssessment | Risk | Protection | SUPPORTED | Quantitative + qualitative score |
| F13 | Payment-Intent Mismatch | PaymentIntentMismatch | Risk | UI | SUPPORTED | Distinguishes stated vs actual intent |
| F14 | Explainable Risk Score | RiskAssessment | Risk | UI | SUPPORTED | Mapped via human_explanation |
| F15 | Before-You-Pay Protection | ProtectionDecision | Risk | UI | SUPPORTED | Mapped via ction enum |
| F16 | Safe Share | ScanRequest | Platform | Backend | SUPPORTED | Sourced via SHARE_INTENT |
| F17 | Privacy-First Android Layer | All schemas | Platform | Backend | SUPPORTED | Data minimization enforced in schema |
| F18 | Post-Scam Recovery Assistant| EvidencePack | Backend | UI | SUPPORTED | Centralized documentation tracking |
| F19 | Automatic Evidence Pack | EvidencePack | Backend | UI | SUPPORTED | Structured, anonymized evidence items |
| F20 | Contextual Micro-Learning | ProtectionDecision | Risk | UI | SUPPORTED | Leverages why_it_matters output |
| F21 | Scam Simulator | ScanResponse | QA | UI | SUPPORTED | Handled through test fixture injection |
| F22 | Personal Safety Score | N/A | N/A | N/A | DEFERRED | OUTSIDE V1 CONTRACT |
| F23 | Scam Chain Visualization | ScamChain | Context | UI | SUPPORTED | Node/Edge graphical model |
| F24 | Scam Knowledge Graph | ScamChain | Context | Backend | SUPPORTED | Leverages Node/Edge output |
| F25 | Crowdsourced Reporting | ScamReport | UI | TI | SUPPORTED | Maps via POST /report API |
| F26 | Real-Time Scam Alert Feed | N/A | N/A | N/A | DEFERRED | OUTSIDE V1 CONTRACT |
| F27 | Referral Earnings Tracker | N/A | N/A | N/A | DEFERRED | OUTSIDE V1 CONTRACT |

## Privacy & API Scope Limitations
- UPI PINs, CVVs, and passwords strictly prohibited.
- V1 Scope strictly adheres to core detection/protection. Advanced features (webhooks, async polling, historical APIs, rate-limiting) are deferred.

## Version Status
API v1 = PROPOSED (Not frozen)
