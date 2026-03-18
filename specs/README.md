# Turaf Platform Specifications

This directory contains detailed specifications extracted from PROJECT.md to enable incremental, AI-assisted implementation of the Turaf SaaS platform.

---

## Overview

These specifications break down the comprehensive PROJECT.md into focused, human- and AI-friendly documents organized by concern. Each specification file contains detailed requirements, schemas, and implementation guidance while preserving all architecture decisions and technology choices from the authoritative design document.

---

## Specification Files

### Architecture Specifications

**`architecture.md`**
- System architecture overview (event-driven microservices)
- Clean Architecture layer structure and dependency rules
- Domain-Driven Design principles and bounded contexts
- Multi-tenant architecture strategy
- SOLID principles application
- Scalability and reliability strategies

**`event-flow.md`**
- Event envelope standard structure
- Domain event definitions and payloads
- Event flow patterns and sequences
- Event-driven workflow examples
- EventBridge integration patterns
- Idempotency and retry strategies

**`domain-model.md`**
- Core entities and aggregates (Organization, User, Problem, Hypothesis, Experiment, Metric, Report)
- Value objects (OrganizationId, UserId, MetricValue, etc.)
- Entity relationships and invariants
- Repository interfaces
- Domain logic and business rules
- Aggregate design rationale

---

### Microservice Specifications

**`identity-service.md`**
- User authentication and authorization
- JWT token management
- API endpoints (register, login, refresh, logout)
- Database schema (users, user_roles, refresh_tokens)
- Security configuration (BCrypt, JWT)
- Password management

**`organization-service.md`**
- Organization lifecycle management
- Membership management
- Multi-tenant context management
- API endpoints (CRUD, members, settings)
- Database schema (organizations, organization_members, organization_settings)
- Authorization and tenant isolation

**`experiment-service.md`**
- Problem, hypothesis, and experiment management
- Experiment state machine (DRAFT → RUNNING → COMPLETED)
- API endpoints (problems, hypotheses, experiments)
- Database schema (problems, hypotheses, experiments, experiment_results)
- Domain events (ProblemCreated, ExperimentStarted, ExperimentCompleted)

**`metrics-service.md`**
- Metric ingestion and validation
- Metric aggregation (avg, sum, count, min, max)
- API endpoints (record, query, aggregate)
- Database schema (metrics, metric_aggregations)
- Batch processing and time-series queries

**`reporting-service.md`**
- Event-driven report generation (AWS Lambda - Python 3.11)
- Report workflow (fetch data, calculate aggregations, generate PDF)
- S3 storage structure
- Event handlers (ExperimentCompleted)
- Template engine (Thymeleaf)

**`notification-service.md`**
- Multi-channel notifications (email, webhooks)
- Event handlers (ExperimentCompleted, ReportGenerated, MemberAdded)
- Amazon SES integration
- Webhook delivery with retry logic
- Notification preferences

---

### Infrastructure Specifications

**`aws-infrastructure.md`**
- Complete AWS infrastructure components
- Compute (ECS Fargate, Lambda)
- Networking (VPC, subnets, security groups, ALB)
- Data storage (RDS PostgreSQL, S3 buckets)
- Messaging (EventBridge, SQS)
- Security (IAM, Secrets Manager, KMS, WAF)
- Monitoring (CloudWatch, X-Ray)
- DNS & CDN (Route 53, CloudFront)

**`terraform-structure.md`**
- Terraform module organization
- Module specifications (networking, compute, database, storage, messaging, lambda, monitoring, security)
- Environment configurations (DEV, QA, PROD)
- State management (S3 backend with DynamoDB locking)
- Variable structure and best practices

---

### Frontend Specifications

**`angular-frontend.md`**
- Angular 17.x application architecture
- Feature module specifications (auth, dashboard, problems, experiments, metrics, reports)
- State management (NgRx)
- API integration with interceptors
- Routing configuration
- UI/UX design system
- Testing strategy

---

### DevOps Specifications

**`ci-cd-pipelines.md`**
- GitHub Actions workflow structure
- CI pipeline (lint, test, build, code quality, security scan)
- CD pipelines per environment (DEV, QA, PROD)
- Infrastructure deployment pipeline
- Blue-green deployment strategy
- AWS OIDC authentication
- Smoke testing and E2E testing

**`event-schemas.md`**
- Event envelope standard
- Complete event schema definitions:
  - ProblemCreated
  - HypothesisCreated
  - ExperimentStarted
  - MetricRecorded
  - ExperimentCompleted
  - ReportGenerated
  - OrganizationCreated
  - MemberAdded
- Event versioning strategy
- Event validation rules

---

## Usage

### For AI-Assisted Development

1. **Read Specifications**: Start with `architecture.md` to understand the overall system
2. **Select Service**: Choose a microservice specification to implement
3. **Follow Clean Architecture**: Implement domain → application → infrastructure → interfaces
4. **Respect Boundaries**: Never violate service boundaries or dependency rules
5. **Publish Events**: Emit domain events for all significant state changes
6. **Test Thoroughly**: Write unit, integration, and API tests

### For Manual Development

1. **Reference Authority**: PROJECT.md remains the authoritative source
2. **Use Specs as Guide**: These specs provide focused implementation guidance
3. **Maintain Consistency**: Follow naming conventions and patterns across specs
4. **Update Specs**: If PROJECT.md changes, regenerate affected specs

---

## Specification Principles

All specifications follow these principles:

- **No Code**: Specifications contain no implementation code, only requirements
- **Focused**: Each file addresses a single concern or service
- **Complete**: All necessary information extracted from PROJECT.md
- **Consistent**: Architecture, service boundaries, and tech stack preserved
- **Traceable**: All specs reference PROJECT.md sections

---

## Technology Stack Summary

**Backend**: Java 17, Spring Boot 3.x, Maven  
**Frontend**: Angular 17.x, NgRx, Angular Material/PrimeNG  
**Database**: PostgreSQL (RDS)  
**Event Bus**: AWS EventBridge  
**Compute**: ECS Fargate (Java services), AWS Lambda (Python 3.11)  
**Storage**: Amazon S3  
**IaC**: Terraform  
**CI/CD**: GitHub Actions  
**Monitoring**: CloudWatch, X-Ray  

---

## Next Steps

After reviewing these specifications, proceed to:

1. **Generate Task Lists**: Break each spec into granular implementation tasks (Step 3 of workflow)
2. **Begin Implementation**: Start with Identity Service, then Organization Service (Step 4 of workflow)
3. **Follow Build Order**: Identity → Organization → Experiment → Metrics → Event Infrastructure → Reporting → Notification → Frontend → DevOps

---

## References

- **PROJECT.md**: Authoritative system design document
- **Workflow**: `.windsurf/workflows/project.md` - AI workflow for implementation
- **Implementation Plan**: `.windsurf/plans/turaf-implementation-plan-a4c6b0.md` - Detailed implementation roadmap

---

## Specification Metadata

**Generated**: 2024-03-14  
**Source**: PROJECT.md (1913 lines)  
**Total Specs**: 14 files  
**Total Lines**: ~5000+ lines of specifications  
**Coverage**: 100% of PROJECT.md architecture, services, infrastructure, and DevOps  

---

**Note**: These specifications are living documents. As PROJECT.md evolves or implementation reveals new requirements, specs should be updated to maintain alignment with the authoritative design.
