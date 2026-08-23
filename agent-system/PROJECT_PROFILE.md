# Project Profile

PROJECT: RefGuard
REPOSITORY: Barathwaj2006/RefGuard
PURPOSE: Ambient, AI-driven cybersecurity layer for Android and Web.
TECH STACK: Node.js, Express, TypeScript (backend), Kotlin, Android (client), JSON Schema/OpenAPI (contracts).
ARCHITECTURE: Android/Web Client -> Express Backend API (/api/v1/scan) -> AnalyzerService -> Protection Decision & Evidence Pack.
DOMAINS: backend, android, contracts, integration, docs.
AGENT MAPPING:
  - Antigravity IDE: Backend
  - Antigravity 2.0: Frontend
  - Google Jules: QA/Testing
  - Qwen Code: Android
  - GitHub Copilot: Documentation
AUTHORIZED PATHS: See agent profiles in `agents/` folder.
RESTRICTED PATHS: `contracts/` (Human integrator owns after lock)
CONTRACTS: JSON Schema and OpenAPI specs located in `contracts/`
CURRENT STAGE: Stage 1 — Actual Product Build
MODE: RAPID BUILD (48h)
DOMAIN BACKLOGS: See `PROJECT_CONTEXT.md`
QUALITY REQUIREMENTS: 5 Gates (Contract, Tests, Integration, Security, UX). Must explicitly check for Payment-Intent Mismatches.
