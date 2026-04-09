---
name: Claude Code Custom Commands
description: Project-level Claude Code slash commands created to replace Windsurf workflow
type: reference
---

Custom commands live in `.claude/commands/` and are invoked as `/command-name` in Claude Code.

| Command | File | Purpose |
|---------|------|---------|
| `/project` | commands/project.md | Load PROJECT.md + active plans into context |
| `/new-plan` | commands/new-plan.md | Create .windsurf/plans/active/<name>.md |
| `/complete-plan` | commands/complete-plan.md | Archive plan, update specs/tasks/ADRs |
| `/implement-task` | commands/implement-task.md | Implement a task file with DDD/SOLID rules |
| `/next-task` | commands/next-task.md | Find next uncompleted task by build order |
| `/run-tests` | commands/run-tests.md | Run and diagnose tests for a service |
| `/verify-consistency` | commands/verify-consistency.md | Run the 7-point consistency checklist |
| `/assess-docs` | commands/assess-docs.md | Score documentation quality 1–5 |
| `/generate-spec` | commands/generate-spec.md | Generate specs/<service>.md from PROJECT.md |
