# Turaf - Event-Driven SaaS Platform: Problem Tracking + Solution Validation

### Turaf = Try Until Results Are Found.

A production-style SaaS platform designed to demonstrate principal-level software engineering, system design, and DevOps practices.

**GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf

---

## Quick Links

### Core Documentation
- **[PROJECT.md](PROJECT.md)** - Complete project specifications and architecture
- **[AWS_ACCOUNTS.md](AWS_ACCOUNTS.md)** - AWS account details and organization structure
- **[GITHUB.md](GITHUB.md)** - GitHub workflows, CI/CD, and DevOps best practices

### Architecture & Strategy
- **[AWS Multi-Account Strategy](docs/AWS_MULTI_ACCOUNT_STRATEGY.md)** - Multi-account architecture rationale and implementation
- **[Technical Specifications](specs/)** - Detailed service and infrastructure specs
- **[Implementation Tasks](tasks/)** - Step-by-step implementation breakdown

---

## AWS Infrastructure

This project uses a **multi-account AWS architecture** for security isolation and environment separation:

| Environment | AWS Account ID | Purpose |
|-------------|---------------|---------|
| **DEV** | 801651112319 | Development and feature testing |
| **QA** | 965932217544 | Integration testing and staging |
| **PROD** | 811783768245 | Production workloads |
| **Ops** | 146072879609 | DevOps tooling and CI/CD |

**Organization**: `o-l3zk5a91yj` (Root: 072456928432)

See [AWS_ACCOUNTS.md](AWS_ACCOUNTS.md) for complete details.

---

## Repository Structure

```
ai-projects-turaf/
├── services/               # Backend microservices (Spring Boot)
│   ├── identity-service/
│   ├── organization-service/
│   ├── experiment-service/
│   ├── metrics-service/
│   ├── reporting-service/
│   └── notification-service/
├── frontend/               # Angular web application
├── infrastructure/         # Terraform infrastructure as code
├── libs/                   # Shared libraries and domain models
├── .github/workflows/      # GitHub Actions CI/CD pipelines
├── docs/                   # Architecture documentation
├── specs/                  # Technical specifications
└── tasks/                  # Implementation task breakdown
```

---

## CI/CD Pipeline

**Platform**: GitHub Actions with AWS OIDC authentication

**Branch Strategy**:
- `main` → Production (PROD account: 811783768245)
- `develop` → Development (DEV account: 801651112319)
- `release/*` → QA/Staging (QA account: 965932217544)

See [GITHUB.md](GITHUB.md) for complete CI/CD documentation.

---

## Getting Started

### Prerequisites

1. **AWS Account Access**: Access to DEV account (801651112319) for development
2. **GitHub Repository**: Clone https://github.com/ryanwaite28/ai-projects-turaf
3. **Required Tools**:
   - Java 17+ (for backend services)
   - Node.js 20+ (for frontend)
   - Terraform 1.5+ (for infrastructure)
   - Docker (for containerization)
   - AWS CLI (for AWS operations)

### Local Development Setup

```bash
# Clone repository
git clone https://github.com/ryanwaite28/ai-projects-turaf.git
cd ai-projects-turaf

# Backend setup (example for experiment-service)
cd services/experiment-service
./mvnw clean install
./mvnw spring-boot:run

# Frontend setup
cd frontend
npm install
npm start

# Infrastructure setup
cd infrastructure/environments/dev
terraform init
terraform plan
```

### Deployment

Deployments are automated via GitHub Actions:

- **DEV**: Automatic on push to `develop`
- **QA**: Automatic on push to `release/*`
- **PROD**: Manual workflow dispatch with approval

---

## Project Overview

This platform enables teams to **identify problems, form hypotheses, run experiments, and validate solutions using measurable metrics**.

### Core Features

- **Problem Management**: Define and track problems
- **Hypothesis Tracking**: Create testable hypotheses
- **Experiment Execution**: Run structured experiments
- **Metrics Collection**: Record quantitative metrics
- **Report Generation**: Automated validation reports

### Technical Highlights

- **Event-Driven Architecture**: Domain events via Amazon EventBridge
- **Multi-Tenant SaaS**: Organization-based isolation
- **Microservices**: Spring Boot services on Amazon ECS
- **Serverless Processing**: AWS Lambda for async workflows
- **Infrastructure as Code**: Terraform for all AWS resources
- **CI/CD Automation**: GitHub Actions with OIDC authentication

---

## Documentation

### Architecture
- [System Architecture](specs/architecture.md)
- [Domain Model](specs/domain-model.md)
- [Event Schemas](specs/event-schemas.md)
- [Event Flow](specs/event-flow.md)

### Infrastructure
- [AWS Infrastructure](specs/aws-infrastructure.md)
- [Terraform Structure](specs/terraform-structure.md)
- [CI/CD Pipelines](specs/ci-cd-pipelines.md)

### Services
- [Identity Service](specs/identity-service.md)
- [Organization Service](specs/organization-service.md)
- [Experiment Service](specs/experiment-service.md)
- [Metrics Service](specs/metrics-service.md)
- [Reporting Service](specs/reporting-service.md)
- [Notification Service](specs/notification-service.md)

---

## Contributing

This is a portfolio project demonstrating principal-level engineering practices. For contribution guidelines, see [GITHUB.md](GITHUB.md).

---

## License

This project is for portfolio demonstration purposes.

---

For complete project specifications, see [PROJECT.md](PROJECT.md).