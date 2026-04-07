# Architecture Testing Specification

**Source**: PROJECT.md (Section 23b)  
**Last Updated**: March 31, 2026  
**Status**: Current  
**Related Documents**: 
- [Testing Strategy](testing-strategy.md)
- [BFF API](bff-api.md)
- [Architecture](architecture.md)
- [PROJECT.md](../PROJECT.md)
- [API Fixes Implementation Summary](../docs/api/api-fixes-implementation-summary.md)
- [API Alignment Review](../docs/assessments/architecture-tests-api-alignment-2026-03-31.md)

This specification defines the architecture testing strategy for the Turaf platform using the Karate framework to validate complete system integration from entry points through all event-driven processes.

---

## Overview

Architecture testing validates end-to-end workflows by testing from the BFF API and WebSocket Gateway entry points through all downstream microservices, event-driven processes (EventBridge, SQS, Lambdas), and data persistence.

**Purpose**: Ensure the complete system works as an integrated whole, validating:
- API entry points (BFF API, WebSocket Gateway)
- Service-to-service communication
- Event-driven workflows (EventBridge → SQS → Lambda)
- Asynchronous processing with proper waiting strategies
- Data persistence and retrieval across services
- Cross-service orchestration and aggregation

**Key Difference from Other Tests**:
- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions with dependencies
- **Component Tests**: Test entry points with mocked downstream services
- **Architecture Tests**: Test complete system with all real components

---

## Testing Framework

### Karate Framework

**Why Karate**:
- Native support for REST APIs and WebSocket testing
- Gherkin-based feature files for readable test scenarios
- Built-in JSON/XML path assertions
- Java integration for custom helpers and waiting strategies
- Parallel test execution support
- Rich HTML reporting with screenshots and logs
- No need for separate assertion libraries

**Version**: 1.4.1 (latest stable)

**Key Features Used**:
- HTTP client for REST API testing
- WebSocket support for real-time messaging tests
- JavaScript functions for custom logic
- Java interop for AWS SDK integration
- Conditional waits and polling
- Data-driven testing with examples
- Test hooks for setup/teardown

---

## Test Architecture

### Service Structure

```
services/architecture-tests/
├── src/
│   ├── test/
│   │   ├── java/
│   │   │   └── com/turaf/architecture/
│   │   │       ├── config/
│   │   │       │   ├── TestConfig.java
│   │   │       │   ├── EnvironmentConfig.java
│   │   │       │   └── KarateTestRunner.java
│   │   │       ├── helpers/
│   │   │       │   ├── WaitHelper.java
│   │   │       │   ├── TokenHelper.java
│   │   │       │   ├── EventHelper.java
│   │   │       │   └── AwsHelper.java
│   │   │       └── runners/
│   │   │           ├── AuthenticationTestRunner.java
│   │   │           ├── ExperimentWorkflowTestRunner.java
│   │   │           ├── OrganizationTestRunner.java
│   │   │           └── WebSocketTestRunner.java
│   │   └── resources/
│   │       ├── karate-config.js
│   │       ├── environments/
│   │       │   ├── local.properties
│   │       │   ├── dev.properties
│   │       │   ├── qa.properties
│   │       │   └── prod.properties
│   │       └── features/
│   │           ├── authentication/
│   │           │   ├── login.feature
│   │           │   ├── registration.feature
│   │           │   └── token-refresh.feature
│   │           ├── organizations/
│   │           │   ├── create-organization.feature
│   │           │   ├── manage-members.feature
│   │           │   └── organization-workflows.feature
│   │           ├── experiments/
│   │           │   ├── experiment-lifecycle.feature
│   │           │   ├── metrics-recording.feature
│   │           │   └── report-generation.feature
│   │           ├── websocket/
│   │           │   ├── messaging.feature
│   │           │   ├── typing-indicators.feature
│   │           │   └── read-receipts.feature
│   │           ├── orchestration/
│   │           │   ├── dashboard-overview.feature
│   │           │   └── cross-service-aggregation.feature
│   │           └── common/
│   │               ├── setup.feature
│   │               ├── cleanup.feature
│   │               └── wait-helpers.feature
├── terraform/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── s3.tf
│   ├── cloudfront.tf
│   └── route53.tf
├── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore
```

---

## Test Categories

### 1. Authentication & Authorization Tests

**File**: `features/authentication/login.feature`

**Scenarios**:
- User login with valid credentials → JWT token returned
- User login with invalid credentials → 401 Unauthorized
- Access protected endpoint with valid token → 200 OK
- Access protected endpoint without token → 401 Unauthorized
- Access protected endpoint with expired token → 401 Unauthorized
- Token refresh flow → new token issued

