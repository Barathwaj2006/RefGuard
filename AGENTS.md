# Multi-Agent Engineering Contract & Universal Workflow

This repository operates as a shared source of truth for multiple AI coding agents. It follows a **Universal Multi-Agent Engineering Workflow** designed to ensure agents work efficiently without collisions.

## 1. SHARED SOURCE OF TRUTH & GIT SAFETY
- GitHub is the authoritative shared state between agents.
- Before modifying anything: inspect git status, branch, recent commits, and uncommitted work.
- Never destroy existing user work. Use isolated branches for meaningful feature work.
- Only the designated integrator should merge agent branches unless explicitly authorized.
- Any agent making project changes must validate its changes, inspect git status/diff, commit logically, push to remote, and verify the remote commit.

## 2. PROJECT-SPECIFIC INSTRUCTIONS
Agents must always consult the project-specific configurations before beginning work:
- **`agent-system/AGENT_TEAM.md`**: Defines project-specific agent responsibilities and available roster.
- **`agent-system/PROJECT_PROFILE.md`**: Contains the project context, architecture, tech stack, and current hackathon mode.
- **`agent-system/agents/`**: Contains exact authorized paths and roles for specific agents.

## 3. STRICT TASK OWNERSHIP & PRIORITY MODEL
An agent may modify only the files/directories explicitly assigned to it.
- **Primary Responsibility**: Highest priority. Cannot abandon unfinished primary work for secondary work.
- **Secondary Responsibility**: Backup responsibility. Requires primary completion or explicit assignment. Overlaps default to the Primary Owner.
- **Tertiary Responsibility**: Last-resort. Requires explicit instruction.
An agent cannot self-declare completion and silently switch responsibilities mid-task. Ownership is temporary and task-specific, but strictly enforced while active. Do not cross domain boundaries.

## 4. TASK LIFECYCLE & HANDOFF PROTOCOL
Tasks transition through: `PENDING` -> `ASSIGNED` -> `RUNNING` -> `COMPLETED` (or `BLOCKED`, `FAILED`, `NEEDS_REVIEW`).
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
The next task should be based on the actual result, not pre-generated.

## 5. CONTEXT & SECRETS HYGIENE
- **Context Hygiene**: Agents should receive only required information (Project Context Header, relevant contracts, current task, relevant repo context, completion criteria). Do not dump entire histories.
- **Secrets Hygiene**: NEVER place live secrets (API keys, passwords, tokens) inside an agent prompt. Reference environment variables or secret-store names instead.

## 6. CONTRACT OWNERSHIP & QUALITY GATES
Once shared contracts/interfaces are locked, they are owned exclusively by the human/integrator. If a change is needed, STOP work, report mismatch, and wait for human review.
All changes must pass 5 Quality Gates (depth determined by Hackathon Mode):
- **Gate 1: CONTRACT** - Conforms to shared interfaces?
- **Gate 2: TESTS** - Do tests pass?
- **Gate 3: INTEGRATION** - Works with the rest of the system?
- **Gate 4: SECURITY** - No secrets, PII leaks, prompt injections, auth issues?
- **Gate 5: UX** - Understandable by user?

## 7. HUMAN ESCALATION & NO-IDLE RULE
Escalate to the human when: blocked, contract mismatch occurs, stage completes, architecture changes, security-sensitive decisions occur, or explicit approval is required.
If an agent is blocked, do not take another agent's domain. Assign bounded work inside its own domain (e.g., analysis, test prep, documentation).

## 8. AUTONOMOUS ORCHESTRATION & ROLLBACK
Future phases may introduce fully autonomous orchestrators. Until then, handoffs are human-mediated.
Rollbacks: Agent work must remain recoverable. A failed branch must not damage stable integration branches.
