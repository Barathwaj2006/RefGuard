# Git Protocol (Mandatory)

This is the non-negotiable Git workflow for all agents making code changes.

## 1. The Workflow
Any agent that makes actual project changes must follow these steps sequentially:
1. **Validate:** Validate all changes (lint, test, build).
2. **Inspect:** Inspect `git status` and relevant diffs.
3. **Commit:** Create a meaningful commit.
4. **Push:** Push the commit to the configured GitHub remote.
5. **Verify:** Verify that the commit exists on the remote.
6. **Complete:** Only after verification, declare the task complete.

## 2. Incomplete Tasks
A task involving code changes is **NOT** complete if:
- Changes remain uncommitted.
- The commit has not been pushed.
- The remote state has not been verified.

## 3. Commit Quality
Meaningful code-changing commits must use:
- A concise, informative subject.
- A useful body explaining what changed and why.
- Enough detail to understand the implementation.

**Prohibited Commit Messages:**
- "update"
- "changes"
- "fix"
- "done"

## 4. Restrictions
- Do not create meaningless commits when a task produced no changes.
- Do not force-push (`git push -f`).
- Do not rewrite history.
- Do not create unsolicited Pull Requests.

## 5. Failure Handling
If an agent successfully commits but cannot push:
- The task must remain **BLOCKED**.
- The agent must report the exact failure.
- The agent must **not** claim completion.
- The agent must **not** force-push.
- The agent should retry safely when appropriate.
