# Project Context

> This file supplies the project-specific values referenced by `AGENT_TEAM.md`. Every agent prompt should quote the header below verbatim before its task.

```text
PROJECT: Multi-Agent Workspace
REPO: github.com/Barathwaj2006/multi-agent-workspace
HACKATHON MODE: N/A — ongoing development
STAGE: Workflow Setup
YOUR ROLE (per responsibility matrix): <Primary | Secondary | Tertiary>
SHARED SURFACE OWNER: <agent name, or "None">
YOUR AUTHORIZED PATHS: <paths this agent may touch>
```

---

## Active Agent Roster for This Project

This is a coordination-only repository (no application code). The following agents are registered for use across projects managed by this workspace:

| Agent | Status | Notes |
|-------|--------|-------|
| Antigravity IDE | Active | Primary: Workflow architecture & backend integration |
| Antigravity 2.0 | Available | Primary: Frontend engineering |
| Google AI Studio | Available | Primary: AI/ML intelligence |
| Google Jules | Available | Primary: Testing & QA |
| Qwen Code | Available | Primary: Platform-specific engineering |
| Stitch | Available | Primary: UI/UX design |
| GitHub Copilot | Available | Primary: Documentation |

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
