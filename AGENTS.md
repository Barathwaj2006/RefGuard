# Multi-Agent Engineering Contract

> **This is the primary entry point for every AI agent working on this repository.**
> Read this file first. Then read the documents it references before taking any action.

This repository follows the **Universal Multi-Agent Engineering Workflow (v3)** — a reusable framework that governs how multiple AI coding agents collaborate through a shared GitHub source of truth.

---

## How This System Works

1. **This file (`AGENTS.md`)** — the permanent contract. Defines principles that never change between projects.
2. **[`agent-system/AGENT_TEAM.md`](agent-system/AGENT_TEAM.md)** — the universal operating model. Defines the execution modes, domain specialization, handoff protocol, quality gates, duration modes, escalation rules, Git safety, rollback, and all operational procedures. Identical across all repositories using this workflow.
3. **[`agent-system/PROJECT_CONTEXT.md`](agent-system/PROJECT_CONTEXT.md)** — the project-specific context. Defines *this* project's name, repo, tech stack, stage, hackathon mode, active agent roster, domain backlogs, authorized paths, and contract locations. **This is the only file that changes between projects.**
4. **[`agent-system/PROJECT_PROFILE.md`](agent-system/PROJECT_PROFILE.md)** — the project profile. Describes the repository's purpose, architecture, domains, and quality requirements.
5. **[`agent-system/agents/`](agent-system/agents/)** — per-agent configuration files with authorized/restricted paths and completion criteria.

---

## Permanent Principles

These principles are non-negotiable and apply to every agent, every project, every duration mode.

### 1. Shared Source of Truth
GitHub is the authoritative shared state. All coordination happens through the repository.

### 2. Strict Task Ownership
An agent may modify **only** the files/directories explicitly assigned to it for its current task.
- Do not modify another agent's owned files.
- Do not refactor unrelated files.
- Do not add features outside your assignment.
- Do not "helpfully" change another agent's implementation.
- Do not cross domain boundaries (e.g., alter UI while assigned to backend).
- If you discover a problem outside your ownership, **report it** — do not fix it.

### 3. Execution Mode Model
See [AGENT_TEAM.md §3](agent-system/AGENT_TEAM.md) for full rules. Summary:
- **RAPID BUILD (Default)**: Optimize for build speed, implementation, and minimal blocking analysis. Every active agent must have a build task.
- **ANALYSIS / DEBUGGING**: Understand the project state, find issues, or debug specific bugs with minimal refactoring.
- **PROTOTYPE TESTING**: Test the actual running product as a user, observing and reporting real-world UX and functional behavior.

### 4. Contract-First Development
Once shared contracts/interfaces are locked, they are owned by the human/integrator. No agent may modify a locked contract. If a change is needed: STOP work → report mismatch → wait for human review. See [AGENT_TEAM.md §13](agent-system/AGENT_TEAM.md).

### 5. Context Hygiene
Agents receive only the information required for their current task: Project Context Header, relevant contract slice, specific task, relevant repo context, completion criteria. Do not dump entire histories, unrelated logs, or other agents' full outputs.

### 6. Secrets Hygiene
**NEVER** place live secrets (API keys, passwords, tokens, credentials) in any agent prompt or committed file. Reference environment variables or secret-store names only. This applies to all agents including cloud-based tools.

### 7. Git Checkpoint Requirement (Non-Negotiable)
Any agent that makes project changes must:
1. Validate its changes (lint, test, build).
2. Inspect `git status` and relevant diffs.
3. Create a meaningful commit with a concise subject and useful body.
4. Push the commit to the configured GitHub remote.
5. Verify the commit exists on the remote.
6. **Only then** declare the task complete.

A task is NOT complete if changes are uncommitted, unpushed, or unverified. Do not force-push. Do not rewrite history. Do not create unsolicited PRs. If push fails, the task remains BLOCKED.

### 8. Quality Gates
All changes must pass 5 gates (depth determined by duration mode):
- **Gate 1 — Contract:** Conforms to shared interfaces?
- **Gate 2 — Tests:** Relevant tests pass?
- **Gate 3 — Integration:** Works with the rest of the system?
- **Gate 4 — Security:** No secrets, PII leaks, prompt injection, auth issues?
- **Gate 5 — UX:** Understandable and usable by the user?

### 9. Handoff Protocol
Every completed or blocked task must produce a Handoff Report:
```
TASK:
STATUS:
WORK COMPLETED:
FILES CREATED:
FILES MODIFIED:
TESTS RUN:
TEST RESULTS:
BLOCKERS:
DEPENDENCIES:
ASSUMPTIONS:
RECOMMENDED NEXT ACTION:
```
The next task is generated from the actual result — never pre-generated as a chain.

### 10. Human Escalation
Escalate when: blocked, contract mismatch, stage completes, architecture changes, security-sensitive decisions, or explicit approval required. Successful in-scope build task completion proceeds to next backlog item without human round-trip.

### 11. No Cross-Domain Takeover
If blocked, perform bounded in-domain work (analysis, tests, docs) — never take another agent's domain. Cross-domain work requires explicit authorization.

### 12. Rollback Safety
Agent work must remain recoverable. Failed branches must not damage sibling branches or the stable integration branch. Cross-agent file collisions at merge time are ownership violations, not routine conflicts.

### 13. No Unauthorized Feature Development
Do not implement features not explicitly requested. This repository is a coordination system — not a product.

---

## For the Full Operating Model

Read **[agent-system/AGENT_TEAM.md](agent-system/AGENT_TEAM.md)** for:
- Execution Modes (RAPID BUILD, ANALYSIS/DEBUGGING, PROTOTYPE TESTING)
- Agent responsibility matrix and per-agent detail
- Overlapping domain resolution
- Duration modes and mode transition rules
- Stage awareness
- Domain backlogs
- Contract ownership protocol
- Manual handoff protocol
- Dynamic handoff principle
- No-idle rule
- Git and branch isolation
- Rollback and branch recovery
- Merge-time ownership violations
- Autonomous orchestrator entry conditions

## For This Project's Context

Read **[agent-system/PROJECT_CONTEXT.md](agent-system/PROJECT_CONTEXT.md)** for:
- Project name, repo, tech stack
- Current stage and hackathon mode
- Active agent roster for this project
- Domain-specific notes
- Current domain backlogs
- Contract locations
