# Docker Compose Services Setup with Multi-Environment Configuration

Add all microservices (BFF and 4 core services) to Docker Compose with application-docker.yml profiles for local development, using container networking and a parameterized Dockerfile for building Spring Boot applications.

---

## Overview

Extend the existing Docker Compose setup to include all Spring Boot microservices, enabling a complete local development environment where services communicate via Docker container names. Each service will have an `application-docker.yml` profile for container-specific configuration, and a shared Dockerfile will build all services using build arguments.

---

## Services to Add

### Core Services (5)
1. **bff-api** (port 8080) - Backend for Frontend, orchestrates calls to microservices
2. **identity-service** (port 8081) - User authentication and authorization
3. **organization-service** (port 8082) - Organization and membership management
4. **experiment-service** (port 8083) - Problems, hypotheses, and experiments
5. **metrics-service** (port 8084) - Metrics collection and aggregation

### Lambda Services (Optional - if LocalStack free tier supports)
6. **reporting-service** - Report generation (Lambda function)
7. **notification-service** - Notification delivery (Lambda function)

**Note**: LocalStack free tier includes Lambda support, so we'll include Lambda deployment configurations for the serverless functions.

---

## Architecture

### Container Networking

```
┌─────────────────────────────────────────────────────────┐
│                    turaf-network                         │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐                                        │
│  │   bff-api    │  Calls via container names:           │
│  │   :8080      │  - http://identity-service:8081       │
│  └──────┬───────┘  - http://organization-service:8082   │
│         │          - http://experiment-service:8083      │
│         │          - http://metrics-service:8084         │
│         │                                                 │
│    ┌────┴────┬────────┬────────┐                        │
│    │         │        │        │                         │
│  ┌─▼──┐  ┌──▼─┐  ┌──▼──┐  ┌──▼──┐                     │
│  │ ID │  │Org │  │Exp  │  │Met  │                      │
│  │8081│  │8082│  │8083 │  │8084 │                      │
│  └─┬──┘  └─┬──┘  └──┬──┘  └──┬──┘                     │
│    │       │        │        │                           │
│    └───────┴────────┴────────┘                          │
│            │                                              │
│      ┌─────▼──────┐                                      │
│      │ PostgreSQL │                                      │
│      │   :5432    │                                      │
│      └────────────┘                                      │
│                                                           │
│  ┌──────────────┐                                        │
│  │ LocalStack   │  Lambda functions:                    │
│  │   :4566      │  - reporting-service                  │
│  └──────────────┘  - notification-service               │
└─────────────────────────────────────────────────────────┘
```

---

## Files to Create

### 1. Dockerfile (Shared)
**Location**: `infrastructure/docker/spring-boot/Dockerfile`

Parameterized Dockerfile that builds any Spring Boot service:
```dockerfile
ARG SERVICE_NAME
FROM maven:3.9-eclipse-temurin-17-alpine AS build
ARG SERVICE_NAME
WORKDIR /app
COPY services/${SERVICE_NAME}/pom.xml ./
COPY services/common ./common
RUN mvn dependency:go-offline
COPY services/${SERVICE_NAME}/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
ARG SERVICE_NAME
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. Application Docker Profiles (5 files)

**BFF API**: `services/bff-api/src/main/resources/application-docker.yml`
```yaml
spring:
  profiles:
    active: docker

internal:
  alb:
    url: http://bff-api:8080  # Not used in local, services called directly

# Service URLs for local Docker environment
services:
  identity:
    url: http://identity-service:8081
  organization:
    url: http://organization-service:8082
  experiment:
    url: http://experiment-service:8083
  metrics:
    url: http://metrics-service:8084

cors:
  allowed-origins: http://localhost:4200

logging:
  level:
    com.turaf.bff: DEBUG
```

**Identity Service**: `services/identity-service/src/main/resources/application-docker.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/turaf?currentSchema=identity_schema
    username: identity_user
    password: ${IDENTITY_USER_PASSWORD}

server:
  port: 8081

logging:
  level:
    com.turaf: DEBUG
```

**Organization Service**: `services/organization-service/src/main/resources/application-docker.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/turaf?currentSchema=organization_schema
    username: organization_user
    password: ${ORGANIZATION_USER_PASSWORD}

server:
  port: 8082

logging:
  level:
    com.turaf: DEBUG
```

**Experiment Service**: `services/experiment-service/src/main/resources/application-docker.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/turaf?currentSchema=experiment_schema
    username: experiment_user
    password: ${EXPERIMENT_USER_PASSWORD}

server:
  port: 8083

logging:
  level:
    com.turaf: DEBUG
