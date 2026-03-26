# API Documentation

**Last Updated**: March 25, 2026  
**Status**: In Progress  
**Related Documents**: [Architecture](../../specs/architecture.md), [Service Specifications](../../specs/)

This directory contains API documentation for all Turaf platform services.

---

## Overview

All Turaf microservices expose RESTful APIs documented using OpenAPI 3.0 specification. API documentation is generated from Spring Boot annotations and maintained alongside the service code.

---

## Services

### Identity Service
**Status**: Pending  
**Base Path**: `/api/identity`  
**Specification**: `identity-service-api.yaml` (to be generated)  
**Source**: `services/identity-service/`

**Key Endpoints**:
- `POST /api/identity/auth/register` - User registration
- `POST /api/identity/auth/login` - User authentication
- `GET /api/identity/users/{id}` - Get user profile
- `PUT /api/identity/users/{id}` - Update user profile

---

### Organization Service
**Status**: Pending  
**Base Path**: `/api/organizations`  
**Specification**: `organization-service-api.yaml` (to be generated)  
**Source**: `services/organization-service/`

**Key Endpoints**:
- `POST /api/organizations` - Create organization
- `GET /api/organizations/{id}` - Get organization details
- `POST /api/organizations/{id}/members` - Add member
- `GET /api/organizations/{id}/teams` - List teams

---

### Experiment Service
**Status**: Pending  
**Base Path**: `/api/experiments`  
**Specification**: `experiment-service-api.yaml` (to be generated)  
**Source**: `services/experiment-service/`

**Key Endpoints**:
- `POST /api/experiments` - Create experiment
- `GET /api/experiments/{id}` - Get experiment details
- `POST /api/experiments/{id}/variants` - Add variant
- `POST /api/experiments/{id}/start` - Start experiment

---

### Metrics Service
**Status**: Pending  
**Base Path**: `/api/metrics`  
**Specification**: `metrics-service-api.yaml` (to be generated)  
**Source**: `services/metrics-service/`

**Key Endpoints**:
- `POST /api/metrics/events` - Record metric event
- `GET /api/metrics/experiments/{id}` - Get experiment metrics
- `GET /api/metrics/reports/{id}` - Generate metric report

---

### Reporting Service
**Status**: Pending  
**Base Path**: `/api/reports`  
**Specification**: `reporting-service-api.yaml` (to be generated)  
**Source**: `services/reporting-service/`

**Key Endpoints**:
- `POST /api/reports` - Generate report
- `GET /api/reports/{id}` - Get report
- `GET /api/reports/{id}/download` - Download report

---

### Notification Service
**Status**: Pending  
**Base Path**: `/api/notifications`  
**Specification**: `notification-service-api.yaml` (to be generated)  
**Source**: `services/notification-service/`

**Key Endpoints**:
- `POST /api/notifications` - Send notification
- `GET /api/notifications/preferences` - Get user preferences
- `PUT /api/notifications/preferences` - Update preferences

---

### BFF API
**Status**: Pending  
**Base Path**: `/api`  
**Specification**: `bff-api.yaml` (to be generated)  
**Source**: `services/bff-api/`

**Purpose**: Backend for Frontend - aggregates microservice calls for web/mobile clients

---

## API Documentation Generation

### Using Springdoc OpenAPI

All services use `springdoc-openapi` to generate OpenAPI specifications from code annotations.

**Dependencies** (in `pom.xml`):
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

**Configuration** (in `application.yml`):
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### Generating API Specs

**During Development**:
```bash
cd services/<service-name>
mvn clean package
# OpenAPI spec generated to target/openapi.json
```

**Accessing Swagger UI**:
- DEV: `http://api.dev.turafapp.com/api/<service>/swagger-ui.html`
- QA: `http://api.qa.turafapp.com/api/<service>/swagger-ui.html`
- PROD: `http://api.turafapp.com/api/<service>/swagger-ui.html`

### Exporting to YAML

```bash
# Start service locally
mvn spring-boot:run

# Download OpenAPI spec
curl http://localhost:8080/api-docs > docs/api/<service>-api.yaml
```

---

## API Standards

### Authentication
All APIs use JWT bearer tokens:
```
Authorization: Bearer <jwt-token>
```

### Request/Response Format
- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8
- **Date Format**: ISO 8601 (`2026-03-25T19:12:00Z`)

### Error Responses
Standard error format:
```json
{
  "timestamp": "2026-03-25T19:12:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/experiments",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

### Pagination
Standard pagination parameters:
- `page`: Page number (0-indexed)
- `size`: Items per page (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

Response includes pagination metadata:
```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## API Versioning

**Strategy**: URL path versioning (future)
- Current: `/api/<service>/<endpoint>`
- Future: `/api/v2/<service>/<endpoint>`

**Backward Compatibility**:
- Maintain v1 for 6 months after v2 release
- Deprecation warnings in response headers
- Migration guide provided

---

## Testing APIs

### Using cURL

```bash
# Get JWT token
TOKEN=$(curl -X POST http://api.dev.turafapp.com/api/identity/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}' \
  | jq -r '.token')

# Make authenticated request
curl http://api.dev.turafapp.com/api/experiments \
  -H "Authorization: Bearer $TOKEN"
```

### Using Postman

Import OpenAPI specs into Postman:
1. Download spec: `curl http://api.dev.turafapp.com/api/<service>/api-docs > spec.yaml`
2. Import in Postman: File → Import → Upload spec.yaml
3. Configure environment variables for tokens

---

## Rate Limiting

**Limits** (per user, per minute):
- DEV: 1000 requests
- QA: 500 requests
- PROD: 100 requests

**Headers**:
- `X-RateLimit-Limit`: Total allowed requests
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Time when limit resets (Unix timestamp)

**429 Response**:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 30 seconds."
}
```

---

## CORS Configuration

**Allowed Origins**:
- DEV: `http://localhost:4200`, `http://app.dev.turafapp.com`
- QA: `http://app.qa.turafapp.com`
- PROD: `https://app.turafapp.com`

**Allowed Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS  
**Allowed Headers**: Authorization, Content-Type, X-Request-ID  
**Max Age**: 3600 seconds

---

## Next Steps

1. **Generate OpenAPI specs** for each service
2. **Export to YAML files** in this directory
3. **Set up API documentation hosting** (e.g., Redoc, Swagger UI)
4. **Create Postman collections** for each service
5. **Add API examples** for common use cases
6. **Document authentication flows** in detail
7. **Create API changelog** for version tracking

---

## References

- **Service Specifications**: `../../specs/`
- **Architecture**: `../../specs/architecture.md`
- **Testing Strategy**: `../../specs/testing-strategy.md`
- **Local Development**: `../LOCAL_DEVELOPMENT.md`

---

**Maintained By**: Development Teams  
**Review Frequency**: After each service update
