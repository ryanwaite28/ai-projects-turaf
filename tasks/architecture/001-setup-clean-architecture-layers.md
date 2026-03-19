# Task: Setup Clean Architecture Layers

**Service**: Architecture Foundation  
**Phase**: 1  
**Estimated Time**: 3-4 hours  

## Objective

Establish the foundational Clean Architecture layer structure across all microservices, defining clear boundaries and dependency rules that enforce separation of concerns.

## Prerequisites

- [ ] Java 17 installed
- [ ] Maven configured
- [ ] Project repository initialized

## Scope

**Java/Spring Boot Services Only**:

This task applies only to services using Java/Spring Boot with Clean Architecture. The reporting-service and notification-service use a different architecture (Python Lambda) and are excluded from this task.

**Directory Structure to Create**:
```
services/
├── identity-service/
│   └── src/main/java/com/turaf/identity/
│       ├── domain/           # Entities, Value Objects, Domain Services
│       ├── application/      # Use Cases, Application Services
│       ├── infrastructure/   # Repositories, External Services
│       └── interfaces/       # REST Controllers, DTOs
├── organization-service/
│   └── src/main/java/com/turaf/organization/
│       ├── domain/
│       ├── application/
│       ├── infrastructure/
│       └── interfaces/
├── experiment-service/
│   └── src/main/java/com/turaf/experiment/
│       ├── domain/
│       ├── application/
│       ├── infrastructure/
│       └── interfaces/
└── metrics-service/
    └── src/main/java/com/turaf/metrics/
        ├── domain/
        ├── application/
        ├── infrastructure/
        └── interfaces/
```

**Files to Create**:
- `services/pom.xml` (parent POM)
- `services/identity-service/pom.xml`
- `services/organization-service/pom.xml`
- `services/experiment-service/pom.xml`
- `services/metrics-service/pom.xml`

## Implementation Details

### Layer Definitions

**Domain Layer** (`domain/`):
- Contains core business logic
- No dependencies on other layers
- Entities, Value Objects, Domain Events
- Domain Services for complex business rules
- Repository interfaces (not implementations)

**Application Layer** (`application/`):
- Orchestrates domain objects
- Implements use cases
- Depends only on domain layer
- Application Services
- DTOs for internal use

**Infrastructure Layer** (`infrastructure/`):
- Implements repository interfaces
- External service integrations
- Database access (JPA entities)
- Event publishing implementations
- Depends on domain and application layers

**Interfaces Layer** (`interfaces/`):
- REST controllers
- Request/Response DTOs
- Exception handlers
- API documentation
- Depends on application layer

### Dependency Rules

1. **Domain** → No dependencies
2. **Application** → Domain only
3. **Infrastructure** → Domain, Application
4. **Interfaces** → Application (not Infrastructure directly)

### Maven Parent POM Structure

```xml
<project>
    <groupId>com.turaf</groupId>
    <artifactId>turaf-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>identity-service</module>
        <module>organization-service</module>
        <module>experiment-service</module>
        <module>metrics-service</module>
        <!-- reporting-service and notification-service are Python Lambda functions -->
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
    </properties>
</project>
```

### Service POM Template

Each service should have:
- Spring Boot parent
- Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation
- Build plugins: spring-boot-maven-plugin, maven-compiler-plugin

### Services Excluded from Clean Architecture

The following services use a different architecture pattern:

**Reporting Service** (`services/reporting-service/`):
- **Architecture**: Event-driven AWS Lambda (Python 3.11)
- **Reason**: Serverless event processor, no REST API needed
- **Build Tool**: pip (requirements.txt)
- **See**: `specs/reporting-service.md` and `tasks/reporting-service/`

**Notification Service** (`services/notification-service/`):
- **Architecture**: Event-driven AWS Lambda (Python 3.11)
- **Reason**: Serverless event processor, no REST API needed
- **Build Tool**: pip (requirements.txt)
- **See**: `specs/notification-service.md` and `tasks/notification-service/`

**Why Python Lambda for These Services?**
1. Event-driven nature (only respond to EventBridge events)
2. No REST API endpoints required
3. Serverless benefits (auto-scaling, pay-per-use)
4. Simpler deployment model
5. Appropriate complexity for focused event processing

## Acceptance Criteria

- [x] All Java service directories created with proper layer structure (4 services)
- [x] Parent POM configured with Java modules only (excludes Python Lambda services)
- [x] Each Java service has its own POM with correct dependencies
- [x] Package structure follows naming convention: `com.turaf.{service}.{layer}`
- [x] README.md created in each Java service explaining layer responsibilities
- [x] Maven build succeeds: `mvn clean install` (requires Maven 3.8+ installation)
- [x] No circular dependencies between layers
- [x] Python Lambda services (reporting, notification) excluded from this task

## Testing Requirements

**Verification**:
- Run `mvn clean install` from services directory
- Verify all modules compile successfully
- Check that package structure is correct in each service

## References

- Specification: `specs/architecture.md` (Clean Architecture section)
- PROJECT.md: Section 9 (Architecture Pattern)
- Clean Architecture by Robert C. Martin
- Related Tasks: All subsequent service implementation tasks depend on this