```

**Metrics Service**: `services/metrics-service/src/main/resources/application-docker.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/turaf?currentSchema=metrics_schema
    username: metrics_user
    password: ${METRICS_USER_PASSWORD}

server:
  port: 8084

logging:
  level:
    com.turaf: DEBUG
```

### 3. Lambda Deployment Scripts (Optional)

**Location**: `infrastructure/docker/localstack/deploy-lambdas.sh`

Script to deploy Lambda functions to LocalStack:
```bash
#!/bin/bash
# Deploy reporting and notification services as Lambda functions
# to LocalStack for local event-driven testing
```

### 4. BFF WebClient Configuration Update

**File**: `services/bff-api/src/main/java/com/turaf/bff/config/WebClientConfig.java`

Update to support direct service URLs in Docker environment:
```java
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceUrlsConfig {
    private String identityUrl;
    private String organizationUrl;
    private String experimentUrl;
    private String metricsUrl;
    // getters/setters
}
```

---

## Docker Compose Updates

### Service Definitions

Add to `docker-compose.yml`:

```yaml
services:
  # ... existing postgres and localstack ...

  # BFF API Service
  bff-api:
    build:
      context: .
      dockerfile: infrastructure/docker/spring-boot/Dockerfile
      args:
        SERVICE_NAME: bff-api
    container_name: turaf-bff-api
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=${JWT_SECRET}
    ports:
      - "8080:8080"
    networks:
      - turaf-network
    depends_on:
      postgres:
        condition: service_healthy
      identity-service:
        condition: service_healthy
      organization-service:
        condition: service_healthy
      experiment-service:
        condition: service_healthy
      metrics-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Identity Service
  identity-service:
    build:
      context: .
      dockerfile: infrastructure/docker/spring-boot/Dockerfile
      args:
        SERVICE_NAME: identity-service
    container_name: turaf-identity-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - IDENTITY_USER_PASSWORD=${IDENTITY_USER_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    ports:
      - "8081:8081"
    networks:
      - turaf-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Organization Service
  organization-service:
    build:
      context: .
      dockerfile: infrastructure/docker/spring-boot/Dockerfile
      args:
        SERVICE_NAME: organization-service
    container_name: turaf-organization-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - ORGANIZATION_USER_PASSWORD=${ORGANIZATION_USER_PASSWORD}
    ports:
      - "8082:8082"
    networks:
      - turaf-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Experiment Service
  experiment-service:
    build:
      context: .
      dockerfile: infrastructure/docker/spring-boot/Dockerfile
      args:
        SERVICE_NAME: experiment-service
    container_name: turaf-experiment-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EXPERIMENT_USER_PASSWORD=${EXPERIMENT_USER_PASSWORD}
    ports:
      - "8083:8083"
    networks:
      - turaf-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Metrics Service
  metrics-service:
    build:
      context: .
      dockerfile: infrastructure/docker/spring-boot/Dockerfile
      args:
        SERVICE_NAME: metrics-service
    container_name: turaf-metrics-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - METRICS_USER_PASSWORD=${METRICS_USER_PASSWORD}
    ports:
      - "8084:8084"
    networks:
      - turaf-network
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

---

## BFF Configuration Changes

### Update WebClientConfig.java

Current implementation uses a single `internal.alb.url` for all services. Need to support individual service URLs for Docker environment.

**Changes Required**:

1. **Create ServiceUrlsConfig.java**:
```java
@Configuration
@ConfigurationProperties(prefix = "services")
@Data
public class ServiceUrlsConfig {
    private String identityUrl;
    private String organizationUrl;
    private String experimentUrl;
    private String metricsUrl;
}
```

2. **Update WebClientConfig.java**:
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient identityWebClient(ServiceUrlsConfig config) {
        return createWebClient(config.getIdentityUrl());
    }
    
    @Bean
    public WebClient organizationWebClient(ServiceUrlsConfig config) {
        return createWebClient(config.getOrganizationUrl());
    }
    
    @Bean
    public WebClient experimentWebClient(ServiceUrlsConfig config) {
        return createWebClient(config.getExperimentUrl());
    }
    
    @Bean
    public WebClient metricsWebClient(ServiceUrlsConfig config) {
        return createWebClient(config.getMetricsUrl());
    }
    
    private WebClient createWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10));
        
        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

3. **Update Service Clients** to use named WebClient beans:
```java
@Component
@RequiredArgsConstructor
public class IdentityServiceClient {
    @Qualifier("identityWebClient")
    private final WebClient webClient;
    // ... rest of implementation
}
```

---

## Lambda Services (LocalStack)

### LocalStack Lambda Support

