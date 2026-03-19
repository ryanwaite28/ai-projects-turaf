# Add BFF API with Dual ALB Architecture

Add a Spring Boot REST API as a Backend for Frontend (BFF) that communicates with microservices through an internal Application Load Balancer, while being exposed to the frontend through a public ALB.

## Overview

This plan introduces a **Spring Boot REST API** serving as a Backend for Frontend (BFF) layer, replacing direct frontend-to-microservice communication. The architecture uses **two Application Load Balancers**:

1. **Public ALB**: Routes frontend traffic to the BFF API
2. **Internal ALB**: Routes BFF traffic to microservices (identity, organization, experiment, metrics)

The BFF handles authentication validation, request orchestration, and provides a unified REST API for the Angular frontend.

## Architecture Design

### Current Architecture
```
Frontend (Angular) → Individual Microservices (direct)
  - Identity Service
  - Organization Service
  - Experiment Service
  - Metrics Service
```

### Target Architecture
```
Internet
  ↓
CloudFront (Static Assets)
  ↓
Public ALB (HTTPS)
  ↓
BFF API (Spring Boot REST)
  ↓
Internal ALB (HTTP)
  ↓
Microservices (ECS Fargate)
  - Identity Service
  - Organization Service
  - Experiment Service
  - Metrics Service
```

### Load Balancer Configuration

#### Public ALB (Internet-Facing)
- **Name**: `turaf-public-alb-{env}`
- **Scheme**: internet-facing
- **Listeners**: 
  - Port 443 (HTTPS) → BFF target group
  - Port 80 (HTTP) → Redirect to 443
- **Target Group**: `turaf-bff-api-{env}`
- **Health Check**: `/actuator/health`
- **SSL Certificate**: ACM certificate for `api.{env}.turaf.com`

#### Internal ALB (Private)
- **Name**: `turaf-internal-alb-{env}`
- **Scheme**: internal
- **Listeners**: Port 80 (HTTP)
- **Path-Based Routing**:
  - `/identity/*` → identity-service target group
  - `/organization/*` → organization-service target group
  - `/experiment/*` → experiment-service target group
  - `/metrics/*` → metrics-service target group
- **Health Checks**: `/actuator/health` per service
- **DNS**: `internal-alb.{env}.turaf.internal` (Route53 private hosted zone)

### BFF API Design

**Technology**: Spring Boot 3.2.0 REST API (Java 17)  
**Pattern**: Backend for Frontend (BFF)  
**Communication**: RestTemplate/WebClient to internal ALB  
**Deployment**: ECS Fargate (2 tasks in prod, 1 in dev/qa)

#### Key Responsibilities
1. **Unified REST API**: Single endpoint for frontend (`/api/v1/*`)
2. **Authentication**: Validate JWT tokens, propagate to services
3. **Orchestration**: Aggregate data from multiple services
4. **Error Handling**: Standardized error responses
5. **CORS**: Configure for Angular frontend
6. **Rate Limiting**: Protect backend services
7. **Logging**: Centralized request/response logging

#### API Structure
```
/api/v1/auth/*           → Identity Service (via internal ALB)
/api/v1/organizations/*  → Organization Service (via internal ALB)
/api/v1/experiments/*    → Experiment Service (via internal ALB)
/api/v1/metrics/*        → Metrics Service (via internal ALB)
/api/v1/dashboard/*      → Orchestration (multiple services)
```

## Implementation Plan

### Phase 1: Update PROJECT.md

Add comprehensive documentation for the dual ALB architecture:

#### 1.1 Add Network Architecture Section
```markdown
## Network Architecture

### Application Load Balancers

**Public ALB** (Internet-Facing):
- Routes frontend traffic to BFF API
- HTTPS termination (ACM certificate)
- CloudFront → Public ALB → BFF
- DNS: api.{env}.turaf.com

**Internal ALB** (Private):
- Routes BFF traffic to microservices
- Path-based routing by service
- HTTP only (internal VPC traffic)
- DNS: internal-alb.{env}.turaf.internal

### Traffic Flow
1. Frontend makes request to api.turaf.com
2. CloudFront routes to Public ALB
3. Public ALB routes to BFF API
4. BFF API calls internal-alb.turaf.internal/{service}/*
5. Internal ALB routes to appropriate microservice
6. Response flows back through chain
```

