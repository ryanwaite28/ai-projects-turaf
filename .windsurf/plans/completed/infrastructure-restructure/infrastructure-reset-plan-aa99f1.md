# Infrastructure Reset & Standardization Plan

Comprehensive cleanup and rebuild of Turaf infrastructure to achieve consistent, reproducible, cost-effective multi-environment deployment using Terraform with demo-ready DEV environment.

---

## Executive Summary

**Objective**: Reset and standardize infrastructure to eliminate drift, consolidate ad-hoc scripts, and establish reproducible Terraform-based deployments aligned with PROJECT.md specifications.

**Approach**: Complete teardown of DEV application resources, rebuild with demo-ready configuration, standardize Terraform modules, archive experimental scripts, and prepare QA/PROD for on-demand deployment.

**Timeline**: 3-4 days  
**Cost Impact**: DEV ~$180-220/month (demo-ready), QA/PROD $0 (configured but not deployed)

---

## Current State Assessment

### Problems Identified

1. **Infrastructure Drift**: 26+ ad-hoc scripts created during troubleshooting, many duplicative or experimental
2. **Inconsistent State**: Partial deployments, failed resources, Terraform state conflicts
3. **Module Configuration Errors**: ECS deployment blocks, Lambda dependencies, monitoring type mismatches
4. **Resource Conflicts**: Old VPC/RDS resources blocking new deployments
5. **Cost Inefficiency**: NAT Gateways deployed but not needed, resources running without purpose
6. **Documentation Gap**: Tasks completed but actual state differs from specs

### Resources to Preserve (Core Infrastructure)

- ✅ AWS Organization structure (5 accounts)
- ✅ IAM OIDC roles for GitHub Actions (all accounts)
- ✅ Service Control Policies (SCPs)
- ✅ Terraform state S3 buckets + DynamoDB locks (all accounts)
- ✅ ECR repositories (all accounts)
- ✅ Route53 hosted zone (turafapp.com)
- ✅ ACM certificates (wildcard *.turafapp.com)
- ✅ Flyway IAM roles (cross-account)
- ✅ CodeBuild Flyway migration projects

### Resources to Destroy (Application Infrastructure)

- ❌ All DEV VPC and networking (subnets, NAT gateways, VPC endpoints, route tables)
- ❌ All DEV RDS instances and DB subnet groups
- ❌ All DEV ECS clusters, services, task definitions
- ❌ All DEV ALB, target groups, listeners
- ❌ All DEV S3 application buckets (keep state bucket)
- ❌ All DEV Secrets Manager secrets (will be recreated)
- ❌ All DEV Lambda functions
- ❌ All DEV EventBridge rules and SQS queues
- ❌ All DEV CloudWatch log groups, alarms, dashboards
- ❌ All DEV security groups (except those managed by core resources)
- ❌ All DEV KMS keys (except state bucket key)

---

## Target Architecture - Demo-Ready DEV Environment

### Infrastructure Configuration (~$180-220/month)

#### Networking (~$50-65/month)
- **VPC**: 10.0.0.0/16
- **Subnets**: 3 AZs (us-east-1a/b/c)
  - Public: 10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24
  - Private: 10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24
  - Database: 10.0.21.0/24, 10.0.22.0/24, 10.0.23.0/24
- **NAT Gateway**: 1 gateway in us-east-1a (single AZ for cost optimization)
- **VPC Endpoints**: 
  - Interface: ECR API, ECR DKR, Secrets Manager, Logs, ECS, ECS Telemetry
  - Gateway: S3

#### Compute (~$80-100/month)
- **ECS Cluster**: turaf-cluster-dev
- **Launch Type**: Fargate (Standard, not Spot for demo reliability)
- **Services**: 3 core services
  - identity-service: 1 task, 0.5 vCPU, 1GB RAM
  - organization-service: 1 task, 0.5 vCPU, 1GB RAM
  - experiment-service: 1 task, 0.5 vCPU, 1GB RAM
- **ALB**: Internet-facing, HTTPS with ACM certificate
- **Target Groups**: Path-based routing per service

