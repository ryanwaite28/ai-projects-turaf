# Flyway IAM Roles Setup

This directory contains scripts for setting up and verifying IAM roles for the centralized Flyway migration service.

## Overview

The centralized Flyway migration service requires two IAM roles per environment:

1. **GitHubActionsFlywayRole** - Allows GitHub Actions to trigger CodeBuild migrations
2. **CodeBuildFlywayRole** - Allows CodeBuild to execute migrations with RDS access

## Scripts

### setup-flyway-iam-roles.sh

**Purpose**: Create IAM roles for database migrations in all environments (dev, qa, prod).

**Prerequisites**:
- AWS CLI configured with profiles: `turaf-dev`, `turaf-qa`, `turaf-prod`
- Sufficient IAM permissions to create roles and policies
- GitHub OIDC provider already configured (Task 009)

**Usage**:

```bash
./setup-flyway-iam-roles.sh
```

**What it does**:

For each environment (dev, qa, prod):

1. **Creates GitHubActionsFlywayRole**:
   - Trust policy: GitHub OIDC provider
   - Permissions: Start CodeBuild, get build status, read CloudWatch logs
   - Repository: `ryanwaite28/ai-projects-turaf`

2. **Creates CodeBuildFlywayRole**:
   - Trust policy: CodeBuild service
   - Permissions: Secrets Manager (DB credentials), CloudWatch Logs, VPC access, ECR
   - Resources: Scoped to migration-specific resources

3. **Outputs role ARNs** for GitHub secrets configuration

**Output Example**:

```
==========================================
✅ All IAM roles configured successfully!
==========================================

Role ARNs for GitHub Secrets:
----------------------------------------

DEV environment:
  AWS_FLYWAY_ROLE_ARN=arn:aws:iam::801651112319:role/GitHubActionsFlywayRole

QA environment:
  AWS_FLYWAY_ROLE_ARN=arn:aws:iam::965932217544:role/GitHubActionsFlywayRole

PROD environment:
  AWS_FLYWAY_ROLE_ARN=arn:aws:iam::811783768245:role/GitHubActionsFlywayRole
```

---

### verify-flyway-iam-roles.sh

**Purpose**: Verify that IAM roles are correctly configured.

**Usage**:

```bash
./verify-flyway-iam-roles.sh
```

**What it checks**:

For each environment:

1. **GitHubActionsFlywayRole**:
   - ✅ Role exists
   - ✅ Trust policy configured for GitHub OIDC
   - ✅ Permissions policy attached
   - ✅ CodeBuild permissions configured

2. **CodeBuildFlywayRole**:
   - ✅ Role exists
   - ✅ Trust policy configured for CodeBuild service
   - ✅ Permissions policy attached
   - ✅ Secrets Manager permissions configured
   - ✅ VPC permissions configured
   - ✅ CloudWatch Logs permissions configured

**Exit codes**:
- `0` - All checks passed
- `1` - Some checks failed

**Output Example**:

```
==========================================
Verification Summary
==========================================

Total checks: 24
Passed: 24
Failed: 0

✅ All checks passed!
```

---

## IAM Roles Details

### GitHubActionsFlywayRole

**Trust Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
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
  }]
}
```

**Permissions**:
- `codebuild:StartBuild` - Trigger migration builds
- `codebuild:BatchGetBuilds` - Get build status
- `logs:GetLogEvents` - Read migration logs
- `logs:FilterLogEvents` - Search migration logs

**Resource Scope**: `turaf-flyway-migrations-{env}` CodeBuild project

---

### CodeBuildFlywayRole

**Trust Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Service": "codebuild.amazonaws.com"
    },
    "Action": "sts:AssumeRole"
  }]
}
```

**Permissions**:

1. **Secrets Manager** - Read database credentials
   - `secretsmanager:GetSecretValue`
   - Resource: `turaf/db/master-*`

2. **CloudWatch Logs** - Write migration logs
   - `logs:CreateLogGroup`
   - `logs:CreateLogStream`
   - `logs:PutLogEvents`
   - Resource: `/aws/codebuild/turaf-flyway-migrations-{env}`