#### 1.2 Update Service Specifications
Add BFF API service specification:
```markdown
## BFF API Service

Responsibilities:
- Unified REST API for frontend
- JWT token validation
- Request orchestration
- Service aggregation

APIs:
- GET /api/v1/dashboard/overview
- All proxied service endpoints

Technology:
- Spring Boot 3.2.0
- RestTemplate/WebClient
- Spring Security (JWT)
```

#### 1.3 Update Architecture Diagrams
Update all mermaid diagrams to show:
- Public ALB
- Internal ALB
- BFF API
- Service communication flow

### Phase 2: Create BFF API Specification

Create `specs/bff-api.md` with complete specification:

#### 2.1 Service Overview
- Purpose: Backend for Frontend pattern
- Position in architecture
- Technology stack (Spring Boot REST)
- Communication pattern (HTTP via internal ALB)

#### 2.2 API Endpoints

**Authentication Endpoints** (proxy to Identity Service):
```
POST /api/v1/auth/login
POST /api/v1/auth/register
GET /api/v1/auth/me
POST /api/v1/auth/logout
```

**Organization Endpoints** (proxy to Organization Service):
```
GET /api/v1/organizations
POST /api/v1/organizations
GET /api/v1/organizations/{id}
PUT /api/v1/organizations/{id}
DELETE /api/v1/organizations/{id}
GET /api/v1/organizations/{id}/members
POST /api/v1/organizations/{id}/members
```

**Experiment Endpoints** (proxy to Experiment Service):
```
GET /api/v1/experiments
POST /api/v1/experiments
GET /api/v1/experiments/{id}
PUT /api/v1/experiments/{id}
DELETE /api/v1/experiments/{id}
POST /api/v1/experiments/{id}/start
POST /api/v1/experiments/{id}/complete
```

**Metrics Endpoints** (proxy to Metrics Service):
```
POST /api/v1/metrics
GET /api/v1/experiments/{id}/metrics
GET /api/v1/metrics/{id}
```

**Orchestration Endpoints** (composite queries):
```
GET /api/v1/dashboard/overview
  - Returns: user info + organizations + active experiments

GET /api/v1/experiments/{id}/full
  - Returns: experiment + metrics + report status

GET /api/v1/organizations/{id}/summary
  - Returns: organization + members + experiment count
```

#### 2.3 Service Communication

**Internal ALB Configuration**:
```java
@Configuration
public class ServiceClientConfig {
    @Value("${internal.alb.url}")
    private String internalAlbUrl; // http://internal-alb.dev.turaf.internal
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .rootUri(internalAlbUrl)
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

**Service Client Example**:
```java
@Service
public class IdentityServiceClient {
    private final RestTemplate restTemplate;
    
