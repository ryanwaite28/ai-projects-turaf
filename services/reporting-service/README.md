# Reporting Service

AWS Lambda-based service for generating experiment reports in the Turaf platform.

## Overview

The Reporting Service is an event-driven Lambda function that listens for `ExperimentCompleted` events and generates comprehensive PDF reports with metrics analysis, insights, and visualizations.

## Architecture

- **Runtime**: AWS Lambda (Python 3.11)
- **Trigger**: AWS EventBridge
- **Storage**: Amazon S3
- **Idempotency**: DynamoDB
- **Pattern**: Event-driven, Clean Architecture, DDD

## Project Structure

```
reporting-service/
├── requirements.txt              # Production dependencies
├── requirements-dev.txt          # Development dependencies
├── src/
│   ├── __init__.py
│   └── lambda_handler.py        # Main Lambda entry point
└── tests/
    ├── __init__.py
    └── test_lambda_handler.py   # Unit tests
```

## Dependencies

### Production
- `boto3` - AWS SDK for Python
- `jinja2` - Template engine for report generation
- `weasyprint` - HTML to PDF conversion
- `requests` - HTTP client for service calls
- `python-json-logger` - Structured logging
- `tenacity` - Retry logic with exponential backoff

### Development
- `pytest` - Testing framework
- `pytest-mock` - Mocking library
- `pytest-cov` - Coverage reporting
- `moto` - AWS service mocking
- `black` - Code formatter
- `flake8` - Linter
- `mypy` - Type checker

## Installation

```bash
# Install production dependencies
pip install -r requirements.txt

# Install development dependencies
pip install -r requirements-dev.txt
```

## Testing

The service includes comprehensive unit tests with >80% code coverage:

- **Test Framework**: pytest
- **AWS Mocking**: moto (S3, DynamoDB, EventBridge)
- **HTTP Mocking**: responses
- **Coverage**: pytest-cov

### Test Structure

```
tests/
├── conftest.py                      # Shared fixtures
├── test_lambda_handler.py           # Lambda entry point tests
├── test_experiment_completed_handler.py  # Event handler tests
├── test_events.py                   # Event model tests
├── test_experiment_client.py        # Experiment service client tests
├── test_metrics_client.py           # Metrics service client tests
├── test_data_fetching.py            # Data fetching orchestration tests
├── test_report_data.py              # Report data model tests
├── test_aggregation.py              # Data aggregation tests
├── test_aggregated_data.py          # Aggregated data model tests
├── test_template_engine.py          # Template rendering tests
├── test_pdf_generation.py           # PDF generation tests
├── test_s3_storage.py               # S3 storage tests
├── test_event_publisher.py          # Event publishing tests
├── test_idempotency.py              # Idempotency service tests
└── test_report_generation.py        # Report generation orchestration tests
```

### Running Tests

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src --cov-report=html

# Run specific test file
pytest tests/test_lambda_handler.py -v

# Run tests matching pattern
pytest -k "test_upload" -v

# Run with detailed output
pytest -vv --tb=long
```

## Development

### Code Style
- Follow PEP 8 guidelines
- Use type hints for function signatures
- Write docstrings for all public functions
- Format code with `black`
- Lint with `flake8`

### Testing
- Write unit tests for all new functionality
- Maintain >80% code coverage
- Use moto for AWS service mocking
- Isolate tests (no shared state)

## Deployment

This service is deployed as an AWS Lambda function with:
- **Memory**: 1024 MB
- **Timeout**: 60 seconds
- **Concurrency**: 10 (reserved)
- **Trigger**: EventBridge rule for ExperimentCompleted events

## Environment Variables

- `ENVIRONMENT` - Deployment environment (dev/qa/prod)
- `S3_BUCKET_NAME` - S3 bucket for report storage
- `EVENT_BUS_NAME` - EventBridge bus name
- `EXPERIMENT_SERVICE_URL` - Experiment service API URL
- `METRICS_SERVICE_URL` - Metrics service API URL
- `IDEMPOTENCY_TABLE_NAME` - DynamoDB table for idempotency

## Event Schema

### Input: ExperimentCompleted Event
```json
{
  "id": "event-uuid",
  "detail-type": "ExperimentCompleted",
  "source": "turaf.experiment-service",
  "detail": {
    "eventId": "uuid",
    "eventType": "ExperimentCompleted",
    "organizationId": "org-id",
    "payload": {
      "experimentId": "exp-id",
      "completedAt": "ISO-8601",
      "result": "SUCCESS|FAILURE"
    }
  }
}
```

### Output: ReportGenerated Event
```json
{
  "eventId": "uuid",
  "eventType": "ReportGenerated",
  "organizationId": "org-id",
  "payload": {
    "reportId": "uuid",
    "experimentId": "exp-id",
    "reportLocation": "s3://bucket/path",
    "reportFormat": "PDF",
    "generatedAt": "ISO-8601"
  }
}
```

## License

Proprietary - Turaf Platform
