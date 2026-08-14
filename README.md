# RefGuard - The UPI Referral & Scam Shield

[![Status](https://img.shields.io/badge/Status-Hackathon_Ready-blue.svg)]()
[![API v1](https://img.shields.io/badge/API-Frozen-green.svg)]()

RefGuard is an intelligent, multi-agent advisory shield designed to protect users against sophisticated social engineering and UPI payment scams.

## The Problem
Scammers no longer just hack systems; they hack humans. By exploiting social engineering, they trick users into authorizing payments under the guise of receiving cashbacks, verifying KYC, or paying fake fees. Traditional static blocklists cannot keep up with zero-day social engineering vectors.

## The Solution
RefGuard intercepts the interaction at the edge (Android/Web), analyzes the *intent* of the message versus the *action* of the payment, and advises the user before the transaction is executed. It specializes in detecting **Payment Intent Mismatch**—when a user thinks they are receiving money, but the technical action is an outbound debit.

## Features
- **Universal Scam Scanning:** Analyzes Text, URLs, QR Codes, and VPAs in real-time.
- **Payment Intent Mismatch Detection:** Correlates the semantic intent of a message with the actual payment direction.
- **Scam Chain Mapping:** Builds relationship graphs to identify coordinated campaigns.
- **Community Reporting:** Allows crowd-sourced intelligence gathering.
- **Advisory Protection:** Provides actionable, explainable warnings (`DISCOURAGE_PROCEED`, `WARN_CAUTION`) without seizing control from the user.

## Architecture
RefGuard is built as a strict API-driven boundary connecting diverse intelligence engines:
- **Ingress:** Android / Web clients.
- **Backend API (Node.js/Express):** Enforces strict schema validation.
- **Contextual AI & Extraction:** (Placeholder) Uses NLP to parse ambiguous intents.
- **Risk & Protection Engines:** (Placeholder) Computes deterministic, explainable risk scores and user instructions.

*(For detailed information, see [ARCHITECTURE.md](ARCHITECTURE.md) and [THREAT_MODEL.md](THREAT_MODEL.md))*

## Tech Stack
- **Backend:** Node.js, Express, TypeScript, Ajv (JSON Schema validation)
- **Testing:** Jest, Supertest
- **Contracts:** OpenAPI / JSON Schema (Draft 7)

## Privacy First
RefGuard operates on a strict "No Credentials" policy. It actively rejects payloads attempting to submit passwords, PINs, or CVVs. Contextual data is used solely for threat modeling and is isolated from permanent user identities.

## Quick Start (Backend)
1. Install dependencies:
   ```shell
   cd backend
   npm install
   ```
2. Run the test suite:
   ```shell
   npm run test
   ```
3. Start the server (Development):
   ```shell
   npm run dev &
   ```

*(See [DEMO.md](DEMO.md) for a complete presentation script and [SAMPLE_PAYLOADS.md](SAMPLE_PAYLOADS.md) for test data).*

## Limitations & Future Scope
- **Current State:** The backend API v1 is fully executable with strict schema validation. However, the internal Extraction, Threat Intelligence, and Risk engines currently utilize static, deterministic placeholders.
- **Future Scope:** Integration with advanced LLMs (like Qwen) for deep NLP-based extraction and real-time contextual threat intelligence, alongside a complete Android accessibility-service based client.
