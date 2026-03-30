# Architecture Tests Implementation Plan

**Created**: 2026-03-29  
**Status**: Active  
**Related Documents**: 
- [Testing Strategy](../../specs/testing-strategy.md)
- [BFF API Spec](../../specs/bff-api.md)
- [Architecture Spec](../../specs/architecture.md)
- [PROJECT.md](../../PROJECT.md)

This plan covers the implementation of full-system architecture tests using the Karate framework to validate the entire platform from entry points (BFF API and WebSocket Gateway) through all event-driven processes.

---

## Overview

Architecture testing validates the complete system integration by testing from the entry points (BFF API, WebSocket Gateway) through all downstream services, event-driven workflows (EventBridge, SQS, Lambdas), and data persistence. Unlike component tests that mock dependencies, architecture tests run against a fully deployed system (local Docker Compose or deployed AWS environments).

**Key Differences from Existing Testing**:
- **Unit Tests (70%)**: Test individual components in isolation
- **Integration Tests (25%)**: Test service components with Testcontainers/LocalStack
- **Component Tests (BFF/Gateway)**: Test entry points with mocked downstream dependencies
- **Architecture Tests (5%)**: Test complete system end-to-end from entry points through all services

---

## Objectives

1. Create dedicated `architecture-tests` service with Karate framework
2. Implement full workflow tests from BFF API through microservices and event processing
3. Support testing against local Docker Compose and deployed environments (DEV/QA/PROD)
4. Create CI/CD pipeline for running architecture tests independently
5. Generate and publish HTML test reports to S3/CloudFront
6. Implement "waiting" strategies for asynchronous event-driven processes
7. Update PROJECT.md and create comprehensive specification and task breakdown

---

## Architecture Tests Scope

### Test Categories

**1. Authentication & Authorization Flows**
- User registration → token generation → profile access
- Login → JWT validation → protected resource access
- Token refresh → new token → continued access
- Logout → token invalidation → access denied

**2. Organization Management Workflows**
- Create organization → verify persistence → list organizations
- Add members → EventBridge notification → member receives invite
- Update organization → verify changes → audit log created

**3. Experiment Lifecycle (Event-Driven)**
- Create problem → ProblemCreated event → notification sent
- Create hypothesis → HypothesisCreated event → linked to problem
- Start experiment → ExperimentStarted event → metrics collection enabled
- Record metrics → MetricRecorded events → aggregation processing
- Complete experiment → ExperimentCompleted event → report generation triggered
- Wait for report generation (async Lambda) → verify report in S3
- Retrieve report → verify content and metrics

**4. Real-Time Communication (WebSocket)**
- Connect to WebSocket Gateway → authenticate → join conversation
- Send message → EventBridge routing → SQS delivery → recipient receives
- Typing indicators → real-time broadcast → all participants notified
- Read receipts → state update → unread count changes

**5. Cross-Service Orchestration**
- Dashboard overview → parallel service calls → aggregated response
- Experiment full details → experiment + metrics + report status
- Organization summary → org + members + experiment counts

**6. Event-Driven Workflows with Waiting**
- Trigger event → wait for EventBridge processing → verify SQS message
- Lambda invocation → wait for completion → verify side effects
- Async report generation → poll for completion → verify S3 object
- Notification delivery → wait for processing → verify delivery status

---

## Implementation Structure

### Directory Structure

```
services/
└── architecture-tests/
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
    │   │   │       │   └── EventHelper.java
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
    │   ├── s3.tf              # Test reports bucket
    │   ├── cloudfront.tf      # CDN for reports
    │   └── iam.tf             # Permissions for CI/CD
    ├── docker-compose.yml     # Local test environment
    ├── pom.xml
    ├── README.md
    └── .gitignore
```

---

## Technology Stack

### Core Dependencies

**Karate Framework**:
- `com.intuit.karate:karate-junit5` - JUnit 5 integration
- `com.intuit.karate:karate-apache` - HTTP client
- `com.intuit.karate:karate-core` - Core framework

**AWS SDK (for validation)**:
- `software.amazon.awssdk:s3` - Verify S3 objects
- `software.amazon.awssdk:sqs` - Check SQS messages
- `software.amazon.awssdk:eventbridge` - Validate events

