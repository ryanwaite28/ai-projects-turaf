# Testing Strategy Specification

**Source**: PROJECT.md (Section 23a)

This specification defines the comprehensive testing strategy for the Turaf platform, including unit testing, integration testing with Testcontainers, and the hybrid approach for AWS service testing.

---

## Overview

The Turaf platform implements a multi-layered testing strategy that ensures code quality, reliability, and maintainability across all services. The strategy balances test coverage with execution speed and infrastructure costs.

**Key Principles**:
- Follow the testing pyramid (70% unit, 25% integration, 5% e2e)
- Use Testcontainers for portable, reproducible integration tests
- Hybrid approach for AWS services (LocalStack + mocks)
- Zero AWS costs for integration tests
- Same tests run locally and in CI/CD

---

## Testing Pyramid

### Unit Tests (70% of tests)

**Purpose**: Test domain logic, services, and utilities in isolation.

**Characteristics**:
- Fast execution (< 1 second per test)
- No external dependencies
- Mock all collaborators
- Focus on business logic

**Tools**:
- JUnit 5 for test framework
- Mockito for mocking
- AssertJ for fluent assertions
- MockMvc for controller tests

**Coverage Target**: 80%+ overall, 90%+ for domain layer

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {
    
    @Mock
    private ExperimentRepository experimentRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private ExperimentService experimentService;
    
    @Test
    void startExperiment_shouldPublishEvent() {
        // Given
        Experiment experiment = Experiment.create("exp-1", "hyp-1");
        when(experimentRepository.findById("exp-1")).thenReturn(Optional.of(experiment));
        
        // When
        experimentService.startExperiment("exp-1");
        
        // Then
        verify(eventPublisher).publish(any(ExperimentStartedEvent.class));
    }
}
```

---

### Integration Tests (25% of tests)

**Purpose**: Validate interactions between components, databases, and AWS services.

**Characteristics**:
- Moderate execution time (1-5 seconds per test)
- Use real databases and message queues (via containers)
- Test actual integrations
- Validate data persistence and messaging

**Tools**:
- Testcontainers for container management
- LocalStack for AWS services (free tier)
- @MockBean for paid AWS services
- @SpringBootTest for full context

**Coverage Target**: 70%+ for infrastructure layer

**Example**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ExperimentRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("turaf_test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private ExperimentRepository experimentRepository;
    
    @Test
    void save_shouldPersistExperiment() {
        // Given
        Experiment experiment = Experiment.create("exp-1", "hyp-1");
        
        // When
        Experiment saved = experimentRepository.save(experiment);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        Optional<Experiment> found = experimentRepository.findById(saved.getId());
        assertThat(found).isPresent();
    }
}
```

---

### End-to-End Tests (5% of tests)

**Purpose**: Verify complete workflows through the UI and APIs.

**Characteristics**:
- Slow execution (10-30 seconds per test)
- Test full user journeys
- Run against deployed environments
- Use Playwright for UI automation

**Tools**:
- Playwright for browser automation
- REST Assured for API testing
- Test data builders

**Coverage Target**: Critical user flows only

**Example**:
```typescript
test('complete experiment workflow', async ({ page }) => {
  // Login
  await page.goto('https://app.qa.turafapp.com');
  await page.fill('[name="email"]', 'test@example.com');
  await page.fill('[name="password"]', 'Test123!');
  await page.click('button[type="submit"]');
  
  // Create experiment
  await page.click('text=New Experiment');
  await page.fill('[name="name"]', 'Test Experiment');
  await page.click('button:has-text("Create")');
  
  // Verify experiment created
  await expect(page.locator('text=Test Experiment')).toBeVisible();
});
```

---

## Testcontainers Configuration

### Overview

Testcontainers provides lightweight, throwaway instances of databases, message queues, and other services for integration testing.

**Benefits**:
- **Portable**: Same tests run on any machine with Docker
- **Isolated**: Each test run gets fresh containers
- **Automatic**: Containers start/stop with test execution
- **Realistic**: Test against actual services, not mocks

### Required Dependencies

**Maven (pom.xml)**:
```xml
<dependencies>
    <!-- Testcontainers Core -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 Integration -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- PostgreSQL Container -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- LocalStack Container -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>localstack</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### PostgreSQL Container Setup

**Test Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("turaf_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### LocalStack Container Setup

**Test Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MessagingIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    )
    .withServices(
        LocalStackContainer.Service.SQS,
        LocalStackContainer.Service.S3,
        LocalStackContainer.Service.DYNAMODB
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.sqs.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("aws.s3.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("aws.region", () -> localstack.getRegion());
        registry.add("aws.accessKeyId", () -> localstack.getAccessKey());
        registry.add("aws.secretAccessKey", () -> localstack.getSecretKey());
    }
}
```

---

## AWS Service Testing Strategy

### Hybrid Approach (Recommended)

The platform uses a **hybrid approach** to balance test coverage with infrastructure costs:

