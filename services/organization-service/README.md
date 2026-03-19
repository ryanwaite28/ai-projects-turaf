# Organization Service

Multi-tenant organization management service for the Turaf platform.

## Clean Architecture Layers

This service follows Clean Architecture principles with clear separation of concerns across four layers:

### Domain Layer (`com.turaf.organization.domain`)

**Purpose**: Contains core business logic and domain models.

**Responsibilities**:
- Define domain entities (Organization, Member, Invitation)
- Define value objects (OrganizationId, MemberRole)
- Define domain events (OrganizationCreated, MemberAdded)
- Define repository interfaces
- Implement domain services for multi-tenancy rules
- Enforce business invariants

**Dependencies**: None

### Application Layer (`com.turaf.organization.application`)

**Purpose**: Orchestrates domain objects to implement use cases.

**Responsibilities**:
- Implement use cases (CreateOrganization, InviteMember, RemoveMember)
- Define application services
- Coordinate domain objects
- Handle transaction boundaries

**Dependencies**: Domain layer only

### Infrastructure Layer (`com.turaf.organization.infrastructure`)

**Purpose**: Implements technical concerns and external integrations.

**Responsibilities**:
- Implement repository interfaces using JPA
- Define JPA entities
- Implement event publishers
- External service integrations
- Database migrations

**Dependencies**: Domain and Application layers

### Interfaces Layer (`com.turaf.organization.interfaces`)

**Purpose**: Exposes application functionality via REST API.

**Responsibilities**:
- REST controllers
- Request/Response DTOs
- Input validation
- Exception handlers
- API documentation

**Dependencies**: Application layer only

## Dependency Rules

```
Interfaces → Application → Domain
Infrastructure → Application → Domain
Infrastructure → Domain
```

## Package Structure

```
com.turaf.organization/
├── domain/
│   ├── model/
│   ├── event/
│   ├── repository/
│   └── service/
├── application/
│   ├── service/
│   ├── dto/
│   └── usecase/
├── infrastructure/
│   ├── persistence/
│   ├── event/
│   ├── config/
│   └── migration/
└── interfaces/
    ├── rest/
    ├── dto/
    └── exception/
```

## Building

```bash
mvn clean install
mvn test
mvn spring-boot:run
```
