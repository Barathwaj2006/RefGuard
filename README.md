# Multi-Agent Workspace

This repository is the coordination and control repository for multi-agent collaboration. It defines how multiple AI coding agents collaborate on software projects through a shared GitHub source of truth.

## Universal Multi-Agent Engineering Workflow (v3)

This repository follows a structured multi-agent engineering workflow. Every agent entering this repository must read and follow these documents in order:

1. **[AGENTS.md](AGENTS.md)** — Primary entry point. Permanent engineering contract and principles.
2. **[agent-system/AGENT_TEAM.md](agent-system/AGENT_TEAM.md)** — Universal operating model. Priority model, responsibility matrix, handoff protocol, quality gates, duration modes, Git safety, rollback, and all operational procedures.
3. **[agent-system/PROJECT_CONTEXT.md](agent-system/PROJECT_CONTEXT.md)** — Project-specific context. This project's name, repo, tech stack, stage, active agent roster, domain backlogs, and contract locations.

## Supporting Documents

- **[agent-system/PROJECT_PROFILE.md](agent-system/PROJECT_PROFILE.md)** — Project metadata and profile.
- **[agent-system/agents/](agent-system/agents/)** — Per-agent configuration files (authorized paths, domains, completion criteria).
- **[docs/architecture/agent-workflow.md](docs/architecture/agent-workflow.md)** — Architecture overview and task flow diagram.

## Core Principles

1. **Shared Source of Truth** — GitHub is the authoritative state.
2. **Strict Task Ownership** — Agents only modify assigned scopes.
3. **Priority Model** — Primary → Secondary → Tertiary, with controlled escalation.
4. **Quality Gates** — Contract, Tests, Integration, Security, UX.
5. **Git Checkpoint** — Tasks are incomplete until committed, pushed, and verified on the remote.
6. **Evidence-Based Handoff** — Next task is generated from actual results, not pre-planned.
7. **Human Escalation** — Humans decide at coordination boundaries, not every micro-step.

**Note:** Do not implement application features in this coordination repository.
