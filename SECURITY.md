# SECURITY.md — Privacy & Security Boundaries (RefGuard)

This document records the verified privacy and security boundaries described in the repository documentation and threat modeling artifacts. It is written to be accurate to what the repo shows and conservative in scope.

Verified security-related artifacts in the repository
- refguard/backend-10777323175845688980/THREAT_MODEL.md (present)
- Backend README and ARCHITECTURE.md reference schema validation and a "No Credentials" policy

Security & privacy principles (implemented / documented)
- Advisory-only model
  - The repository documentation repeatedly states that RefGuard produces advisory decisions and does not execute or reverse payments. This advisory-only stance is a documented, verified design constraint.

- No-credentials ingestion policy
  - The backend documentation and threat model indicate that payload validation drops or rejects content that appears to be credentials (OTP, PIN, CVV, passwords) before it reaches analytics pipelines. Inspect THREAT_MODEL.md and backend README for exact validation rules and confirm them in the backend code before production use.

- Evidence handling
  - EvidencePacks are documented as collections of extracted, relevant indicators (URLs, VPAs, phone numbers, timestamps) used to explain a verdict. The repository advises sanitizing or redacting credential-like tokens from evidence before storing or sharing.

- Opt-in collection & demo hygiene
  - Community reporting and telemetry (if present) should be opt-in according to backend README and demo notes. Demonstrations should prefer SAMPLE_PAYLOADS.md and sanitized examples rather than live user data.

- Sensitive-data operations explicitly excluded
  - The repository does NOT claim to capture or transmit OTPs, UPI PINs, CVVs, or other authentication secrets. Do not rely on the project for credential recovery or transaction reversal.

Operational recommendations (based on repository docs)
- During the hackathon demo, use the provided SAMPLE_PAYLOADS.md and DEMO.md to avoid exposing sensitive user data.
- Before deploying any component beyond a demo: review the backend schema validators, the evidence pack builder and the demo data flows to confirm redaction and retention policies are implemented as required.

Where to audit in the repository (verified locations)
- refguard/backend-10777323175845688980/THREAT_MODEL.md
- refguard/backend-10777323175845688980/README.md
- Contracts (refguard/contracts and refguard/mvp-integration/contracts) — inspect the schemas for fields and allowed values to validate the no-credentials policy.

Limitations & disclaimers
- This SECURITY.md documents the repository's stated design and guidance based on files present in the repository. It is not a substitute for a formal security review or penetration test.
- Do not assume production readiness or compliance from these docs alone. Further verification of code and runtime behavior is required for deployment.