    public UserDto login(LoginRequest request) {
        return restTemplate.postForObject(
            "/identity/auth/login",
            request,
            UserDto.class
        );
    }
}
```

#### 2.4 Authentication Flow
1. Frontend sends request with JWT token to BFF
2. BFF validates JWT signature and expiration
3. BFF extracts user context (userId, organizationId)
4. BFF adds `X-User-Id` and `X-Organization-Id` headers
5. BFF forwards request to internal ALB
6. Internal ALB routes to appropriate service
7. Service processes request with user context
8. Response flows back to frontend

#### 2.5 Error Handling
- Standardized error response format
- Circuit breaker for service failures
- Timeout handling
- Retry logic with exponential backoff
- Fallback responses for orchestration endpoints

#### 2.6 Observability
- Request/response logging with correlation IDs
- Metrics: request rate, latency, error rate per endpoint
- Distributed tracing (X-Ray or Zipkin)
- Health checks for ALB
- Actuator endpoints

### Phase 3: Create Task Breakdown

Create `tasks/bff-api/` directory with tasks:

#### 001-setup-bff-project.md
- Create Spring Boot project structure
- Configure Maven dependencies
- Setup application.yml
- Create BffApiApplication.java
- Configure Clean Architecture layers

#### 002-configure-service-clients.md
- Create RestTemplate bean with internal ALB URL
- Create service client interfaces
- Implement IdentityServiceClient
- Implement OrganizationServiceClient
- Implement ExperimentServiceClient
- Implement MetricsServiceClient
- Add connection pooling and timeouts

#### 003-implement-authentication.md
- Create JWT validation filter
- Extract user context from token
- Add security configuration
- Propagate user headers to services
- Handle authentication errors

#### 004-implement-proxy-endpoints.md
- Create controllers for each service domain
- Implement auth endpoints (proxy to Identity)
- Implement organization endpoints (proxy to Organization)
- Implement experiment endpoints (proxy to Experiment)
- Implement metrics endpoints (proxy to Metrics)
- Add request/response logging

#### 005-implement-orchestration-endpoints.md
- Create DashboardController
- Implement /dashboard/overview (parallel calls)
- Implement /experiments/{id}/full
- Implement /organizations/{id}/summary
- Add error handling for partial failures
- Add response caching

#### 006-implement-cors-configuration.md
- Configure CORS for Angular frontend
- Define allowed origins per environment
- Set allowed methods and headers
- Configure credentials support

#### 007-implement-error-handling.md
- Create global exception handler
- Define standard error response format
- Handle service unavailable errors
- Handle timeout errors
- Add circuit breaker with Resilience4j

#### 008-implement-rate-limiting.md
- Add rate limiting per user
- Configure limits per endpoint
- Return 429 Too Many Requests
- Add rate limit headers

#### 009-add-observability.md
- Configure Spring Boot Actuator
- Add custom metrics
- Integrate distributed tracing
- Add correlation ID generation
- Configure health checks

#### 010-add-unit-tests.md
- Test service clients with MockRestServiceServer
- Test authentication filter
- Test orchestration logic
- Test error handling
- Test CORS configuration

### Phase 4: Update AWS Infrastructure Specification

Update `specs/aws-infrastructure.md`:

#### 4.1 Add Public ALB Section
```markdown
### Public Application Load Balancer

**Purpose**: Route frontend traffic to BFF API

**Configuration**:
- Name: turaf-public-alb-{env}
- Scheme: internet-facing
- Subnets: Public subnets in 2 AZs
- Security Group: Allow 443 from CloudFront, 80 from internet

**Listeners**:
- Port 443 (HTTPS):
  - SSL Certificate: ACM cert for api.{env}.turaf.com
  - Default Action: Forward to bff-api target group
- Port 80 (HTTP):
  - Default Action: Redirect to 443

**Target Group**:
- Name: turaf-bff-api-{env}
- Protocol: HTTP
- Port: 8080
- Health Check: /actuator/health
- Deregistration Delay: 30 seconds
```

#### 4.2 Add Internal ALB Section
```markdown
### Internal Application Load Balancer

**Purpose**: Route BFF traffic to microservices

**Configuration**:
- Name: turaf-internal-alb-{env}
- Scheme: internal
- Subnets: Private subnets in 2 AZs
- Security Group: Allow 80 from BFF security group

