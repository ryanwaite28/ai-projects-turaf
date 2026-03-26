# Turaf Documentation Index

**Last Updated**: March 25, 2026

This is the master index for all Turaf platform documentation. Use this as your starting point for finding information.

---

## 🚀 Getting Started

**New to the project?** Start here:
1. [README.md](README.md) - Project overview
2. [PROJECT.md](PROJECT.md) - Complete system design (authoritative source)
3. [BEST_PRACTICES.md](BEST_PRACTICES.md) - AI-assisted development best practices
4. [LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md) - Set up local environment
5. [AWS_ACCOUNTS.md](AWS_ACCOUNTS.md) - AWS account structure

---

## 📐 Architecture & Design

### Core Specifications (`/specs/`)
- [Architecture Overview](specs/architecture.md)
- [Event Flow](specs/event-flow.md)
- [Domain Model](specs/domain-model.md)
- [Event Schemas](specs/event-schemas.md)
- [AWS Infrastructure](specs/aws-infrastructure.md)
- [CI/CD Pipelines](specs/ci-cd-pipelines.md)
- [Testing Strategy](specs/testing-strategy.md)

### Security & Access (`/docs/`)
- [IAM Roles Reference](docs/IAM_ROLES.md) - **Authoritative source for all IAM role names and ARNs** 
- [Identity Service](specs/identity-service.md) - Authentication and user management
- [Organization Service](specs/organization-service.md) - Multi-tenant organization management
- [Experiment Service](specs/experiment-service.md) - A/B testing and experimentation
- [Metrics Service](specs/metrics-service.md) - Metrics collection and analysis
- [Reporting Service](specs/reporting-service.md) - Report generation
- [Notification Service](specs/notification-service.md) - Multi-channel notifications
- [Communications Service](specs/communications-service.md) - Email/SMS delivery
- [BFF API](specs/bff-api.md) - Backend for Frontend
- [WebSocket Gateway](specs/ws-gateway.md) - Real-time communication

### Frontend
- [Angular Frontend](specs/angular-frontend.md) - SPA architecture and components

---

## 🏗️ Infrastructure

### Main Documentation (`/infrastructure/docs/`)
- [Infrastructure Documentation Index](infrastructure/docs/README.md) - Start here for infrastructure

### Planning
- [Infrastructure Plan](infrastructure/docs/planning/INFRASTRUCTURE_PLAN.md) - Complete architecture
- [Infrastructure Costs](infrastructure/docs/planning/INFRASTRUCTURE_COSTS.md) - Cost analysis

### Deployment
- [Deployment Guide](infrastructure/docs/deployment/DEPLOYMENT_GUIDE.md) - How to deploy
- [DEV Infrastructure Status](infrastructure/docs/deployment/DEV_INFRASTRUCTURE_STATUS.md) - Current state
- [Deployment Summary](infrastructure/docs/deployment/DEPLOYMENT_SUMMARY.md) - Recent deployments
- [Deployed Infrastructure](infrastructure/docs/deployment/DEPLOYED_INFRASTRUCTURE.md) - Resource inventory

### CI/CD
- [CI/CD Pipelines Spec](specs/ci-cd-pipelines.md) - Complete CI/CD architecture
- [CI/CD Infrastructure Guide](infrastructure/docs/cicd/CICD_INFRASTRUCTURE_GUIDE.md) - Prerequisites
- [CI/CD Prerequisites Status](infrastructure/docs/cicd/CICD_PREREQUISITES_STATUS.md) - Readiness
- [Service Deployment Pattern](.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md) - How services deploy

### Architecture Details
- [AWS Infrastructure Spec](specs/aws-infrastructure.md) - AWS resources
- [Terraform Structure](specs/terraform-structure.md) - IaC organization
- [Compute Infrastructure Analysis](infrastructure/docs/architecture/COMPUTE_INFRASTRUCTURE_ANALYSIS.md) - ECS/ALB
- [Infrastructure Restructure Summary](infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md) - Recent changes

---

## ✅ Implementation Tasks

### Task Overview (`/tasks/`)
- [Task Index](tasks/README.md) - Overview of all tasks
- [Task Summary](tasks/TASK_SUMMARY.md) - Progress tracking
- [Completion Summary](tasks/COMPLETION_SUMMARY.md) - What's done

