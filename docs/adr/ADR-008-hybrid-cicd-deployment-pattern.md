# ADR-008: Hybrid CI/CD Deployment Pattern

**Date**: March 25, 2026  
**Status**: Accepted  
**Decision Makers**: DevOps Team, Architecture Team  
**Related ADRs**: ADR-007 (Infrastructure Restructure)

---

## Context

Following the infrastructure restructure (ADR-007), we needed to define how services would deploy their infrastructure via CI/CD pipelines. The key questions were:

1. **Deployment Mechanism**: How do services deploy their ECS resources?
2. **Workflow Structure**: One workflow per environment or per service?
3. **Terraform vs AWS CLI**: Use Terraform or direct AWS API calls?
4. **State Management**: Where and how to store service Terraform state?
5. **Approval Gates**: How to enforce environment-specific approval requirements?

---

## Decision

We will implement a **hybrid CI/CD deployment pattern** where:

### Shared Infrastructure Deployment
- **Tool**: Terraform
- **Location**: `infrastructure/terraform/`
- **Workflow**: `infrastructure.yml`
- **Trigger**: Changes to infrastructure code, manual dispatch
- **Responsibility**: Platform team
- **Scope**: VPC, ECS cluster, ALB, databases, messaging

### Service-Specific Deployment
- **Tool**: Terraform (for infrastructure) + Docker (for images)
- **Location**: `services/<service>/terraform/`
- **Workflow**: `service-<name>-<env>.yml` (one per service per environment)
- **Trigger**: Changes to service code, path-filtered
- **Responsibility**: Service teams
- **Scope**: ECS service, task definition, target group, listener rule

### Workflow Pattern (Per Service, Per Environment)
Each service has 3 workflows:
- `service-identity-dev.yml` - Auto-deploy on `develop` branch
- `service-identity-qa.yml` - Manual approval on `release/*` branch
- `service-identity-prod.yml` - Strict approval on `main` branch

### Deployment Jobs (3-stage)
1. **build-and-push**: Build Docker image, push to ECR, security scan
2. **deploy-service**: Run Terraform to deploy/update service infrastructure
3. **verify-deployment**: Wait for ECS stability, run health checks

---

## Rationale

### Why Terraform for Service Deployment?

**Considered Alternatives**:
1. **AWS CLI (`aws ecs update-service`)**: Simple but doesn't manage infrastructure
2. **CloudFormation**: Verbose, slower than Terraform
3. **CDK**: Requires TypeScript/Python knowledge, adds complexity
4. **Terraform**: Declarative, familiar to team, manages full lifecycle

**Decision**: Use Terraform because:
- Manages complete infrastructure lifecycle (create, update, delete)
- Declarative approach prevents drift
- State tracking ensures idempotency
- Team already familiar with Terraform
- Easy to reference shared infrastructure via `terraform_remote_state`

### Why Per-Service Workflows?

**Considered Alternatives**:
1. **Single CD workflow with matrix**: Deploy all services together
2. **Environment-specific workflows**: `cd-dev.yml`, `cd-qa.yml`, `cd-prod.yml`
3. **Per-service, per-environment workflows**: `service-<name>-<env>.yml`

**Decision**: Per-service, per-environment workflows because:
- **Independence**: Services deploy without affecting others
- **Path Filtering**: Only trigger when service code changes
- **Clear Ownership**: Each service team owns their workflows
- **Flexibility**: Different approval rules per environment
- **Scalability**: Easy to add new services

### Why 3-Stage Deployment?

**Stage 1 - Build & Push**:
- Builds Docker image with commit SHA tag
- Pushes to ECR for environment
- Runs security scan (Trivy)
- Fails fast if vulnerabilities found

**Stage 2 - Deploy Service**:
- Runs Terraform to create/update service resources
- Uses image from Stage 1
- Requires manual approval for QA/PROD
- Manages ECS service, task definition, target group, listener rule

**Stage 3 - Verify Deployment**:
- Waits for ECS service stability
- Runs health checks
- Executes smoke tests
- Monitors for errors

This separation allows:
- Fast feedback on build failures
- Manual review before infrastructure changes
- Automated verification after deployment

### Why Separate State Files?

Each service maintains its own Terraform state:
- **Location**: `s3://turaf-<service>-<env>-state/terraform.tfstate`
- **Locking**: DynamoDB table per service
- **Isolation**: Service changes don't affect other services
- **Smaller State**: Faster Terraform operations

---

## Consequences

### Positive

1. **Independent Deployments**: Services deploy on their own schedule
2. **Fast Feedback**: Path filtering prevents unnecessary runs
3. **Clear Ownership**: Service teams control their deployment
4. **Environment Safety**: Approval gates prevent accidental PROD deployments
5. **Audit Trail**: Each deployment tracked in GitHub Actions
6. **Rollback Capability**: Terraform state enables easy rollback

### Negative

1. **Workflow Proliferation**: 3 services × 3 environments = 9 workflows
2. **Duplication**: Similar workflow code across services
3. **Maintenance Overhead**: Updates require changing multiple workflows
4. **Learning Curve**: Service teams must understand Terraform and GitHub Actions
5. **State Management**: More state files to manage and backup