**Listener Rules** (Path-based routing):
1. Path: /identity/* → identity-service target group
2. Path: /organization/* → organization-service target group
3. Path: /experiment/* → experiment-service target group
4. Path: /metrics/* → metrics-service target group

**Target Groups**:
- turaf-identity-service-{env}
- turaf-organization-service-{env}
- turaf-experiment-service-{env}
- turaf-metrics-service-{env}

**DNS**:
- Private hosted zone: turaf.internal
- Record: internal-alb.{env}.turaf.internal → Internal ALB
```

#### 4.3 Add BFF ECS Service
```markdown
### BFF API Service

**Task Definition**:
- Family: turaf-bff-api-{env}
- CPU: 512 (0.5 vCPU)
- Memory: 1024 MB
- Container Port: 8080

**Service Configuration**:
- Desired Count: 2 (prod), 1 (dev/qa)
- Launch Type: Fargate
- Subnets: Private subnets
- Security Group: Allow 8080 from Public ALB
- Load Balancer: Public ALB, bff-api target group
- Auto-scaling: 2-10 tasks (prod)

**Environment Variables**:
- INTERNAL_ALB_URL=http://internal-alb.{env}.turaf.internal
- JWT_SECRET_KEY=(from Secrets Manager)
- ENVIRONMENT={env}
```

#### 4.4 Update Security Groups
```markdown
### Security Group Rules

**Public ALB Security Group**:
- Inbound: 443 from 0.0.0.0/0 (internet)
- Inbound: 80 from 0.0.0.0/0 (internet)
- Outbound: 8080 to BFF security group

**BFF API Security Group**:
- Inbound: 8080 from Public ALB security group
- Outbound: 80 to Internal ALB security group

**Internal ALB Security Group**:
- Inbound: 80 from BFF security group
- Outbound: 8080 to service security groups

**Service Security Groups** (identity, organization, experiment, metrics):
- Inbound: 8080 from Internal ALB security group
- Outbound: 5432 to RDS security group (PostgreSQL)
```

### Phase 5: Update Existing Specifications

#### 5.1 Update `specs/angular-frontend.md`
- Change API base URL to public ALB endpoint
- Update environment configuration
- Update HTTP interceptor
- Add correlation ID header generation

**Changes**:
```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'https://api.dev.turaf.com/api/v1'
};

// Before (multiple service URLs)
const IDENTITY_API = 'https://identity.dev.turaf.com';
const EXPERIMENT_API = 'https://experiment.dev.turaf.com';

// After (single BFF URL)
const API_BASE_URL = environment.apiUrl;
```

#### 5.2 Update `specs/identity-service.md`
- Note all requests come through internal ALB
- Add section on user context headers
- Update API paths to include `/identity` prefix
- Document internal ALB health check endpoint

#### 5.3 Update `specs/organization-service.md`
- Update API paths to include `/organization` prefix
- Note user context from BFF headers
- Document internal ALB integration

#### 5.4 Update `specs/experiment-service.md`
- Update API paths to include `/experiment` prefix
- Note orchestration endpoints in BFF
- Document internal ALB integration

#### 5.5 Update `specs/metrics-service.md`
- Update API paths to include `/metrics` prefix
- Note rate limiting at BFF level
- Document internal ALB integration

#### 5.6 Update `specs/architecture.md`
- Add dual ALB architecture diagram
- Add BFF API to system layers
- Update request flow documentation
- Add section on service-to-service communication

#### 5.7 Update `specs/ci-cd-pipelines.md`
- Add BFF API build/test/deploy pipeline
- Update deployment order (BFF after services, before frontend)
- Add ALB configuration in infrastructure pipeline

### Phase 6: Update Existing Tasks

#### 6.1 Update `tasks/frontend/*`
- Update API service files to use BFF URL
- Update environment configuration
- Update HTTP interceptors
- Remove service-specific API clients

#### 6.2 Update `tasks/infrastructure/*`
- Add public ALB Terraform module
- Add internal ALB Terraform module
- Add BFF ECS task definition
- Add BFF ECS service
- Update security group rules
- Add Route53 private hosted zone
- Add ALB listener rules

#### 6.3 Update `tasks/cicd/*`
- Add BFF API pipeline
- Update deployment orchestration
- Add ALB health checks to deployment
- Add infrastructure deployment for ALBs

#### 6.4 Update Service Tasks
For each service (identity, organization, experiment, metrics):
- Update API paths to include service prefix
- Add internal ALB health check endpoint
- Document user context headers
- Update integration tests to expect headers

### Phase 7: Update Workflow

Update `.windsurf/workflows/project.md`:

#### 7.1 Update Suggested Build Order
```
1. Identity Service
2. Organization Service
3. Experiment Service
4. Metrics Service
5. BFF API ← NEW
6. Event Infrastructure
7. Reporting Service
8. Notification Service
9. Angular Frontend (updated to use BFF)
10. DevOps Infrastructure (including ALBs)
```

#### 7.2 Add BFF-Specific Workflow
```markdown
## BFF API Implementation

**Goal:** Implement Backend for Frontend API with dual ALB architecture.

* Prerequisites:
  - All backend microservices implemented and deployed
  - Internal ALB configured with path-based routing
  - Public ALB configured for BFF

* Prompt:
```
Implement BFF API using Spring Boot REST.
Follow specs/bff-api.md specification.
Implement service clients, authentication, orchestration per tasks/bff-api/.
Configure communication via internal ALB.
Ensure all endpoints tested and documented.
```

* Validation:
  - All proxy endpoints work correctly
  - Authentication validates JWT tokens
  - Orchestration endpoints aggregate data
  - Service clients use internal ALB
  - CORS configured for frontend
  - Circuit breakers prevent failures
  - Health checks pass
```

### Phase 8: Update Parent POM

Update `services/pom.xml`:
- Add `<module>bff-api</module>` to modules list

```xml
<modules>
    <module>identity-service</module>
    <module>organization-service</module>
    <module>experiment-service</module>
    <module>metrics-service</module>
    <module>bff-api</module>
</modules>
```

### Phase 9: Create Terraform Modules

Create infrastructure modules for ALBs:

#### 9.1 Public ALB Module
```
infrastructure/terraform/modules/public-alb/
├── main.tf
├── variables.tf
├── outputs.tf
└── README.md
```

#### 9.2 Internal ALB Module
```
infrastructure/terraform/modules/internal-alb/
├── main.tf
├── variables.tf
├── outputs.tf
└── README.md
```

#### 9.3 BFF ECS Service Module
```
infrastructure/terraform/modules/bff-service/
├── main.tf
├── variables.tf
├── outputs.tf
└── README.md
```

## Project Structure

### New Service Structure
```
services/bff-api/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/turaf/bff/
│   │   │   ├── BffApiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   ├── controllers/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── OrganizationController.java
│   │   │   │   ├── ExperimentController.java
│   │   │   │   ├── MetricsController.java
│   │   │   │   └── DashboardController.java
│   │   │   ├── clients/
│   │   │   │   ├── IdentityServiceClient.java
│   │   │   │   ├── OrganizationServiceClient.java
│   │   │   │   ├── ExperimentServiceClient.java
│   │   │   │   └── MetricsServiceClient.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtTokenValidator.java
│   │   │   ├── dto/
│   │   │   │   └── (shared DTOs)
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-qa.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/turaf/bff/
│           ├── controllers/
│           ├── clients/
│           └── security/
```

## Files to Create

### New Specifications
1. `specs/bff-api.md` - Complete BFF API specification

### New Tasks
1. `tasks/bff-api/001-setup-bff-project.md`
2. `tasks/bff-api/002-configure-service-clients.md`
3. `tasks/bff-api/003-implement-authentication.md`
4. `tasks/bff-api/004-implement-proxy-endpoints.md`
5. `tasks/bff-api/005-implement-orchestration-endpoints.md`
6. `tasks/bff-api/006-implement-cors-configuration.md`
7. `tasks/bff-api/007-implement-error-handling.md`
8. `tasks/bff-api/008-implement-rate-limiting.md`
9. `tasks/bff-api/009-add-observability.md`
10. `tasks/bff-api/010-add-unit-tests.md`

### New Infrastructure Tasks
1. `tasks/infrastructure/013-create-public-alb.md`
2. `tasks/infrastructure/014-create-internal-alb.md`
3. `tasks/infrastructure/015-configure-alb-routing.md`
4. `tasks/infrastructure/016-create-route53-private-zone.md`

### New Service Files
1. `services/bff-api/pom.xml`
2. `services/bff-api/README.md`
3. `services/bff-api/src/main/java/com/turaf/bff/BffApiApplication.java`
4. (All other BFF source files per structure above)

### Files to Update
1. `PROJECT.md` - Add dual ALB architecture, BFF service
2. `specs/architecture.md` - Add BFF and ALB layers
3. `specs/angular-frontend.md` - Update API communication
4. `specs/identity-service.md` - Note internal ALB integration
5. `specs/organization-service.md` - Note internal ALB integration
6. `specs/experiment-service.md` - Note internal ALB integration
7. `specs/metrics-service.md` - Note internal ALB integration
8. `specs/aws-infrastructure.md` - Add ALB and BFF infrastructure
9. `specs/ci-cd-pipelines.md` - Add BFF pipeline
10. `.windsurf/workflows/project.md` - Update build order and workflow
11. `services/pom.xml` - Add bff-api module

## Benefits of This Architecture

### Security
- **Defense in depth**: Public ALB → BFF → Internal ALB → Services
- **Service isolation**: Microservices not exposed to internet
- **Centralized auth**: JWT validation in one place
- **Network segmentation**: Public/private subnet separation

### Scalability
- **Independent scaling**: BFF and services scale separately
- **Load distribution**: ALBs distribute traffic evenly
- **Auto-scaling**: ECS tasks scale based on load
- **Connection pooling**: BFF maintains pools to internal ALB

### Maintainability
- **Single frontend endpoint**: Frontend only knows about BFF
- **Service flexibility**: Can change service locations without frontend changes
- **Centralized CORS**: One place to manage CORS policies
- **Unified logging**: All requests flow through BFF

### Performance
- **Orchestration**: Reduce frontend round trips with composite endpoints
- **Caching**: BFF can cache frequently accessed data
- **Connection reuse**: HTTP keep-alive to internal ALB
- **Parallel requests**: BFF makes parallel calls to services

## Migration Strategy

### Phase 1: Infrastructure Setup
1. Deploy internal ALB with path-based routing
2. Update microservices to register with internal ALB
3. Deploy public ALB
4. Configure DNS for api.{env}.turaf.com

### Phase 2: Deploy BFF (Non-Breaking)
1. Deploy BFF API alongside existing services
2. BFF routes to services via internal ALB
3. Frontend can still call services directly (for now)
4. Test BFF endpoints thoroughly

### Phase 3: Update Frontend (Gradual)
1. Update frontend to use BFF endpoints
2. Monitor BFF performance and errors
3. Keep direct service calls as fallback initially

### Phase 4: Lock Down Services (Breaking)
1. Update security groups to only allow internal ALB → service traffic
2. Remove public routes to services
3. All traffic must flow through BFF
4. Update monitoring and alerts

## Testing Strategy

### Unit Tests
- Service client tests with MockRestServiceServer
- Controller tests with MockMvc
- Authentication filter tests
- Orchestration logic tests
- Error handling tests

### Integration Tests
- End-to-end BFF → Internal ALB → Service tests
- Authentication flow tests
- Orchestration endpoint tests
- Circuit breaker behavior tests
- Timeout and retry tests

### Load Tests
- BFF throughput testing
- ALB performance under load
- Connection pool sizing
- Auto-scaling validation

## Risks and Mitigations

### Risk: Additional Network Hop
**Mitigation**: 
- Use HTTP keep-alive connections
- Connection pooling in BFF
- Monitor latency metrics
- Internal ALB has minimal overhead

### Risk: BFF as Single Point of Failure
**Mitigation**:
- Deploy multiple BFF instances (auto-scaling)
- Health checks and auto-recovery
- Circuit breakers prevent cascade failures
- ALB distributes traffic

### Risk: Increased Complexity
**Mitigation**:
- Comprehensive documentation
- Clear separation of concerns
- Incremental rollout
- Extensive testing

### Risk: Internal ALB Cost
**Mitigation**:
- Cost-benefit analysis (security + simplicity)
- Use single internal ALB for all services
- Monitor and optimize

## Success Criteria

- ✅ Public ALB deployed and routing to BFF
- ✅ Internal ALB deployed with path-based routing
- ✅ BFF API service deployed and running
- ✅ All proxy endpoints working correctly
- ✅ JWT authentication validation working
- ✅ Orchestration endpoints returning aggregated data
- ✅ Service clients using internal ALB
- ✅ CORS configured for Angular frontend
- ✅ Circuit breakers preventing cascade failures
- ✅ Logging and metrics integrated
- ✅ Frontend updated to use BFF
- ✅ All tests passing (unit + integration + load)
- ✅ Documentation complete (spec + tasks + README)
- ✅ Infrastructure as code (Terraform) complete

## Timeline Estimate

### Planning Phase
- Update PROJECT.md: 2 hours
- Create BFF specification: 3 hours
- Create task breakdown: 2 hours
- Update existing specs: 3 hours
- Update workflow: 1 hour
- **Total Planning**: ~11 hours

### Implementation Phase
- Infrastructure (ALBs): 8-10 hours
- BFF service implementation: 20-25 hours
- Frontend updates: 5-8 hours
- Testing: 8-10 hours
- Documentation: 3-5 hours
- **Total Implementation**: ~45-60 hours

## Next Steps

1. ✅ Review and approve this plan
2. Begin Phase 1: Update PROJECT.md with dual ALB architecture
3. Phase 2: Create comprehensive BFF API specification
4. Phase 3: Create task breakdown for BFF implementation
5. Phase 4: Update AWS infrastructure specification
6. Phase 5: Update existing service specifications
7. Phase 6: Update existing tasks
8. Phase 7: Update workflow
9. Phase 8: Update parent POM
10. Begin implementation following tasks/bff-api/
