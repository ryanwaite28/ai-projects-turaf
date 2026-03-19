# BFF API Service

Backend for Frontend (BFF) API service that provides a unified REST API for the Angular frontend.

## Architecture

**Type**: Backend for Frontend (Spring Boot REST API)  
**Pattern**: API Gateway + Orchestration  
**Deployment**: ECS Fargate (behind Public ALB)  
**Port**: 8080 (internal)

## Overview

The BFF API serves as the single entry point for all frontend requests, providing:

1. **Unified REST API**: Single endpoint (`/api/v1/*`) for frontend
2. **Authentication**: JWT token validation and user context extraction
3. **Orchestration**: Aggregate data from multiple microservices
4. **Proxy**: Route requests to appropriate microservices via internal ALB
5. **Cross-Cutting Concerns**: CORS, rate limiting, logging, error handling

## Technology Stack

- **Spring Boot**: 3.2.0
- **Java**: 17
- **HTTP Client**: WebClient (reactive)
- **Security**: Spring Security + JWT
- **Resilience**: Resilience4j (circuit breakers, rate limiting, retry)
- **Metrics**: Micrometer + Prometheus
- **Testing**: JUnit 5, MockWebServer

## Project Structure

```
services/bff-api/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/turaf/bff/
│   │   │   ├── BffApiApplication.java
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controllers/      # REST controllers
│   │   │   ├── clients/          # Service clients (WebClient)
│   │   │   ├── security/         # JWT validation, filters
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── exception/        # Exception handlers
│   │   │   ├── filter/           # Request/response filters
│   │   │   └── metrics/          # Custom metrics
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-qa.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/turaf/bff/
│           ├── controllers/
│           ├── clients/
│           ├── security/
│           └── filter/
└── target/
```

## API Endpoints

### Base Path
All endpoints are prefixed with `/api/v1`

### Authentication Endpoints
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `GET /api/v1/auth/me` - Get current user
- `POST /api/v1/auth/logout` - User logout

### Organization Endpoints
- `GET /api/v1/organizations` - List organizations
- `POST /api/v1/organizations` - Create organization
- `GET /api/v1/organizations/{id}` - Get organization
- `PUT /api/v1/organizations/{id}` - Update organization
- `DELETE /api/v1/organizations/{id}` - Delete organization
- `GET /api/v1/organizations/{id}/members` - List members
- `POST /api/v1/organizations/{id}/members` - Add member

### Experiment Endpoints
- `GET /api/v1/experiments` - List experiments
- `POST /api/v1/experiments` - Create experiment
- `GET /api/v1/experiments/{id}` - Get experiment
- `PUT /api/v1/experiments/{id}` - Update experiment
- `DELETE /api/v1/experiments/{id}` - Delete experiment
- `POST /api/v1/experiments/{id}/start` - Start experiment
- `POST /api/v1/experiments/{id}/complete` - Complete experiment

### Metrics Endpoints
- `POST /api/v1/metrics` - Record metric
- `GET /api/v1/experiments/{id}/metrics` - Get experiment metrics
- `GET /api/v1/metrics/{id}` - Get metric

### Orchestration Endpoints
- `GET /api/v1/dashboard/overview` - Dashboard overview (composite)
- `GET /api/v1/experiments/{id}/full` - Full experiment details (composite)
- `GET /api/v1/organizations/{id}/summary` - Organization summary (composite)

## Service Communication

### Internal ALB
- **Base URL**: `http://internal-alb.{env}.turafapp.com`
- **Service Paths**:
  - Identity Service: `/identity/*`
  - Organization Service: `/organization/*`
  - Experiment Service: `/experiment/*`
  - Metrics Service: `/metrics/*`

### User Context Propagation
- BFF validates JWT tokens
- Extracts user context (userId, organizationId)
- Adds headers to downstream requests:
  - `X-User-Id`: User ID
  - `X-Organization-Id`: Organization ID
  - `Authorization`: Original JWT token

## Configuration

### Environment Variables
- `SPRING_PROFILES_ACTIVE` - Environment (dev/qa/prod)
- `JWT_SECRET_KEY` - JWT signing secret (from Secrets Manager)
- `INTERNAL_ALB_URL` - Internal ALB base URL (optional, defaults per profile)
- `CORS_ALLOWED_ORIGINS` - Comma-separated allowed origins (optional)

### Application Profiles
- **dev**: Development environment
- **qa**: QA/Staging environment
- **prod**: Production environment

## Development

### Prerequisites
- Java 17
- Maven 3.8+

### Build
```bash
mvn clean install
```

### Run Locally
```bash
mvn spring-boot:run
```

### Run with Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run Tests
```bash
mvn test
```

### Run with Coverage
```bash
mvn test jacoco:report
```

## Health Checks

### Actuator Endpoints
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/metrics` - Prometheus metrics
- `/actuator/info` - Application info

### Health Indicators
- Database connectivity (if applicable)
- Internal ALB reachability
- Circuit breaker states
- Memory and CPU usage

## Observability

### Logging
- Structured JSON logging (production)
- Console logging (development)
- Correlation IDs for request tracing
- MDC context includes: correlationId, userId, organizationId

### Metrics
- `http_server_requests_seconds` - Request duration
- `bff_service_call_duration_seconds` - Service call duration
- `bff_service_call_total` - Total service calls
- `bff_orchestration_duration_seconds` - Orchestration duration
- `bff_authentication_attempts_total` - Authentication attempts
- `bff_rate_limit_exceeded_total` - Rate limit exceeded count

### Circuit Breakers
- Configured per service (identity, organization, experiment, metrics)
- Failure rate threshold: 50%
- Sliding window: 10 calls
- Wait duration in open state: 10 seconds

## Security

### Authentication
- JWT token validation
- Token expiration checking
- User context extraction

### CORS
- Configured per environment
- Allowed origins from configuration
- Credentials support enabled

### Rate Limiting
- Per-user: 100 requests per minute
- Per-IP (unauthenticated): 10 requests per minute
- Public endpoints (login, register): 10 requests per minute

## Implementation Status

🚧 **In Progress** - Task 001 completed:
- [x] Maven project setup
- [x] Main application class
- [x] Application configuration files
- [x] Directory structure (Clean Architecture)
- [x] README documentation

**Next Tasks**:
- [ ] Configure service clients (Task 002)
- [ ] Implement authentication (Task 003)
- [ ] Implement proxy endpoints (Task 004)
- [ ] Implement orchestration endpoints (Task 005)
- [ ] Configure CORS (Task 006)
- [ ] Implement error handling (Task 007)
- [ ] Implement rate limiting (Task 008)
- [ ] Add observability (Task 009)
- [ ] Add unit tests (Task 010)

## References

- **Specification**: `specs/bff-api.md`
- **Tasks**: `tasks/bff-api/`
- **PROJECT.md**: Section 10a (Network Architecture), Section 40 (BFF API Service)
