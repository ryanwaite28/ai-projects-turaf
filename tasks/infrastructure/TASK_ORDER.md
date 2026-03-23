# Infrastructure Tasks - Sequential Implementation Order

This document defines the correct sequential order for implementing infrastructure tasks. Tasks are numbered to reflect their implementation dependencies and logical flow.

---

## Task Execution Order

### **Phase 1: AWS Organization Setup** (Tasks 001-003)

#### 001: Verify AWS Organization
- **Objective**: Verify AWS Organization structure and member accounts
- **Dependencies**: None (prerequisite verification)
- **Duration**: 30 minutes
- **Status**: Must be completed first

#### 002: Create Organizational Units
- **Objective**: Create Workloads OU and move accounts
- **Dependencies**: Task 001
- **Duration**: 1 hour
- **Status**: Required before enabling services

#### 003: Enable AWS Services Organization-Wide
- **Objective**: Enable CloudTrail, Config, GuardDuty, Security Hub
- **Dependencies**: Task 002
- **Duration**: 1-2 hours
- **Status**: Required for organization-wide security

---

### **Phase 2: Domain and DNS Configuration** (Tasks 004-006)

#### 004: Configure Route 53 Hosted Zone
- **Objective**: Create hosted zone and delegate DNS from whois.com
- **Dependencies**: Task 001
- **Duration**: 1 hour (+ 24-48 hours DNS propagation)
- **Status**: Critical path - start early

#### 005: Request ACM Certificates
- **Objective**: Request wildcard SSL/TLS certificates
- **Dependencies**: Task 004
- **Duration**: 1 hour (+ 5-30 minutes validation)
- **Status**: Required for HTTPS

#### 006: Configure Email Forwarding
- **Objective**: Set up email aliases at titan.email
- **Dependencies**: None (can run in parallel)
- **Duration**: 30 minutes
- **Status**: Required for AWS account emails

---

### **Phase 3: Security and Access** (Tasks 007-009)

#### 007: Create Service Control Policies
- **Objective**: Implement 5 SCPs for security guardrails
- **Dependencies**: Task 002
- **Duration**: 2 hours
- **Status**: Required for organization security

#### 008: Setup Terraform State Backend
- **Objective**: Create S3 buckets and DynamoDB tables for Terraform state
- **Dependencies**: Task 003
- **Duration**: 1-2 hours
- **Status**: **CRITICAL** - Required before Terraform init

#### 009: Configure IAM OIDC for GitHub Actions
- **Objective**: Create OIDC providers and deployment roles
- **Dependencies**: Task 003
- **Duration**: 2 hours
- **Status**: Required for CI/CD

---

### **Phase 4: Backend Services** (Tasks 010-012)

#### 010: Configure Amazon SES
- **Objective**: Set up email sending with domain verification
- **Dependencies**: Task 004 (Route 53)
- **Duration**: 2 hours (+ 24-48 hours production access)
- **Status**: Required for notification service

#### 011: Create ECR Repositories
- **Objective**: Create container registries for all services
- **Dependencies**: Task 009
- **Duration**: 1 hour
- **Status**: Required before building Docker images

#### 012: Configure GitHub Environments and Secrets
- **Objective**: Set up GitHub environments with secrets
- **Dependencies**: Task 009, 011
- **Duration**: 1.5 hours
- **Status**: Required for CI/CD pipelines

---

### **Phase 5: Terraform Infrastructure** (Tasks 013-021)

#### 013: Setup Terraform Structure
- **Objective**: Create Terraform directory structure and configuration
- **Dependencies**: Task 008 (State Backend)
- **Duration**: 3 hours
- **Status**: ✅ **COMPLETED** - Foundation ready

#### 014: Create Networking Module
- **Objective**: Implement VPC, subnets, NAT, IGW
- **Dependencies**: Task 013
- **Duration**: 4 hours
- **Status**: Foundation for all other modules

#### 015: Create Security Modules
- **Objective**: Implement security groups, IAM roles, KMS
- **Dependencies**: Task 014
- **Duration**: 3 hours
- **Status**: Required before compute resources

#### 016: Create Database Module
- **Objective**: Implement RDS PostgreSQL
- **Dependencies**: Task 014, 015
- **Duration**: 3 hours
- **Status**: Required for application services

#### 017: Create Storage Modules
- **Objective**: Implement S3 buckets for reports and uploads
- **Dependencies**: Task 014, 015
- **Duration**: 2 hours
- **Status**: Required for file storage

#### 018: Create Messaging Modules
- **Objective**: Implement EventBridge and SQS
- **Dependencies**: Task 014, 015
- **Duration**: 3 hours
- **Status**: Required for event-driven architecture

#### 019: Create Compute Modules
- **Objective**: Implement ECS, ALB, Auto Scaling
- **Dependencies**: Task 014, 015, 016, 017, 018
- **Duration**: 4 hours
- **Status**: Core application infrastructure

#### 020: Create Lambda Module
- **Objective**: Implement Lambda functions for reporting/notifications
- **Dependencies**: Task 014, 015, 017, 018
- **Duration**: 3 hours
- **Status**: Required for serverless components

#### 021: Create Monitoring Modules
- **Objective**: Implement CloudWatch, X-Ray
- **Dependencies**: Task 014
- **Duration**: 3 hours
- **Status**: Required for observability

---

### **Phase 6: Environment Configuration** (Tasks 022-024)

#### 022: Configure DEV Environment
- **Objective**: Deploy infrastructure to dev account
- **Dependencies**: Tasks 013-021
- **Duration**: 2 hours
- **Status**: First environment deployment

