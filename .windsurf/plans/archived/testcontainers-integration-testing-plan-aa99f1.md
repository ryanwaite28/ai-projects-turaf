# Testcontainers + LocalStack Integration Testing Strategy

Update PROJECT.md, specs, and workflow to document the comprehensive testing strategy using Testcontainers + LocalStack for integration tests that run both locally and in GitHub Actions CI/CD pipelines, with a hybrid approach for handling AWS services not in LocalStack's free tier.

---

## Scope

### 1. **PROJECT.md Updates**
Add a new section **"# 23a. Testing Strategy"** after the existing "# 23. Engineering Principles" section that covers:
- Testing pyramid (unit, integration, e2e)
- Testcontainers + LocalStack approach for AWS service integration
- **Hybrid approach**: LocalStack for free-tier services + @MockBean for paid services
- Where integration tests run in the CI/CD pipeline
- Local development testing workflow
- Test coverage expectations

### 2. **Create New Spec: testing-strategy.md**
Create `/specs/testing-strategy.md` with detailed documentation:
- Unit testing standards (JUnit 5, Mockito, 80%+ coverage)
- Integration testing with Testcontainers
- LocalStack configuration for free-tier AWS services (SQS, S3, DynamoDB)
- **AWS Service Mocking Strategy** (new section):
  - Document all three approaches: @MockBean, Custom Mocks, Hybrid
  - **Recommended: Hybrid Approach** - LocalStack for free services, @MockBean for paid
  - Specific guidance for EventBridge, CloudWatch, and other paid services
  - Code examples for each approach
- Test data management and isolation
- GitHub Actions integration
- Maven/Gradle test execution profiles
- Best practices for writing integration tests

### 3. **Update Spec: ci-cd-pipelines.md**
Update existing `/specs/ci-cd-pipelines.md` to include:
- Integration test stage in pipeline
- Docker-in-Docker configuration for Testcontainers
- Test result reporting and artifacts
- Parallel test execution strategy
- Environment-specific test configurations

### 4. **Update Workflow: project.md**
Update `.windsurf/workflows/project.md` to include:
- Testing guidance in task implementation prompts
- Integration test requirements for each service
- Test execution verification steps
- CI/CD testing validation

---

## Key Technical Details

### Testcontainers Configuration
- **LocalStack**: Single container for free-tier AWS services (SQS, S3, DynamoDB)
- **PostgreSQL**: Separate container for database integration tests
- **Automatic lifecycle**: Containers start/stop with test execution
- **Port mapping**: Dynamic port allocation to avoid conflicts

### AWS Service Mocking Strategy (Hybrid Approach)

**Free-Tier Services (Use LocalStack)**:
- SQS (Simple Queue Service)
- S3 (Object Storage)
- DynamoDB (NoSQL Database)
- Lambda (Serverless Functions)
- SNS (Simple Notification Service)

**Paid Services (Use @MockBean)**:
- EventBridge (Event Bus) - Not in LocalStack free tier
- CloudWatch Logs/Metrics - Limited in free tier
- Step Functions - Not in LocalStack free tier
- AppSync - Not in LocalStack free tier

**Implementation**:
```java
@SpringBootTest
@Testcontainers
class IntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(SQS, S3, DYNAMODB);
    
    @MockBean
    private EventBridgeClient eventBridgeClient;  // Mock paid service
    
    // Tests verify business logic without actual EventBridge costs
}
```

### GitHub Actions Integration
- Docker pre-installed on `ubuntu-latest` runners
- No additional setup required for Testcontainers
- Test reports published using `dorny/test-reporter@v1`
- Artifacts uploaded for test results and coverage
- No AWS costs for integration tests (free LocalStack + mocks)

### Maven Configuration
- Test profiles: `unit-tests`, `integration-tests`, `all-tests`
- Surefire plugin for unit tests
- Failsafe plugin for integration tests
- JaCoCo for code coverage reporting

---

## Files to Create/Update

1. **PROJECT.md** - Add section 23a (Testing Strategy)
2. **/specs/testing-strategy.md** - New comprehensive testing spec
3. **/specs/ci-cd-pipelines.md** - Update with integration test pipeline stages
4. **/.windsurf/workflows/project.md** - Update with testing guidance

---

## Success Criteria

- ✅ PROJECT.md includes high-level testing strategy
- ✅ testing-strategy.md provides detailed implementation guidance
- ✅ ci-cd-pipelines.md documents integration test execution in GitHub Actions
- ✅ Workflow includes testing requirements for AI-driven development
- ✅ All documentation is consistent and references Testcontainers + LocalStack
- ✅ Clear guidance for both local development and CI/CD environments

---

## Implementation Notes

- Existing services (communications-service) already have integration tests created
- Focus is on documentation and standardization across all services
- Integration tests use `@Testcontainers` annotation for automatic container management
- Tests are portable: same tests run locally and in CI/CD
- No manual infrastructure setup required (containers are ephemeral)

### AWS Service Mocking Guidelines

**When to Use LocalStack** (Free Tier):
- Service is in LocalStack free tier
- Need to test actual AWS SDK behavior
- Testing message queuing, storage, or database operations

**When to Use @MockBean** (Paid Services):
- Service not in LocalStack free tier (EventBridge, CloudWatch, Step Functions)
- Focus is on business logic, not AWS SDK integration
- Want to avoid LocalStack Pro costs

**When to Use Custom Mocks**:
- Need more realistic behavior than @MockBean
- Want to simulate specific AWS service responses
- Testing error handling and edge cases

**Example Services**:
- **Communications Service**: LocalStack for SQS, @MockBean for EventBridge
- **Metrics Service**: LocalStack for DynamoDB, @MockBean for CloudWatch
- **Reporting Service**: LocalStack for S3, @MockBean for Step Functions