**Example**:
```gherkin
Feature: User Authentication

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }

  Scenario: Successful login returns LoginResponseDto with tokens
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    And match response.accessToken != null
    And match response.refreshToken != null
    And match response.user.email == testUser.email
    And match response.tokenType == 'Bearer'
    And match response.expiresIn > 0
    
    # Verify token works for protected endpoint
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + response.accessToken
    When method GET
    Then status 200
    And match response.email == testUser.email
```

### 2. Organization Management Tests

**File**: `features/organizations/create-organization.feature`

**Scenarios**:
- Create organization → verify persistence
- List user's organizations → verify newly created org appears
- Update organization → verify changes persisted
- Add member to organization → EventBridge event published
- Update member role → verify role changed
- Remove member → verify access revoked

**Organization Listing**:
```gherkin
Scenario: List user's organizations
  # Login
  Given path '/api/v1/auth/login'
  And request { email: 'test@example.com', password: 'Test123!' }
  When method POST
  Then status 200
  * def token = response.accessToken
  
  # List organizations
  Given path '/api/v1/organizations'
  And header Authorization = 'Bearer ' + token
  When method GET
  Then status 200
  And match response != null
  And match response[*].id != null
  And match response[*].name != null
```

**Event-Driven Validation**:
```gherkin
Scenario: Adding member triggers notification event
  # Create organization
  Given path '/api/v1/organizations'
  And request { name: 'Test Org', description: 'Test' }
  When method POST
  Then status 201
  * def orgId = response.id
  
  # Add member
  Given path '/api/v1/organizations', orgId, 'members'
  And request { userId: 'user-123', role: 'MEMBER' }
  When method POST
  Then status 201
  * def memberId = response.id
  
  # Wait for EventBridge processing
  * def helper = Java.type('com.turaf.architecture.helpers.EventHelper')
  * helper.waitForEventProcessing('MemberAdded', 10)
  
  # Update member role
  Given path '/api/v1/organizations', orgId, 'members', memberId
  And request { role: 'ADMIN' }
  When method PATCH
  Then status 200
  And match response.role == 'ADMIN'
  
  # Remove member
  Given path '/api/v1/organizations', orgId, 'members', memberId
  When method DELETE
  Then status 200
  
  # Verify member removed
  Given path '/api/v1/organizations', orgId, 'members'
  When method GET
  Then status 200
  And match response[?(@.id == memberId)] == []
```

### 3. Experiment Lifecycle Tests (Event-Driven)

**File**: `features/experiments/experiment-lifecycle.feature`

**Scenarios**:
- Create problem → ProblemCreated event published
- Create hypothesis linked to problem
- Start experiment → ExperimentStarted event → metrics collection enabled
- Record multiple metrics → MetricRecorded events published
- Complete experiment → ExperimentCompleted event → triggers report generation
- Wait for async report generation (Lambda)
- Verify report exists in S3
- Retrieve report via API → verify content

**Complete Workflow Example**:
```gherkin
Feature: Complete Experiment Lifecycle

  Background:
    * url baseUrl
    * def waitHelper = Java.type('com.turaf.architecture.helpers.WaitHelper')
    * def awsHelper = Java.type('com.turaf.architecture.helpers.AwsHelper')

  Scenario: Complete experiment workflow with async report generation
    # Create problem
    Given path '/api/v1/problems'
    And request { title: 'Test Problem', description: 'Problem description' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    # Create hypothesis
    Given path '/api/v1/hypotheses'
    And request { problemId: '#(problemId)', statement: 'If we do X, Y will improve' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    # Create experiment
    Given path '/api/v1/experiments'
    And request { name: 'Test Experiment', hypothesisId: '#(hypothesisId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    # Start experiment
    Given path '/api/v1/experiments', experimentId, 'start'
    When method POST
    Then status 200
    And match response.status == 'RUNNING'
    
    # Record metrics
    Given path '/api/v1/metrics'
    And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.25 }
    When method POST
    Then status 201
    
    Given path '/api/v1/metrics'
    And request { experimentId: '#(experimentId)', name: 'user_engagement', value: 0.75 }
    When method POST
    Then status 201
    
    # Complete experiment (triggers async report generation)
    Given path '/api/v1/experiments', experimentId, 'complete'
    And request { resultSummary: 'Experiment completed successfully' }
    When method POST
    Then status 200
    And match response.status == 'COMPLETED'
    
    # Wait for report generation (async Lambda process)
    # Poll API endpoint until report is ready
    * def reportReady = waitHelper.waitForCondition('/api/v1/experiments/' + experimentId + '/report', 30)
    * match reportReady == true
    
    # Verify report exists
    Given path '/api/v1/experiments', experimentId, 'report'
    When method GET
    Then status 200
    And match response.reportUrl != null
    And match response.generated == true
    
    # Optionally verify S3 object exists
    * def s3Exists = awsHelper.verifyS3Object('turaf-reports-dev', response.s3Key)
    * match s3Exists == true

  Scenario: Cancel running experiment
    # Start experiment
    Given path '/api/v1/experiments', experimentId, 'start'
    When method POST
    Then status 200
    And match response.status == 'RUNNING'
    
    # Cancel experiment
    Given path '/api/v1/experiments', experimentId, 'cancel'
    When method POST
    Then status 200
    And match response.status == 'CANCELLED'
    
    # Verify experiment is cancelled
    Given path '/api/v1/experiments', experimentId
    When method GET
    Then status 200
    And match response.status == 'CANCELLED'
```

