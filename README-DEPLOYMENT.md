# Turaf Platform - Deployment Guide

**Version**: 1.0.0  
**Last Updated**: 2026-03-27

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Local Development](#local-development)
3. [Testing](#testing)
4. [Production Deployment](#production-deployment)
5. [Monitoring & Observability](#monitoring--observability)
6. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Java** 17+
- **Maven** 3.8+
- **Node.js** 18+ (for WebSocket Gateway)

### One-Command Setup

```bash
./scripts/local-dev-setup.sh
```

This script will:
1. Check all prerequisites
2. Build all services
3. Start Docker infrastructure
4. Start all application services
5. Verify health of all services

---

## Local Development

### Manual Setup

#### 1. Environment Configuration

Copy the example environment file:
```bash
cp .env.example .env
```

Edit `.env` with your configuration:
```bash
# Database
DB_NAME=turaf
DB_ADMIN_USER=turaf_admin
DB_ADMIN_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your-256-bit-secret-key-change-in-production

# Service Ports
IDENTITY_SERVICE_PORT=8081
ORGANIZATION_SERVICE_PORT=8082
EXPERIMENT_SERVICE_PORT=8083
METRICS_SERVICE_PORT=8084
BFF_API_PORT=8080
```

#### 2. Build Services

```bash
mvn clean install
```

#### 3. Start Infrastructure

```bash
docker-compose up -d postgres localstack redis
```

Wait for services to be ready:
```bash
# Check PostgreSQL
docker-compose exec postgres pg_isready -U turaf_admin -d turaf

# Check LocalStack
curl http://localhost:4566/_localstack/health

# Check Redis
docker-compose exec redis redis-cli ping
```

#### 4. Start Application Services

```bash
docker-compose up -d
```

#### 5. Verify Health

```bash
curl http://localhost:8081/actuator/health  # Identity Service
curl http://localhost:8082/actuator/health  # Organization Service
curl http://localhost:8083/actuator/health  # Experiment Service
curl http://localhost:8084/actuator/health  # Metrics Service
curl http://localhost:8080/actuator/health  # BFF API
```

### Development Workflow

#### Running Individual Services Locally

```bash
# Identity Service
cd services/identity-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Organization Service
cd services/organization-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Experiment Service
cd services/experiment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Metrics Service
cd services/metrics-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Hot Reload

For development with hot reload, use Spring Boot DevTools:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

#### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f identity-service

# Tail last 100 lines
docker-compose logs --tail=100 experiment-service
```

---

## Testing

### Unit Tests

```bash
# All services
mvn test

# Specific service
cd services/experiment-service
mvn test

# With coverage
mvn test jacoco:report
```

### Integration Tests

```bash
# Run integration test script
./scripts/integration-test.sh

# Or manually
mvn verify -P integration-tests
```

### Test Coverage

View coverage reports:
```bash
# Generate reports
mvn clean verify jacoco:report

# Open in browser
open services/experiment-service/target/site/jacoco/index.html
```

### Manual API Testing

#### Register a User

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "name": "Test User",
    "organizationId": "org-123"
  }'
```

#### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

#### Create Experiment (with JWT)

```bash
curl -X POST http://localhost:8083/api/v1/experiments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Organization-Id: org-123" \
  -H "X-User-Id: user-123" \
  -d '{
    "hypothesisId": "hyp-123",
    "name": "Test Experiment",
    "description": "Testing the API"
  }'
```

---

## Production Deployment

### AWS Deployment (Recommended)

#### Architecture

```
┌─────────────────┐
│   CloudFront    │  CDN
└────────┬────────┘
         │
┌────────▼────────┐
│   API Gateway   │  Entry point
└────────┬────────┘
         │
┌────────▼────────┐
│   ALB/NLB       │  Load balancer
└────────┬────────┘
         │
    ┌────┴────┐
    │   ECS   │  Container orchestration
    └────┬────┘
         │
    ┌────┴────┐
    │   RDS   │  PostgreSQL
    └─────────┘
```

#### Prerequisites

- AWS Account
- AWS CLI configured
- ECR repositories created
- RDS PostgreSQL instance
- EventBridge event bus
- S3 buckets for storage

#### Build and Push Docker Images

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build and push each service
for service in identity organization experiment metrics; do
  docker build -t turaf-$service-service \
    -f infrastructure/docker/spring-boot/Dockerfile \
    --build-arg SERVICE_NAME=$service-service .
  
  docker tag turaf-$service-service:latest \
    YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/turaf-$service-service:latest
  
  docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/turaf-$service-service:latest
done
```

#### Deploy with ECS

```bash
# Update ECS services
aws ecs update-service \
  --cluster turaf-cluster \
  --service identity-service \
  --force-new-deployment

aws ecs update-service \
  --cluster turaf-cluster \
  --service organization-service \
  --force-new-deployment

aws ecs update-service \
  --cluster turaf-cluster \
  --service experiment-service \
  --force-new-deployment

aws ecs update-service \
  --cluster turaf-cluster \
  --service metrics-service \
  --force-new-deployment
```

### Kubernetes Deployment

#### Prerequisites

- Kubernetes cluster (EKS, GKE, AKS, or self-hosted)
- kubectl configured
- Helm 3+ installed

#### Deploy with Helm

```bash
# Add Turaf Helm repository
helm repo add turaf https://charts.turaf.com
helm repo update

# Install Turaf platform
helm install turaf turaf/turaf-platform \
  --namespace turaf \
  --create-namespace \
  --set global.domain=turaf.example.com \
  --set postgresql.enabled=true \
  --set redis.enabled=true
```

#### Manual Kubernetes Deployment

```bash
# Apply Kubernetes manifests
kubectl apply -f infrastructure/k8s/namespace.yaml
kubectl apply -f infrastructure/k8s/configmaps/
kubectl apply -f infrastructure/k8s/secrets/
kubectl apply -f infrastructure/k8s/deployments/
kubectl apply -f infrastructure/k8s/services/
kubectl apply -f infrastructure/k8s/ingress.yaml
```

---

## Monitoring & Observability

### Health Checks

All services expose Spring Boot Actuator endpoints:

- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/metrics` - Prometheus metrics
- `/actuator/info` - Service information

### Prometheus Metrics

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'turaf-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'identity-service:8081'
        - 'organization-service:8082'
        - 'experiment-service:8083'
        - 'metrics-service:8084'
```

### Grafana Dashboards

Import pre-built dashboards:
- Spring Boot 2.x Statistics
- JVM Micrometer
- PostgreSQL Database
- Application Metrics

### Distributed Tracing

Configure Jaeger or Zipkin:

```yaml
# application.yml
spring:
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    base-url: http://zipkin:9411
```

### Log Aggregation

#### ELK Stack

```yaml
# logstash.conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  if [service] == "identity-service" {
    mutate {
      add_tag => ["identity"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "turaf-%{+YYYY.MM.dd}"
  }
}
```

---

## Troubleshooting

### Common Issues

#### Services Won't Start

**Problem**: Services fail to start or crash immediately

**Solutions**:
1. Check logs: `docker-compose logs [service-name]`
2. Verify database is running: `docker-compose ps postgres`
3. Check environment variables: `docker-compose config`
4. Ensure ports are not in use: `lsof -i :8081`

#### Database Connection Errors

**Problem**: Services can't connect to PostgreSQL

**Solutions**:
1. Verify PostgreSQL is healthy: `docker-compose ps postgres`
2. Check database credentials in `.env`
3. Ensure schemas are created: `docker-compose exec postgres psql -U turaf_admin -d turaf -c "\dn"`
4. Check network connectivity: `docker network inspect turaf-network`

#### Authorization Failures

**Problem**: API requests return 401/403 errors

**Solutions**:
1. Verify JWT token is valid
2. Check `X-Organization-Id` and `X-User-Id` headers
3. Ensure user belongs to the organization
4. Review authorization logs: `docker-compose logs identity-service | grep Authorization`

#### Event Publishing Issues

**Problem**: Domain events not being published

**Solutions**:
1. Check LocalStack is running: `curl http://localhost:4566/_localstack/health`
2. Verify EventBridge bus exists: `aws --endpoint-url=http://localhost:4566 events list-event-buses`
3. Check event publisher logs
4. Verify event structure includes all required fields

### Debug Mode

Enable debug logging:

```yaml
# application-debug.yml
logging:
  level:
    com.turaf: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

Run with debug profile:
```bash
docker-compose -f docker-compose.yml -f docker-compose.debug.yml up
```

### Performance Issues

#### Slow Database Queries

1. Enable SQL logging:
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

2. Check query execution plans:
   ```sql
   EXPLAIN ANALYZE SELECT * FROM experiments WHERE organization_id = 'org-123';
   ```

3. Add indexes if needed:
   ```sql
   CREATE INDEX idx_experiments_org_id ON experiments(organization_id);
   ```

#### High Memory Usage

1. Adjust JVM heap size:
   ```yaml
   environment:
     - JAVA_OPTS=-Xms512m -Xmx1024m
   ```

2. Monitor with JConsole or VisualVM
3. Check for memory leaks with heap dumps

---

## Support

### Documentation
- Architecture: `/specs/architecture.md`
- Domain Model: `/specs/domain-model.md`
- API Documentation: Swagger UI at `http://localhost:8080/swagger-ui.html`

### Getting Help
- Review audit reports in `/docs/audits/`
- Check implementation tracker: `/docs/IMPLEMENTATION-TRACKER.md`
- Consult final summary: `/docs/FINAL-IMPLEMENTATION-SUMMARY.md`

---

## License

Copyright © 2026 Turaf Platform. All rights reserved.
