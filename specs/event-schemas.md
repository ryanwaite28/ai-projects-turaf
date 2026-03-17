# Event Schemas Specification

**Source**: PROJECT.md (Section 39)

This specification defines all event schemas used in the Turaf platform's event-driven architecture.

---

## Event Envelope Standard

All events must follow this standardized envelope structure:

```json
{
  "eventId": "uuid",
  "eventType": "string",
  "eventVersion": 1,
  "timestamp": "ISO-8601",
  "sourceService": "string",
  "organizationId": "string",
  "payload": {}
}
```

### Envelope Field Definitions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| eventId | UUID | Yes | Unique identifier for this event instance |
| eventType | String | Yes | Event name (PascalCase) |
| eventVersion | Integer | Yes | Schema version number |
| timestamp | ISO-8601 | Yes | When the event occurred (UTC) |
| sourceService | String | Yes | Service that published the event |
| organizationId | UUID | Yes | Tenant context for multi-tenancy |
| payload | Object | Yes | Event-specific data |

---

## Domain Events

### ProblemCreated

**Published By**: Experiment Service  
**Event Type**: `ProblemCreated`  
**Version**: 1  

**Payload Schema**:
```json
{
  "problemId": "uuid",
  "title": "string",
  "description": "string",
  "affectedUsers": "string",
  "context": "string",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Payload Fields**:
- `problemId` (UUID): Unique problem identifier
- `title` (String): Problem title (1-200 chars)
- `description` (String): Detailed description (1-5000 chars)
- `affectedUsers` (String): Description of affected users
- `context` (String): Additional context
- `createdBy` (UUID): User who created the problem
- `createdAt` (ISO-8601): Creation timestamp

**Example**:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "ProblemCreated",
  "eventVersion": 1,
  "timestamp": "2024-03-14T22:30:00.000Z",
  "sourceService": "experiment-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "problemId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "title": "Low user engagement on dashboard",
    "description": "Users spend less than 2 minutes on the dashboard",
    "affectedUsers": "All users",
    "context": "Dashboard was redesigned 3 months ago",
    "createdBy": "9f8e7d6c-5b4a-3210-fedc-ba0987654321",
    "createdAt": "2024-03-14T22:30:00.000Z"
  }
}
```

**Consumers**: None currently (future: analytics service)

---

### HypothesisCreated

**Published By**: Experiment Service  
**Event Type**: `HypothesisCreated`  
**Version**: 1  

**Payload Schema**:
```json
{
  "hypothesisId": "uuid",
  "problemId": "uuid",
  "statement": "string",
  "expectedOutcome": "string",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Payload Fields**:
- `hypothesisId` (UUID): Unique hypothesis identifier
- `problemId` (UUID): Parent problem identifier
- `statement` (String): Hypothesis statement (1-500 chars)
- `expectedOutcome` (String): Expected outcome description
- `createdBy` (UUID): User who created the hypothesis
- `createdAt` (ISO-8601): Creation timestamp

**Example**:
```json
{
  "eventId": "660f9511-f3ac-52e5-b827-557766551111",
  "eventType": "HypothesisCreated",
  "eventVersion": 1,
  "timestamp": "2024-03-14T22:35:00.000Z",
  "sourceService": "experiment-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "hypothesisId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "problemId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "statement": "If we add quick actions to the dashboard, user engagement will increase by 20%",
    "expectedOutcome": "Average session time increases from 2 to 2.4 minutes",
    "createdBy": "9f8e7d6c-5b4a-3210-fedc-ba0987654321",
    "createdAt": "2024-03-14T22:35:00.000Z"
  }
}
```

**Consumers**: None currently (future: analytics service)

---

### ExperimentStarted

**Published By**: Experiment Service  
**Event Type**: `ExperimentStarted`  
**Version**: 1  

**Payload Schema**:
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "name": "string",
  "description": "string",
  "startTime": "ISO-8601",
  "expectedEndTime": "ISO-8601"
}
```

**Payload Fields**:
- `experimentId` (UUID): Unique experiment identifier
- `hypothesisId` (UUID): Parent hypothesis identifier
- `name` (String): Experiment name (1-200 chars)
- `description` (String): Experiment description
- `startTime` (ISO-8601): When experiment started
- `expectedEndTime` (ISO-8601): Expected completion time (optional)

