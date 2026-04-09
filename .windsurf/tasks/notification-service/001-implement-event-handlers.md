# Task: Implement Event Handlers

**Status**: COMPLETED  
**Completed**: 2026-04-09  
**Service**: `services/notification-service`  
**Type**: Implementation — Event Handler Orchestration  

---

## Problem

All three event handlers were stubs. They extracted event fields, logged them,
and returned a 200 status — without calling any of the notification infrastructure
(`EmailService`, `RecipientService`, `WebhookService`) that was already fully
implemented.

Additionally, the event field extraction was incorrect: handlers read fields
directly from `detail` instead of following the standard turaf event envelope
where event-specific fields live under `detail.payload`.

| Handler | File | State |
|---|---|---|
| `ExperimentCompleted` | `handlers/experiment_completed.py` | Stub |
| `ReportGenerated` | `handlers/report_generated.py` | Stub |
| `MemberAdded` | `handlers/member_added.py` | Stub |

---

## Solution

Fully implemented all three handlers. Each follows the same pipeline:

1. **Extract** from correct event envelope (`detail.organizationId`, `detail.payload.*`)
2. **Validate** required fields — return 400 on failure
3. **Resolve recipients** via `RecipientService` (filtered by event type and channel preference)
4. **Send emails** via `EmailService` — per-recipient, errors logged and skipped
5. **Deliver webhooks** via `WebhookService` — fan-out to all configured org endpoints
6. **Return** structured 200 with counts of emails/webhooks delivered

### ExperimentCompleted

```
ExperimentCompleted event
  → RecipientService.get_recipients(org, 'experiment.completed', 'email')
  → EmailService.send_experiment_completed_email() [per recipient]
  → WebhookService.send_webhooks('experiment.completed')
```

### ReportGenerated

```
ReportGenerated event (from reporting-service Lambda)
  → RecipientService.get_recipients(org, 'report.generated', 'email')
  → EmailService.send_report_generated_email() [per recipient, with report URL]
  → WebhookService.send_webhooks('report.generated')
```

### MemberAdded

```
MemberAdded event (from organization-service)
  → OrganizationClient.get_organization() [for org name in welcome email]
  → EmailService.send_member_added_email() [directly to the new member]
  → WebhookService.send_webhooks('member.added')
```

Note: MemberAdded targets the new member directly rather than using
`RecipientService`, as the welcome email is not preference-gated.

---

## Event Envelope Correction

The turaf standard event envelope wraps event-specific data in `detail.payload`:

```json
{
  "detail": {
    "organizationId": "org-id",
    "payload": {
      "experimentId": "exp-id",
      "..."
    }
  }
}
```

All handlers now extract `organization_id` from `detail` and event-specific
fields from `detail.payload`, matching the pattern used by the reporting-service
and the Java microservices' `EventEnvelope`.

---

## Files Changed

| File | Change |
|---|---|
| `handlers/experiment_completed.py` | Full rewrite — stub → production pipeline |
| `handlers/report_generated.py` | Full rewrite — stub → production pipeline |
| `handlers/member_added.py` | Full rewrite — stub → production pipeline |

---

## Resilience Notes

- **Email errors** are caught per-recipient and logged; one failure does not block others
- **Recipient resolution errors** return empty list (fail-open) — handler still delivers webhooks
- **Webhook errors** are caught per-endpoint and logged in `WebhookService`
- **Org name fetch failure** in MemberAdded falls back to `Organization {id}` — welcome email still sent
- **HTTP client sessions** are closed via `finally` blocks in all handlers

---

## Verification

Run unit tests:
```bash
cd services/notification-service
python -m pytest tests/ -v
```

Run integration tests (requires MiniStack + SES sandbox):
```bash
python -m pytest tests/integration/ -v
```