### 4. WebSocket Real-Time Communication Tests

**File**: `features/websocket/messaging.feature`

**Scenarios**:
- Connect to WebSocket Gateway → authenticate
- Join conversation → receive join confirmation
- Send message → EventBridge routing → SQS delivery → recipient receives
- Typing indicators → broadcast to participants
- Read receipts → update unread count
- Disconnect → cleanup resources

**WebSocket Example**:
```gherkin
Feature: Real-Time Messaging

  Background:
    * url baseUrl
    * def wsUrl = karate.properties['wsUrl']

  Scenario: Send message through WebSocket with event-driven delivery
    # Login to get token
    Given path '/api/v1/auth/login'
    And request { email: 'user1@example.com', password: 'Test123!' }
    When method POST
    Then status 200
    * def token = response.token
    * def userId = response.user.id
    
    # Create conversation
    Given path '/api/v1/conversations'
    And request { name: 'Test Chat', participants: ['user2@example.com'] }
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 201
    * def conversationId = response.id
    
    # Connect to WebSocket
    * def ws = karate.webSocket(wsUrl + '/chat?token=' + token)
    
    # Join conversation
    * def joinMessage = { type: 'join', conversationId: '#(conversationId)' }
    * ws.send(joinMessage)
    * def joinResponse = ws.listen(5000)
    * match joinResponse.type == 'joined'
    
    # Send message
    * def message = { type: 'message', conversationId: '#(conversationId)', text: 'Hello World' }
    * ws.send(message)
    
    # Wait for EventBridge processing
    * def helper = Java.type('com.turaf.architecture.helpers.EventHelper')
    * helper.waitForEventProcessing('MessageSent', 10)
    
    # Verify message delivered via API
    Given path '/api/v1/conversations', conversationId, 'messages'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.messages[0].text == 'Hello World'
    And match response.messages[0].senderId == userId
    And match response.messages[0].deliveryStatus == 'DELIVERED'
    
    # Close connection
    * ws.close()
```

### 5. Report Management Tests

**File**: `features/reports/report-management.feature`

**Scenarios**:
- List reports with filters (type, status)
- Create report for experiment
- Get report details
- Download report as PDF
- Delete report

**Report Management Example**:
```gherkin
Feature: Report Management

  Background:
    * url baseUrl
    * def token = <auth token>

  Scenario: List and filter reports
    Given path '/api/v1/reports'
    And param type = 'EXPERIMENT'
    And param status = 'COMPLETED'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[*].type == 'EXPERIMENT'
    And match response[*].status == 'COMPLETED'

  Scenario: Create and download report
    # Create report
    Given path '/api/v1/reports'
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)' }
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 201
    * def reportId = response.id
    
    # Wait for report generation
    * def waitHelper = Java.type('com.turaf.architecture.helpers.WaitHelper')
    * def reportReady = waitHelper.waitForFieldValue('/api/v1/reports/' + reportId, '$.status', 'COMPLETED', 30)
    * match reportReady == true
    
    # Download report
    Given path '/api/v1/reports', reportId, 'download'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match responseHeaders['Content-Type'][0] == 'application/pdf'
    And match responseHeaders['Content-Disposition'][0] contains 'attachment'
```

### 6. Cross-Service Orchestration Tests

**File**: `features/orchestration/dashboard-overview.feature`

**Scenarios**:
- Dashboard overview → parallel calls to multiple services
- Verify data aggregation from Identity, Organization, Experiment, Metrics services
- Handle partial service failures gracefully
- Verify response time < 500ms