1. **LocalStack** for free-tier AWS services
2. **@MockBean** for paid AWS services
3. **Custom mocks** when needed for specific behavior

This approach provides:
- ✅ Zero AWS costs for integration tests
- ✅ Realistic testing for core services
- ✅ Business logic validation for all services
- ✅ Fast execution in CI/CD pipelines

---

### Option 1: LocalStack (Free-Tier Services)

**When to Use**:
- Service is in LocalStack free tier
- Need to test actual AWS SDK behavior
- Testing message queuing, storage, or database operations

**Free-Tier Services**:
- **SQS** (Simple Queue Service) - Message queuing
- **S3** (Object Storage) - File storage
- **DynamoDB** (NoSQL Database) - Key-value storage
- **Lambda** (Serverless Functions) - Function execution
- **SNS** (Simple Notification Service) - Pub/sub messaging

**Example**:
```java
@SpringBootTest
@Testcontainers
class SqsIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.SQS);
    
    @Autowired
    private SqsAsyncClient sqsClient;
    
    @Test
    void sendMessage_shouldDeliverToQueue() {
        // Create queue
        String queueUrl = sqsClient.createQueue(r -> r.queueName("test-queue"))
            .join().queueUrl();
        
        // Send message
        sqsClient.sendMessage(r -> r
            .queueUrl(queueUrl)
            .messageBody("Test message")
        ).join();
        
        // Receive message
        ReceiveMessageResponse response = sqsClient.receiveMessage(r -> r
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
        ).join();
        
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).body()).isEqualTo("Test message");
    }
}
```

---

### Option 2: @MockBean (Paid Services)

**When to Use**:
- Service not in LocalStack free tier
- Focus is on business logic, not AWS SDK integration
- Want to avoid LocalStack Pro costs

**Paid Services**:
- **EventBridge** (Event Bus) - Not in free tier
- **CloudWatch Logs/Metrics** - Limited in free tier
- **Step Functions** - Not in free tier
- **AppSync** - Not in free tier
- **Kinesis Data Streams** - Not in free tier

**Example**:
```java
@SpringBootTest
@Testcontainers
class EventBridgeIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.SQS);
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Test
    void publishEvent_shouldCallEventBridge() {
        // Given
        ExperimentCompletedEvent event = new ExperimentCompletedEvent("exp-1");
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                PutEventsResponse.builder()
                    .failedEntryCount(0)
                    .build()
            ));
        
        // When
        eventPublisher.publish(event);
        
        // Then
        verify(eventBridgeClient).putEvents(argThat(request ->
            request.entries().size() == 1 &&
            request.entries().get(0).detailType().equals("ExperimentCompleted")
        ));
    }
}
```

---

### Option 3: Custom Mock Implementations

**When to Use**:
- Need more realistic behavior than @MockBean
- Want to simulate specific AWS service responses
- Testing error handling and edge cases
- Multiple tests need shared mock behavior

**Example**:
```java
public class InMemoryEventBridge implements EventPublisher {
    
    private final List<DomainEvent> publishedEvents = new ArrayList<>();
    
    @Override
    public void publish(DomainEvent event) {
        publishedEvents.add(event);
    }
    
    public List<DomainEvent> getPublishedEvents() {
        return List.copyOf(publishedEvents);
    }
    
    public void clear() {
        publishedEvents.clear();
    }
}

@TestConfiguration
class TestConfig {
    
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return new InMemoryEventBridge();
    }
}

@SpringBootTest
@Import(TestConfig.class)
class EventPublishingTest {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private ExperimentService experimentService;
    
    @Test
    void completeExperiment_shouldPublishEvent() {
        // When
        experimentService.completeExperiment("exp-1");
        
        // Then
        InMemoryEventBridge bridge = (InMemoryEventBridge) eventPublisher;
        assertThat(bridge.getPublishedEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(ExperimentCompletedEvent.class);
    }
}
```

---

## Service-Specific Testing Guidelines

### Communications Service

**LocalStack Services**:
- SQS for message queuing

**Mocked Services**:
- EventBridge for MessageDelivered events

**Example**:
```java
@SpringBootTest
@Testcontainers
class CommunicationsServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.SQS);
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    @Test
    void sendMessage_shouldQueueAndPublishEvent() {
        // Test SQS message queuing with LocalStack
        // Verify EventBridge event publishing with mock
    }
}
```

---

### Metrics Service

**LocalStack Services**:
- DynamoDB for metrics storage

**Mocked Services**:
- CloudWatch for metrics aggregation

**Example**:
```java
@SpringBootTest
@Testcontainers
class MetricsServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.DYNAMODB);
    
    @MockBean
    private CloudWatchClient cloudWatchClient;
    
    @Test
    void recordMetric_shouldStoreInDynamoDB() {
        // Test DynamoDB storage with LocalStack
        // Verify CloudWatch publishing with mock
    }
}
```

---

### Reporting Service

**LocalStack Services**:
- S3 for report storage