#### Database & Caching (~$30-40/month)
- **RDS PostgreSQL**: db.t3.micro, 20GB, single-AZ
  - Multi-schema: identity_schema, organization_schema, experiment_schema, metrics_schema
  - Service users: identity_user, organization_user, experiment_user, metrics_user
  - Backup retention: 7 days
- **ElastiCache Redis**: cache.t3.micro, 1 node
  - For session caching and rate limiting
  - Snapshot retention: 1 day

#### Messaging & Events (~$2-5/month)
- **EventBridge**: Custom event bus (turaf-events-dev)
- **SQS Queues**:
  - experiment-events-queue
  - notification-queue
  - reporting-queue
  - Dead letter queues for each
- **Event Rules**: 7 rules for domain events

#### Lambda Functions (~$5/month)
- **reporting-service**: Python 3.11, 1024MB, 60s timeout
  - Trigger: ExperimentCompleted event
  - Generates PDF reports, stores in S3
- **notification-service**: Python 3.11, 512MB, 30s timeout
  - Triggers: ExperimentCompleted, ReportGenerated, MemberAdded
  - Sends emails via SES

#### Storage (~$5-10/month)
- **S3 Buckets**:
  - turaf-dev-{account-id}: Primary application bucket
  - turaf-reports-dev-{account-id}: Generated reports
- **Versioning**: Enabled on reports bucket
- **Lifecycle**: 90-day transition to IA, 365-day expiration

#### Monitoring & Observability (~$8-12/month)
- **CloudWatch Logs**: 7-day retention for all services
- **CloudWatch Dashboards**: 3 custom dashboards
  - ECS Services Dashboard
  - Database Performance Dashboard
  - Lambda Execution Dashboard
- **CloudWatch Alarms**: 10 alarms
  - ECS CPU/Memory high
  - RDS connections/CPU high
  - ALB 5xx errors
  - Lambda errors/throttles
- **X-Ray Tracing**: Enabled for ECS services and Lambda
- **Container Insights**: Enabled for ECS cluster

#### Security
- **KMS Keys**: 
  - RDS encryption key
  - S3 encryption key
  - Secrets Manager encryption key
- **Secrets Manager**: 6 secrets
  - DB admin password
  - 4 service user passwords
  - Redis auth token
- **Security Groups**: 5 groups
  - ALB, ECS tasks, RDS, ElastiCache, Lambda

---

## Implementation Plan

### Phase 1: Preparation & Cleanup (Day 1, 4-6 hours)

#### 1.1 Archive Ad-Hoc Scripts
- **Action**: Move all experimental/troubleshooting scripts to archive
- **Location**: `infrastructure/scripts/archive/2026-03-25-pre-reset/`
- **Scripts to Archive**: All 26 current scripts
- **Preserve**: Script history for reference and learning

#### 1.2 Document Current State
- **Action**: Capture current resource inventory before destruction
- **Output**: `infrastructure/docs/pre-reset-inventory.md`
- **Include**:
  - All running resources with IDs
  - Current costs
  - Terraform state snapshot
  - Known issues and blockers

#### 1.3 Backup Critical Data
- **Action**: Export any data that should be preserved
- **Items**:
  - RDS database dump (if any test data exists)
  - Terraform state files (already in S3)
  - CloudWatch logs (export to S3 if needed)
  - Any S3 bucket contents

#### 1.4 Complete Teardown of DEV Environment
- **Action**: Destroy all application resources in DEV account
- **Method**: Create comprehensive destroy script
- **Script**: `infrastructure/scripts/teardown-dev-environment.sh`
- **Process**:
  1. Assume role in DEV account
  2. Delete ECS services (graceful shutdown)
  3. Delete ALB and target groups
  4. Delete Lambda functions
  5. Delete EventBridge rules and SQS queues
  6. Delete RDS instance (skip final snapshot)
  7. Delete ElastiCache cluster
  8. Delete NAT gateways and Elastic IPs
  9. Delete VPC endpoints
  10. Delete subnets and route tables
  11. Delete VPC
  12. Delete security groups
  13. Delete KMS keys (schedule deletion)
  14. Force-delete Secrets Manager secrets
  15. Delete CloudWatch log groups, alarms, dashboards
  16. Clean Terraform state
