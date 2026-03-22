# Integration Tests for Notification Service

This directory contains integration tests for the notification-service following the hybrid testing strategy defined in PROJECT.md Section 23a and `specs/testing-strategy.md`.

## Testing Strategy

**Python equivalent of Testcontainers + LocalStack hybrid approach:**

- **Use moto** for AWS services (Python equivalent of LocalStack):
  - DynamoDB for idempotency tracking (free tier)
  
- **Mock SES client** (not in free tier):
  - Use `unittest.mock` for SES operations
  - Verify email sending with mock assertions

- **Use requests-mock** for webhooks (Python equivalent of WireMock):
  - Mock HTTP endpoints for webhook delivery
  - Test retry logic and error handling

## Test Files

### 1. `test_email_notification_integration.py` (8 tests)
Tests email notification workflow with mocked SES:
- Email composition with real templates
- SES client interaction (mocked)
- Multiple recipient handling
- Error handling (SES failures)
- Template rendering with special characters
- Minimal data handling

**Coverage**: Email service end-to-end

### 2. `test_webhook_notification_integration.py` (8 tests)
Tests webhook delivery with requests-mock:
- Webhook payload construction
- HTTP delivery to multiple endpoints
- Retry logic on failures
- Event type filtering
- Disabled webhook handling
- Timeout error handling
- Signature header verification

**Coverage**: Webhook service end-to-end

### 3. `test_event_handler_integration.py` (10 tests)
Tests complete event handling workflow:
- Event routing (ReportGenerated, ExperimentCompleted, MemberAdded)
- Idempotency checking (duplicate detection)
- Handler invocation
- Error handling scenarios
- Configuration validation
- Complete notification workflow (email + webhooks)

**Coverage**: Lambda handler orchestration

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
pip install -r requirements.txt
pip install pytest pytest-mock moto requests-mock
```

### Run All Integration Tests

```bash
# Run all integration tests
pytest tests/integration/ -v

# Run with coverage
pytest tests/integration/ -v --cov=. --cov-report=html

# Run specific test file
pytest tests/integration/test_email_notification_integration.py -v

# Run specific test
pytest tests/integration/test_event_handler_integration.py::TestEventHandlerIntegration::test_handle_report_generated_event_successfully -v
```

### Run in CI/CD

Integration tests run automatically in GitHub Actions:

```yaml
- name: Run Integration Tests
  run: |
    cd services/notification-service
    python -m pytest tests/integration/ -v --cov=.
```

## Benefits of This Approach

1. **Zero AWS Costs**: All AWS services mocked with moto
2. **Fast Execution**: No network calls, runs in seconds
3. **Portable**: Same tests run locally and in CI/CD
4. **Realistic**: Moto simulates actual AWS SDK behavior
5. **Isolated**: Each test is independent and repeatable
6. **Webhook Testing**: requests-mock simulates external endpoints

## Test Coverage Goals

- **Integration Tests**: Cover all AWS service interactions
- **Email Operations**: 100% coverage of email service
- **Webhook Operations**: 100% coverage of webhook service
- **Event Handling**: 100% coverage of handler logic
- **Error Scenarios**: All error paths tested

## Key Testing Patterns

### Mocking SES (Not in Free Tier)
```python
@pytest.fixture
def mock_ses_client():
    client = Mock(spec=SesClient)
    client.send_email = Mock(return_value='msg-123')
    return client
```

### Mocking Webhooks (requests-mock)
```python
def test_webhook_delivery():
    with requests_mock.Mocker() as m:
        m.post('https://webhook.example.com/notify', status_code=200)
        # Test webhook delivery
```

### Mocking DynamoDB (Moto)
```python
@pytest.fixture
def dynamodb_setup():
    with mock_aws():
        dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
        # Create table and yield
```

## References

- **Testing Strategy**: `../../specs/testing-strategy.md`
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `../../specs/ci-cd-pipelines.md`
- **Moto Documentation**: https://docs.getmoto.org/
- **requests-mock Documentation**: https://requests-mock.readthedocs.io/