**WebSocket Support**:
- `com.intuit.karate:karate-websocket` - WebSocket testing
- Or custom WebSocket client for advanced scenarios

**Reporting**:
- `com.intuit.karate:karate-html-report` - HTML report generation
- Cucumber JSON reports for CI/CD integration

**Test Utilities**:
- `org.awaitility:awaitility` - Polling and waiting utilities
- `com.fasterxml.jackson.core:jackson-databind` - JSON processing

---

## Key Features

### 1. Environment Configuration

**karate-config.js**:
```javascript
function fn() {
  var env = karate.env || 'local';
  var config = {
    baseUrl: 'http://localhost:8080',
    wsUrl: 'ws://localhost:8081',
    waitTimeout: 30000,
    pollInterval: 1000
  };
  
  if (env === 'dev') {
    config.baseUrl = 'https://api.dev.turafapp.com';
    config.wsUrl = 'wss://ws.dev.turafapp.com';
  } else if (env === 'qa') {
    config.baseUrl = 'https://api.qa.turafapp.com';
    config.wsUrl = 'wss://ws.qa.turafapp.com';
  } else if (env === 'prod') {
    config.baseUrl = 'https://api.turafapp.com';
    config.wsUrl = 'wss://ws.turafapp.com';
  }
  
  return config;
}
```

### 2. Waiting Strategies for Async Processes

**WaitHelper.java**:
```java
public class WaitHelper {
    
    // Poll API endpoint until condition met
    public static void waitForCondition(
        String endpoint, 
        Predicate<Response> condition, 
        int timeoutSeconds
    ) {
        await()
            .atMost(timeoutSeconds, SECONDS)
            .pollInterval(1, SECONDS)
            .until(() -> {
                Response response = given().get(endpoint);
                return condition.test(response);
            });
    }
    
    // Wait for S3 object to exist
    public static void waitForS3Object(String bucket, String key, int timeoutSeconds) {
        // Implementation
    }
    
    // Wait for SQS message
    public static void waitForSqsMessage(String queueUrl, int timeoutSeconds) {
        // Implementation
    }
}
```

**Karate Feature Example**:
```gherkin
Feature: Experiment Report Generation

  Background:
    * url baseUrl
    * def waitForReport = read('classpath:helpers/wait-for-report.js')

  Scenario: Complete experiment triggers async report generation
    Given path '/api/v1/experiments'
    And request { name: 'Test Experiment', hypothesisId: '#(hypothesisId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    # Start experiment
    Given path '/api/v1/experiments', experimentId, 'start'
    When method POST
    Then status 200
    
    # Record metrics
    Given path '/api/v1/metrics'
    And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.25 }
    When method POST
    Then status 201
    
    # Complete experiment (triggers async report generation)
    Given path '/api/v1/experiments', experimentId, 'complete'
    And request { resultSummary: 'Test completed successfully' }
    When method POST
    Then status 200
    
    # Wait for report generation (async Lambda process)
    * def reportReady = waitForReport(experimentId, 30000)
    * match reportReady == true
    
    # Verify report exists
    Given path '/api/v1/experiments', experimentId, 'report'
    When method GET
    Then status 200
    And match response.reportUrl != null
    And match response.generated == true
```

### 3. Event-Driven Workflow Testing

**Example: Test EventBridge → SQS → Lambda flow**:
```gherkin
Feature: Message Delivery Workflow

  Scenario: Send message triggers event-driven delivery
    # Send message via WebSocket
    * def ws = karate.webSocket(wsUrl + '/chat')
    * ws.send({ type: 'message', conversationId: '#(convId)', text: 'Hello' })
    
    # Wait for EventBridge processing
    * def helper = Java.type('com.turaf.architecture.helpers.EventHelper')
    * helper.waitForEventProcessing('MessageSent', 10)
    
    # Verify SQS message queued
    * def sqsMessage = helper.getSqsMessage(queueUrl)
    * match sqsMessage.body contains 'Hello'
    
    # Wait for Lambda processing
    * helper.waitForLambdaExecution('message-delivery-lambda', 15)
    
    # Verify message delivered to recipient
    Given path '/api/v1/conversations', convId, 'messages'
    When method GET
    Then status 200
    And match response.messages[0].text == 'Hello'
    And match response.messages[0].deliveryStatus == 'DELIVERED'
```

