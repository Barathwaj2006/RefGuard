# RefGuard — Architecture (polished, evidence-oriented)

This architecture document describes RefGuard as implemented/present in the repository and maps components to the branches and files verified during inspection.

Verified repository artifacts
- refguard/backend-10777323175845688980/ARCHITECTURE.md (existing)
- refguard/backend-10777323175845688980/README.md
- refguard/backend-10777323175845688980/DEMO.md
- refguard/mvp-integration/README.md and directory layout (android/, backend/, contracts/, docs/)
- refguard/contracts README and contracts/ directory

Design overview (intended runtime pieces — verify by inspecting branch code before running)
- Client (Android or Web demo)
  - Role: package user-submitted content into a ScanRequest payload and present the advisory result to the user.
  - Verified location: refguard/mvp-integration/android (directory present) and refguard/mvp-integration README which documents the demo flows.

- Backend API
  - Role: receive ScanRequest, validate against contracts, run extractors and heuristics/contextual analysis, produce a ScanResponse with human-readable advisory and EvidencePack.
  - Verified location: refguard/backend-10777323175845688980 (contains README, ARCHITECTURE.md, DEMO.md, SAMPLE_PAYLOADS.md and backend/ directory).

- Contracts / Schemas
  - Role: define ScanRequest, ExtractionResult, EvidencePack, and ScanResponse shapes used by client and backend.
  - Verified location: refguard/contracts branch and contracts directories in backend/mvp-integration branches.

- Evidence & Demo artifacts
  - DEMO.md and SAMPLE_PAYLOADS.md in backend branch provide scripted inputs and expected flows for judge verification.

Data flow (conceptual — map to verified code before executing):

User Event (message / UPI intent / referral)
  ↓
Client packages content → ScanRequest (contracts)
  ↓
Backend /api/v1/scan (validates against JSON Schema)
  ↓
Extraction Engine → ExtractionResult (entities: URLs, VPAs, phone numbers, amounts)
  ↓
Threat Intelligence & Deterministic Heuristics → ThreatAssessment
  ↓
Contextual Analysis (lightweight NLP heuristics when present) → ContextSignals
  ↓
Risk Engine (rules + contextual signals) → RiskAssessment + EvidencePack
  ↓
ScanResponse (advisory string, evidence items) → Client UI

Important component notes (evidence-based)
- The backend README and ARCHITECTURE.md explicitly document schema validation and a /api/v1/scan entrypoint — use these files to confirm exact endpoints and payload shapes before running.
- The repo contains SAMPLE_PAYLOADS.md — use these canonical payloads to exercise the backend in a reproducible way during the demo.
- The contracts/ directories contain the schema artifacts referenced by the backend README — inspect them to confirm data envelopes and to generate clients.

Operational guidance for judges
- To run the backend demo:
  1. Checkout: git checkout refguard/backend-10777323175845688980
  2. cd backend (if backend folder contains package.json)
  3. npm install
  4. npm run dev (or npm run start) — backend README documents exact scripts
  5. Use SAMPLE_PAYLOADS.md to POST to /api/v1/scan and show the ScanResponse

- To inspect contracts and generate a client or a validator, open the contracts folder in the relevant branch (refguard/contracts or refguard/mvp-integration/contracts).

Security & privacy boundary (short)
- The architecture enforces a "no credentials" policy at schema validation and ingestion points (see THREAT_MODEL.md and backend README).
- EvidencePack builders and demo payloads are intended for non-sensitive data only and should be used with sanitized payloads during public demos.

Files to consult (verified in the repo)
- refguard/backend-10777323175845688980/ARCHITECTURE.md
- refguard/backend-10777323175845688980/README.md
- refguard/backend-10777323175845688980/DEMO.md
- refguard/backend-10777323175845688980/SAMPLE_PAYLOADS.md
- refguard/backend-10777323175845688980/THREAT_MODEL.md
- refguard/mvp-integration/README.md
- refguard/contracts/README.md

