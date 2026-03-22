# Integration Tests for Reporting Service

This directory contains integration tests for the reporting-service following the hybrid testing strategy defined in PROJECT.md Section 23a and `specs/testing-strategy.md`.

## Testing Strategy

**Python equivalent of Testcontainers + LocalStack hybrid approach:**

- **Use moto** for AWS services (Python equivalent of LocalStack):
  - S3 for report storage (free tier)
  - DynamoDB for idempotency tracking (free tier)
  
- **Mock EventBridge** client (not in free tier):
  - Use `unittest.mock` for EventBridge operations
  - Verify event publishing with mock assertions

## Test Files

### 1. `test_s3_storage_integration.py`
Tests S3 storage operations with moto:
- PDF report upload to S3
- HTML report upload to S3
- Presigned URL generation
- Object metadata retrieval
- S3 key structure validation
- Input validation

**Coverage**: S3 storage service end-to-end

### 2. `test_event_handler_integration.py`
Tests event handling workflow:
- ExperimentCompleted event processing
- Idempotency checking (duplicate detection)
- Event parsing and validation
- Error handling scenarios
- EventBridge mocking (not in free tier)

**Coverage**: Event handler orchestration

### 3. `test_report_generation_integration.py`
Tests complete report generation workflow:
- End-to-end report generation
- Idempotency prevention
- Error handling
- Various data scenarios
- Complete workflow integration

**Coverage**: Report generation service

## Running Integration Tests

### Setup Virtual Environment

```bash
# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate  # On macOS/Linux
# or
venv\Scripts\activate  # On Windows

# Install dependencies
pip install -r requirements-dev.txt
```

### Run All Integration Tests

```bash
# Run all integration tests
pytest tests/integration/ -v

# Run with coverage
pytest tests/integration/ -v --cov=src --cov-report=html

# Run specific test file
pytest tests/integration/test_s3_storage_integration.py -v
```

### Run in CI/CD

Integration tests run automatically in GitHub Actions:

```yaml
- name: Run Integration Tests
  run: |
    cd services/reporting-service
    python -m pytest tests/integration/ -v --cov=src
```

## Benefits of This Approach

1. **Zero AWS Costs**: All AWS services mocked with moto
2. **Fast Execution**: No network calls, runs in seconds
3. **Portable**: Same tests run locally and in CI/CD
4. **Realistic**: Moto simulates actual AWS SDK behavior
5. **Isolated**: Each test is independent and repeatable

## Test Coverage Goals

- **Integration Tests**: Cover all AWS service interactions
- **S3 Operations**: 100% coverage of storage service
- **Event Handling**: 100% coverage of handler logic
- **Error Scenarios**: All error paths tested

## References

- **Testing Strategy**: `../../specs/testing-strategy.md`
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `../../specs/ci-cd-pipelines.md`
- **Moto Documentation**: https://docs.getmoto.org/