- **Verification**: Confirm all resources deleted via AWS Console
- **Estimated Time**: 30-60 minutes (mostly waiting for RDS/NAT deletion)

### Phase 2: Terraform Module Standardization (Day 1-2, 8-10 hours)

#### 2.1 Review and Fix Existing Modules
- **Action**: Audit all 8 Terraform modules against specs
- **Modules**: networking, security, database, storage, messaging, compute, lambda, monitoring
- **Fixes Required**:
  - **Compute Module**: Fix ECS service deployment_configuration block syntax
  - **Messaging Module**: Fix Lambda function name references (use variables)
  - **Monitoring Module**: Fix conditional expression type mismatches
  - **Database Module**: Verify Redis/DocumentDB conditional creation
  - **All Modules**: Ensure consistent variable naming and output structure

#### 2.2 Create Environment-Specific Variable Files
- **Action**: Update terraform.tfvars for demo-ready DEV configuration
- **File**: `infrastructure/terraform/environments/dev/terraform.tfvars`
- **Configuration**:
  ```hcl
  environment = "dev"
  aws_region  = "us-east-1"
  
  # Networking
  vpc_cidr             = "10.0.0.0/16"
  availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
  enable_nat_gateway   = true
  single_nat_gateway   = true  # Cost optimization
  enable_flow_logs     = false # Save cost
  
  # Compute
  ecs_task_cpu         = "512"
  ecs_task_memory      = "1024"
  ecs_desired_count    = 1
  use_fargate_spot     = false # Demo reliability
  enable_container_insights = true
  
  # Database
  db_instance_class    = "db.t3.micro"
  db_allocated_storage = 20
  enable_multi_az      = false
  backup_retention_days = 7
  enable_performance_insights = false
  
  # Caching
  enable_redis         = true
  redis_node_type      = "cache.t3.micro"
  redis_num_cache_nodes = 1
  
  # NoSQL
  enable_documentdb    = false # Not needed for demo
  
  # Lambda
  enable_lambda_reporting    = true
  enable_lambda_notification = true
  lambda_reporting_memory    = 1024
  lambda_notification_memory = 512
  
  # Monitoring
  enable_xray          = true
  enable_dashboards    = true
  enable_alarms        = true
  log_retention_days   = 7
  
  # Messaging
  enable_eventbridge   = true
  enable_sqs           = true
  ```

#### 2.3 Update QA and PROD Configurations
- **Action**: Ensure QA/PROD tfvars are ready for future deployment
- **QA**: Production-like with 2 tasks per service, Multi-AZ RDS
- **PROD**: Full HA with 3 AZs, auto-scaling, all features
- **Status**: Configured but not deployed

#### 2.4 Validate Module Dependencies
- **Action**: Ensure correct module output references
- **Check**: All module.*.output references match actual module outputs
- **Test**: Run `terraform validate` on all environments

### Phase 3: Standardized Deployment Scripts (Day 2, 4-6 hours)

#### 3.1 Create Core Deployment Scripts
- **Location**: `infrastructure/scripts/`
- **Scripts to Create**:

**1. `deploy-environment.sh`** (Universal deployment script)
```bash
#!/bin/bash
# Usage: ./deploy-environment.sh <env> [plan|apply|destroy]
# Handles AWS role assumption, Terraform execution
# Supports: dev, qa, prod
```

**2. `verify-environment.sh`** (Post-deployment verification)
```bash
#!/bin/bash
# Usage: ./verify-environment.sh <env>
# Checks: VPC, RDS, ECS, ALB, Lambda, monitoring
# Outputs: Health report with resource status
```

**3. `cost-estimate.sh`** (Cost analysis)
```bash
#!/bin/bash
# Usage: ./cost-estimate.sh <env>
# Uses terraform plan and AWS Pricing API
# Outputs: Monthly cost estimate breakdown
```

**4. `backup-state.sh`** (State backup utility)
```bash
#!/bin/bash
# Usage: ./backup-state.sh <env>
# Exports Terraform state to timestamped backup
```