### Mitigation Strategies

**For Workflow Proliferation**:
- Use workflow templates (`.github/workflow-templates/`)
- Document standard patterns
- Provide copy-paste examples

**For Duplication**:
- Extract common steps to reusable actions
- Use environment variables for service-specific values
- Consider composite actions for repeated logic

**For Maintenance**:
- Version workflow templates
- Communicate changes via team channels
- Provide migration guides for breaking changes

**For Learning Curve**:
- Comprehensive documentation in `specs/ci-cd-pipelines.md`
- Example service deployments
- Terraform training sessions

---

## Implementation

### Workflow Structure

```yaml
name: Deploy <Service> to <ENV>

on:
  push:
    branches: [<branch>]
    paths: ['services/<service>/**']
  workflow_dispatch:

env:
  SERVICE_NAME: <service>
  ENVIRONMENT: <env>
  AWS_ACCOUNT_ID: <account-id>

jobs:
  build-and-push:
    # Build Docker image, push to ECR, security scan
  
  deploy-service:
    needs: build-and-push
    environment: <env>  # Approval gate
    # Run Terraform to deploy service
  
  verify-deployment:
    needs: deploy-service
    # Wait for stability, run health checks
```

### Service Terraform Structure

```
services/<service>/terraform/
├── backend.tf          # S3 backend configuration
├── data.tf             # Reference shared infrastructure
├── main.tf             # Service resources
├── variables.tf        # Service variables
└── outputs.tf          # Service outputs
```

### Shared Infrastructure Reference

```hcl
data "terraform_remote_state" "infra" {
  backend = "s3"
  config = {
    bucket = "turaf-terraform-state-${var.environment}"
    key    = "terraform.tfstate"
    region = "us-east-1"
  }
}

# Use shared infrastructure
cluster_arn = data.terraform_remote_state.infra.outputs.cluster_arn
vpc_id      = data.terraform_remote_state.infra.outputs.vpc_id
```

---

## Environment-Specific Configuration

### DEV
- **Branch**: `develop`
- **Approval**: None (auto-deploy)
- **Desired Count**: 1
- **Deployment**: Rolling update

### QA
- **Branch**: `release/*`
- **Approval**: 1 reviewer required
- **Desired Count**: 2
- **Deployment**: Rolling update
- **Tests**: Integration tests

### PROD
- **Branch**: `main`
- **Approval**: 2+ reviewers required
- **Desired Count**: 3
- **Deployment**: Blue-green (CodeDeploy)
- **Tests**: E2E tests
- **Monitoring**: 5-minute observation period

---

## Alternatives Considered

### Alternative 1: Monolithic CD Pipeline
**Rejected**: Doesn't support independent service deployments

### Alternative 2: AWS CodePipeline
**Rejected**: Less flexible than GitHub Actions, team prefers GitHub-native

### Alternative 3: ArgoCD (GitOps)
**Rejected**: Overkill for current scale, adds operational complexity

### Alternative 4: Pulumi
**Rejected**: Team not familiar, Terraform already adopted

---

## Security Considerations

1. **OIDC Authentication**: No long-lived AWS credentials in GitHub
2. **Least Privilege IAM**: Service-specific permissions per environment
3. **Image Scanning**: Trivy blocks vulnerable images
4. **Approval Gates**: Manual review for QA/PROD
5. **Audit Logging**: All deployments logged in CloudTrail
6. **State Encryption**: Terraform state encrypted at rest in S3

---

## Monitoring & Observability

1. **Deployment Metrics**: Track deployment frequency, duration, success rate
2. **CloudWatch Alarms**: Auto-rollback on error rate spikes
3. **GitHub Actions Logs**: Full deployment audit trail
4. **ECS Events**: Track service deployment events
5. **X-Ray Tracing**: Monitor service health post-deployment

---

## References

- **CI/CD Specification**: `specs/ci-cd-pipelines.md`
- **Deployment Pattern**: `.windsurf/plans/completed/cicd/cicd-service-deployment-pattern.md`
- **Infrastructure Restructure**: `infrastructure/docs/architecture/INFRASTRUCTURE_RESTRUCTURE_SUMMARY.md`
- **Task Files**: `tasks/cicd/002-setup-cd-dev-pipeline.md`, `003-setup-cd-qa-pipeline.md`, `004-setup-cd-prod-pipeline.md`

---

## Success Metrics

**Target Metrics** (after full rollout):
- Deployment frequency: 10+ per day (DEV), 5+ per week (QA), 2+ per week (PROD)
- Deployment duration: <10 minutes per service
- Success rate: >95%
- Rollback rate: <5%
- Mean time to recovery: <15 minutes

**Current Status**: Pattern defined, awaiting service migration

---

## Lessons Learned

1. **Path Filtering Critical**: Prevents unnecessary workflow runs
2. **Approval Gates Essential**: Protects production from accidents
3. **Security Scanning Early**: Catch vulnerabilities before deployment
4. **Health Checks Required**: Automated verification prevents bad deployments
5. **Documentation Key**: Service teams need clear examples

---

**Decision**: Accepted  
**Next Review**: After first 3 services migrated (Q2 2026)  
**Migration Status**: Pattern defined, implementation pending
