# Notification Service

Event-driven AWS Lambda function for sending notifications via multiple channels.

## Architecture

**Type**: AWS Lambda (Python 3.11)  
**Pattern**: Event-driven processor  
**Build Tool**: pip  
**Deployment**: Serverless (Lambda + EventBridge)

## Overview

The Notification Service is a serverless function that sends notifications to users based on domain events. It listens for events like `ExperimentCompleted`, `ReportGenerated`, and `MemberAdded`, then sends notifications via email (Amazon SES) and webhooks.

## Technology Stack

- **Runtime**: Python 3.11
- **AWS Services**: Lambda, EventBridge, SES, DynamoDB
- **Email**: Amazon SES with Jinja2 templates
- **HTTP Client**: requests (for webhooks)
- **Testing**: pytest, moto

## Project Structure

```
services/notification-service/
├── requirements.txt           # Production dependencies
├── requirements-dev.txt       # Development dependencies (planned)
├── src/
│   ├── notification_handler.py  # Main Lambda entry point
│   ├── handlers/              # Event-specific handlers
│   ├── services/              # Email and webhook services
│   ├── templates/             # Jinja2 email templates
│   └── config.py              # Configuration
└── tests/                     # Unit tests (planned)
```

## Event Flow

1. **Trigger**: Domain events from EventBridge
   - `ExperimentCompleted`
   - `ReportGenerated`
   - `MemberAdded`
2. **Process**:
   - Check idempotency (DynamoDB)
   - Determine recipients
   - Check user preferences
   - Send email notifications (SES)
   - Send webhook notifications (HTTP)
   - Mark event as processed
3. **Output**: Notifications delivered via email and webhooks

## Key Features

- **Multi-Channel**: Email (SES) and webhooks
- **Idempotency**: DynamoDB-based deduplication
- **Template Engine**: Jinja2 for email templates
- **Webhook Security**: HMAC signature verification
- **User Preferences**: Respect notification opt-out settings
- **Retry Logic**: Exponential backoff for failed deliveries
- **Recipient Selection**: Smart recipient determination based on roles

## Notification Channels

### Email (Amazon SES)
- Transactional emails with HTML templates
- Experiment completion notifications
- Report ready notifications
- Welcome emails for new members

### Webhooks
- Real-time event delivery to external systems
- HMAC-SHA256 signature for security
- Retry logic for failed deliveries
- Per-organization webhook configuration

## Development

### Setup

```bash
# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt  # When available
```

### Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src --cov-report=html
```

## Deployment

Deployed as AWS Lambda function with:
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Concurrency**: 20 (reserved)
- **Triggers**: EventBridge rules for multiple event types

See `infrastructure/terraform/modules/lambda/notification-service.tf` for infrastructure configuration.

## Environment Variables

- `ENVIRONMENT`: dev/qa/prod
- `SES_FROM_EMAIL`: notifications@turaf.com
- `EXPERIMENT_SERVICE_URL`: https://api.{env}.turaf.com
- `ORGANIZATION_SERVICE_URL`: https://api.{env}.turaf.com
- `FRONTEND_URL`: https://app.{env}.turaf.com
- `IDEMPOTENCY_TABLE_NAME`: processed_notification_events

## Implementation Status

🚧 **Partially Implemented** - 8 tasks defined:
1. ✅ Setup Lambda project
2. ⏳ Implement event handlers
3. ⏳ Implement email service
4. ⏳ Create email templates
5. ⏳ Implement webhook service
6. ⏳ Implement recipient selection
7. ⏳ Add idempotency
8. ⏳ Add unit tests

## Email Templates

### Available Templates
- `experiment-completed.html`: Experiment completion notification
- `report-ready.html`: Report ready notification
- `welcome.html`: Welcome to organization
- `experiment-started.html`: Experiment started notification

### Template Variables
Templates use Jinja2 syntax with variables like:
- `user_name`: Recipient's name
- `experiment_name`: Experiment name
- `organization_name`: Organization name
- `experiment_url`: Link to experiment details
- `report_url`: Link to download report

## Webhook Configuration

Organizations can configure webhooks in their settings:

```json
{
  "webhookUrl": "https://example.com/webhooks/turaf",
  "events": ["experiment.completed", "report.generated"],
  "secret": "webhook-signing-secret",
  "enabled": true
}
```

Webhooks include HMAC-SHA256 signature in `X-Turaf-Signature` header for verification.

## References

- **Specification**: `specs/notification-service.md`
- **Tasks**: `tasks/notification-service/`
- **Event Schemas**: `specs/event-schemas.md`
- **PROJECT.md**: Section 40 (Notification Service)

## Why Python Lambda?

This service uses Python Lambda instead of Java/Spring Boot because:

1. **Event-Driven**: Only responds to EventBridge events, no REST API
2. **Serverless Benefits**: Auto-scaling, pay-per-use, no infrastructure management
3. **Simpler Deployment**: Single function deployment
4. **Appropriate Complexity**: Focused event processing doesn't need full Spring Boot stack
5. **Cost Efficiency**: Lambda pricing model better for sporadic event processing
6. **Python Ecosystem**: Excellent libraries for email templating and HTTP requests
