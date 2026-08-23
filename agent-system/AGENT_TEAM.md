# Agent Team Operating Model (Universal — v3)

> This file is identical across all repositories in this workflow. Project-specific details (name, repo, duration mode, current stage, authorized paths) live in `PROJECT_CONTEXT.md` in the same repo — never edit those into this file.

## 1. Purpose

Defines how AI agents collaborate on any project in this workflow while preserving clear ownership, controlled parallelism, duration-aware execution, contract integrity, quality gates, efficient handoffs, credential hygiene, and human oversight.

Four principles govern the team:

1. **Specialization** — each agent has a defined primary responsibility.
2. **Controlled escalation** — secondary/tertiary responsibilities activate only on evidence, never self-judgment.
3. **Evidence-based handoff** — the next task is generated from the actual result of the previous task.
4. **Minimum necessary context** — agents receive only what they need for their current task.

---

## 2. Agent Priority Model

```text
PRIMARY   → main responsibility
SECONDARY → backup responsibility
TERTIARY  → last-resort responsibility
```

---

## 3. Priority Rules

1. Primary work has the highest priority.
2. An agent must not abandon unfinished primary work merely because secondary work is available.
3. Secondary work requires either primary completion or explicit assignment.
4. Tertiary work requires explicit instruction.
5. An agent must not silently take ownership of another agent's primary domain.
6. If blocked, the agent performs bounded analysis/review within its own domain rather than inventing functionality elsewhere.
7. Agents must report blockers instead of pretending a dependency is complete.
8. Agents must not modify shared contracts without authorization.
9. Agents must not declare another agent's work complete without evidence.
10. The human remains the final authority for major architectural decisions.
11. An agent may escalate to secondary/tertiary responsibility only after submitting a Handoff Report (Section 16) with STATUS: COMPLETED or STATUS: BLOCKED for its current primary task.

---

## 3A. Overlapping Secondary Domains

Where two agents' secondary domains overlap (most commonly backend/frontend integration surfaces):

> **The Primary owner of that layer retains authority over the shared surface.**

The exact split must be stated explicitly in `PROJECT_CONTEXT.md` for the current task — never assumed by the agent. If ownership is ambiguous, the task is **BLOCKED** until the human/integrator resolves it.

---

## 4. Agent Responsibility Matrix (Default Roster)

Adjust this table in `PROJECT_CONTEXT.md` if a given project uses a different tool roster — the roles/rules below still apply to whatever agents are actually in use.

| Agent              | Primary                           | Secondary                              | Tertiary                          |
| ------------------ | ---------------------------------- | --------------------------------------- | ----------------------------------- |
| Antigravity IDE    | Backend engineering & integration | Frontend implementation                | Full-stack debugging/performance   |
| Antigravity 2.0    | Frontend engineering              | Backend/API implementation             | Integration/performance            |
| Google AI Studio   | Gemini / AI / ML intelligence     | ML experimentation/evaluation          | AI architecture / domain taxonomy  |
| Google Jules       | Testing & QA                      | Code review/security review            | Debugging/integration fixes        |
| Qwen Code          | Platform-specific engineering (project-dependent) | Backend/data integration | Performance/device testing         |
| Stitch             | UI/UX design                      | Design system/component specification  | Frontend implementation guidance   |
| GitHub Copilot     | Documentation                     | Architecture/research documentation    | Pitch/demo/presentation            |

---

## 5. Agent Responsibilities

### Antigravity IDE
**Primary** — backend services, APIs, AI/model integration, backend business logic, server-side architecture, database integration, backend orchestration.
**Secondary** — API integration, state integration, connecting backend functionality to existing frontend components (UI-facing portion of shared surfaces only).
**Tertiary** — integration failures, cross-layer bugs, performance optimization.

### Antigravity 2.0
**Primary** — production UI, screens, components, interactions, state management, responsive behavior.
**Secondary** — frontend-facing API requirements, API integration support, UI-required backend adjustments. Backend/contract-facing implementation remains owned by Antigravity IDE unless explicitly reassigned.
**Tertiary** — cross-system integration, frontend performance, end-to-end debugging.

### Google AI Studio
**Primary** — model system prompts, structured outputs, reasoning design, few-shot examples, AI evaluation, model behavior, AI safety.
**Secondary** — dataset analysis, baseline models, model evaluation, metric comparison, ML feasibility.
**Tertiary** — domain taxonomy, reasoning architecture, future ML architecture, intelligence pipeline design.

