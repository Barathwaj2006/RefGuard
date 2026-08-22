# Coordination System

This system tracks active tasks, dependencies, conflicts, and parallel execution.

## Tracking State
Agents must maintain awareness of:
- **Active Agents:** Which agents are currently working.
- **Task Assignments:** What each agent is doing.
- **Ownership Scopes:** Which files/directories are currently locked by an agent.
- **Task Status:** Completed, In-Progress, or Blocked.
- **Dependencies:** Which tasks block other tasks.

## Parallel Execution
- Agents may work in parallel **only** when their file/directory scopes are completely independent.
- Do not parallelize tasks with overlapping ownership.
- Do not parallelize tasks with unresolved dependencies.
- Concurrent modification of the same ownership scope is strictly prohibited unless explicitly authorized by the task coordinator.
