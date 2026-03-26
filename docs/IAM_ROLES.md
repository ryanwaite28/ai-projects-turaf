# IAM Roles Reference

**Last Updated**: March 25, 2026  
**Status**: Current - Source of Truth  
**Related Documents**: [AWS_ACCOUNTS.md](../AWS_ACCOUNTS.md), [GITHUB.md](../GITHUB.md), [infrastructure/github-oidc-roles.md](../infrastructure/github-oidc-roles.md)

---

## Overview

This document is the **authoritative reference** for all IAM roles used in the Turaf platform. All documentation, code, and workflows must reference role names and ARNs from this document.

---

## GitHub Actions OIDC Roles

### Role Name Convention

**Standard**: `GitHubActionsDeploymentRole` (same name in all accounts)

**Rationale**:
- Environment is distinguished by AWS account ID
- Simplifies role management
- Consistent across all environments
- No environment suffix needed

### Role ARNs by Account

| Environment | AWS Account ID | Role Name | Role ARN |
|-------------|----------------|-----------|----------|
| **Ops** | 146072879609 | `GitHubActionsDeploymentRole` | `arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole` |
| **Dev** | 801651112319 | `GitHubActionsDeploymentRole` | `arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole` |
| **QA** | 965932217544 | `GitHubActionsDeploymentRole` | `arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole` |
| **Prod** | 811783768245 | `GitHubActionsDeploymentRole` | `arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole` |

### Trust Policy

All `GitHubActionsDeploymentRole` roles use OIDC federation with GitHub Actions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::{ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
        }
      }
    }
  ]
}
```

### Permissions Summary

Each `GitHubActionsDeploymentRole` has permissions for:
- **ECR**: Push/pull Docker images
- **ECS**: Update services, register task definitions
- **IAM**: Pass roles to ECS tasks
- **S3**: Upload/download artifacts
- **CloudWatch Logs**: Write deployment logs
- **Terraform**: Manage infrastructure (limited scope)

**Full details**: See [infrastructure/github-oidc-roles.md](../infrastructure/github-oidc-roles.md)

### Usage in GitHub Actions

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsDeploymentRole
    aws-region: us-east-1
```

**Environment-specific examples**:

```yaml
# Dev environment
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
    aws-region: us-east-1

# QA environment
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole
    aws-region: us-east-1

# Prod environment
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole
    aws-region: us-east-1
```

---

## ECS Task Execution Roles

### Standard ECS Task Execution Role

**Role Name**: `ecsTaskExecutionRole`  
**Purpose**: Allows ECS to pull images from ECR and write logs to CloudWatch

**ARNs by Account**:
| Environment | Role ARN |
|-------------|----------|
| Dev | `arn:aws:iam::801651112319:role/ecsTaskExecutionRole` |
| QA | `arn:aws:iam::965932217544:role/ecsTaskExecutionRole` |
| Prod | `arn:aws:iam::811783768245:role/ecsTaskExecutionRole` |

**Permissions**:
- `ecr:GetAuthorizationToken`
- `ecr:BatchCheckLayerAvailability`
- `ecr:GetDownloadUrlForLayer`
- `ecr:BatchGetImage`
- `logs:CreateLogStream`
- `logs:PutLogEvents`

---

## ECS Task Roles

### Service-Specific Task Roles

ECS tasks assume service-specific roles to access AWS resources:

| Service | Role Name Pattern | Purpose |
|---------|-------------------|---------|
| Identity Service | `turaf-{env}-identity-task-role` | Access to DynamoDB, SQS, EventBridge |
| Organization Service | `turaf-{env}-organization-task-role` | Access to DynamoDB, SQS, EventBridge |
| Experiment Service | `turaf-{env}-experiment-task-role` | Access to DynamoDB, SQS, EventBridge |
| Metrics Service | `turaf-{env}-metrics-task-role` | Access to DynamoDB, SQS, EventBridge, CloudWatch |
| Reporting Service | `turaf-{env}-reporting-task-role` | Access to DynamoDB, SQS, S3 |
| Notification Service | `turaf-{env}-notification-task-role` | Access to SES, SQS, EventBridge |

**Example ARN**: `arn:aws:iam::801651112319:role/turaf-dev-identity-task-role`

**Common Permissions**:
- DynamoDB table access (scoped to service tables)
- SQS queue access (send/receive)
- EventBridge PutEvents
- CloudWatch metrics
- Secrets Manager (for database credentials)

---

## Lambda Execution Roles

### Standard Lambda Execution Role

**Role Name Pattern**: `turaf-{env}-lambda-execution-role`

**ARNs by Account**:
| Environment | Role ARN |
|-------------|----------|
| Dev | `arn:aws:iam::801651112319:role/turaf-dev-lambda-execution-role` |
| QA | `arn:aws:iam::965932217544:role/turaf-qa-lambda-execution-role` |
| Prod | `arn:aws:iam::811783768245:role/turaf-prod-lambda-execution-role` |

**Permissions**:
- CloudWatch Logs (create log groups/streams, put events)
- VPC access (if Lambda in VPC)
- Service-specific permissions (DynamoDB, S3, etc.)

---

## Database Migration Roles

### Flyway Migration Role

**Role Name**: `GitHubActionsFlywayRole`

**Purpose**: Execute database migrations via AWS CodeBuild

