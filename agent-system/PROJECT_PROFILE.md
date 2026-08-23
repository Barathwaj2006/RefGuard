# Project Profile

PROJECT: Multi-Agent Workspace
REPOSITORY: github.com/Barathwaj2006/multi-agent-workspace
PURPOSE: Coordination and control repository that defines and manages how multiple AI coding agents collaborate on software projects through a shared GitHub source of truth.
TECH STACK: Markdown (documentation-only repository)
ARCHITECTURE: Rule-based coordination layer — no application code. Universal operating model (AGENT_TEAM.md) + project-specific context (PROJECT_CONTEXT.md) per repository.
PRIMARY DOMAINS: Workflow Documentation
AGENT MAPPING:
  - Antigravity IDE: Workflow Architecture & Setup (Active)
  - Antigravity 2.0: Frontend Engineering (Available)
  - Google AI Studio: AI/ML Intelligence (Available)
  - Google Jules: Testing & QA (Available)
  - Qwen Code: Platform-specific Engineering (Available)
  - Stitch: UI/UX Design (Available)
  - GitHub Copilot: Documentation (Available)
DOMAIN OWNERS:
  - Workflow Documentation: Antigravity IDE
AUTHORIZED PATHS: `AGENTS.md`, `README.md`, `agent-system/*`, `docs/*`
RESTRICTED PATHS: None (no application code exists)
CONTRACTS: `AGENTS.md`, `agent-system/AGENT_TEAM.md`
CURRENT STAGE: Workflow Setup
HACKATHON MODE: N/A — ongoing development
DOMAIN BACKLOGS: See `agent-system/PROJECT_CONTEXT.md`
QUALITY REQUIREMENTS: Markdown validation, non-contradictory instructions, no duplicated rules across files, zero live secrets, all cross-references resolve.