### Tasks by Component
- [Infrastructure Tasks](tasks/infrastructure/) - AWS infrastructure setup
- [Identity Service Tasks](tasks/identity-service/) - Auth service implementation
- [Organization Service Tasks](tasks/organization-service/) - Org service implementation
- [Experiment Service Tasks](tasks/experiment-service/) - Experiment service implementation
- [Metrics Service Tasks](tasks/metrics-service/) - Metrics service implementation
- [Reporting Service Tasks](tasks/reporting-service/) - Reporting service implementation
- [Notification Service Tasks](tasks/notification-service/) - Notification service implementation
- [Communications Service Tasks](tasks/communications-service/) - Communications service implementation
- [BFF API Tasks](tasks/bff-api/) - BFF implementation
- [Frontend Tasks](tasks/frontend/) - Angular app implementation
- [CI/CD Tasks](tasks/cicd/) - Pipeline setup
- [Events Tasks](tasks/events/) - Event infrastructure

---

## 🔧 Operations

### Development
- [Local Development Setup](docs/LOCAL_DEVELOPMENT.md) - Docker Compose environment
- [Database Setup](docs/DATABASE_SETUP.md) - Database configuration
- [Database Architecture](docs/DATABASE_ARCHITECTURE_IMPLEMENTATION.md) - Multi-schema design

### Deployment & Operations
- [Deployment Runbook](docs/DEPLOYMENT_RUNBOOK.md) - Deployment procedures
- [Operational Runbooks](docs/operations/RUNBOOKS.md) - Standard operating procedures
- [Multi-Account Strategy](docs/AWS_MULTI_ACCOUNT_STRATEGY.md) - AWS account design
- [Implementation Summary](docs/IMPLEMENTATION_SUMMARY.md) - What's implemented

