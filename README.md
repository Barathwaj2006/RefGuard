# Multi-Agent Workspace

This repository is the coordination and control repository for multi-agent collaboration.

## Purpose
This repository defines and coordinates how multiple AI coding agents collaborate on software projects through a shared GitHub source of truth.

It is designed to be reusable for future application repositories and supports any agent type (frontend, backend, testing, design, etc.).

## Core Principles
1. **Shared Source of Truth:** GitHub is the authoritative state.
2. **Strict Task Ownership:** Agents only modify assigned scopes.
3. **Agent Awareness:** Agents track active tasks and boundaries.
4. **Parallel Execution:** Only allowed for independent scopes.
5. **Git Checkpoint Requirement:** Tasks are incomplete until changes are committed, pushed, and verified on the remote.

## Directory Structure
- `AGENTS.md`: Permanent multi-agent engineering contract.
- `.ai/AGENT_REGISTRY.md`: Participating agents and capabilities.
- `.ai/OWNERSHIP.md`: Ownership boundaries and rules.
- `.ai/COORDINATION.md`: State tracking and parallel execution rules.
- `.ai/GIT_PROTOCOL.md`: Mandatory Git workflow.
- `.ai/INTEGRATION.md`: Synchronization and integration rules.

**Note:** Do not implement application features in this coordination repository.