---

## CI/CD Integration

### GitHub Actions Workflow

**`.github/workflows/architecture-tests.yml`**:
```yaml
name: Architecture Tests

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - dev
          - qa
          - prod
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours

jobs:
  architecture-tests:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment || 'dev' }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
      
      - name: Run architecture tests
        run: |
          cd services/architecture-tests
          mvn clean test -Dkarate.env=${{ github.event.inputs.environment || 'dev' }}
      
      - name: Generate HTML report
        if: always()
        run: |
          cd services/architecture-tests
          mvn karate:report
      
      - name: Upload report to S3
        if: always()
        run: |
          TIMESTAMP=$(date +%Y%m%d-%H%M%S)
          ENV=${{ github.event.inputs.environment || 'dev' }}
          aws s3 cp target/karate-reports/ \
            s3://turaf-architecture-test-reports-${ENV}/reports/${TIMESTAMP}/ \
            --recursive
          
          echo "REPORT_TIMESTAMP=${TIMESTAMP}" >> $GITHUB_ENV
          echo "REPORT_ENV=${ENV}" >> $GITHUB_ENV
          echo "Report URL: https://reports.${ENV}.turafapp.com/reports/${TIMESTAMP}/karate-summary.html"
      
      - name: Invalidate CloudFront cache
        if: always()
        run: |
          # Get CloudFront distribution ID from Terraform outputs
          DISTRIBUTION_ID=$(aws cloudfront list-distributions \
            --query "DistributionList.Items[?Aliases.Items[?contains(@, 'reports.${{ env.REPORT_ENV }}.turafapp.com')]].Id | [0]" \
            --output text)
          
          if [ -n "$DISTRIBUTION_ID" ]; then
            echo "Invalidating CloudFront distribution: $DISTRIBUTION_ID"
            aws cloudfront create-invalidation \
              --distribution-id $DISTRIBUTION_ID \
              --paths "/reports/${{ env.REPORT_TIMESTAMP }}/*"
            echo "✅ CloudFront cache invalidated"
          else
            echo "⚠️ CloudFront distribution not found, skipping invalidation"
          fi
      
      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Architecture Test Results
          path: services/architecture-tests/target/karate-reports/*.json
          reporter: java-junit
      
      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const report = JSON.parse(fs.readFileSync('services/architecture-tests/target/karate-reports/karate-summary.json'));
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## Architecture Test Results\n\n` +
                    `✅ Passed: ${report.scenariosPassed}\n` +
                    `❌ Failed: ${report.scenariosFailed}\n` +
                    `⏱️ Duration: ${report.elapsedTime}ms\n\n` +
                    `[View Full Report](${process.env.REPORT_URL})`
            });
```

---

## Infrastructure for Test Reports

### IAM Role Updates

**Update Existing GitHubActionsDeploymentRole**:

The architecture tests will use the existing `GitHubActionsDeploymentRole` in each environment. This role needs additional permissions for:
1. Uploading test reports to S3 bucket
2. Invalidating CloudFront cache after report uploads

**Required Permission Updates**:

Update `/infrastructure/iam-policies/github-actions-permissions-policy.json` to add:

```json
{
  "Sid": "ArchitectureTestReports",
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:PutObjectAcl",
    "s3:GetObject",
    "s3:ListBucket"
  ],
  "Resource": [
    "arn:aws:s3:::turaf-architecture-test-reports-*",
    "arn:aws:s3:::turaf-architecture-test-reports-*/*"
  ]
},
{
  "Sid": "CloudFrontInvalidation",
  "Effect": "Allow",
  "Action": [
    "cloudfront:CreateInvalidation",
    "cloudfront:GetInvalidation",
    "cloudfront:ListInvalidations"
  ],
  "Resource": "arn:aws:cloudfront::${ACCOUNT_ID}:distribution/*"
}
```

**Deployment Process**:
1. Update the permissions policy JSON file
2. Apply to all environment accounts (dev, qa, prod)
3. Verify permissions with test workflow

### Terraform Configuration

**terraform/main.tf**:
```hcl
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "turaf-terraform-state"
    key    = "architecture-tests/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Turaf"
      Component   = "ArchitectureTests"
      ManagedBy   = "Terraform"
      Environment = var.environment
    }
  }
}
```

**terraform/s3.tf**:
```hcl
resource "aws_s3_bucket" "test_reports" {
  bucket = "turaf-architecture-test-reports-${var.environment}"
}