**ARNs by Account**:
| Environment | Role ARN |
|-------------|----------|
| Dev | `arn:aws:iam::801651112319:role/GitHubActionsFlywayRole` |
| QA | `arn:aws:iam::965932217544:role/GitHubActionsFlywayRole` |
| Prod | `arn:aws:iam::811783768245:role/GitHubActionsFlywayRole` |

**Permissions**:
- CodeBuild: Start builds
- Secrets Manager: Read database credentials
- CloudWatch Logs: Write migration logs

**Usage**:
```yaml
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::${{ env.AWS_ACCOUNT_ID }}:role/GitHubActionsFlywayRole
    aws-region: us-east-1
```

---

## Cross-Account Roles

### Ops Account Access Roles

**Purpose**: Allow Ops account to access resources in workload accounts

**Role Name Pattern**: `OrganizationAccountAccessRole`

**Trust Policy**: Trusts Ops account (146072879609)

**Permissions**: Administrative access for centralized management

---

## Service Control Policies (SCPs)

While not IAM roles, SCPs affect what roles can do:

**Applied SCPs**:
- `DenyRootAccount` - Prevents root user actions
- `RequireMFA` - Requires MFA for sensitive operations
- `DenyRegionRestriction` - Limits to us-east-1 region
- `DenyUnencryptedStorage` - Requires encryption for S3/EBS

**Details**: See [AWS_ACCOUNTS.md](../AWS_ACCOUNTS.md)

---

## Role Naming Conventions

### Standard Patterns

| Role Type | Pattern | Example |
|-----------|---------|---------|
| GitHub Actions OIDC | `GitHubActionsDeploymentRole` | `GitHubActionsDeploymentRole` |
| GitHub Actions Flyway | `GitHubActionsFlywayRole` | `GitHubActionsFlywayRole` |
| ECS Task Execution | `ecsTaskExecutionRole` | `ecsTaskExecutionRole` |
| ECS Task Role | `turaf-{env}-{service}-task-role` | `turaf-dev-identity-task-role` |
| Lambda Execution | `turaf-{env}-lambda-execution-role` | `turaf-dev-lambda-execution-role` |
| Cross-Account Access | `OrganizationAccountAccessRole` | `OrganizationAccountAccessRole` |

### Naming Rules

1. **No environment suffix for cross-account roles** (distinguished by account ID)
2. **Include environment for service-specific roles** (e.g., `turaf-dev-*`)
3. **Use kebab-case** for multi-word role names
4. **Prefix with `turaf-`** for project-specific roles
5. **Use descriptive suffixes** (`-task-role`, `-execution-role`, `-deployment-role`)

---

## Security Best Practices

### Least Privilege

- Roles have only permissions needed for their function
- Resource-level permissions where possible (scoped to `turaf-*` resources)
- No wildcard permissions except where necessary (e.g., ECR GetAuthorizationToken)

### Trust Policies

- GitHub Actions roles: Trust only `ryanwaite28/ai-projects-turaf` repository
- ECS task roles: Trust only ECS service
- Lambda roles: Trust only Lambda service
- Cross-account roles: Trust only Ops account

### Session Duration

- GitHub Actions roles: 1 hour maximum
- ECS task roles: Duration of task execution
- Lambda roles: Duration of function execution

### Monitoring

- All role assumptions logged in CloudTrail
- CloudWatch alarms for unusual role usage
- Regular access reviews via IAM Access Analyzer

---

## Verification Commands

### List Roles in Account

```bash
# List all roles
aws iam list-roles --profile turaf-dev --query 'Roles[].RoleName' --output table

# Get specific role details
aws iam get-role --role-name GitHubActionsDeploymentRole --profile turaf-dev

# Get role ARN
aws iam get-role \
  --role-name GitHubActionsDeploymentRole \
  --profile turaf-dev \
  --query 'Role.Arn' \
  --output text
```

### Test Role Assumption (GitHub Actions)

```yaml
name: Test Role Assumption

on: workflow_dispatch

permissions:
  id-token: write
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
          aws-region: us-east-1
      
      - run: aws sts get-caller-identity
```

---

## Troubleshooting

### Common Issues

**Issue**: "User is not authorized to perform: sts:AssumeRoleWithWebIdentity"

**Causes**:
- Trust policy doesn't match repository name
- OIDC provider not configured
- Workflow missing `id-token: write` permission

**Solution**: Verify trust policy and workflow permissions

---

**Issue**: "AccessDenied" when accessing AWS resources

**Causes**:
- Role permissions policy missing required permissions
- Resource policy blocking access
- SCP restricting action

**Solution**: Review role permissions and SCPs

---

## References

- **Detailed OIDC Configuration**: [infrastructure/github-oidc-roles.md](../infrastructure/github-oidc-roles.md)
- **Implementation Task**: [tasks/infrastructure/009-configure-iam-oidc-github-actions.md](../tasks/infrastructure/009-configure-iam-oidc-github-actions.md)
- **AWS Accounts**: [AWS_ACCOUNTS.md](../AWS_ACCOUNTS.md)
- **GitHub Configuration**: [GITHUB.md](../GITHUB.md)
- **Best Practices**: [BEST_PRACTICES.md](../BEST_PRACTICES.md)

---

## Change History

| Date | Change | Author |
|------|--------|--------|
| 2026-03-25 | Initial creation as source-of-truth document | AI Assistant |
| 2024-03-23 | GitHubActionsDeploymentRole created in all accounts | Infrastructure Team |

---

**Maintained By**: DevOps Team  
**Review Frequency**: Quarterly or when roles are added/modified