#### 3.2 Create Utility Scripts
**1. `assume-role.sh`** (Reusable role assumption)
```bash
#!/bin/bash
# Usage: source assume-role.sh <env>
# Exports AWS credentials for environment
```

**2. `check-prerequisites.sh`** (Pre-deployment checks)
```bash
#!/bin/bash
# Verifies: Terraform installed, AWS CLI, jq, credentials
# Checks: S3 state bucket, DynamoDB lock table, ACM cert
```

#### 3.3 Update Documentation
- **File**: `infrastructure/scripts/README.md`
- **Content**:
  - Script inventory and purposes
  - Usage examples
  - Troubleshooting guide
  - Archive folder explanation

### Phase 4: DEV Environment Deployment (Day 2-3, 6-8 hours)

#### 4.1 Pre-Deployment Validation
- **Action**: Run prerequisite checks
- **Script**: `./check-prerequisites.sh dev`
- **Verify**:
  - Terraform v1.5+ installed
  - AWS CLI configured
  - S3 state bucket exists (turaf-terraform-state-dev)
  - DynamoDB lock table exists (turaf-terraform-locks)
  - ACM certificate exists (*.turafapp.com)
  - ECR repositories exist (3 services)

#### 4.2 Terraform Initialization
- **Action**: Initialize Terraform with S3 backend
- **Commands**:
  ```bash
  cd infrastructure/terraform/environments/dev
  terraform init -backend-config=backend.tfvars
  ```
- **Verify**: Backend initialized, modules downloaded

#### 4.3 Terraform Plan
- **Action**: Generate and review execution plan
- **Command**: `terraform plan -out=tfplan`
- **Review**:
  - ~90-100 resources to create
  - No unexpected deletions
  - Correct variable values
  - Proper module dependencies
- **Save**: Plan output to `plans/dev-initial-deployment-plan.txt`

#### 4.4 Terraform Apply
- **Action**: Deploy infrastructure
- **Command**: `terraform apply tfplan`
- **Monitor**: Watch for errors, especially:
  - VPC endpoint creation (can be slow)
  - NAT gateway creation (~2 minutes)
  - RDS instance creation (~10-15 minutes)
  - ECS service stabilization
- **Estimated Time**: 20-30 minutes total

#### 4.5 Post-Deployment Verification
- **Action**: Run verification script
- **Script**: `./verify-environment.sh dev`
- **Checks**:
  - ✅ VPC and subnets created
  - ✅ NAT gateway operational
  - ✅ RDS instance running and accessible
  - ✅ Redis cluster available
  - ✅ ECS cluster created
  - ✅ ALB healthy and responding
  - ✅ ECS services running (0 tasks initially - no images)
  - ✅ Lambda functions deployed
  - ✅ EventBridge rules active
  - ✅ SQS queues created
  - ✅ CloudWatch dashboards visible
  - ✅ Secrets Manager secrets populated

### Phase 5: Database Schema Setup (Day 3, 2-3 hours)

#### 5.1 Run Flyway Migrations
- **Action**: Execute database schema creation
- **Method**: Use existing CodeBuild Flyway project
- **Trigger**: Manual build via AWS Console or CLI
- **Migrations**: Create schemas and users
  - identity_schema + identity_user
  - organization_schema + organization_user
  - experiment_schema + experiment_user
  - metrics_schema + metrics_user

#### 5.2 Verify Database Setup
- **Action**: Connect to RDS and verify schemas
- **Script**: `./verify-database.sh dev`
- **Checks**:
  - All 4 schemas exist
  - All 4 service users exist
  - Permissions correctly granted
  - Secrets Manager has correct credentials

### Phase 6: Documentation & Handoff (Day 3-4, 4-6 hours)

#### 6.1 Update Task Status
- **Action**: Mark infrastructure tasks as complete
- **Files**: Update all `tasks/infrastructure/*.md` files
- **Status**: Mark DEV deployment complete, QA/PROD configured

#### 6.2 Create Deployment Guide
- **File**: `infrastructure/docs/DEPLOYMENT_GUIDE.md`
- **Content**:
  - Architecture overview
  - Prerequisites
  - Step-by-step deployment instructions
  - Troubleshooting common issues
  - Cost management tips
  - Scaling and feature toggle guide

