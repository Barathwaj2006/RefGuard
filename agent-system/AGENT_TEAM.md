# Agent Team Operating Model (Universal — v4)

> This file is identical across all repositories in this workflow. Project-specific details (name, repo, duration mode, current stage, authorized paths) live in `PROJECT_CONTEXT.md` in the same repo — never edit those into this file.

## 1. Purpose

Defines how AI agents collaborate on any project in this workflow while preserving clear ownership, controlled parallelism, duration-aware execution, contract integrity, quality gates, efficient handoffs, credential hygiene, and human oversight.

Four principles govern the team:

1. **Active Build Task** — each agent executes a specific assigned build task.
2. **Execution Modes** — agents operate in one of three modes: RAPID BUILD, ANALYSIS/DEBUGGING, or PROTOTYPE TESTING.
3. **Evidence-based handoff** — the next task is generated from the actual result of the previous task.
4. **Minimum necessary context** — agents receive only what they need for their current task.

---

## 2. Execution Mode Model

The universal workflow revolves around three modes.

                    PROJECT
                       │
                       ▼
                EXECUTION MODE
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
 RAPID BUILD      ANALYSIS       PROTOTYPE
                  / DEBUGGING      TESTING
        │              │              │
        ▼              ▼              ▼
   BUILD FAST      UNDERSTAND     USE PRODUCT
   MIN ANALYSIS    FIND ISSUES    MANUAL TEST
        │              │              │
        └──────────────┼──────────────┘
                       ▼
                  NEXT DECISION

---

## 3. Mode Rules

### MODE 1 — RAPID BUILD (DEFAULT)
Purpose: BUILD THE PRODUCT AS FAST AS REASONABLY POSSIBLE.
- **Rule 1**: Every active agent must have a build task. (No dedicated non-building agents).
- **Rule 2**: Prioritize implementation, integration, and working functionality.
- **Rule 3**: Do NOT turn tasks into research projects. Avoid unnecessary deep analysis before implementation.
- **Rule 4**: If blocked, DO NOT spend excessive time on unrelated work. Report the blocker and wait for the orchestrator.

### MODE 2 — ANALYSIS / DEBUGGING
Purpose: Understand the current state of the project, or debug a specific problem.
- **Analysis**: Identify what is working, broken, blocked, and the distance to MVP.
- **Debugging Submode**: Reproduce the problem, identify root cause, implement a minimal fix, verify, and report. Do not refactor unrestricted.

### MODE 3 — PROTOTYPE TESTING
Purpose: TEST THE ACTUAL RUNNING PRODUCT AS A USER.
- Start the application (web, mobile, or desktop).
- Perform realistic user flows.
- Observe and report bugs, UX issues, and performance issues.

---

## 3A. Overlapping Domains

Where two agents' domains overlap (most commonly backend/frontend integration surfaces):

> **The owner of that layer retains authority over the shared surface.**

The exact split must be stated explicitly in `PROJECT_CONTEXT.md` for the current task — never assumed by the agent. If ownership is ambiguous, the task is **BLOCKED** until the human/integrator resolves it.

---

## 4. Agent Domain Specialization (Default Roster)

Adjust this table in `PROJECT_CONTEXT.md` if a given project uses a different tool roster.

| Agent              | Domain / Build Specialization                      |
| ------------------ | -------------------------------------------------- |
| Antigravity IDE    | Backend engineering, APIs, architecture            |
| Antigravity 2.0    | Frontend engineering, UI/UX implementation         |
| Google AI Studio   | AI / ML intelligence, reasoning models             |
| Google Jules       | Testing, QA, security review                       |
| Qwen Code          | Platform-specific (Android/CLI) engineering        |
| Stitch             | UI/UX design deliverables                          |
| GitHub Copilot     | Documentation, developer guides                    |

During RAPID BUILD, available agents should be assigned productive build tasks matching their domain.

---

## 5. Agent Responsibilities (Task Focus)

Rather than maintaining rigid tiers, agents focus on their assigned **ACTIVE BUILD TASK** within their domain:

- **Antigravity IDE**: Builds backend services, APIs, AI/model integration, business logic, server-side architecture.
- **Antigravity 2.0**: Builds production UI, screens, components, interactions, state management.
- **Google AI Studio**: Builds model prompts, structured outputs, evaluation pipelines.
- **Google Jules**: Builds test suites, runs QA gates, verifies features.
- **Qwen Code**: Builds platform-specific engineering (Android/CLI as defined per project).
- **Stitch**: Builds UI/UX design deliverables.
- **GitHub Copilot**: Builds documentation and guides.

---

## 6. Project Context Header

Every agent prompt must begin with the header defined in this repo's `PROJECT_CONTEXT.md`:

```text
PROJECT: <name>
REPO: <path/url>
MODE: RAPID BUILD (or ANALYSIS / PROTOTYPE TESTING)
STAGE: <stage>
YOUR ROLE: <Specialization>
SHARED SURFACE OWNER: <if applicable>
YOUR AUTHORIZED PATHS: <paths>
CURRENT BUILD TASK: <task>
```

Treat this header as authoritative for the current task.

---

## 7. Context Hygiene

Rapid Build prompts should contain only: Project Context, Current mode, Current task, Relevant architecture, Relevant contracts, Known dependencies, Expected output. Do not paste entire project histories.

---

## 7A. Credential Hygiene in Agent Prompts

