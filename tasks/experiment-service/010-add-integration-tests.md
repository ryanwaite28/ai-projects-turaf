# Task: Add Integration Tests

**Service**: Experiment Service  
**Phase**: 4  
**Estimated Time**: 3 hours  

## Objective

Create integration tests that verify the complete experiment management flow from API endpoints through to database.

## Prerequisites

- [x] All experiment-service implementation tasks completed
- [x] Task 009: Unit tests added

## Scope

**Test Files to Create**:
- `ExperimentControllerIntegrationTest.java`
- `ExperimentFlowIntegrationTest.java`

## Testing Strategy

**Follow the hybrid AWS approach** (PROJECT.md Section 23a, specs/testing-strategy.md):

- **Use Testcontainers** for:
  - PostgreSQL database integration tests
  - State machine transitions
  - Repository queries
  
- **Use @MockBean** for:
  - EventBridge publisher (not in LocalStack free tier)
  - Verify event publishing with mocks

**Example Configuration**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ExperimentServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("experiment_test");
    
    @MockBean
    private EventBridgeClient eventBridgeClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## Implementation Details

Test complete flow:
1. Create problem
2. Create hypothesis for problem
3. Create experiment for hypothesis
4. Start experiment (verify ExperimentStarted event)
5. Complete experiment (verify ExperimentCompleted event)
6. Verify state transitions
7. Verify events published using @MockBean verification

## Acceptance Criteria

- [x] All API endpoints tested end-to-end
- [x] Complete experiment flow tested
- [x] State transitions verified
- [x] Event publishing verified
- [x] All integration tests pass

## Testing Requirements

**Integration Test Coverage**:
- Problem, Hypothesis, Experiment CRUD
- State transitions
- Event publishing
- Tenant isolation
- Error scenarios

## References

- **Testing Strategy**: `specs/testing-strategy.md` (comprehensive guide)
- **PROJECT.md**: Section 23a - Testing Strategy
- **CI/CD Pipeline**: `specs/ci-cd-pipelines.md` (integration test stage)
- **Experiment Service**: `specs/experiment-service.md`
- **Event Schemas**: `specs/event-schemas.md`
- **Related Tasks**: All experiment-service tasks
