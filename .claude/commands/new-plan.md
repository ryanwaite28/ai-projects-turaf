# /new-plan — Create a New Implementation Plan

Arguments: `$ARGUMENTS` (format: `<plan-name> [brief description]`)

Create a new implementation plan document for: **$ARGUMENTS**

## Steps

1. Read `PROJECT.md` to understand architectural constraints relevant to this work.
2. Check `specs/` for any existing spec that covers this feature/fix.
3. Check `tasks/` for any existing task files related to this work.
4. Check `.windsurf/plans/active/` to ensure no duplicate plan exists.

## Plan Document

Create the file `.windsurf/plans/active/<kebab-case-name>.md` with this structure:

```markdown
# Plan: <Title>

**Status**: Active
**Created**: <today's date YYYY-MM-DD>
**Related Docs**: <link to relevant specs and tasks>
**Related Services**: <comma-separated list of affected services>

---

## Problem / Goal

<1-3 sentence description of what this plan achieves and why>

---

## Implementation Plan

### Phase 1 — <Phase Name>
**1.1** <specific task>
**1.2** <specific task>

### Phase 2 — <Phase Name>
...

---

## Implementation Order

<Note any dependencies between phases>

---

## Files to Create / Modify

### New Files
- `path/to/new/file.java` — purpose

### Modified Files
- `path/to/existing/file.java` — what changes

---

## Consistency Checklist
- [ ] Changes align with PROJECT.md
- [ ] Related specs updated
- [ ] Related tasks updated
- [ ] Tests written
- [ ] Plan moved to completed/ when done
```

After creating the file, output the plan path and a one-line summary of what it covers.
