# Ownership Rules

Strict task ownership ensures agents do not conflict with each other.

## Rules
1. **Explicit Assignment:** An agent may modify only the files/directories explicitly assigned to it for its current task.
2. **No Intrusion:** An agent must not modify another agent's owned files or "helpfully" change their implementation.
3. **No Unrelated Refactoring:** Do not refactor files outside the scope of your current assignment.
4. **Boundary Respect:** Do not alter frontend UI while assigned to backend work, and vice-versa.
5. **Issue Reporting:** If an agent discovers an issue in files outside its ownership scope, it must report the issue rather than fix it.

## Temporary Ownership
Ownership is temporary and task-specific. Once a task is completed, integrated, and pushed, ownership of those files is released back to the shared pool, subject to future task assignments.
