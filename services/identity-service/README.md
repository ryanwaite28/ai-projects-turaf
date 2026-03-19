# Identity Service

Authentication and user identity management service for the Turaf platform.

## Clean Architecture Layers

This service follows Clean Architecture principles with clear separation of concerns across four layers:

### Domain Layer (`com.turaf.identity.domain`)

**Purpose**: Contains core business logic and domain models.

**Responsibilities**:
- Define domain entities (User, Role, Permission)
- Define value objects (Email, Password, UserId)
- Define domain events (UserRegistered, UserLoggedIn)
- Define repository interfaces (not implementations)
- Implement domain services for complex business rules
- Enforce business invariants and validation rules

**Dependencies**: None (pure business logic)

**Key Principles**:
- No framework dependencies
- No infrastructure concerns
- Framework-agnostic
- Testable in isolation

### Application Layer (`com.turaf.identity.application`)

**Purpose**: Orchestrates domain objects to implement use cases.

**Responsibilities**:
- Implement use cases (RegisterUser, AuthenticateUser, ChangePassword)
- Define application services
- Coordinate domain objects
- Handle transaction boundaries
- Define DTOs for internal communication

**Dependencies**: Domain layer only

**Key Principles**:
- Stateless services
- Single Responsibility per use case
- No direct infrastructure access

### Infrastructure Layer (`com.turaf.identity.infrastructure`)

**Purpose**: Implements technical concerns and external integrations.

**Responsibilities**:
- Implement repository interfaces using JPA
- Define JPA entities (separate from domain entities)
- Implement event publishers (EventBridge)
- External service integrations
- Database migrations
- Configuration

**Dependencies**: Domain and Application layers

**Key Principles**:
- Adapts external frameworks to domain needs
- Implements ports defined in domain layer
- Contains all framework-specific code

### Interfaces Layer (`com.turaf.identity.interfaces`)

**Purpose**: Exposes application functionality via REST API.

**Responsibilities**:
- REST controllers
- Request/Response DTOs
- Input validation
- Exception handlers
- API documentation (OpenAPI/Swagger)
- HTTP-specific concerns

**Dependencies**: Application layer only

**Key Principles**:
- Thin controllers (delegate to application services)
- No business logic in controllers
- HTTP-specific error handling

## Dependency Rules

```
Interfaces → Application → Domain
Infrastructure → Application → Domain
Infrastructure → Domain
```

**Critical Rules**:
1. Domain has NO dependencies on other layers
2. Application depends ONLY on Domain
3. Infrastructure can depend on Domain and Application
4. Interfaces depends ONLY on Application (not Infrastructure)

## Package Structure

```
com.turaf.identity/
├── domain/
│   ├── model/           # Entities and Value Objects
│   ├── event/           # Domain Events
│   ├── repository/      # Repository Interfaces
│   └── service/         # Domain Services
├── application/
│   ├── service/         # Application Services
│   ├── dto/             # Internal DTOs
│   └── usecase/         # Use Case Implementations
├── infrastructure/
│   ├── persistence/     # JPA Repositories
│   ├── event/           # Event Publishers
│   ├── config/          # Configuration
│   └── migration/       # Database Migrations
└── interfaces/
    ├── rest/            # REST Controllers
    ├── dto/             # Request/Response DTOs
    └── exception/       # Exception Handlers
```

## Building

```bash
# Build this service
mvn clean install

# Run tests
mvn test

# Run the service
mvn spring-boot:run
```

## Testing Strategy

- **Domain Layer**: Pure unit tests, no mocks needed
- **Application Layer**: Unit tests with mocked repositories
- **Infrastructure Layer**: Integration tests with test containers
- **Interfaces Layer**: API tests with MockMvc

## References

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