### Google Jules
**Primary** — unit/integration/E2E/regression/contract/failure-path testing.
**Secondary** — code review, security review, contract review, quality assessment.
**Tertiary** — reproducing failures, diagnosing issues, applying targeted fixes when explicitly assigned.

### Qwen Code
**Primary** — platform-specific engineering (Android/CLI/backend as defined per project in PROJECT_CONTEXT.md).
**Secondary** — API models, networking integration, backend contract integration. Contract ownership remains with the contract owner.
**Tertiary** — performance, memory, device/environment testing.

### Stitch
**Primary** — user flows, wireframes, screen designs, interaction design, visual hierarchy, loading/error/state variants.
**Secondary** — component specifications, typography, spacing, visual states, reusable design patterns.
**Tertiary** — implementation specifications, interaction specifications, frontend design review.

**Note:** Stitch's deliverables are not code and do not live on a feature branch — they live in a `design/` folder or an external design-link registry. Branch rollback (Section 21) does not apply to Stitch's output; a bad iteration is discarded by replacing the file/link.

### GitHub Copilot
**Primary** — README, technical documentation, developer guides, implementation notes.
**Secondary** — architecture diagrams, architecture decisions, technical explanations, research documentation.
**Tertiary** — pitch, demo script, presentation material, judge/stakeholder-facing explanations.

---

## 6. Project Context Header

Every agent prompt must begin with the header defined in this repo's `PROJECT_CONTEXT.md`:

```text
PROJECT: <name>
REPO: <path/url>
HACKATHON MODE: <duration, or "N/A — ongoing development">
STAGE: <stage>
YOUR ROLE (per responsibility matrix): <Primary/Secondary/Tertiary>
SHARED SURFACE OWNER: <if applicable>
YOUR AUTHORIZED PATHS: <paths>
```

Treat this header as authoritative for the current task. If `PROJECT_CONTEXT.md` is missing or stale, stop and report a blocker rather than guessing project specifics.

---

## 7. Context Hygiene

Agent prompts should contain only: the Project Context Header, the relevant slice of the locked contract, the specific task, relevant repository files/context, and completion criteria. Do not paste unrelated prior handoff reports, other agents' full outputs, complete project history, unrelated files, or unnecessary logs.

---

## 7A. Credential Hygiene in Agent Prompts

**Never paste live credentials, API keys, tokens, or secrets into any agent prompt** — reference them by environment-variable name only, even when explaining how auth works. Applies to every cloud-based agent in the roster; a key pasted for "context" is a leak the moment it leaves the machine.

---

## 8. Domain Backlog

At Contract Lock time, each implementation agent receives ~2–4 pre-approved tasks within its own domain, so it isn't idle waiting on a human between steps. The human/integrator may reorder, block, add, remove, or reprioritize — a Domain Backlog never authorizes cross-domain work.

---

## 9. Duration Modes

Duration is set per-project in `PROJECT_CONTEXT.md`. Default matrix:

| Duration        | Intelligence Phase             | ML Feasibility                                 | Contract Lock | QA Depth                                            |
| ---------------- | ------------------------------- | ------------------------------------------------ | -------------- | ------------------------------------------------------ |
| **12h — Blitz**  | ~10 min skim, no documentation  | Skipped — deterministic + reasoning-model hybrid assumed | ~15 min        | Smoke test only                                        |
| **24h**          | ~20–30 min                      | Skipped                                          | ~30 min        | Core paths only                                        |
| **36h**          | ~45 min                         | Skipped unless labeled dataset already exists    | ~45 min        | Fuller unit suite                                      |
| **48h**          | ~1 hr                           | Quick sanity check only if trivial               | ~1 hr          | Full QA gates + light security                         |
| **7-day**        | Full intelligence phase         | Only if fixtures provide enough labeled data     | Full           | All quality gates                                      |
| **14-day**       | Everything in 7-day mode        | Real ML bake-off with proper evaluation          | Full           | Full QA + orchestrator prototype becomes realistic     |
| **Ongoing (no deadline)** | Full, revisited per milestone | Evaluated per feature, not compressed  | Full, versioned | All gates, every release |