**Orchestration Example**:
```gherkin
Feature: Dashboard Overview Orchestration

  Scenario: Dashboard aggregates data from multiple services
    # Login
    Given path '/api/v1/auth/login'
    And request { email: 'test@example.com', password: 'Test123!' }
    When method POST
    Then status 200
    * def token = response.token
    
    # Call dashboard overview (orchestrates multiple services)
    Given path '/api/v1/dashboard/overview'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    
    # Verify user data (from Identity Service)
    And match response.user.email == 'test@example.com'
    And match response.user.name != null
    
    # Verify organizations (from Organization Service)
    And match response.organizations != null
    And match response.organizations[*].id != null
    
    # Verify active experiments (from Experiment Service)
    And match response.activeExperiments != null
    
    # Verify recent metrics (from Metrics Service)
    And match response.recentMetrics != null
    
    # Verify response time
    And match responseTime < 500
```

---

## Waiting Strategies

### WaitHelper.java

```java
package com.turaf.architecture.helpers;

import com.intuit.karate.http.HttpClient;
import com.intuit.karate.http.Response;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.function.Predicate;

public class WaitHelper {
    
    /**
     * Wait for API endpoint to return expected condition
     */
    public static boolean waitForCondition(String endpoint, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    // Make HTTP request to check condition
                    // Return true when condition is met
                    return checkEndpoint(endpoint);
                });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Wait for report generation to complete
     */
    public static boolean waitForReport(String experimentId, int timeoutMs) {
        String endpoint = "/api/v1/experiments/" + experimentId + "/report";
        return waitForCondition(endpoint, timeoutMs / 1000);
    }
    
    /**
     * Poll endpoint until specific field has expected value
     */
    public static boolean waitForFieldValue(String endpoint, String jsonPath, Object expectedValue, int timeoutSeconds) {
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    Response response = makeRequest(endpoint);
                    Object actualValue = response.json().read(jsonPath);
                    return expectedValue.equals(actualValue);
                });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean checkEndpoint(String endpoint) {
        // Implementation to check if endpoint returns success
        return true;
    }
    
    private static Response makeRequest(String endpoint) {
        // Implementation to make HTTP request
        return null;
    }
}
```

### EventHelper.java

```java
package com.turaf.architecture.helpers;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;

public class EventHelper {
    
    private static final SqsClient sqsClient = SqsClient.create();
    
    /**
     * Wait for EventBridge event to be processed
     */
    public static void waitForEventProcessing(String eventType, int timeoutSeconds) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(timeoutSeconds))
            .pollInterval(Duration.ofSeconds(1))
            .until(() -> {
                // Check if event was processed (implementation specific)
                return true;
            });
    }
    
    /**
     * Get SQS message from queue
     */
    public static String getSqsMessage(String queueUrl) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(5)
            .build();
            
        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        List<Message> messages = response.messages();
        
        return messages.isEmpty() ? null : messages.get(0).body();
    }
    
    /**
     * Wait for Lambda execution to complete
     */
    public static void waitForLambdaExecution(String functionName, int timeoutSeconds) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(timeoutSeconds))
            .pollInterval(Duration.ofSeconds(2))
            .until(() -> {
                // Check CloudWatch logs or Lambda invocation status
                return true;
            });
    }
}
```

### AwsHelper.java

```java
package com.turaf.architecture.helpers;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class AwsHelper {
    
    private static final S3Client s3Client = S3Client.create();
    
    /**
     * Verify S3 object exists
     */
    public static boolean verifyS3Object(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
                
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
    
    /**
     * Wait for S3 object to exist
     */
    public static boolean waitForS3Object(String bucket, String key, int timeoutSeconds) {
        try {
            org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(timeoutSeconds))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .until(() -> verifyS3Object(bucket, key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Environment Configuration

### karate-config.js

```javascript
function fn() {
  var env = karate.env || 'local';
  karate.log('karate.env system property was:', env);
  
  var config = {
    baseUrl: 'http://localhost:8080',
    wsUrl: 'ws://localhost:8081',
    waitTimeout: 30000,
    pollInterval: 1000,
    awsRegion: 'us-east-1'
  };
  
  if (env === 'dev') {
    config.baseUrl = 'https://api.dev.turafapp.com';
    config.wsUrl = 'wss://ws.dev.turafapp.com';
    config.awsAccountId = '801651112319';
  } else if (env === 'qa') {
    config.baseUrl = 'https://api.qa.turafapp.com';
    config.wsUrl = 'wss://ws.qa.turafapp.com';
    config.awsAccountId = '965932217544';
  } else if (env === 'prod') {
    config.baseUrl = 'https://api.turafapp.com';
    config.wsUrl = 'wss://ws.turafapp.com';
    config.awsAccountId = '811783768245';
  }
  
  // Load environment-specific properties
  var stream = read('classpath:environments/' + env + '.properties');
  var props = karate.toMap(stream);
  karate.configure('connectTimeout', 10000);
  karate.configure('readTimeout', 30000);
  
  return config;
}
```

### Environment Properties Files

**local.properties**:
```properties
aws.region=us-east-1
aws.endpoint.override=http://localhost:4566
test.data.cleanup=true
parallel.threads=1
```

**dev.properties**:
```properties
aws.region=us-east-1
aws.account.id=801651112319
test.data.cleanup=true
parallel.threads=4
```

**qa.properties**:
```properties
aws.region=us-east-1
aws.account.id=965932217544
test.data.cleanup=true
parallel.threads=4
```

**prod.properties**:
```properties
aws.region=us-east-1
aws.account.id=811783768245
test.data.cleanup=false
parallel.threads=2
smoke.tests.only=true
```

---

## Test Execution

### Local Development

```bash
# Run all tests against local Docker Compose
cd services/architecture-tests
mvn clean test -Dkarate.env=local

