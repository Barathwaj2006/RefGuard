# RefGuard Architecture

RefGuard is designed as a modular, multi-agent system focusing on universal scam scanning and community reporting. This document outlines the end-to-end data flow and architectural components.

## End-to-End Data Flow

The typical lifecycle of a scam assessment through RefGuard:

1. **User / Ingress (Android/Web):** A user receives a message, scans a QR code, or interacts with a UPI link. The client application extracts this interaction.
2. **ScanRequest Generation:** The client formats a standard `ScanRequest` payload containing the content type, value, context (if available), and timestamp.
3. **Backend API (Entrypoint):** The `POST /api/v1/scan` endpoint receives the request and strictly validates the payload against the frozen JSON contract.
4. **Extraction Engine:** Parses the input into an `ExtractionResult` containing normalized text and extracted entities (URLs, VPAs, phone numbers).
5. **Threat Intelligence Engine:** Evaluates the extracted entities against known threat signatures, returning a `ThreatAssessment` with confidence scores.
6. **Contextual AI Engine:** (Where applicable) Analyzes the broader intent, generating insights based on language patterns and source context.
7. **Risk Engine:** Consolidates intelligence into a unified `RiskAssessment` containing a severity level, confidence score, and explainable signals.
8. **Payment Intent Mismatch:** Evaluates if the user's stated intent (e.g., "receiving money") contradicts the actual payment action (e.g., "outbound debit").
9. **Scam Chain Engine:** Maps the relationships between actors, URLs, and platforms, generating a directed graph of the attack vector.
10. **Evidence Pack Builder:** Compiles relevant indicators into a verifiable `EvidencePack`.
11. **Protection Decision Engine:** Computes an advisory `ProtectionDecision` (e.g., `DISCOURAGE_PROCEED`, `WARN_CAUTION`) with actionable human-readable context.
12. **UI (Result Delivery):** The client application receives the final `ScanResponse` and presents the recommendation to the user.

## Component Distinctions

### Deterministic vs Contextual AI

- **Deterministic Components:** The Backend API routing, schema validation, Evidence Pack building, and Protection Decision formulation rely on strict rules and static thresholds to ensure predictable, reliable boundaries.
- **Contextual AI Components:** The Extraction, Threat Intelligence, and Risk Engine use probabilistic analysis (like NLP) to interpret ambiguous text, zero-day scams, and evolving threat patterns. These components are isolated behind strict API interfaces to prevent unexpected regressions.

### Advisory-Only Protection Decisions

RefGuard computes an *advisory* `ProtectionDecision`. It generates recommendations like `REQUIRE_CONFIRMATION` or `DISCOURAGE_PROCEED`.
RefGuard **does not** execute, block, or reverse transactions directly. The final execution control remains entirely with the payment application and the user.

### Privacy Boundary

Privacy is central to the architecture.
- RefGuard strictly forbids the transmission of sensitive data (passwords, UPI PINs, CVVs).
- Request validation drops payloads containing credentials before they reach the analytical engines.
- `source_context` is collected only to aid AI threat modeling, not for persistent user tracking.