**Mocked Services**:
- Step Functions for report generation workflow

**Example**:
```java
@SpringBootTest
@Testcontainers
class ReportingServiceIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.S3);
    
    @MockBean
    private SfnClient stepFunctionsClient;
    
    @Test
    void generateReport_shouldStoreInS3() {
        // Test S3 storage with LocalStack
        // Verify Step Functions execution with mock
    }
}
```

---

## Test Data Management

### Test Data Builders

Use builder pattern for creating test data:

```java
public class ExperimentTestBuilder {
    
    private String id = UUID.randomUUID().toString();
    private String hypothesisId = "hyp-1";
    private String name = "Test Experiment";
    private ExperimentStatus status = ExperimentStatus.DRAFT;
    
    public static ExperimentTestBuilder anExperiment() {
        return new ExperimentTestBuilder();
    }
    
    public ExperimentTestBuilder withId(String id) {
        this.id = id;
        return this;
    }
    
    public ExperimentTestBuilder withStatus(ExperimentStatus status) {
        this.status = status;
        return this;
    }
    
    public Experiment build() {
        Experiment experiment = Experiment.create(id, hypothesisId);
        experiment.setName(name);
        experiment.setStatus(status);
        return experiment;
    }
}

// Usage
@Test
void test() {
    Experiment experiment = anExperiment()
        .withId("exp-1")
        .withStatus(ExperimentStatus.RUNNING)
        .build();
}
```

---

### Test Data Isolation

**Database Tests**:
- Use `@Transactional` for automatic rollback
- Or use `@DirtiesContext` to reset Spring context
- Each test gets fresh database state

**Messaging Tests**:
- Create unique queue names per test
- Clean up queues in `@AfterEach`
- Use test-specific message attributes

---

## Maven Test Configuration

### Test Profiles

**pom.xml**:
```xml
<profiles>
    <!-- Unit tests only -->
    <profile>
        <id>unit-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>**/*IntegrationTest.java</exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
    
    <!-- Integration tests only -->
    <profile>
        <id>integration-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*IntegrationTest.java</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### Test Execution Commands

```bash
# Run unit tests only (fast)
mvn test -P unit-tests

# Run integration tests only (requires Docker)
mvn verify -P integration-tests

# Run all tests
mvn verify

# Run specific test class
mvn test -Dtest=ExperimentServiceTest

# Run tests with coverage
mvn test jacoco:report
```

---

## GitHub Actions Integration

### CI Pipeline Configuration

**Integration Test Job**:
```yaml
integration-tests:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    # Docker is pre-installed on ubuntu-latest
    
    - name: Run integration tests
      run: |
        cd services
        mvn verify -P integration-tests
    
    - name: Publish test results
      uses: dorny/test-reporter@v1
      if: always()
      with:
        name: Integration Test Results
        path: services/*/target/surefire-reports/*.xml
        reporter: java-junit
    
    - name: Upload coverage
      uses: codecov/codecov-action@v4
      with:
        files: services/*/target/site/jacoco/jacoco.xml
        flags: integration
```

### Test Result Reporting

GitHub Actions automatically:
- Displays test results in PR checks
- Shows test trends over time
- Highlights failing tests
- Provides downloadable test artifacts

---

## Best Practices

### Writing Integration Tests

1. **Use descriptive test names**: `shouldPublishEventWhenExperimentCompletes`
2. **Follow AAA pattern**: Arrange, Act, Assert
3. **One assertion per test**: Focus on single behavior
4. **Clean up resources**: Use `@AfterEach` for cleanup
5. **Avoid test interdependencies**: Tests should run in any order

### Container Management

1. **Reuse containers**: Use `static` containers shared across tests
2. **Minimize container restarts**: Group related tests in same class
3. **Configure timeouts**: Set reasonable startup timeouts
4. **Use specific versions**: Pin container image versions

### Performance Optimization

1. **Parallel execution**: Run tests in parallel where possible
2. **Selective testing**: Run only affected tests in PR builds
3. **Container caching**: Cache Docker images in CI/CD
4. **Fast feedback**: Run unit tests before integration tests

---

## Troubleshooting

### Common Issues

**Docker not running**:
```
Error: Could not find a valid Docker environment
Solution: Start Docker Desktop or Docker Engine
```

**Port conflicts**:
```
Error: Port 5432 is already in use
Solution: Use dynamic port allocation with Testcontainers
```

**Container startup timeout**:
```
Error: Container did not start within 60 seconds
Solution: Increase timeout or check Docker resources
```

**LocalStack connection errors**:
```
Error: Unable to connect to LocalStack
Solution: Verify LocalStack container is running and accessible
```

---

## References

- **Testcontainers Documentation**: https://www.testcontainers.org/
- **LocalStack Documentation**: https://docs.localstack.cloud/
- **JUnit 5 User Guide**: https://junit.org/junit5/docs/current/user-guide/
- **Spring Boot Testing**: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
- **PROJECT.md**: Section 23a - Testing Strategy