### Troubleshooting & Support
- [Common Issues](docs/troubleshooting/COMMON_ISSUES.md) - Solutions to frequent problems
- [Infrastructure Troubleshooting](docs/troubleshooting/COMMON_ISSUES.md#infrastructure-issues)
- [CI/CD Troubleshooting](docs/troubleshooting/COMMON_ISSUES.md#cicd-issues)
- [Database Troubleshooting](docs/troubleshooting/COMMON_ISSUES.md#database-issues)

### API Documentation
- [API Documentation Index](docs/api/README.md) - Overview of all service APIs
- [API Standards](docs/api/README.md#api-standards) - Authentication, error handling, pagination
- [OpenAPI Specifications](docs/api/README.md#api-documentation-generation) - How to generate API specs

### GitHub & CI/CD
- [GitHub Configuration](GITHUB.md) - Repository setup and workflows

---

## 📝 Decision Records

### Architectural Decision Records (`/docs/adr/`)
- [ADR-006: Single Database Multi-Schema](docs/adr/ADR-006-single-database-multi-schema.md)

---

## 📊 Change History

### Changelog (`/changelog/`)
- [2026-03-25: Infrastructure Reset](changelog/2026-03-25-infrastructure-reset.md)
- [2026-03-23: Centralized Flyway Migration](changelog/2026-03-23-centralized-flyway-migration-service.md)
- [2026-03-23: Infrastructure Cost Optimization](changelog/2026-03-23-infrastructure-cost-optimization.md)
- [2026-03-21: Add Communications Component](changelog/2026-03-21-add-communications-component.md)

---

## 🤖 AI Plans & Assessments

### Active Plans (`/.windsurf/plans/active/`)
- [Documentation Assessment](docs/assessments/workspace-docs/20260325_182700.assessment.md)
- [Documentation Improvement Plan](.windsurf/plans/active/assessments/workspace-docs/20260325_182700.plan.md)

### Completed Plans (`/.windsurf/plans/completed/`)
- [Infrastructure Plans](.windsurf/plans/completed/infrastructure-restructure/) - Infrastructure refactoring
- [CI/CD Plans](.windsurf/plans/completed/cicd/) - CI/CD pattern evolution
- [Architecture Plans](.windsurf/plans/completed/architecture/) - Major architectural changes

### Plans Index
- [Plans README](.windsurf/plans/README.md) - Complete plan organization

---

## 🔍 Quick Reference

### AWS Accounts
| Environment | Account ID | Purpose |
|-------------|------------|---------|
| **Root** | 072456928432 | AWS Organizations management |
| **Ops** | 146072879609 | DevOps tooling, CI/CD |
| **DEV** | 801651112319 | Development environment |
| **QA** | 965932217544 | QA/Staging environment |
| **PROD** | 811783768245 | Production environment |

### Key Technologies
- **Backend**: Java 17, Spring Boot 3.x
- **Frontend**: Angular 17+
- **Infrastructure**: AWS, Terraform
- **CI/CD**: GitHub Actions
- **Database**: PostgreSQL (multi-schema), Redis
- **Messaging**: EventBridge, SQS
- **Observability**: CloudWatch, X-Ray

### Important Links
- **GitHub Repository**: https://github.com/ryanwaite28/ai-projects-turaf
- **Domain**: turafapp.com
- **API Endpoints**: 
  - DEV: `api.dev.turafapp.com`
  - QA: `api.qa.turafapp.com`
  - PROD: `api.turafapp.com`

---

## 📚 Documentation Organization

```
/
├── PROJECT.md                    # Authoritative system design
├── README.md                     # Project overview
├── DOCUMENTATION_INDEX.md        # This file
├── AWS_ACCOUNTS.md               # Account structure
├── GITHUB.md                     # GitHub setup
│
├── specs/                        # Specifications
│   ├── architecture.md
│   ├── ci-cd-pipelines.md
│   ├── [service]-service.md
│   └── archive/                  # Old spec versions
│
├── tasks/                        # Implementation tasks
│   ├── [component]/
│   └── README.md
│
├── docs/                         # General documentation
│   ├── LOCAL_DEVELOPMENT.md
│   ├── DEPLOYMENT_RUNBOOK.md
│   ├── adr/                      # Decision records
│   ├── assessments/              # Quality assessments
│   ├── api/                      # API documentation
│   │   └── README.md
│   ├── troubleshooting/          # Problem solving
│   │   └── COMMON_ISSUES.md
│   └── operations/               # Operational runbooks
│       └── RUNBOOKS.md
│
├── infrastructure/
│   └── docs/                     # Infrastructure docs
│       ├── README.md
│       ├── planning/
│       ├── deployment/
│       ├── cicd/
│       └── architecture/
│
├── changelog/                    # Change tracking
│
└── .windsurf/
    ├── plans/                    # AI plans
    │   ├── active/
    │   ├── completed/
    │   └── archived/
    └── workflows/                # AI workflows
        └── project.md
```

---

## 🎯 Navigation Tips

### Finding Information
- **Architecture questions**: Start with `PROJECT.md` or `specs/architecture.md`
- **Service details**: Check `specs/[service]-service.md`
- **Implementation tasks**: Look in `tasks/[component]/`
- **Infrastructure**: Go to `infrastructure/docs/README.md`
- **Deployment**: See `infrastructure/docs/deployment/` or `docs/operations/RUNBOOKS.md`
- **CI/CD**: Check `specs/ci-cd-pipelines.md` and `infrastructure/docs/cicd/`
- **Troubleshooting**: See `docs/troubleshooting/COMMON_ISSUES.md`
- **API documentation**: Check `docs/api/README.md`
- **Operational procedures**: See `docs/operations/RUNBOOKS.md`
- **Recent changes**: Review `changelog/`
- **Architectural decisions**: See `docs/adr/`

### Using Your IDE
- **File search**: Cmd/Ctrl+P to quickly find documents
- **Text search**: Cmd/Ctrl+Shift+F to search across all files
- **Follow links**: Click on file paths in markdown to navigate

### For AI Assistants
- **Read first**: [BEST_PRACTICES.md](BEST_PRACTICES.md) for efficient AI workflow
- Always start with `PROJECT.md` for authoritative context
- Check this index to locate relevant documentation
- Review recent changelog entries for context on changes
- Check `.windsurf/plans/completed/` for implementation patterns
- Use status metadata and cross-references for context gathering

---

## 🆘 Need Help?

1. **System Design**: Read [PROJECT.md](PROJECT.md) - it's the single source of truth
2. **Getting Started**: Follow [LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md)
3. **Troubleshooting**: Check [Common Issues](docs/troubleshooting/COMMON_ISSUES.md) first
4. **Operations**: See [Runbooks](docs/operations/RUNBOOKS.md) for procedures
5. **Infrastructure**: Start with [Infrastructure Docs](infrastructure/docs/README.md)
6. **API Questions**: See [API Documentation](docs/api/README.md)
7. **Tasks**: Check [Task Summary](tasks/TASK_SUMMARY.md) for what's done
8. **Recent Changes**: Review [Changelog](changelog/)
9. **Decisions**: Read [ADRs](docs/adr/) for architectural context

---

**Last Updated**: March 25, 2026  
**Maintained By**: AI-assisted development workflow  
**Authoritative Source**: [PROJECT.md](PROJECT.md)
