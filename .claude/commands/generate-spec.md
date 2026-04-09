# /generate-spec — Generate a Spec Document for a Service or Feature

Arguments: `$ARGUMENTS` (service or feature name, e.g. `reporting-service`, `event-flow`)

Generate a spec document for: **$ARGUMENTS**

## Steps

1. Read `PROJECT.md` — extract all sections relevant to `$ARGUMENTS` (architecture, data model, API contract, events, AWS integrations).
2. Check `specs/` for any existing partial spec to avoid duplication.
3. Check `tasks/$ARGUMENTS/` for any existing task files that imply spec content.

## Spec Document

Create `specs/$ARGUMENTS.md` using this structure:

```markdown
# <Service/Feature Name> Specification

**Status**: Draft
**Last Updated**: <today's date>
**Related Documents**: PROJECT.md §<section>, tasks/<service>/

---

## Overview
<2-3 sentences: what this service/feature does and why>

## Responsibilities
- <responsibility 1>
- <responsibility 2>

## Domain Model
<Key entities and their relationships>

## API Contract
<Endpoint list with method, path, request/response shape, auth requirements>

## Events
### Published
| Event | Trigger | Payload |
|-------|---------|---------|
| <EventName> | <when> | <key fields> |

### Consumed
| Event | Source | Action |
|-------|--------|--------|

## AWS Integrations
<SQS queues, EventBridge rules, S3 buckets, Lambda functions, DynamoDB tables used>

## Configuration
<Required environment variables and their purpose>

## Testing Approach
<Unit test targets, integration test targets, architecture test scenarios>

## Open Questions
- <any unresolved design decisions>
```

After creating the spec, output the file path and a one-line summary.
Ask: "Should I generate task files for this spec?"
