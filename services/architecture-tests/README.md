# Architecture Tests

Full-system architecture tests for the Turaf platform using Karate framework.

## Overview

These tests validate complete system integration from entry points (BFF API, WebSocket Gateway) through all downstream services, event-driven processes (EventBridge, SQS, Lambdas), and data persistence.

**Key Features**:
- Test complete user workflows from API entry points
- Validate event-driven processes with proper waiting strategies
- Run against fully deployed systems (local Docker Compose or AWS environments)
- Independent execution that does not block service deployments
- Automated test report generation and publishing

## Prerequisites

- Java 17
- Maven 3.8+
- Docker (for local testing)
- AWS credentials (for deployed environment testing)

## Running Tests

### Local (Docker Compose)
```bash
mvn test -Dkarate.env=local
```

### DEV Environment
```bash
mvn test -Dkarate.env=dev
```

### QA Environment
```bash
mvn test -Dkarate.env=qa
```

### PROD Environment
```bash
mvn test -Dkarate.env=prod
```

### Specific Test Suite
```bash
mvn test -Dtest=ExperimentWorkflowTestRunner -Dkarate.env=local
```

### Parallel Execution
```bash
mvn test -Dkarate.env=dev -Dkarate.threads=4
```

## Test Reports

HTML reports are generated in `target/karate-reports/karate-summary.html`

Open in browser:
```bash
open target/karate-reports/karate-summary.html
```

## Project Structure

```
architecture-tests/
├── src/test/
│   ├── java/com/turaf/architecture/
│   │   ├── config/          # Test configuration
│   │   ├── helpers/         # Wait helpers, AWS helpers
│   │   └── runners/         # JUnit test runners
│   └── resources/
│       ├── features/        # Karate test scenarios
│       ├── environments/    # Environment configs
│       └── karate-config.js # Karate configuration
├── terraform/               # Infrastructure for test reports
├── pom.xml
└── README.md
```

## Test Categories

### Authentication Tests
- User login and registration
- JWT token validation
- Token refresh and logout

### Organization Tests
- Create and manage organizations
- Add/remove members
- Organization workflows

### Experiment Lifecycle Tests
- Complete experiment workflow
- Metrics recording
- Async report generation
- Event-driven processes

### WebSocket Tests
- Real-time messaging
- Typing indicators
- Read receipts

### Orchestration Tests
- Dashboard overview
- Cross-service aggregation
- Parallel service calls

## Environment Configuration

Environment is set via `-Dkarate.env` system property:
- `local` - Local Docker Compose (default)
- `dev` - DEV environment (https://api.dev.turafapp.com)
- `qa` - QA environment (https://api.qa.turafapp.com)
- `prod` - PROD environment (https://api.turafapp.com)

## CI/CD Integration

Tests run automatically via GitHub Actions:
- Manual trigger: `gh workflow run architecture-tests.yml -f environment=dev`
- Scheduled: Every 6 hours against DEV
- Reports published to: `https://reports.{env}.turafapp.com`

## Documentation

- [Architecture Testing Specification](../../specs/architecture-testing.md)
- [Testing Strategy](../../specs/testing-strategy.md)
- [Task Breakdown](../../tasks/architecture-tests/README.md)
- [PROJECT.md Section 23b](../../PROJECT.md#23b-architecture-testing-strategy)

## Troubleshooting

### Tests fail to connect
- Verify environment is deployed and accessible
- Check AWS credentials if testing against deployed environments
- Ensure Docker is running for local tests

### Timeouts on async processes
- Increase timeout values in test scenarios
- Check CloudWatch logs for Lambda errors
- Verify EventBridge rules are configured

### Report generation fails
- Check S3 bucket permissions
- Verify CloudFront distribution exists
- Check IAM role has required permissions

## Contributing

When adding new tests:
1. Create feature file in appropriate category
2. Use existing helpers for waiting/validation
3. Follow Gherkin best practices
4. Add test data cleanup
5. Update documentation