**Never paste live credentials, API keys, tokens, or secrets into any agent prompt** — reference them by environment-variable name only.

---

## 8. Domain Backlog

Agents receive tasks within their domain backlog. The human/integrator may reorder, block, add, remove, or reprioritize. A Domain Backlog never authorizes cross-domain work.

---

## 9. Hackathon Duration Modes

Duration is set per-project in `PROJECT_CONTEXT.md`. It dictates BUILD SPEED and verification depth, not an enforced intelligence phase:

- **12h**: MAXIMUM BUILD SPEED, minimal verification.
- **24h**: Rapid build, focused verification.
- **36h**: Rapid build, moderate verification.
- **48h**: Rapid build, integration verification, prototype testing before final submission.
- **Ongoing**: Full gates on every meaningful release.

---

## 10. Input Trigger Syntax

```text
HACKATHON MODE: <duration> | Project: <name> | MODE: <mode>
```

---

## 11. Contract Ownership

Locked contracts remain human/integrator-owned. No agent may silently change a locked contract.
A contract change: STOP → REPORT → HUMAN DECISION → RE-SYNC → RESUME BUILD.

---

## 12. Manual Handoff Protocol

```text
Agent A → Mode Report → Human/Message Bus → Next Prompt → Agent B
```

---

## 13. Human Escalation Rules

Route to the human when: (1) contract changes are required, (2) architecture changes are required, (3) security-critical decisions arise, (4) an agent is genuinely blocked, (5) mode transition is required, (6) final integration/release decision is required. Do not interrupt the human for routine successful build progress.

---

## 14. Mode Reports

### RAPID BUILD Report

```text
AGENT IDENTITY: "I am <agent>, the <role> for <project>."
TASK: <what was assigned>
STATUS: COMPLETED / IN PROGRESS / BLOCKED / FAILED
WORK DONE: <actual implementation>
FILES CREATED: <files>
FILES MODIFIED: <files>
TESTS / VERIFICATION: <what was actually checked>
BLOCKERS: <actual blockers>
RECOMMENDED CHANGES: <improvements discovered but not implemented>
RECOMMENDED NEXT TASK: <what should happen next>
KNOWN RISKS: <important risks discovered>
```

### ANALYSIS Report

```text
PROJECT STATUS: <overall>
BUILD COMPLETION: <estimated percentage>
COMPLETED: <list>
IN PROGRESS: <list>
NOT STARTED: <list>
BLOCKED: <list>
BUGS: <list>
INTEGRATION GAPS: <list>
SECURITY RISKS: <list>
PERFORMANCE RISKS: <list>
TECHNICAL DEBT: <list>
DISTANCE TO MVP: <assessment>
DISTANCE TO DEMO-READY: <assessment>
RECOMMENDED NEXT TASKS: <ordered list>
TIME RISK: <low / medium / high>
```

### DEBUGGING Report

```text
BUG: <description>
REPRODUCTION: <steps>
ROOT CAUSE: <cause>
AFFECTED FILES: <files>
FIX: <what changed>
VERIFICATION: <tests/manual verification>
REGRESSION RISK: <assessment>
REMAINING ISSUES: <if any>
```

### PROTOTYPE TESTING Report

```text
TEST ENVIRONMENT: <environment>
BUILD VERSION: <version/commit>
TESTED FLOW: <flow>
RESULT: PASS / FAIL / PARTIAL
STEPS PERFORMED: <steps>
OBSERVED: <what happened>
EXPECTED: <what should happen>
BUGS FOUND: <bugs>
UX ISSUES: <issues>
PERFORMANCE ISSUES: <issues>
REQUESTED CHANGES: <changes required>
PRIORITY: CRITICAL / HIGH / MEDIUM / LOW
RECOMMENDED FIX: <fix>
```

---

## 15. Dynamic Handoff Principle

```text
Agent A → actual result → orchestrator evaluates → NEXT BUILD TASK
```

Never pre-generate a fixed chain of future prompts.

---

## 16. Git and Branch Isolation

```text
main → project sprint branch → agent feature branches
```

Agents must: inspect status, preserve user work, use appropriate branches, avoid modifying unrelated domains, and produce recoverable changes.

### Git Checkpoint Protocol (Non-Negotiable)

1. **Validate** all changes.
2. **Inspect** `git status` and diffs.
3. **Commit** with informative subject/body.
4. **Push** to the remote.
5. **Verify** the commit exists.
6. **Only then** declare task complete.

---

## 17. Quality Gates

**Gate 1 — Contract:** conforms to the shared contract?
**Gate 2 — Tests:** relevant tests pass?
**Gate 3 — Integration:** functions correctly?
**Gate 4 — Security:** PII leakage, exposed keys, prompt injection?
**Gate 5 — UX:** understandable and usable?

---

## 18. Final Rules

1. Know the project context before acting.
2. Follow the active Execution Mode.
3. Rapid Build is the Default mode.
4. Do not silently take another agent's domain.
5. Respect locked contracts.
6. Report actual results using the correct Mode Report format.
7. Report blockers honestly.
8. Do not invent missing functionality.
9. Preserve existing work.
10. Ask for human approval when required.
11. Keep agent context limited to what is necessary.
12. Never paste live credentials or secrets into any agent prompt.
13. Treat cross-agent file collisions at merge time as ownership violations, not routine conflicts.

---

## 19. One-Line Model

```text
MODE → ACTUAL RESULT → NEXT DECISION
```
