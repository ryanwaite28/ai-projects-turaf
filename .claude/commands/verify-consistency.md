# /verify-consistency — Run Consistency Verification Checklist

Run the full consistency verification defined in `.windsurf/workflows/project.md` for the current working state.

## Steps

1. **Active plans** — list all files in `.windsurf/plans/active/`. For each, note status and any items marked incomplete.

2. **PROJECT.md alignment** — read the modified files (from `git diff --name-only HEAD`) and for each changed service or spec, verify the change is consistent with the relevant section of `PROJECT.md`. Report any conflicts.

3. **Spec currency** — for each spec in `specs/` that corresponds to a recently changed service, check if `**Last Updated**` is current. List any stale specs.

4. **Task completion** — for each task file in `tasks/` that relates to recent changes, verify completed items are marked `[x]`. List any unchecked items that appear to be done.

5. **Cross-references** — for each plan in `active/` or recently moved to `completed/`, verify links in related specs and task files point to the correct location (no broken `active/` references after moves).

6. **ADR check** — if any recent change constitutes an architectural decision (new pattern, new library, changed security model, etc.), confirm an ADR exists in `docs/adr/`. List missing ADRs.

7. **Changelog check** — if `PROJECT.md` was modified in recent commits, confirm a changelog entry exists in `changelog/`. List missing entries.

## Output Format

For each check, output one of:
- `✓ <check name>` — if passing
- `✗ <check name>: <what's wrong and where>` — if failing

End with a summary: `X/7 checks passing`. For each failure, suggest the minimal fix.
