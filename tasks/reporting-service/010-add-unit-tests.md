# Task: Add Unit Tests

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 3 hours  

## Objective

Create comprehensive unit tests for all reporting service components using pytest.

## Prerequisites

- [x] All reporting-service implementation tasks completed

## Scope

**Test Files to Create**:
- `tests/test_lambda_handler.py`
- `tests/test_experiment_completed_handler.py`
- `tests/test_data_fetching.py`
- `tests/test_experiment_client.py`
- `tests/test_metrics_client.py`
- `tests/test_aggregation.py`
- `tests/test_template_engine.py`
- `tests/test_pdf_generation.py`
- `tests/test_s3_storage.py`
- `tests/test_event_publisher.py`
- `tests/test_idempotency.py`

## Implementation Details

### Test Configuration

**conftest.py**:
```python
import pytest
import os
from moto import mock_s3, mock_dynamodb, mock_events

@pytest.fixture
def aws_credentials():
    """Mock AWS credentials for moto"""
    os.environ['AWS_ACCESS_KEY_ID'] = 'testing'
    os.environ['AWS_SECRET_ACCESS_KEY'] = 'testing'
    os.environ['AWS_SECURITY_TOKEN'] = 'testing'
    os.environ['AWS_SESSION_TOKEN'] = 'testing'
    os.environ['AWS_DEFAULT_REGION'] = 'us-east-1'

@pytest.fixture
def s3_client(aws_credentials):
    """Mock S3 client"""
    with mock_s3():
        yield boto3.client('s3', region_name='us-east-1')

@pytest.fixture
def dynamodb_resource(aws_credentials):
    """Mock DynamoDB resource"""
    with mock_dynamodb():
        yield boto3.resource('dynamodb', region_name='us-east-1')

@pytest.fixture
def eventbridge_client(aws_credentials):
    """Mock EventBridge client"""
    with mock_events():
        yield boto3.client('events', region_name='us-east-1')
```

### Testing Framework

**pytest.ini**:
```ini
[pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
addopts = 
    --verbose
    --cov=src
    --cov-report=html
    --cov-report=term-missing
    --cov-fail-under=80
```

## Acceptance Criteria

- [x] All components tested
- [x] Code coverage > 80%
- [x] All edge cases covered
- [x] Mock AWS services properly (using moto)
- [x] Use pytest fixtures
- [x] Use pytest-mock for mocking
- [x] All tests pass independently
- [x] Tests are isolated

## Testing Requirements

**Unit Test Coverage**:
- Lambda handler
- Event handling
- Data fetching (with retry logic)
- HTTP client calls
- Aggregation logic
- Template rendering
- PDF generation
- S3 storage (upload, presigned URLs)
- Event publishing
- Idempotency (DynamoDB)

**Testing Tools**:
- pytest - Test framework
- pytest-mock - Mocking library
- pytest-cov - Coverage reporting
- moto - AWS service mocking
- responses - HTTP mocking

## References

- Specification: `specs/reporting-service.md`
- pytest Documentation: https://docs.pytest.org/
- moto Documentation: http://docs.getmoto.org/
- Related Tasks: All reporting-service tasks
