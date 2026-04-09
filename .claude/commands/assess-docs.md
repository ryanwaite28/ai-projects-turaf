# /assess-docs — Assess Workspace Documentation Quality

Evaluate the current state of all project documentation. **Do not make any code changes.**

## Evaluation Criteria

For each documentation area, score 1–5 (1 = poor, 5 = excellent) on:
- **Consistency**: Does it match PROJECT.md and other docs?
- **Completeness**: Are all services/flows/decisions documented?
- **Currency**: Is it up to date with recent code changes?
- **Organization**: Is it easy to navigate and find what you need?

## Areas to Assess

1. **PROJECT.md** — Is it the genuine single source of truth? Are sections complete? Any stale sections?
2. **`specs/`** — Does every service have a spec? Are specs current with the codebase?
3. **`tasks/`** — Do tasks reflect current implementation state? Are completed tasks marked?
4. **`.windsurf/plans/`** — Are active plans still active? Any completed plans left in `active/`?
5. **`docs/adr/`** — Are key architectural decisions documented? Any undocumented decisions in the code?
6. **`DOCUMENTATION_INDEX.md`** — Is it complete and navigable?
7. **Cross-references** — Do links between docs work? Any orphaned docs?

## Output Format

```
## Documentation Assessment — <today's date>

### Scores
| Area | Consistency | Completeness | Currency | Organization | Notes |
|------|------------|--------------|----------|--------------|-------|
| PROJECT.md | 4 | 3 | 4 | 5 | Missing § for Lambda services |
...

### Critical Issues (score ≤ 2)
- <issue>: <what's wrong> → <recommended fix>

### Recommendations
1. <highest priority fix>
2. <next fix>
...
```

After the assessment, ask: "Should I create a plan to address these documentation gaps?"

If yes: create the plan at `docs/assessments/workspace-docs/<YYYYMMDD>.assessment.md` and a corresponding plan at `.windsurf/plans/assessments/workspace-docs/<YYYYMMDD>.plan.md`.