LocalStack **free tier includes Lambda** with the following features:
- Deploy Lambda functions
- Invoke functions synchronously/asynchronously
- EventBridge → Lambda integration
- S3 → Lambda triggers
- SQS → Lambda triggers

### Lambda Deployment Strategy

**Option 1: Deploy as ZIP files**
- Package Lambda functions as JAR/ZIP
- Deploy via AWS CLI to LocalStack
- Configure EventBridge rules to trigger Lambdas

**Option 2: Mock with Spring Boot services**
- Run reporting/notification services as Spring Boot apps
- Listen to SQS queues instead of EventBridge
- Simpler for local development

**Recommendation**: Use Option 2 for simplicity. Add reporting and notification services to Docker Compose as regular Spring Boot services that poll SQS queues.

---

## Environment Variables

### Update .env.example

Add service-specific variables:

```bash
# Service Ports (for host access)
BFF_API_PORT=8080
IDENTITY_SERVICE_PORT=8081
ORGANIZATION_SERVICE_PORT=8082
EXPERIMENT_SERVICE_PORT=8083
METRICS_SERVICE_PORT=8084

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-for-local-development-only-change-in-production-must-be-at-least-32-characters-long

# AWS LocalStack
AWS_ENDPOINT_URL=http://localhost:4566
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
```

---

## Usage

### Start All Services

```bash
# Build and start all services
docker-compose up --build

# Or start in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f bff-api
docker-compose logs -f identity-service
```

### Verify Services

```bash
# Check all services are healthy
docker-compose ps

# Test BFF API
curl http://localhost:8080/actuator/health

# Test identity service directly
curl http://localhost:8081/actuator/health

# Test via BFF
curl http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

### Development Workflow

**Option 1: Run all in Docker**
```bash
docker-compose up -d
# All services run in containers
```

**Option 2: Hybrid (some in Docker, some local)**
```bash
# Start infrastructure only
docker-compose up -d postgres localstack

# Run services locally via Maven
cd services/identity-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Option 3: Selective service restart**
```bash
# Rebuild and restart single service
docker-compose up -d --build identity-service

# View logs for debugging
docker-compose logs -f identity-service
```

---

## Benefits

### Complete Local Environment
- ✅ All services running together
- ✅ Service-to-service communication via Docker network
- ✅ Realistic deployment simulation
- ✅ End-to-end testing capability

### Multi-Environment Support
- ✅ `application.yml` - Base configuration
- ✅ `application-docker.yml` - Docker Compose environment
- ✅ `application-dev.yml` - AWS DEV environment
- ✅ `application-qa.yml` - AWS QA environment
- ✅ `application-prod.yml` - AWS PROD environment

### Developer Flexibility
- ✅ Run all services in Docker
- ✅ Run some in Docker, some locally
- ✅ Easy service restart for debugging
- ✅ Consistent with production architecture

---

## Files Summary

**New Files** (12):
1. `infrastructure/docker/spring-boot/Dockerfile` - Shared Spring Boot Dockerfile
2. `services/bff-api/src/main/resources/application-docker.yml`
3. `services/identity-service/src/main/resources/application-docker.yml`
4. `services/organization-service/src/main/resources/application-docker.yml`
5. `services/experiment-service/src/main/resources/application-docker.yml`
6. `services/metrics-service/src/main/resources/application-docker.yml`
7. `services/bff-api/src/main/java/com/turaf/bff/config/ServiceUrlsConfig.java`
8. `infrastructure/docker/localstack/deploy-lambdas.sh` (optional)

**Updated Files** (5):
1. `docker-compose.yml` - Add 5 service definitions
2. `.env.example` - Add service ports and JWT secret
3. `services/bff-api/src/main/java/com/turaf/bff/config/WebClientConfig.java`
4. `services/bff-api/src/main/java/com/turaf/bff/clients/IdentityServiceClient.java`
5. `docs/LOCAL_DEVELOPMENT.md` - Add Docker Compose services section

**Total**: 12 new files, 5 updated files

---

## Implementation Notes

### Build Context
The Dockerfile uses the project root as build context to access both the service and common module:
```yaml
build:
  context: .  # Project root
  dockerfile: infrastructure/docker/spring-boot/Dockerfile
```

### Service Dependencies
Services start in order:
1. PostgreSQL (with schema initialization)
2. LocalStack (with AWS resources)
3. Microservices (identity, organization, experiment, metrics)
4. BFF API (depends on all microservices)

### Health Checks
All services include health checks to ensure proper startup order and readiness.

### Port Mapping
Services expose ports to host for direct access during development, but communicate internally via container names.