**Example**:
```json
{
  "eventId": "770fa622-g4bd-63f6-c938-668877662222",
  "eventType": "ExperimentStarted",
  "eventVersion": 1,
  "timestamp": "2024-03-14T22:40:00.000Z",
  "sourceService": "experiment-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "experimentId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "hypothesisId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "name": "Quick Actions A/B Test",
    "description": "Test quick actions feature with 50% of users",
    "startTime": "2024-03-14T22:40:00.000Z",
    "expectedEndTime": "2024-03-21T22:40:00.000Z"
  }
}
```

**Consumers**:
- Notification Service (send start notification)

---

### MetricRecorded

**Published By**: Metrics Service  
**Event Type**: `MetricRecorded`  
**Version**: 1  

**Payload Schema**:
```json
{
  "metricId": "uuid",
  "experimentId": "uuid",
  "metricName": "string",
  "metricValue": "number",
  "unit": "string",
  "recordedAt": "ISO-8601",
  "metadata": {}
}
```

**Payload Fields**:
- `metricId` (UUID): Unique metric identifier
- `experimentId` (UUID): Parent experiment identifier
- `metricName` (String): Metric name (1-100 chars)
- `metricValue` (Number): Measured value
- `unit` (String): Unit of measurement (optional)
- `recordedAt` (ISO-8601): When metric was recorded
- `metadata` (Object): Additional context (optional)

**Example**:
```json
{
  "eventId": "880fb733-h5ce-74g7-d049-779988773333",
  "eventType": "MetricRecorded",
  "eventVersion": 1,
  "timestamp": "2024-03-15T10:15:00.000Z",
  "sourceService": "metrics-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "metricId": "d4e5f6a7-b8c9-0123-def1-234567890123",
    "experimentId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "metricName": "conversion_rate",
    "metricValue": 0.25,
    "unit": "percentage",
    "recordedAt": "2024-03-15T10:15:00.000Z",
    "metadata": {
      "variant": "A",
      "source": "web"
    }
  }
}
```

**Consumers**: None currently (future: real-time analytics)

---

### ExperimentCompleted

**Published By**: Experiment Service  
**Event Type**: `ExperimentCompleted`  
**Version**: 1  

**Payload Schema**:
```json
{
  "experimentId": "uuid",
  "hypothesisId": "uuid",
  "name": "string",
  "endTime": "ISO-8601",
  "resultSummary": "string",
  "outcome": "string"
}
```

**Payload Fields**:
- `experimentId` (UUID): Unique experiment identifier
- `hypothesisId` (UUID): Parent hypothesis identifier
- `name` (String): Experiment name
- `endTime` (ISO-8601): When experiment completed
- `resultSummary` (String): Summary of results
- `outcome` (String): Outcome classification (VALIDATED, INVALIDATED, INCONCLUSIVE)

**Example**:
```json
{
  "eventId": "990fc844-i6df-85h8-e150-880099884444",
  "eventType": "ExperimentCompleted",
  "eventVersion": 1,
  "timestamp": "2024-03-21T22:40:00.000Z",
  "sourceService": "experiment-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "experimentId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "hypothesisId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "name": "Quick Actions A/B Test",
    "endTime": "2024-03-21T22:40:00.000Z",
    "resultSummary": "Quick actions increased engagement by 25%",
    "outcome": "VALIDATED"
  }
}
```

**Consumers**:
- Reporting Service (generate report)
- Notification Service (send completion notification)

**Critical Event**: Triggers multiple downstream processes

---

### ReportGenerated

**Published By**: Reporting Service  
**Event Type**: `ReportGenerated`  
**Version**: 1  

**Payload Schema**:
```json
{
  "reportId": "uuid",
  "experimentId": "uuid",
  "reportLocation": "string",
  "reportFormat": "string",
  "generatedAt": "ISO-8601"
}
```

**Payload Fields**:
- `reportId` (UUID): Unique report identifier
- `experimentId` (UUID): Parent experiment identifier
- `reportLocation` (String): S3 URI of report file
- `reportFormat` (String): Format of report (PDF, HTML)
- `generatedAt` (ISO-8601): When report was generated