#### 023: Configure QA Environment
- **Objective**: Deploy infrastructure to QA account
- **Dependencies**: Task 022
- **Duration**: 2 hours
- **Status**: Second environment deployment

#### 024: Configure PROD Environment
- **Objective**: Deploy infrastructure to production account
- **Dependencies**: Task 023
- **Duration**: 3 hours
- **Status**: Production deployment

---

### **Phase 7: Database Initialization** (Task 025)

#### 025: Setup Database Schemas
- **Objective**: Create all database schemas and tables
- **Dependencies**: Task 022 (dev environment deployed)
- **Duration**: 4 hours
- **Status**: Required before application deployment

---

## Task Dependency Graph

```
001 (Verify AWS Org)
 ├─→ 002 (Create OUs)
 │    ├─→ 003 (Enable Services)
 │    │    ├─→ 007 (SCPs)
 │    │    ├─→ 008 (Terraform State) → 013 (Terraform Structure)
 │    │    └─→ 009 (IAM OIDC)
 │    │         ├─→ 011 (ECR)
 │    │         └─→ 012 (GitHub Secrets)
 │    └─→ 004 (Route 53)
 │         ├─→ 005 (ACM Certs)
 │         └─→ 010 (SES)
 └─→ 006 (Email Forwarding)

013 (Terraform Structure)
 └─→ 014 (Networking)
      ├─→ 015 (Security)
      │    ├─→ 016 (Database)
      │    ├─→ 017 (Storage)
      │    ├─→ 018 (Messaging)
      │    ├─→ 019 (Compute) [depends on 016, 017, 018]
      │    └─→ 020 (Lambda) [depends on 017, 018]
      └─→ 021 (Monitoring)

014-021 (All Modules)
 └─→ 022 (DEV Environment)
      └─→ 023 (QA Environment)
           └─→ 024 (PROD Environment)
                └─→ 025 (Database Schemas)
```

---

## Critical Path

The critical path for infrastructure deployment:

1. **001** → **002** → **003** → **008** (Terraform State Backend)
2. **008** → **013** (Terraform Structure)
3. **013** → **014** (Networking) → **015** (Security)
4. **015** → **016-021** (All other modules)
5. **016-021** → **022** (DEV deployment)
6. **022** → **025** (Database schemas)

**Estimated Total Time**: 3-4 weeks for complete infrastructure setup

---

## Parallel Execution Opportunities

Tasks that can be executed in parallel:

### **Week 1: Foundation**
- **Parallel Track 1**: 001 → 002 → 003 → 007
- **Parallel Track 2**: 004 → 005 (wait for DNS propagation)
- **Parallel Track 3**: 006 (independent)

### **Week 2: Security & Backend**
- **Parallel Track 1**: 008 (Terraform State)
- **Parallel Track 2**: 009 (IAM OIDC) → 011 (ECR) → 012 (GitHub)
- **Parallel Track 3**: 010 (SES - wait for production access)

### **Week 3: Terraform Modules**
- **Sequential**: 013 → 014 → 015
- **Parallel after 015**: 016, 017, 018, 021
- **After 016-018**: 019, 020

### **Week 4: Deployment**
- **Sequential**: 022 → 023 → 024 → 025

---

## Task Status Tracking

| Task | Name | Status | Completed Date |
|------|------|--------|----------------|
| 001 | Verify AWS Organization | ⏳ Pending | - |
| 002 | Create Organizational Units | ⏳ Pending | - |
| 003 | Enable AWS Services | ⏳ Pending | - |
| 004 | Configure Route 53 | ⏳ Pending | - |
| 005 | Request ACM Certificates | ⏳ Pending | - |
| 006 | Configure Email Forwarding | ⏳ Pending | - |
| 007 | Create Service Control Policies | ⏳ Pending | - |
| 008 | Setup Terraform State Backend | ⏳ Pending | - |
| 009 | Configure IAM OIDC | ⏳ Pending | - |
| 010 | Configure Amazon SES | ⏳ Pending | - |
| 011 | Create ECR Repositories | ⏳ Pending | - |
| 012 | Configure GitHub Secrets | ⏳ Pending | - |
| 013 | Setup Terraform Structure | ✅ Completed | 2024-03-23 |
| 014 | Create Networking Module | ⏳ Pending | - |
| 015 | Create Security Modules | ⏳ Pending | - |
| 016 | Create Database Module | ⏳ Pending | - |
| 017 | Create Storage Modules | ⏳ Pending | - |
| 018 | Create Messaging Modules | ⏳ Pending | - |
| 019 | Create Compute Modules | ⏳ Pending | - |
| 020 | Create Lambda Module | ⏳ Pending | - |
| 021 | Create Monitoring Modules | ⏳ Pending | - |
| 022 | Configure DEV Environment | ⏳ Pending | - |
| 023 | Configure QA Environment | ⏳ Pending | - |
| 024 | Configure PROD Environment | ⏳ Pending | - |
| 025 | Setup Database Schemas | ⏳ Pending | - |

---

## Next Task to Execute

**Task 001: Verify AWS Organization**

This is the first task in the sequence and has no dependencies. It verifies that the AWS Organization structure is correctly set up before proceeding with any infrastructure changes.

---

## Notes

- **DNS Propagation**: Tasks 004-005 require 24-48 hours for DNS propagation
- **SES Production Access**: Task 010 requires 24-48 hours for AWS approval
- **Terraform State**: Task 008 MUST be completed before Task 013 can run `terraform init`
- **Environment Order**: Always deploy dev → qa → prod to validate changes
- **Rollback**: Each task should be reversible in case of issues

---

**Last Updated**: 2024-03-23  
**Total Tasks**: 25  
**Completed**: 1 (4%)  
**Remaining**: 24 (96%)