#### 6.3 Create Operations Runbook
- **File**: `infrastructure/docs/OPERATIONS_RUNBOOK.md`
- **Content**:
  - Daily operations (monitoring, logs)
  - Backup and restore procedures
  - Scaling procedures
  - Incident response
  - Cost optimization opportunities

#### 6.4 Update PROJECT.md
- **Action**: Reflect current infrastructure state
- **Sections to Update**:
  - Deployment status (DEV deployed, QA/PROD configured)
  - Cost estimates (actual vs projected)
  - Known limitations
  - Next steps

#### 6.5 Create Change Log
- **File**: `changelog/2026-03-25-infrastructure-reset.md`
- **Content**:
  - Summary of changes
  - Resources destroyed
  - Resources created
  - Configuration changes
  - Cost impact
  - Breaking changes (if any)

---

## Success Criteria

### Infrastructure
- ✅ All DEV application resources destroyed cleanly
- ✅ New DEV environment deployed via Terraform
- ✅ All 8 Terraform modules working without errors
- ✅ Terraform state clean and consistent
- ✅ All resources properly tagged
- ✅ No manual resources (everything in Terraform)

### Functionality
- ✅ VPC with NAT gateway providing internet access
- ✅ RDS PostgreSQL accessible from ECS tasks
- ✅ Redis cluster accessible from ECS tasks
- ✅ ALB responding to HTTPS requests
- ✅ ECS services can pull images from ECR
- ✅ Lambda functions can be invoked
- ✅ EventBridge rules triggering correctly
- ✅ SQS queues receiving messages
- ✅ CloudWatch logs collecting data
- ✅ X-Ray traces visible

### Reproducibility
- ✅ `terraform plan` shows no changes after apply
- ✅ `terraform destroy` cleanly removes all resources
- ✅ Deployment can be repeated from scratch
- ✅ QA/PROD can be deployed using same modules
- ✅ All configuration in version control

### Documentation
- ✅ All scripts documented in README
- ✅ Deployment guide complete
- ✅ Operations runbook created
- ✅ Task status updated
- ✅ Change log created
- ✅ Cost analysis documented

### Cost
- ✅ Monthly cost within $180-220 range
- ✅ No unexpected charges
- ✅ Cost allocation tags applied
- ✅ Budget alerts configured

---

## Risk Mitigation

### Risk 1: Data Loss
- **Mitigation**: Backup RDS before destruction, export CloudWatch logs
- **Recovery**: Restore from backup if needed

### Risk 2: Terraform State Corruption
- **Mitigation**: Backup state before major operations
- **Recovery**: Restore from S3 versioned state bucket

### Risk 3: Deployment Failures
- **Mitigation**: Incremental deployment, validate each phase
- **Recovery**: Destroy partial deployment, fix issues, retry

### Risk 4: Cost Overruns
- **Mitigation**: Set budget alerts, monitor costs daily
- **Recovery**: Disable expensive features (NAT, Redis, monitoring)

### Risk 5: Service Interruption
- **Mitigation**: No production services running yet
- **Recovery**: N/A (development environment only)

---

## Post-Reset Workflow

### Daily Operations
1. Monitor CloudWatch dashboards
2. Check cost reports
3. Review CloudWatch logs for errors
4. Verify ECS service health

### Weekly Maintenance
1. Review and rotate logs
2. Check for Terraform drift (`terraform plan`)
3. Update documentation if changes made
4. Review cost trends

### Monthly Tasks
1. Review and optimize costs
2. Update Terraform modules if needed
3. Test disaster recovery procedures
4. Review security group rules

### Feature Toggles
Enable/disable features via Terraform variables:
- `enable_redis`: Toggle Redis cluster
- `enable_lambda_*`: Toggle Lambda functions
- `enable_xray`: Toggle X-Ray tracing
- `enable_container_insights`: Toggle Container Insights
- `enable_nat_gateway`: Toggle NAT gateway (requires VPC endpoint adjustments)

---

## Future Enhancements

