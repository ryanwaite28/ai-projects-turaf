# Refactor BFF API from WebClient to RestClient

This plan refactors the BFF API to use Spring Boot 3.2+ RestClient with @HttpExchange declarative interfaces, replacing the reactive WebClient implementation with synchronous/blocking HTTP clients.

## Scope

**Service:** bff-api only

**Approach:** Incremental conversion - one service client at a time to maintain stability

## Configuration Changes

### 1. Update pom.xml Dependencies
- Remove: `spring-boot-starter-webflux`
- Add: `spring-boot-starter-web` (already present, ensure it's the only web starter)
- Remove Reactor dependencies if explicitly declared

### 2. Create RestClientConfig
- Replace `WebClientConfig.java` with `RestClientConfig.java`
- Configure 4 RestClient beans (identity, organization, experiment, metrics)
- Set timeouts: 10s connect, 20s read/write
- Remove Reactor/Netty-specific configuration

## Service Client Conversions (6 clients)

### Phase 1: IdentityServiceClient
- Create `IdentityServiceHttpExchange.java` interface with `@HttpExchange` annotations
- Convert all methods to synchronous return types:
  - `Mono<LoginResponseDto>` → `LoginResponseDto`
  - `Mono<UserDto>` → `UserDto`
  - `Mono<Void>` → `void`
- Throw exceptions instead of returning error signals
- Update `IdentityServiceClient.java` to delegate to HttpExchange interface
- Update `AuthController.java` to handle synchronous returns
- Update unit tests: `IdentityServiceClientTest.java`, `AuthControllerTest.java`

### Phase 2: OrganizationServiceClient
- Create `OrganizationServiceHttpExchange.java` interface
- Convert methods:
  - `Flux<OrganizationDto>` → `List<OrganizationDto>`
  - `Mono<OrganizationDto>` → `OrganizationDto`
  - `Mono<MemberDto>` → `MemberDto`
  - `Mono<Void>` → `void`
- Update `OrganizationServiceClient.java` to delegate
- Update `OrganizationController.java` to handle synchronous returns
- Update unit tests: `OrganizationServiceClientTest.java`, `OrganizationControllerTest.java`

### Phase 3: ExperimentServiceClient
- Create `ExperimentServiceHttpExchange.java` interface
- Convert methods:
  - `Flux<ExperimentDto>` → `List<ExperimentDto>`
  - `Mono<ExperimentDto>` → `ExperimentDto`
  - `Mono<Void>` → `void`
- Update `ExperimentServiceClient.java` to delegate
- Update `ExperimentController.java` to handle synchronous returns
- Update unit tests: `ExperimentServiceClientTest.java`, `ExperimentControllerTest.java`

### Phase 4: MetricsServiceClient
- Create `MetricsServiceHttpExchange.java` interface
- Convert methods:
  - `Flux<MetricDto>` → `List<MetricDto>`
  - `Mono<MetricDto>` → `MetricDto`
  - `Mono<Void>` → `void`
- Update `MetricsServiceClient.java` to delegate
- Update `MetricsController.java` to handle synchronous returns
- Update unit tests: `MetricsServiceClientTest.java`, `MetricsControllerTest.java`

### Phase 5: ProblemServiceClient
- Create `ProblemServiceHttpExchange.java` interface
- Convert methods:
  - `Flux<ProblemDto>` → `List<ProblemDto>`
  - `Mono<ProblemDto>` → `ProblemDto`
  - `Mono<Void>` → `void`
- Update `ProblemServiceClient.java` to delegate (uses experimentWebClient)
- Update `ProblemController.java` to handle synchronous returns
- Update unit tests: (add if missing)

### Phase 6: HypothesisServiceClient
- Create `HypothesisServiceHttpExchange.java` interface
- Convert methods:
  - `Flux<HypothesisDto>` → `List<HypothesisDto>`
  - `Mono<HypothesisDto>` → `HypothesisDto`
  - `Mono<Void>` → `void`
- Update `HypothesisServiceClient.java` to delegate (uses experimentWebClient)
- Update `HypothesisController.java` to handle synchronous returns
- Update unit tests: (add if missing)

## Controller Updates (7 controllers)

For each controller:
- Remove `Mono<>` and `Flux<>` return types
- Use direct return types (e.g., `ResponseEntity<T>`, `List<T>`, `void`)
- Remove `.map()`, `.doOnSuccess()`, `.doOnError()` reactive operators
- Let exceptions propagate to `GlobalExceptionHandler`
- Controllers: AuthController, OrganizationController, ExperimentController, MetricsController, HypothesisController, ProblemController, DashboardController

## Exception Handling Updates

### GlobalExceptionHandler
- Remove `WebClientResponseException` handler
- Add handlers for RestClient exceptions:
  - `RestClientResponseException` (similar to WebClientResponseException)
  - `ResourceAccessException` (connection failures)
  - `HttpServerErrorException` (5xx errors)
  - `HttpClientErrorException` (4xx errors)

## Unit Test Updates

### Client Tests (6 files)
- Remove Reactor test utilities (`StepVerifier`, etc.)
- Use standard JUnit assertions
- Mock HttpExchange interfaces with `@MockBean`
- Test exception throwing instead of error signals

### Controller Tests (7 files)
- Remove WebFlux test setup
- Use `@WebMvcTest` instead of `@WebFluxTest`
- Mock service clients to return direct values
- Test exception handling via `GlobalExceptionHandler`

### Integration Tests (5 files)
- Update to work with synchronous controllers
- No changes expected if they test HTTP endpoints only

## Implementation Order

1. Update pom.xml dependencies
2. Create RestClientConfig
3. Convert IdentityServiceClient (Phase 1)
4. Convert OrganizationServiceClient (Phase 2)
5. Convert ExperimentServiceClient (Phase 3)
6. Convert MetricsServiceClient (Phase 4)
7. Convert ProblemServiceClient (Phase 5)
8. Convert HypothesisServiceClient (Phase 6)
9. Update GlobalExceptionHandler
10. Delete WebClientConfig
11. Run full test suite

## Notes

- ProblemServiceClient and HypothesisServiceClient share the experimentWebClient - they will share the experiment RestClient bean
- Query parameters in HttpExchange use `@RequestParam` annotations
- Path variables use `@PathVariable` annotations
- Request bodies use `@RequestBody` annotations
- Headers can be configured at the RestClient level or per-request using `@RequestHeader`