### 12-Hour Blitz
Ship the minimum reliable working system. No exploratory ML — assume deterministic rules + a reasoning-model layer as baseline. Focus: core functionality, integration, smoke testing, demo reliability.
**Git granularity:** a "task" is a feature slice, not a file — one coherent commit per slice.

### 24-Hour Mode
Short repository/architecture inspection. Skip ML experimentation unless a validated model already exists. Focus: core implementation, integration, essential tests, demo stability.

### 36-Hour Mode
Deeper intelligence review. ML experimentation only if a suitable labeled dataset already exists.

### 48-Hour Mode
~1hr intelligence work. Lightweight ML sanity check only if it won't threaten the main timeline. Full QA gates.

### 7-Day Mode
Full intelligence phase: repository architecture, domain-specific taxonomy/pattern investigation, dataset quality, ML feasibility, reasoning-model performance, hybrid architecture. Implement ML only with evidence it improves the system.

### 14-Day Mode
Full process. First duration where a real ML bake-off and an initial orchestration prototype become realistic — the orchestrator remains a separate concern and must not jeopardize the main product.

### Ongoing / No Fixed Deadline
Standard production discipline — no compression. Full gates on every meaningful release, not just at a sprint boundary.

---

## 10. Input Trigger Syntax

```text
HACKATHON MODE: <12h | 24h | 36h | 48h | 7-day | 14-day | ongoing> | Project: <name>
```

Set once per project/sprint in `PROJECT_CONTEXT.md`; every subsequent agent task must respect it.

---

## 11. Mode Transition Rule

When mode becomes deeper, previously completed work is not automatically sufficient: re-run **Gate 2 (Tests)** and **Gate 4 (Security)** at the new depth before further build work continues. Other gates are revisited where the deeper mode materially changes requirements. Work already done is preserved; its validation status is upgraded, not assumed.

---

## 12. Stage Awareness

```text
Repository Intelligence → Machine Learning Intelligence → Architecture → Contract Lock →
Parallel Build → Integration → QA → Security Review → Release
```

An agent must not assume a later stage merely because a task sounds like implementation work.

---

## 13. Contract Ownership

Once locked, files under `contracts/` are owned exclusively by the human/integrator. No agent — including the Primary owner of that domain — may modify a locked contract file.

```text
Agent discovers required change → STOP affected work → report contract mismatch →
human/integrator reviews → contract updated if approved → re-sync broadcast →
active agents receive updated contract → affected tasks resume
```

A contract change also invalidates any Domain Backlog items referencing the changed contract — those are marked BLOCKED pending re-sync, never silently executed against stale assumptions.

---

## 14. Manual Handoff Protocol

```text
Agent A → Handoff Report → Human/Message Bus → Next Prompt → Agent B
```

The human is not required to manually route every successful task — only meaningful coordination points (Section 15).

---

## 15. Human Escalation Rules

Route to the human when: (1) an agent is BLOCKED, (2) a contract mismatch is discovered, (3) a stage completes, (4) explicit human escalation is required, (5) a security-sensitive or architectural decision needs approval. A successful, in-scope completion of primary-domain work does not require a round trip — the agent proceeds to its next Domain Backlog item.

---

## 16. Handoff Report

