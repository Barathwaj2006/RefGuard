# CONTRACT REVIEW REPORT

## Architecture Overview
RefGuard Contract Architecture is designed to securely route and analyze 27 scam protection capabilities. The schema relies on isolated components minimizing data collection and tracking specific provenance/confidence metrics, ensuring no payment credentials like UPI PINs or passwords are collected.

## Schema Inventory
1. scan-request.json - Handles text, URL, UPI, images
2. scan-response.json - Final presentation payload to Android UI
3. extraction-result.json - Segregates extracted vs inferred intelligence
4. threat-assessment.json - Encodes intelligence with source type and freshness
5. risk-assessment.json - The final qualitative/quantitative risk score
6. payment-intent-mismatch.json - A dedicated structure comparing user-facing intent against payment action
7. protection-decision.json - Advisory action recommendations
8. evidence-pack.json - Sanitized collection of incident factors
9. scam-report.json - Feedback structure for community reporting
10. error-response.json - Universal failure payload
11. scam-chain.json - DAG structure modeling relationship graph

## Endpoint Inventory
- POST /api/v1/scan : Universal gateway
- POST /api/v1/report : Community intelligence feedback

## 27-Feature Traceability
All features (1-27) trace down into the schemas.
- Payment Intent Mismatch (13) directly supported via payment-intent-mismatch.json.
- Scam Chain Visualization (23) supported via scam-chain.json.
- Explanable Risk Score (14) supported via human_explanation in isk-assessment.json.
- Scam Simulator (21) and Education (20) supported via combinations of ScanResponse test-cases and ProtectionDecision warnings.

## Privacy Analysis
No fields collect UPI PINs, CVVs, or Passwords. Data minimization is prioritized.

## Capability Limitations
- RefGuard relies on source_context to infer message flows; it cannot natively break OS sandbox to intercept WhatsApp unless explicitly shared.

## Known Assumptions
- Android UI can ingest complex ScanResponse trees efficiently.
- Threat Intelligence can return within standard API timeouts.

## Peer-Agent Dependencies
- Backend Agent requires all schemas to build REST endpoints.
- UI Agent requires scan-response.json to build views.

## Version Status
API v1 = PROPOSED