### Phase 7: QA Environment (When Needed)
- Deploy QA using same modules
- Configure for integration testing
- Enable additional monitoring
- Estimated cost: ~$70-90/month

### Phase 8: PROD Environment (For Demo)
- Deploy PROD with full HA
- Enable all monitoring and alerting
- Configure auto-scaling
- Estimated cost: ~$350-450/month
- **Strategy**: Deploy on-demand for demos, destroy after

### Phase 9: CI/CD Integration
- GitHub Actions workflows for Terraform
- Automated testing of infrastructure changes
- Drift detection and remediation
- Cost reporting in PRs

### Phase 10: Advanced Features
- Multi-region failover (PROD only)
- Blue-green deployments
- Canary releases
- Advanced monitoring (Datadog, New Relic)

---

## Appendix

### A. Script Archive Structure
```
infrastructure/scripts/
├── archive/
│   └── 2026-03-25-pre-reset/
│       ├── cleanup-scheduled-deletions.sh
│       ├── create-codebuild-flyway-dev.sh
│       ├── delete-db-subnet-group.sh
│       ├── delete-old-rds-instance.sh
│       ├── deploy-*.sh (10 variants)
│       ├── destroy-standalone-dev.sh
│       ├── import-existing-resources.sh
│       ├── unlock-terraform-state.sh
│       └── ... (16 more scripts)
├── deploy-environment.sh (NEW)
├── verify-environment.sh (NEW)
├── cost-estimate.sh (NEW)
├── backup-state.sh (NEW)
├── assume-role.sh (NEW)
├── check-prerequisites.sh (NEW)
└── README.md (UPDATED)
```

### B. Terraform Module Structure (Final)
```
infrastructure/terraform/
├── modules/
│   ├── networking/      (FIXED)
│   ├── security/        (VERIFIED)
│   ├── database/        (FIXED)
│   ├── storage/         (VERIFIED)
│   ├── messaging/       (FIXED)
│   ├── compute/         (FIXED)
│   ├── lambda/          (FIXED)
│   └── monitoring/      (FIXED)
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── backend.tf
│   │   ├── terraform.tfvars (UPDATED - demo-ready)
│   │   └── backend.tfvars
│   ├── qa/
│   │   └── ... (CONFIGURED, not deployed)
│   └── prod/
│       └── ... (CONFIGURED, not deployed)
└── docs/
    ├── DEPLOYMENT_GUIDE.md (NEW)
    ├── OPERATIONS_RUNBOOK.md (NEW)
    └── pre-reset-inventory.md (NEW)
```

### C. Resource Naming Conventions
- VPC: `turaf-vpc-{env}`
- Subnets: `turaf-{type}-subnet-{env}-{az}`
- Security Groups: `turaf-{service}-sg-{env}`
- RDS: `turaf-postgres-{env}`
- Redis: `turaf-redis-{env}`
- ECS Cluster: `turaf-cluster-{env}`
- ECS Services: `turaf-{service}-{env}`
- ALB: `turaf-alb-{env}`
- Lambda: `turaf-{function}-{env}`
- S3: `turaf-{purpose}-{env}-{account-id}`
- KMS: `turaf-{service}-key-{env}`

### D. Estimated Timeline
- **Day 1 AM**: Preparation, documentation, backup (3 hours)
- **Day 1 PM**: Teardown, module fixes (5 hours)
- **Day 2 AM**: Script creation, variable updates (4 hours)
- **Day 2 PM**: Terraform plan and apply (4 hours)
- **Day 3 AM**: Verification, database setup (3 hours)
- **Day 3 PM**: Documentation (4 hours)
- **Day 4**: Buffer for issues, final testing (4 hours)
- **Total**: 27 hours over 4 days

---

## Conclusion

This plan provides a comprehensive path to reset and standardize the Turaf infrastructure, eliminating drift and establishing a solid foundation for future development. The demo-ready DEV environment will showcase all platform capabilities while remaining cost-effective at ~$200/month. QA and PROD environments are configured and ready for on-demand deployment when needed.

**Next Step**: Confirm plan approval to begin Phase 1 (Preparation & Cleanup).
