# Project Context

> This file supplies the project-specific values referenced by `AGENT_TEAM.md`. Every agent prompt should quote the header below verbatim before its task.

```text
PROJECT: RefGuard
REPO: C:\Users\barat\OneDrive\Desktop\RefGuard
MODE: RAPID BUILD
STAGE: Stage 1 — Actual Product Build
YOUR ROLE: <Specialization>
SHARED SURFACE OWNER: <agent name, or "None">
YOUR AUTHORIZED PATHS: <paths this agent may touch>
CURRENT BUILD TASK: <task>
```

---

## Active Agent Roster for This Project

| Agent | Status | Notes |
|-------|--------|-------|
| Antigravity IDE | Active | Specialization: Backend engineering & APIs |
| Antigravity 2.0 | Available | Specialization: Frontend engineering |
| Google AI Studio | Available | Specialization: AI/ML intelligence |
| Google Jules | Available | Specialization: Testing & QA |
| Qwen Code | Available | Specialization: Platform-specific engineering |
| Stitch | Available | Specialization: UI/UX design |
| GitHub Copilot | Available | Specialization: Documentation |

Active = currently performing tasks in this repo.
Available = registered and can be assigned when a product project begins.

---

## Domain-Specific Notes

- This repository contains **no application code** — it is the coordination control plane.
- The "contracts" for this repository are the operating rules themselves (`AGENTS.md`, `AGENT_TEAM.md`).
- There is no frontend/backend/database domain split — the only domain is **workflow documentation**.
- When this workflow is applied to a product repository, populate this section with domain-specific details (e.g., what "the reasoning-model layer" means, what the contract's core entities are, what counts as PII for Gate 4).

---

## Current Domain Backlog

### Workflow Architecture (Antigravity IDE)
1. ~~Install Universal Multi-Agent Engineering Workflow v3~~ — COMPLETED
2. Validate cross-file consistency — PENDING
3. Apply workflow to first product repository — PENDING

### Documentation (GitHub Copilot)
- Unassigned — no product documentation exists yet.

### All Other Domains
- Not applicable until a product project is initialized.

---

## Contract Location

The operating contract for this repository is:
- `AGENTS.md` — permanent principles
- `agent-system/AGENT_TEAM.md` — universal operating model

See AGENT_TEAM.md Section 13 for contract ownership rules.