**Example**:
```json
{
  "eventId": "aa0fd955-j7eg-96i9-f261-991100995555",
  "eventType": "ReportGenerated",
  "eventVersion": 1,
  "timestamp": "2024-03-21T22:45:00.000Z",
  "sourceService": "reporting-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "reportId": "e5f6a7b8-c9d0-1234-ef12-345678901234",
    "experimentId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "reportLocation": "s3://turaf-reports-prod/7c9e6679-7425-40de-944b-e07fc1f90ae7/c3d4e5f6-a7b8-9012-cdef-123456789012/e5f6a7b8-c9d0-1234-ef12-345678901234.pdf",
    "reportFormat": "PDF",
    "generatedAt": "2024-03-21T22:45:00.000Z"
  }
}
```

**Consumers**:
- Notification Service (send report ready notification)

---

### OrganizationCreated

**Published By**: Organization Service  
**Event Type**: `OrganizationCreated`  
**Version**: 1  

**Payload Schema**:
```json
{
  "organizationId": "uuid",
  "name": "string",
  "slug": "string",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Payload Fields**:
- `organizationId` (UUID): Unique organization identifier
- `name` (String): Organization name (1-100 chars)
- `slug` (String): URL-friendly identifier (3-50 chars)
- `createdBy` (UUID): User who created the organization
- `createdAt` (ISO-8601): Creation timestamp

**Example**:
```json
{
  "eventId": "bb0fea66-k8fh-a7j0-g372-aa2211aa6666",
  "eventType": "OrganizationCreated",
  "eventVersion": 1,
  "timestamp": "2024-03-14T20:00:00.000Z",
  "sourceService": "organization-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "createdBy": "9f8e7d6c-5b4a-3210-fedc-ba0987654321",
    "createdAt": "2024-03-14T20:00:00.000Z"
  }
}
```

**Consumers**: None currently (future: onboarding service)

---

### OrganizationUpdated

**Published By**: Organization Service  
**Event Type**: `OrganizationUpdated`  
**Version**: 1  

**Payload Schema**:
```json
{
  "organizationId": "uuid",
  "name": "string",
  "updatedBy": "uuid",
  "updatedAt": "ISO-8601"
}
```

**Payload Fields**:
- `organizationId` (UUID): Unique organization identifier
- `name` (String): Updated organization name
- `updatedBy` (UUID): User who updated the organization
- `updatedAt` (ISO-8601): Update timestamp

**Consumers**: None currently

---

### MemberAdded

**Published By**: Organization Service  
**Event Type**: `MemberAdded`  
**Version**: 1  

**Payload Schema**:
```json
{
  "organizationId": "uuid",
  "userId": "uuid",
  "role": "string",
  "addedBy": "uuid",
  "addedAt": "ISO-8601"
}
```

**Payload Fields**:
- `organizationId` (UUID): Organization identifier
- `userId` (UUID): User being added
- `role` (String): Role assigned (MEMBER, ADMIN)
- `addedBy` (UUID): User who added the member
- `addedAt` (ISO-8601): When member was added

**Example**:
```json
{
  "eventId": "cc0gfb77-l9gi-b8k1-h483-bb3322bb7777",
  "eventType": "MemberAdded",
  "eventVersion": 1,
  "timestamp": "2024-03-14T21:00:00.000Z",
  "sourceService": "organization-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "userId": "f6a7b8c9-d0e1-2345-f123-456789012345",
    "role": "MEMBER",
    "addedBy": "9f8e7d6c-5b4a-3210-fedc-ba0987654321",
    "addedAt": "2024-03-14T21:00:00.000Z"
  }
}
```

**Consumers**:
- Notification Service (send welcome email)

---

### MemberRemoved

**Published By**: Organization Service  
**Event Type**: `MemberRemoved`  
**Version**: 1  

**Payload Schema**:
```json
{
  "organizationId": "uuid",
  "userId": "uuid",
  "removedBy": "uuid",
  "removedAt": "ISO-8601"
}
```

**Payload Fields**:
- `organizationId` (UUID): Organization identifier
- `userId` (UUID): User being removed
- `removedBy` (UUID): User who removed the member
- `removedAt` (ISO-8601): When member was removed

**Consumers**: None currently

---

### MetricBatchRecorded

**Published By**: Metrics Service  
**Event Type**: `MetricBatchRecorded`  
**Version**: 1  

**Payload Schema**:
```json
{
  "experimentId": "uuid",
  "metricsCount": "number",
  "metricNames": ["string"],
  "recordedAt": "ISO-8601"
}
```

**Payload Fields**:
- `experimentId` (UUID): Parent experiment identifier
- `metricsCount` (Number): Number of metrics in batch
- `metricNames` (Array<String>): Names of metrics recorded
- `recordedAt` (ISO-8601): When batch was recorded

**Example**:
```json
{
  "eventId": "dd0hgc88-m0hj-c9l2-i594-cc4433cc8888",
  "eventType": "MetricBatchRecorded",
  "eventVersion": 1,
  "timestamp": "2024-03-15T12:00:00.000Z",
  "sourceService": "metrics-service",
  "organizationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "payload": {
    "experimentId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "metricsCount": 10,
    "metricNames": ["conversion_rate", "session_time", "bounce_rate"],
    "recordedAt": "2024-03-15T12:00:00.000Z"
  }
}
```

**Consumers**: None currently

---

## Event Versioning Strategy

### Version Evolution Rules

**Adding Fields** (Non-Breaking Change):
- Increment `eventVersion` to next number
- Add new optional fields to payload
- Old consumers ignore new fields
- New consumers use new fields
- Both versions supported simultaneously

**Example**:
```json
// Version 1
{
  "eventType": "ExperimentCompleted",
  "eventVersion": 1,
  "payload": {
    "experimentId": "uuid",
    "endTime": "ISO-8601"
  }
}