```text
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

The next agent receives only the relevant slice.

---

## 17. Dynamic Handoff Principle

```text
Agent A → actual result → analyze result → determine next task →
generate next prompt → Agent B
```

Never pre-generate a fixed chain of future prompts — every next task is generated from the actual prior result.

---

## 18. No-Idle Rule

If an agent's primary task is blocked, it performs bounded, in-domain preparation work — never cross-domain work.

---

## 19. No Cross-Domain Takeover

```text
Agent blocked → report blocker → orchestrator/human evaluates dependency →
dependency owner receives task
```

Cross-domain takeover requires explicit instruction, never silent action.

---

## 20. Git and Branch Isolation

```text
main → project sprint branch → agent feature branches
```

Only the designated integrator merges agent branches. Agents do not merge each other's work unless explicitly authorized.

### Git Checkpoint Protocol (Non-Negotiable)

Any agent making project changes must:
1. **Validate** all changes (lint, test, build).
2. **Inspect** `git status` and relevant diffs.
3. **Commit** with a concise, informative subject and a useful body explaining what changed and why.
4. **Push** the commit to the configured GitHub remote.
5. **Verify** the commit exists on the remote.
6. **Only then** declare the task complete.

**Incomplete tasks:** A task is NOT complete if changes remain uncommitted, the commit has not been pushed, or the remote state has not been verified.

**Prohibited commit messages:** "update", "changes", "fix", "done".

**Restrictions:** Do not create meaningless commits when no changes occurred. Do not force-push. Do not rewrite history. Do not create unsolicited Pull Requests.

**Push failure handling:** If push fails, the task remains BLOCKED. Report the exact failure. Do not claim completion. Do not force-push. Retry safely when appropriate.

---

## 21. Rollback and Branch Recovery

A failed agent run is recoverable without affecting sibling work: discard, reset, or recreate a feature branch from the last known-good integration point. If two branches modify the same file despite Section 3A's ownership split, treat it as an ownership violation, not a routine merge conflict — trace which agent worked outside its authorized paths before resolving.

---

## 22. Quality Gates

**Gate 1 — Contract:** conforms to the shared contract?
**Gate 2 — Tests:** relevant tests pass?
**Gate 3 — Integration:** functions correctly with the rest of the system?
**Gate 4 — Security:** PII leakage, exposed keys, insecure data handling, unsafe external requests, prompt injection risks, auth/authz problems?
**Gate 5 — UX:** can the user understand the result and take the correct action?

---

## 23. Short-Duration Rule

> **Compress the process; do not abandon the process.**

---

## 24. Task Lifecycle

Tasks transition through these states:

```text
PENDING → ASSIGNED → RUNNING → COMPLETED
```

Alternative flows:

```text
RUNNING → FAILED → RETRY → RUNNING
RUNNING → BLOCKED → (dependency resolved) → RUNNING
RUNNING → NEEDS_REVIEW → RUNNING → COMPLETED
ASSIGNED → CANCELLED
RUNNING → SUPERSEDED
```

---

## 25. Autonomous Orchestrator — Phase 2

Not part of any project's build unless its duration mode explicitly allows it. **Entry condition: do not begin building this until at least one project has shipped using the human-mediated handoff protocol.** Design it from observed coordination problems, not assumptions.

---

## 26. Why Direct Agent Communication Is Not Assumed

The current tool roster does not share a communication bus. Use the human-mediated message bus (Section 14) until real integration glue exists.

---

## 27. Long-Term Architecture

```text
Multi-Agent Orchestrator
        │
        ├── Project A
        ├── Project B
        └── Project C
```

This operating model is identical across projects. Only `PROJECT_CONTEXT.md` changes.

---

## 28. Core Operating Principle

> Maximum useful parallelism + minimum coordination overhead + minimum integration conflict + recoverable failures.

---

## 29. Final Rules

1. Know the project context before acting — read `PROJECT_CONTEXT.md` first.
2. Follow the selected hackathon/duration mode.
3. Prioritize primary responsibility.
4. Do not abandon primary work unnecessarily.
5. Do not silently take another agent's domain.
6. Respect locked contracts.
7. Report actual results.
8. Report blockers honestly.
9. Do not invent missing functionality or missing project context.
10. Use evidence to determine completion.
11. Preserve existing work.
12. Ask for human approval when required.
13. Generate future work from actual results.
14. Do not assume direct agent-to-agent communication exists.
15. Do not begin autonomous orchestration before the human-mediated workflow has shipped a project.
16. Resolve overlapping secondary ownership explicitly.
17. Keep agent context limited to what is necessary.
18. Never paste live credentials or secrets into any agent prompt.
19. Re-run required quality/security gates after a duration-mode upgrade.
20. Maintain isolated, recoverable branches.
21. Treat locked contracts as human-owned.
22. Escalate to secondary/tertiary only after a completed or blocked Handoff Report.
23. Treat cross-agent file collisions at merge time as ownership violations, not routine conflicts.

---

## 30. One-Line Model

```text
PROJECT CONTEXT → DURATION MODE → STAGE → DOMAIN BACKLOG → SPECIALIZED AGENT →
PRIMARY → SECONDARY → TERTIARY → ACTUAL RESULT → VALIDATION →
HANDOFF / NEXT BACKLOG ITEM → NEXT TASK
```

The human is involved at decision boundaries, not every successful micro-step.
