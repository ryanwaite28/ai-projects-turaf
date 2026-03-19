# Reconcile Service Architecture Inconsistencies

Remove Java/Maven references for reporting-service and notification-service from architecture tasks, keeping them as Python Lambda functions per their specifications.

## Problem Statement

Task 001 (Setup Clean Architecture Layers) incorrectly created reporting-service and notification-service as Java/Maven Spring Boot projects, but these services are specified as Python Lambda functions in their respective specs. The reporting-service has already been fully implemented in Python (10 completed tasks), confirming the Python Lambda architecture is correct.

## Inconsistencies Identified

### 1. Parent POM (`services/pom.xml`)
- **Issue**: Includes `reporting-service` and `notification-service` as Maven modules
- **Fix**: Remove these two modules from parent POM

### 2. Task 001 (`tasks/architecture/001-setup-clean-architecture-layers.md`)
- **Issue**: Lists reporting-service and notification-service in scope and acceptance criteria
- **Fix**: Update task to exclude these services, add note explaining they're Python Lambda functions

### 3. Service Directory Structure
- **Issue**: Created Java package structure for both services:
  - `services/reporting-service/src/main/java/com/turaf/reporting/`
  - `services/notification-service/src/main/java/com/turaf/notification/`
- **Fix**: Remove Java directories (Python structure already exists)

### 4. Service POMs
- **Issue**: Created `pom.xml` files for both services
- **Fix**: Remove these POMs (not needed for Python projects)

### 5. Service READMEs
- **Issue**: READMEs describe Java/Spring Boot Clean Architecture
- **Fix**: Remove or replace with Python Lambda documentation

## Reconciliation Actions

### Files to Delete
1. `services/reporting-service/pom.xml`
2. `services/notification-service/pom.xml`
3. `services/reporting-service/src/main/java/` (entire directory)
4. `services/notification-service/src/main/java/` (entire directory)
5. `services/reporting-service/src/test/java/` (entire directory)
6. `services/notification-service/src/test/java/` (entire directory)
7. `services/reporting-service/README.md` (replace with Python-specific version)
8. `services/notification-service/README.md` (replace with Python-specific version)

### Files to Update

#### 1. `services/pom.xml`
**Change**: Remove reporting-service and notification-service from `<modules>` section

**Before**:
```xml
<modules>
    <module>identity-service</module>
    <module>organization-service</module>
    <module>experiment-service</module>
    <module>metrics-service</module>
    <module>reporting-service</module>
    <module>notification-service</module>
</modules>
```

**After**:
```xml
<modules>
    <module>identity-service</module>
    <module>organization-service</module>
    <module>experiment-service</module>
    <module>metrics-service</module>
    <!-- reporting-service and notification-service are Python Lambda functions -->
</modules>
```

#### 2. `tasks/architecture/001-setup-clean-architecture-layers.md`
**Changes**:
- Update scope to exclude reporting-service and notification-service
- Add note explaining these are Python Lambda functions
- Update acceptance criteria
- Update directory structure diagram

**Add to Implementation Details section**:
```markdown
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
```

#### 3. `services/reporting-service/README.md`
**Replace** with Python Lambda documentation referencing existing implementation

#### 4. `services/notification-service/README.md`
**Replace** with Python Lambda documentation

### Documentation Updates

#### PROJECT.md
- **Status**: Already correct - describes these as Lambda functions
- **Action**: No changes needed

#### Specs
- **Status**: Already correct - both specs define Python Lambda architecture
- **Action**: No changes needed

#### Task Files
- **Status**: Already correct - all task files reference Python/Lambda
- **Action**: No changes needed

## Verification Steps

1. ✅ Verify parent POM only includes 4 Java services
2. ✅ Verify Maven build succeeds with only Java services
3. ✅ Verify reporting-service Python structure intact
4. ✅ Verify notification-service Python structure intact
5. ✅ Verify Task 001 accurately reflects scope
6. ✅ Verify no Java artifacts remain for these services

## Final Service Architecture

### Java/Spring Boot Services (Maven)
- identity-service
- organization-service
- experiment-service
- metrics-service

### Python Lambda Services (pip)
- reporting-service (✅ fully implemented)
- notification-service (partially implemented)

## Rationale for Hybrid Architecture

**Why Python Lambda for Reporting and Notification?**

1. **Event-Driven Nature**: These services only respond to EventBridge events, no REST APIs
2. **Serverless Benefits**: Auto-scaling, pay-per-use, no infrastructure management
3. **Simpler Deployment**: Single function deployment vs containerized service
4. **Appropriate Complexity**: Focused event processing doesn't need full Spring Boot stack
5. **Cost Efficiency**: Lambda pricing model better suited for sporadic event processing

**Why Java/Spring Boot for Core Services?**

1. **REST APIs**: Identity, Organization, Experiment, Metrics all expose REST endpoints
2. **Complex Business Logic**: Domain-driven design benefits from Clean Architecture
3. **Consistency**: Shared domain models, patterns, and practices
4. **Enterprise Patterns**: Repository pattern, dependency injection, transaction management
5. **Team Expertise**: Java/Spring Boot is industry standard for microservices

## Implementation Order

1. Update parent POM (remove modules)
2. Delete Java directories for both services
3. Delete POM files for both services
4. Update Task 001 documentation
5. Create/update Python-specific READMEs
6. Verify Maven build
7. Update task checklist

## Impact Assessment

**Low Risk Changes**:
- Removing incorrect Java artifacts
- Documentation updates
- No impact on existing Python implementations

**No Breaking Changes**:
- Reporting service already fully implemented in Python
- Notification service tasks already reference Python
- Other services unaffected

**Benefits**:
- Eliminates confusion between specs and tasks
- Clarifies architectural decisions
- Maintains existing working implementations
- Provides clear guidance for future development
