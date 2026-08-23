# RefGuard Repository Review

I have cloned and reviewed the `RefGuard` repository. Here is an overview of its structure, technology stack, and intended architecture.

## Overview

**RefGuard** is described as an ambient, AI-driven cybersecurity layer for Android and Web. It analyzes suspicious messages, referrals, and UPI requests to stop financial fraud before the payment gateway. It relies heavily on extracting entities (like URLs, VPAs, amounts) and applying heuristics and real-time threat intelligence.

## Repository Structure

The `main` branch includes the MVP integration code and documentation.

- **`backend/`**: A Node.js/TypeScript Express backend.
  - **Dependencies**: `express`, `ajv` (for JSON schema validation), `uuid`.
  - **Structure**: Clean MVC-like structure (`controllers`, `routes`, `services`, `models`, `middleware`).
  - **Tests**: Configured with `jest` and `supertest`.
- **`android/`**: The Android platform ingestion module.
  - Uses Gradle build system.
  - Contains an app module and a `platform/` module for ingress channels, permissions, and offline queuing.
- **`contracts/`**: Contains the authoritative JSON/OpenAPI schema definitions for communication between the client and backend.
- **`integration/`**: Contains the MVP test suite and interactive web demo.
- **`docs/`**: Additional documentation.

## Architecture

The architecture enforces a "no credentials" policy at schema validation and ingestion points.

1. **Ingress**: The client (Android/Web) packages content into a `ScanRequest` (defined in `contracts/`).
2. **Backend**: Validates the request using `ajv`, runs extraction engines, and evaluates threat intelligence.
3. **Response**: The backend produces a `ScanResponse` containing a human-readable advisory and an `EvidencePack` explaining the risk score and protective action guidance.

## Next Steps

What specific area would you like to focus on?
- We can run the integration tests (`npm test` in the root).
- We can inspect a specific component's code (e.g., the risk engine in the backend, or the Android ingestion service).
- We can run the demo to see it in action.
- I can help you implement a new feature.
