# CONTRACT REVIEW REPORT

## Architecture Overview
RefGuard Contract Architecture securely routes 27 scam protection capabilities. Schema constraints enforce data minimization and explicitly track extraction provenance and confidence.

## Schema Inventory & Ownership

1. scan-request.json
   - Producer: Android Platform Agent
   - Consumer: Backend API Gateway

2. scan-response.json
   - Producer: Backend / Risk Engine
   - Consumer: Android UI Agent

3. extraction-result.json
   - Producer: Extraction Engine (Qwen)
   - Consumer: Threat Intelligence (Qwen) / Risk Engine (Jules)

4. threat-assessment.json
   - Producer: Threat Intelligence (Qwen)
   - Consumer: Risk Engine (Jules)

5. risk-assessment.json
   - Producer: Risk Engine (Jules)
   - Consumer: Protection / UI Agent (Stitch)

6. payment-intent-mismatch.json
   - Producer: Risk Engine (Jules)
   - Consumer: Protection / UI Agent (Stitch)

7. protection-decision.json
   - Producer: Risk Engine (Jules)
   - Consumer: Android UI Agent (Stitch)

8. evidence-pack.json
   - Producer: Backend / Protection Engine
   - Consumer: Ecosystem / Recovery Assistant

9. scam-report.json
   - Producer: Android UI Agent
   - Consumer: Threat Intelligence / Ecosystem

10. error-response.json
    - Producer: Backend API
    - Consumer: Android UI Agent

11. scam-chain.json
    - Producer: Context Engine
    - Consumer: Android UI Agent

## 27-Feature Traceability
All features trace down into schemas. Examples:
- Payment Intent Mismatch (13): payment-intent-mismatch.json
- Scam Chain Visualization (23): scam-chain.json
- Explainable Risk Score (14): isk-assessment.json
- Privacy-First Safety Layer (17): Enforced via schema data minimization.

## Privacy Analysis
No fields collect UPI PINs, CVVs, or Passwords.

## Threat Intelligence Sources
Restricted to PUBLIC, CURATED, COMMUNITY, LOCAL_RULE, and THIRD_PARTY. No direct internal bank DB access is implied.

## Version Status
API v1 = PROPOSED (Not frozen)
