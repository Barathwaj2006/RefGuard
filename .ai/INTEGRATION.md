# Integration Protocol

This protocol defines how completed agent work becomes available to other agents.

## Synchronization
- Completed agent work is shared exclusively through the GitHub remote repository.
- Agents must pull or otherwise synchronize with the latest repository state before beginning new or dependent work.

## Integration Steps
1. **Pull Latest:** Before starting, ensure the local workspace is fully synced with the remote source of truth.
2. **Verify Dependencies:** Ensure any tasks that your work depends on are marked as Completed and have been merged/pushed to the remote.
3. **Execute Task:** Perform the assigned work adhering to OWNERSHIP.md and AGENTS.md.
4. **Push & Verify:** Follow GIT_PROTOCOL.md to push changes back to the shared remote.

By strictly adhering to remote synchronization, agents ensure they are never working on stale code or stepping on completed work.