# Run specific test suite
mvn test -Dtest=ExperimentWorkflowTestRunner -Dkarate.env=local

# Run specific feature file
mvn test -Dkarate.options="classpath:features/experiments/experiment-lifecycle.feature" -Dkarate.env=local

# Run with parallel execution
mvn test -Dkarate.env=local -Dkarate.threads=4

# Generate HTML report
mvn karate:report
```

### CI/CD Execution

Tests run via GitHub Actions workflow:

```bash
# Triggered manually
gh workflow run architecture-tests.yml -f environment=dev

# Triggered on schedule (every 6 hours)
# Automatic via cron schedule in workflow
```

---

## Test Reports

### Report Generation

Karate automatically generates:
- **HTML Report**: `target/karate-reports/karate-summary.html`
- **JSON Report**: `target/karate-reports/karate-summary.json`
- **JUnit XML**: `target/surefire-reports/*.xml`
- **Timeline**: Visual timeline of test execution
- **Screenshots**: For failed scenarios (if applicable)

### Report Publishing

Reports are published to S3 and served via CloudFront:

**S3 Structure**:
```
turaf-architecture-test-reports-{env}/
└── reports/
    └── {timestamp}/
        ├── karate-summary.html
        ├── karate-summary.json
        ├── timeline.html
        └── screenshots/
```

**Access URL**: `https://reports.{env}.turafapp.com/reports/{timestamp}/karate-summary.html`

**Retention**: 90 days (S3 lifecycle policy)

---

## Best Practices

### 1. Test Data Management

**Use unique identifiers**:
```gherkin
* def timestamp = new Date().getTime()
* def uniqueEmail = 'test+' + timestamp + '@example.com'
```

**Clean up test data**:
```gherkin
* def cleanup = call read('classpath:common/cleanup.feature')
```

### 2. Reusable Functions

Create reusable JavaScript functions:
```javascript
// wait-helpers.js
function waitForReport(experimentId, timeout) {
  var helper = Java.type('com.turaf.architecture.helpers.WaitHelper');
  return helper.waitForReport(experimentId, timeout);
}
```

### 3. Error Handling

```gherkin
* def response = call read('api-call.feature')
* if (response.status != 200) karate.fail('API call failed: ' + response.status)
```

### 4. Parallel Execution

Configure in `pom.xml`:
```xml
<systemProperties>
  <karate.threads>4</karate.threads>
</systemProperties>
```

### 5. Test Isolation

Each test should be independent:
- Create own test data
- Clean up after execution
- Don't depend on execution order

---

## Troubleshooting

### Common Issues

**Issue**: Tests fail in CI but pass locally
**Solution**: Check environment configuration, ensure AWS credentials are set

**Issue**: Timeouts waiting for async processes
**Solution**: Increase timeout values, check CloudWatch logs for Lambda errors

**Issue**: WebSocket connection failures
**Solution**: Verify WebSocket Gateway is deployed, check authentication token

**Issue**: S3 report upload fails
**Solution**: Verify IAM permissions for GitHubActionsDeploymentRole

---

## References

- [Karate Framework Documentation](https://github.com/karatelabs/karate)
- [PROJECT.md Section 23b](../PROJECT.md#23b-architecture-testing-strategy)
- [Testing Strategy](testing-strategy.md)
- [BFF API Specification](bff-api.md)
- [WebSocket Gateway Specification](ws-gateway.md)