3. **VPC Access** - Connect to RDS in private subnet
   - `ec2:CreateNetworkInterface`
   - `ec2:DescribeNetworkInterfaces`
   - `ec2:DeleteNetworkInterface`
   - `ec2:DescribeSubnets`
   - `ec2:DescribeSecurityGroups`
   - `ec2:DescribeDhcpOptions`
   - `ec2:DescribeVpcs`
   - `ec2:CreateNetworkInterfacePermission`

4. **ECR Access** - Pull Docker images (if needed)
   - `ecr:GetAuthorizationToken`
   - `ecr:BatchCheckLayerAvailability`
   - `ecr:GetDownloadUrlForLayer`
   - `ecr:BatchGetImage`

---

## Workflow

### Initial Setup

```bash
# 1. Run setup script
./setup-flyway-iam-roles.sh

# 2. Verify roles
./verify-flyway-iam-roles.sh

# 3. Copy role ARNs from output

# 4. Add to GitHub environment secrets (Task 012)
# Settings → Environments → dev → Add secret
# Name: AWS_FLYWAY_ROLE_ARN
# Value: arn:aws:iam::801651112319:role/GitHubActionsFlywayRole
```

### Update Existing Roles

If you need to update permissions:

```bash
# Re-run setup script (idempotent)
./setup-flyway-iam-roles.sh

# Verify changes
./verify-flyway-iam-roles.sh
```

---

## Troubleshooting

### Error: "An error occurred (EntityAlreadyExists)"

**Cause**: Role already exists.

**Solution**: This is normal. The script will update the existing role's policies.

### Error: "User is not authorized to perform: iam:CreateRole"

**Cause**: Insufficient IAM permissions.

**Solution**: Ensure your AWS profile has IAM admin permissions or the following:
- `iam:CreateRole`
- `iam:PutRolePolicy`
- `iam:GetRole`
- `iam:TagRole`

### Error: "The security token included in the request is invalid"

**Cause**: AWS credentials expired or incorrect profile.

**Solution**: 
```bash
# Refresh credentials
aws sso login --profile turaf-dev

# Verify profile
aws sts get-caller-identity --profile turaf-dev
```

### Verification fails: "Trust policy missing GitHub OIDC configuration"

**Cause**: OIDC provider not configured or trust policy incorrect.

**Solution**:
```bash
# Verify OIDC provider exists
aws iam list-open-id-connect-providers --profile turaf-dev

# If missing, run Task 009 first
```

### Verification fails: "Missing CodeBuild permissions"

**Cause**: Permissions policy not attached or incorrect.

**Solution**: Re-run `setup-flyway-iam-roles.sh` to update policies.

---

## Security Best Practices

### Least Privilege

- Roles have minimal permissions required for their function
- Resource scoping limits access to specific CodeBuild projects and log groups
- No wildcard permissions except for VPC/ECR (AWS requirement)

### OIDC Authentication

- No long-lived AWS credentials in GitHub
- Temporary credentials issued per workflow run
- Repository-scoped trust policy

### Audit Trail

- All role assumptions logged in CloudTrail
- CloudWatch Logs capture all migration executions
- IAM role tags identify purpose and environment

---

## Next Steps

After running these scripts:

1. ✅ **Task 026 Complete** - IAM roles configured
2. ⏭️ **Task 027** - Configure database migration network access
3. ⏭️ **Task 028** - Create CodeBuild migration projects
4. ⏭️ **Update Task 012** - Add role ARNs to GitHub secrets

---

## References

- **Task 026**: Configure Database Migration IAM Roles
- **Task 009**: Configure IAM OIDC for GitHub Actions
- **specs/flyway-service.md**: Centralized migration service documentation
- **INFRASTRUCTURE_PLAN.md**: Phase 2.3 - Database Migration IAM Roles

---

## Role ARN Template

For GitHub environment secrets configuration:

```bash
# DEV
AWS_FLYWAY_ROLE_ARN=arn:aws:iam::801651112319:role/GitHubActionsFlywayRole

# QA
AWS_FLYWAY_ROLE_ARN=arn:aws:iam::965932217544:role/GitHubActionsFlywayRole

# PROD
AWS_FLYWAY_ROLE_ARN=arn:aws:iam::811783768245:role/GitHubActionsFlywayRole
```

Copy these values to GitHub repository settings after running the setup script.
