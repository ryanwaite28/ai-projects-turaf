# /next-task — Find and Begin the Next Implementation Task

Identify and begin the next task to implement based on the current project state.

## Steps

1. **Check active plans** — read all files in `.windsurf/plans/active/`. Identify the plan with the most in-progress or unstarted phases.

2. **Check suggested build order** from `.windsurf/workflows/project.md`:
   1. Identity Service
   2. Organization Service
   3. Experiment Service
   4. Metrics Service
   5. BFF API
   6. Event Infrastructure
   7. Reporting Service
   8. Notification Service
   9. Angular Frontend
   10. DevOps Infrastructure

3. **Find the next uncompleted task** — scan `tasks/` for the lowest-numbered incomplete task file for the current build-order service. A task is incomplete if it has unchecked items (`- [ ]`) and no `**Completion Date**`.

4. **Summarize what's next**:
   - Which plan it belongs to (if any)
   - Task file path
   - What the task requires implementing
   - Any blockers (e.g., a previous task must complete first)

5. **Ask**: "Should I implement this task now?" — if yes, proceed as per `/implement-task` behavior using the identified task file.

## Output Format

```
## Next Task

**Plan**: .windsurf/plans/active/<name>.md (Phase X)
**Task file**: tasks/<service>/<NNN>-<name>.md
**Summary**: <one-line description>
**Blockers**: <none / describe>

Ready to implement? (yes/no)
```
