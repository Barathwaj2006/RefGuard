# Multi-Agent Engineering Contract

This repository operates as a shared source of truth for multiple AI coding agents.

## 1. SHARED SOURCE OF TRUTH
GitHub is the authoritative shared state between agents.

## 2. STRICT TASK OWNERSHIP
An agent may modify only the files/directories explicitly assigned to it for its current task.
- Do not modify another agent's owned files.
- Do not refactor unrelated files.
- Do not add features outside your assignment.
- Do not "helpfully" change another agent's implementation.
- Do not cross domain boundaries (e.g., alter UI while assigned to backend work).
- If you discover a problem outside your ownership, report it rather than modify it.

## 3. AGENT AWARENESS
Every participating agent must be able to determine:
- Active agents
- Agent task assignments
- Ownership boundaries
- Task statuses (completed, in-progress, blocked)
- Task dependencies

## 4. PARALLEL EXECUTION
Agents should work in parallel only when their scopes are independent. Do not parallelize tasks with overlapping ownership or unresolved dependencies. The coordination system must explicitly prevent concurrent agents from modifying the same ownership scope unless authorized.

## 5. GIT CHECKPOINT REQUIREMENT (NON-NEGOTIABLE)
Any agent that makes project changes must:
1. Validate its changes.
2. Inspect git status and relevant diff.
3. Create a meaningful commit.
4. Push the commit to the configured GitHub remote.
5. Verify that the commit exists on the remote.
6. Only then declare the task complete.

## 6. UNAUTHORIZED FEATURE DEVELOPMENT IS PROHIBITED
Do not implement application features not explicitly requested. Keep changes minimal and maintainable.
