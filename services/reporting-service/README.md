# Reporting Service

Event-driven AWS Lambda function for generating experiment reports.

## Architecture

**Type**: AWS Lambda (Python 3.11)  
**Pattern**: Event-driven processor  
**Build Tool**: pip  
**Deployment**: Serverless (Lambda + EventBridge)

## Overview

The Reporting Service is a serverless function that automatically generates comprehensive reports when experiments are completed. It listens for `ExperimentCompleted` events, fetches relevant data, performs analysis, generates PDF/HTML reports, and publishes `ReportGenerated` events.

## Technology Stack

- **Runtime**: Python 3.11
- **AWS Services**: Lambda, EventBridge, S3, DynamoDB
- **Report Generation**: WeasyPrint (PDF), Jinja2 (templates)
- **HTTP Client**: requests (with tenacity for retries)
- **Testing**: pytest, moto

## Project Structure

```
services/reporting-service/
├── requirements.txt           # Production dependencies
├── requirements-dev.txt       # Development dependencies
├── src/
│   ├── lambda_handler.py      # Main Lambda entry point
│   ├── handlers/              # Event handlers
│   ├── models/                # Domain models and events
│   ├── services/              # Application services
│   ├── clients/               # HTTP clients for other services
│   ├── events/                # Event publishing
│   └── templates/             # Jinja2 report templates
├── tests/                     # Unit tests
└── pytest.ini                 # Test configuration
```

## Event Flow

1. **Trigger**: `ExperimentCompleted` event from EventBridge
2. **Process**: 
   - Check idempotency (DynamoDB)
   - Fetch experiment data (Experiment Service API)
   - Fetch metrics data (Metrics Service API)
   - Aggregate and analyze metrics
   - Render HTML template
   - Generate PDF report
   - Upload to S3
   - Publish `ReportGenerated` event
3. **Output**: Report stored in S3, event published

## Key Features

- **Idempotency**: DynamoDB-based deduplication prevents duplicate reports
- **Data Aggregation**: Statistical analysis of experiment metrics
- **Template Engine**: Jinja2 for flexible HTML report generation
- **PDF Generation**: WeasyPrint for professional PDF output
- **S3 Storage**: Hierarchical storage with presigned URLs
- **Event Publishing**: EventBridge integration for downstream consumers
- **Error Handling**: Retry logic with exponential backoff
- **Comprehensive Testing**: 200+ unit tests with >80% coverage

## Development

### Setup

```bash
# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt
```

### Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src --cov-report=html

# Run specific test file
pytest tests/test_lambda_handler.py -v
```

### Local Development

```bash
# Run tests in watch mode
pytest-watch

# Format code
black src/ tests/

# Lint code
flake8 src/ tests/
```

## Deployment

Deployed as AWS Lambda function with:
- **Memory**: 1024 MB
- **Timeout**: 60 seconds
- **Concurrency**: 10 (reserved)
- **Trigger**: EventBridge rule for `ExperimentCompleted` events

See `infrastructure/terraform/modules/lambda/reporting-service.tf` for infrastructure configuration.

## Environment Variables

- `ENVIRONMENT`: dev/qa/prod
- `S3_BUCKET_NAME`: turaf-reports-{env}
- `EVENT_BUS_NAME`: turaf-event-bus-{env}
- `EXPERIMENT_SERVICE_URL`: https://api.{env}.turaf.com
- `METRICS_SERVICE_URL`: https://api.{env}.turaf.com
- `IDEMPOTENCY_TABLE_NAME`: processed_events

## Implementation Status

✅ **Fully Implemented** - All 10 tasks completed:
1. ✅ Setup Lambda project
2. ✅ Implement event handler
3. ✅ Implement data fetching
4. ✅ Implement aggregation logic
5. ✅ Create report templates
6. ✅ Implement PDF generation
7. ✅ Implement S3 storage
8. ✅ Implement event publishing
9. ✅ Add idempotency
10. ✅ Add unit tests

## References

- **Specification**: `specs/reporting-service.md`
- **Tasks**: `tasks/reporting-service/`
- **Event Schemas**: `specs/event-schemas.md`
- **PROJECT.md**: Section 40 (Reporting Service)

## Why Python Lambda?

This service uses Python Lambda instead of Java/Spring Boot because:

1. **Event-Driven**: Only responds to EventBridge events, no REST API
2. **Serverless Benefits**: Auto-scaling, pay-per-use, no infrastructure management
3. **Simpler Deployment**: Single function deployment
4. **Appropriate Complexity**: Focused event processing doesn't need full Spring Boot stack
5. **Cost Efficiency**: Lambda pricing model better for sporadic event processing
6. **Python Ecosystem**: Excellent libraries for PDF generation (WeasyPrint) and templating (Jinja2)