// Version 2 (added resultSummary)
{
  "eventType": "ExperimentCompleted",
  "eventVersion": 2,
  "payload": {
    "experimentId": "uuid",
    "endTime": "ISO-8601",
    "resultSummary": "string"  // New field
  }
}
```

**Removing Fields** (Breaking Change):
- Create new event type with different name
- Example: `ExperimentCompletedV2`
- Run both versions in parallel during migration
- Deprecate old version after migration complete

**Changing Field Types** (Breaking Change):
- Treat as new event type
- Follow removal strategy above

---

## Event Validation

### Schema Validation Rules

**Envelope Validation**:
- All envelope fields are required
- `eventId` must be valid UUID
- `eventVersion` must be positive integer
- `timestamp` must be valid ISO-8601
- `sourceService` must match known services
- `organizationId` must be valid UUID

**Payload Validation**:
- Validate against event-specific schema
- Required fields must be present
- Field types must match schema
- String lengths must be within limits
- UUIDs must be valid format

---

## Event Naming Conventions

**Event Type Naming**:
- PascalCase format
- Past tense verb (Created, Updated, Deleted, Started, Completed)
- Entity name first (Problem, Hypothesis, Experiment)
- Examples: `ProblemCreated`, `ExperimentCompleted`

**Field Naming**:
- camelCase format
- Descriptive names
- Consistent across events
- Examples: `experimentId`, `createdBy`, `recordedAt`

---

## Event Publishing Best Practices

1. **Immutability**: Events cannot be changed once published
2. **Idempotency**: Event handlers must be idempotent
3. **Ordering**: Do not rely on event order
4. **Completeness**: Include all necessary context in payload
5. **Security**: Never include sensitive data (passwords, tokens)
6. **Timestamps**: Always use UTC timezone
7. **Correlation**: Use eventId for tracking and debugging

---

## Event Consumption Best Practices

1. **Idempotency Checks**: Check if event already processed
2. **Error Handling**: Handle errors gracefully, use retries
3. **Dead Letter Queues**: Send failed events to DLQ
4. **Logging**: Log all event processing attempts
5. **Metrics**: Track event processing latency and failures
6. **Validation**: Validate event schema before processing

---

## References

- PROJECT.md: Event definitions and workflows
- event-flow.md: Event flow patterns
- AWS EventBridge Event Patterns
- JSON Schema Specification
