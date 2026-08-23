# Agent Workflow Architecture

This document describes the high-level architecture of the Universal Multi-Agent Engineering Workflow.

## System Overview

```
┌──────────────────────────────────────────────────────────┐
│                    AGENTS.md                             │
│              (Primary Entry Point)                       │
│         Permanent principles & contract                  │
└──────────────────┬───────────────────────────────────────┘
                   │ references
                   ▼
┌──────────────────────────────────────────────────────────┐
│            agent-system/AGENT_TEAM.md                    │
│          (Universal Operating Model — v3)                │
│   Priority model, responsibility matrix, handoff         │
│   protocol, quality gates, duration modes,               │
│   escalation, Git safety, rollback, task lifecycle       │
│                                                          │
│   *** Identical across all repositories ***              │
└──────────────────┬───────────────────────────────────────┘
                   │ references
                   ▼
┌──────────────────────────────────────────────────────────┐
│         agent-system/PROJECT_CONTEXT.md                  │
│          (Project-Specific Context)                      │
│   Project name, repo, tech stack, stage, duration        │
│   mode, active roster, domain backlogs, contracts        │
│                                                          │
│   *** Changes per project / per sprint ***               │
└──────────────────┬───────────────────────────────────────┘
                   │ informs
                   ▼
┌──────────────────────────────────────────────────────────┐
│           agent-system/agents/*.md                       │
│          (Per-Agent Configurations)                      │
│   Each file: primary/secondary/tertiary,                 │
│   domain, authorized/restricted paths,                   │
│   expected outputs, completion criteria                  │
└──────────────────────────────────────────────────────────┘
```

## Task Flow

```
Human/Orchestrator
       │
       ▼
  Project Context Header
  (from PROJECT_CONTEXT.md)
       │
       ▼
  Agent receives task prompt
       │
       ▼
  Agent reads AGENTS.md → AGENT_TEAM.md → PROJECT_CONTEXT.md
       │
       ▼
  Agent verifies: authorized paths, domain ownership, current stage
       │
       ▼
  Agent executes task within its scope
       │
       ▼
  Quality Gates (Contract → Tests → Integration → Security → UX)
       │
       ▼
  Git Checkpoint (validate → inspect → commit → push → verify)
       │
       ▼
  Handoff Report submitted
       │
       ▼
  Human/Orchestrator evaluates → determines next task
```

## File Hierarchy

```
repository-root/
├── AGENTS.md                          # Entry point — permanent contract
├── README.md                          # Project overview
├── agent-system/
│   ├── AGENT_TEAM.md                  # Universal operating model (v3)
│   ├── PROJECT_CONTEXT.md             # Project-specific context
│   ├── PROJECT_PROFILE.md             # Project profile & metadata
│   └── agents/
│       ├── antigravity-ide.md         # Backend & integration
│       ├── antigravity-2.md           # Frontend engineering
│       ├── ai-studio.md              # AI/ML intelligence
│       ├── jules.md                  # Testing & QA
│       ├── qwen.md                   # Platform engineering
│       ├── stitch.md                 # UI/UX design
│       └── copilot.md               # Documentation
└── docs/
    └── architecture/
        └── agent-workflow.md          # This file
```

## Portability

To apply this workflow to a new repository:
1. Copy `AGENTS.md` and `agent-system/AGENT_TEAM.md` as-is (universal).
2. Create a new `agent-system/PROJECT_CONTEXT.md` for the new project.
3. Create a new `agent-system/PROJECT_PROFILE.md` for the new project.
4. Add/remove agent configs in `agent-system/agents/` based on the project's needs.
5. Update `README.md` minimally to reference the workflow.
