# /complete-plan — Complete and Archive an Implementation Plan

Arguments: `$ARGUMENTS` (format: `<category>/<filename>` e.g. `architecture/fix-auth-architecture-and-tests.md`)

Complete the plan: **$ARGUMENTS**

Valid categories: `architecture`, `cicd`, `infrastructure-restructure`, `assessments`

## Steps

1. **Read** `.windsurf/plans/active/$ARGUMENTS` to confirm it exists and review its content.

2. **Update plan status** — edit the file's metadata:
   ```
   **Status**: Completed
   **Completed**: <today's date YYYY-MM-DD>
   ```

3. **Move the plan** from `active/` to the correct completed subdirectory:
   ```
   .windsurf/plans/active/<filename>  →  .windsurf/plans/completed/<category>/<filename>
   ```

4. **Update related specs** — for each spec listed in "Related Docs":
   - Add or update `**Last Updated**: <today's date>` and `**Status**: Current` metadata

5. **Update related tasks** — for each task file listed in "Related Docs":
   - Mark completed items with `- [x]`
   - Add `**Completion Date**: <today's date>`
   - Add `**Implementation**: See .windsurf/plans/completed/<category>/<filename>`

6. **Create ADR** (only if an architectural decision was made):
   - File: `docs/adr/ADR-XXX-<decision-title>.md`
   - Use the ADR template from `BEST_PRACTICES.md` if it exists

7. **Create changelog entry** (only if PROJECT.md was modified):
   - File: `changelog/<today's date>-<description>.md`

8. **Verify** — confirm no broken references to the old `active/` path.

Output a summary: what was completed, what was updated, and the final archived path.