resource "aws_s3_bucket_versioning" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  rule {
    id     = "delete-old-reports"
    status = "Enabled"
    
    expiration {
      days = 90  # Keep reports for 90 days
    }
    
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_s3_bucket_public_access_block" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

**terraform/cloudfront.tf**:
```hcl
resource "aws_cloudfront_origin_access_identity" "test_reports" {
  comment = "OAI for architecture test reports"
}

resource "aws_cloudfront_distribution" "test_reports" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "Architecture test reports CDN"
  default_root_object = "index.html"
  
  aliases = ["reports.${var.environment}.turafapp.com"]
  
  origin {
    domain_name = aws_s3_bucket.test_reports.bucket_regional_domain_name
    origin_id   = "S3-test-reports"
    
    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.test_reports.cloudfront_access_identity_path
    }
  }
  
  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-test-reports"
    
    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
    
    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
    compress               = true
  }
  
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  
  viewer_certificate {
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
  
  tags = {
    Name = "turaf-architecture-test-reports-${var.environment}"
  }
}

resource "aws_s3_bucket_policy" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_cloudfront_origin_access_identity.test_reports.iam_arn
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.test_reports.arn}/*"
      }
    ]
  })
}

# Output CloudFront distribution ID for cache invalidation
output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID for cache invalidation"
  value       = aws_cloudfront_distribution.test_reports.id
}

output "cloudfront_domain_name" {
  description = "CloudFront domain name for accessing reports"
  value       = aws_cloudfront_distribution.test_reports.domain_name
}

output "reports_url" {
  description = "Full URL for accessing test reports"
  value       = "https://reports.${var.environment}.turafapp.com"
}
```

---

## Component Tests (BFF & WebSocket Gateway)

### BFF API Component Tests

**Location**: `services/bff-api/src/test/java/com/turaf/bff/component/`

**Characteristics**:
- Mock downstream microservices using WireMock
- Mock AWS services (EventBridge, SQS) using Testcontainers/LocalStack
- Test BFF orchestration logic, authentication, error handling
- Fast execution (< 5 seconds per test)

**Example**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class BffOrchestrationComponentTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void dashboardOverview_shouldAggregateFromMultipleServices() {
        // Mock Identity Service
        stubFor(get("/identity/auth/me")
            .willReturn(okJson("{\"id\":\"user-1\",\"name\":\"Test User\"}")));
        
        // Mock Organization Service
        stubFor(get("/organization/organizations")
            .willReturn(okJson("[{\"id\":\"org-1\",\"name\":\"Test Org\"}]")));
        
        // Mock Experiment Service
        stubFor(get("/experiment/experiments?status=RUNNING")
            .willReturn(okJson("[{\"id\":\"exp-1\",\"name\":\"Test Exp\"}]")));
        
        // Call BFF orchestration endpoint
        ResponseEntity<DashboardDto> response = restTemplate
            .exchange("/api/v1/dashboard/overview", HttpMethod.GET, 
                      createAuthRequest(), DashboardDto.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUser().getName()).isEqualTo("Test User");
        assertThat(response.getBody().getOrganizations()).hasSize(1);
        assertThat(response.getBody().getActiveExperiments()).hasSize(1);
    }
}
```

### WebSocket Gateway Component Tests

**Location**: `services/ws-gateway/src/test/java/com/turaf/ws/component/`

**Characteristics**:
- Mock Communications Service using WireMock
- Mock EventBridge for message publishing
- Test WebSocket connection, authentication, message routing
- Use WebSocket test clients

---

## Documentation Updates

### 1. PROJECT.md Updates

**Add new section: "23b. Architecture Testing Strategy"**:
- Overview of architecture testing approach
- Karate framework rationale
- Test execution environments
- Relationship to other testing layers
- CI/CD integration for architecture tests

**Update Section 23a (Testing Strategy)**:
- Add architecture tests as 5% of testing pyramid
- Clarify distinction between integration, component, and architecture tests
- Reference new architecture-tests service

### 2. Create Specification

**File**: `specs/architecture-testing.md`

**Contents**:
- Overview and objectives
- Architecture test scope and categories
- Karate framework configuration
- Waiting strategies for async processes
- Environment configuration (local, dev, qa, prod)
- Test execution and reporting
- CI/CD pipeline integration
- Relationship to component tests
- Best practices and patterns

### 3. Create Task Breakdown

**Directory**: `tasks/architecture-tests/`

**Tasks**:
1. `001-setup-project-structure.md` - Maven project, dependencies, directory structure
2. `002-configure-karate-framework.md` - karate-config.js, environment configs
3. `003-implement-wait-helpers.md` - Java helpers for async waiting
4. `004-authentication-tests.md` - Login, registration, token flows
5. `005-organization-tests.md` - Organization CRUD and workflows
6. `006-experiment-lifecycle-tests.md` - Full experiment workflow with events
7. `007-websocket-tests.md` - Real-time messaging tests
8. `008-orchestration-tests.md` - Cross-service aggregation tests
9. `009-event-driven-workflow-tests.md` - EventBridge, SQS, Lambda flows
10. `010-setup-test-infrastructure.md` - Terraform for S3 and CloudFront
11. `011-create-cicd-pipeline.md` - GitHub Actions workflow
12. `012-docker-compose-setup.md` - Local test environment

---

## Implementation Phases

### Phase 1: Foundation (Week 1)
- [ ] Create architecture-tests service structure
- [ ] Configure Maven with Karate dependencies
- [ ] Implement environment configuration (local, dev, qa, prod)
- [ ] Create wait helpers for async processes
- [ ] Set up basic test runners

### Phase 2: Core Test Scenarios (Week 2)
- [ ] Implement authentication flow tests
- [ ] Implement organization management tests
- [ ] Implement basic experiment lifecycle tests
- [ ] Create common test utilities and helpers

### Phase 3: Event-Driven Workflows (Week 3)
- [ ] Implement experiment with metrics and report generation
- [ ] Add EventBridge → SQS → Lambda validation
- [ ] Implement WebSocket messaging tests
- [ ] Add orchestration/aggregation tests

### Phase 4: Infrastructure & CI/CD (Week 4)
- [ ] Update GitHubActionsDeploymentRole permissions policy
  - [ ] Add S3 permissions for test reports bucket
  - [ ] Add CloudFront invalidation permissions
  - [ ] Apply to DEV account
  - [ ] Apply to QA account
  - [ ] Apply to PROD account
- [ ] Create Terraform for S3 bucket and CloudFront
- [ ] Deploy test report infrastructure to DEV
- [ ] Create GitHub Actions workflow
- [ ] Integrate with CI/CD pipeline
- [ ] Test against deployed DEV environment

### Phase 5: Component Tests (Week 5)
- [ ] Add BFF API component tests with WireMock
- [ ] Add WebSocket Gateway component tests
- [ ] Ensure clear separation from architecture tests
- [ ] Document component vs architecture testing

### Phase 6: Documentation & Refinement (Week 6)
- [ ] Update PROJECT.md with architecture testing section
- [ ] Create architecture-testing.md specification
- [ ] Create task breakdown in tasks/architecture-tests/
- [ ] Write comprehensive README for architecture-tests service
- [ ] Create runbook for executing tests
- [ ] Update infrastructure/github-oidc-roles.md with new permissions
- [ ] Deploy to QA and validate

---

## Success Criteria

- [ ] Architecture tests service successfully created and configured
- [ ] Tests run against local Docker Compose environment
- [ ] Tests run against deployed DEV/QA environments via CI/CD
- [ ] Async event-driven workflows tested with proper waiting strategies
- [ ] Test reports generated and published to S3/CloudFront
- [ ] Component tests implemented for BFF and WebSocket Gateway
- [ ] Clear documentation distinguishing architecture vs component tests
- [ ] PROJECT.md updated with architecture testing strategy
- [ ] Specification and task breakdown created
- [ ] CI/CD pipeline runs independently without blocking deployments

---

## References

- [Testing Strategy](../../specs/testing-strategy.md)
- [BFF API Specification](../../specs/bff-api.md)
- [Architecture Specification](../../specs/architecture.md)
- [Karate Framework Documentation](https://github.com/karatelabs/karate)
- [PROJECT.md Section 23a](../../PROJECT.md#23a-testing-strategy)
