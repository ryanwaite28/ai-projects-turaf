# Task: Configure IAM Roles and OIDC for GitHub Actions

**Service**: Infrastructure  
**Type**: IAM and CI/CD Security  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 020-create-service-control-policies

---

## Objective

Configure AWS IAM OIDC identity providers and roles in each account to enable GitHub Actions workflows to authenticate with AWS without using long-lived credentials.

---

## Acceptance Criteria

- [ ] OIDC provider created in each account (Ops, dev, qa, prod)
- [ ] GitHub Actions deployment role created in each account
- [ ] Trust policies configured for GitHub repository
- [ ] Permissions policies attached to roles
- [ ] OIDC configuration tested with GitHub Actions
- [ ] Role ARNs documented for GitHub secrets

---

## Implementation

### 1. Create OIDC Provider in Each Account

**For Ops Account**:

```bash
# Switch to Ops account
export AWS_PROFILE=turaf-ops

# Create OIDC provider for GitHub Actions
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1

# Save the provider ARN from output
```

**Repeat for dev, qa, and prod accounts**:

```bash
# Dev account
export AWS_PROFILE=turaf-dev
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1

# QA account
export AWS_PROFILE=turaf-qa
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1

# Prod account
export AWS_PROFILE=turaf-prod
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

### 2. Create IAM Role Trust Policy

Create `iam-policies/github-actions-trust-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
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

### 3. Create IAM Role Permissions Policy

Create `iam-policies/github-actions-permissions-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRPermissions",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECSPermissions",
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeTaskDefinition",
        "ecs:RegisterTaskDefinition",
        "ecs:ListTasks",
        "ecs:DescribeTasks"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAMPassRole",
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": [
        "arn:aws:iam::${ACCOUNT_ID}:role/ecsTaskExecutionRole",
        "arn:aws:iam::${ACCOUNT_ID}:role/ecsTaskRole"
      ]
    },
    {
      "Sid": "S3Permissions",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::turaf-artifacts-*",
        "arn:aws:s3:::turaf-artifacts-*/*"
      ]
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/aws/ecs/*"
    }
  ]
}
```

### 4. Create GitHub Actions Role in Ops Account

```bash
export AWS_PROFILE=turaf-ops
ACCOUNT_ID=146072879609

# Replace placeholder in trust policy
sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-trust-policy.json > \
  iam-policies/github-actions-trust-policy-ops.json

# Create IAM role
aws iam create-role \
  --role-name GitHubActionsDeploymentRole \
  --assume-role-policy-document file://iam-policies/github-actions-trust-policy-ops.json \
  --description "Role for GitHub Actions to deploy to Ops account"

# Replace placeholder in permissions policy
sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-permissions-policy.json > \
  iam-policies/github-actions-permissions-policy-ops.json

# Create inline policy
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://iam-policies/github-actions-permissions-policy-ops.json

# Get role ARN
aws iam get-role \
  --role-name GitHubActionsDeploymentRole \
  --query 'Role.Arn' \
  --output text
```

### 5. Create GitHub Actions Role in Dev Account

```bash
export AWS_PROFILE=turaf-dev
ACCOUNT_ID=801651112319

# Replace placeholder and create role
sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-trust-policy.json > \
  iam-policies/github-actions-trust-policy-dev.json

aws iam create-role \
  --role-name GitHubActionsDeploymentRole \
  --assume-role-policy-document file://iam-policies/github-actions-trust-policy-dev.json \
  --description "Role for GitHub Actions to deploy to dev account"

sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-permissions-policy.json > \
  iam-policies/github-actions-permissions-policy-dev.json

aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://iam-policies/github-actions-permissions-policy-dev.json
```

### 6. Create GitHub Actions Role in QA Account

```bash
export AWS_PROFILE=turaf-qa
ACCOUNT_ID=965932217544

sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-trust-policy.json > \
  iam-policies/github-actions-trust-policy-qa.json

aws iam create-role \
  --role-name GitHubActionsDeploymentRole \
  --assume-role-policy-document file://iam-policies/github-actions-trust-policy-qa.json \
  --description "Role for GitHub Actions to deploy to QA account"

sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-permissions-policy.json > \
  iam-policies/github-actions-permissions-policy-qa.json

aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://iam-policies/github-actions-permissions-policy-qa.json
```

### 7. Create GitHub Actions Role in Prod Account

```bash
export AWS_PROFILE=turaf-prod
ACCOUNT_ID=811783768245

sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-trust-policy.json > \
  iam-policies/github-actions-trust-policy-prod.json

aws iam create-role \
  --role-name GitHubActionsDeploymentRole \
  --assume-role-policy-document file://iam-policies/github-actions-trust-policy-prod.json \
  --description "Role for GitHub Actions to deploy to prod account"

sed "s/\${ACCOUNT_ID}/${ACCOUNT_ID}/g" \
  iam-policies/github-actions-permissions-policy.json > \
  iam-policies/github-actions-permissions-policy-prod.json

aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://iam-policies/github-actions-permissions-policy-prod.json
```

---

## Verification

### 1. Verify OIDC Providers

```bash
# List OIDC providers in each account
for profile in turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws iam list-open-id-connect-providers --profile $profile
done

# Expected: OIDC provider for token.actions.githubusercontent.com in each account
```

### 2. Verify IAM Roles

```bash
# Get role details
for profile in turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws iam get-role \
    --role-name GitHubActionsDeploymentRole \
    --profile $profile \
    --query 'Role.Arn'
done

# Save ARNs for GitHub secrets
```

### 3. Test OIDC Authentication

Create test GitHub Actions workflow `.github/workflows/test-oidc.yml`:

```yaml
name: Test OIDC Authentication

on:
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

jobs:
  test-dev:
    runs-on: ubuntu-latest
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole
          aws-region: us-east-1
      
      - name: Test AWS access
        run: |
          aws sts get-caller-identity
          aws ecr describe-repositories || echo "No ECR repos yet"
```

**Run workflow and verify**:
- Workflow completes successfully
- AWS credentials are assumed
- No errors in authentication

---

## Automation Script

Create `scripts/setup-github-oidc.sh`:

```bash
#!/bin/bash

set -e

ACCOUNTS=(
  "ops:turaf-ops:146072879609"
  "dev:turaf-dev:801651112319"
  "qa:turaf-qa:965932217544"
  "prod:turaf-prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"
THUMBPRINT="6938fd4d98bab03faadb97b34396831e3780aea1"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  echo "Setting up OIDC for ${ENV} account (${ACCOUNT_ID})..."
  
  # Create OIDC provider
  aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list ${THUMBPRINT} \
    --profile ${PROFILE} || echo "OIDC provider already exists"
  
  # Create trust policy
  cat > /tmp/trust-policy-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:${GITHUB_REPO}:*"
      }
    }
  }]
}
EOF
  
  # Create IAM role
  aws iam create-role \
    --role-name GitHubActionsDeploymentRole \
    --assume-role-policy-document file:///tmp/trust-policy-${ENV}.json \
    --description "Role for GitHub Actions to deploy to ${ENV} account" \
    --profile ${PROFILE} || echo "Role already exists"
  
  # Attach permissions (simplified - add full policy as needed)
  aws iam attach-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-arn arn:aws:iam::aws:policy/PowerUserAccess \
    --profile ${PROFILE} || echo "Policy already attached"
  
  # Get role ARN
  ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsDeploymentRole \
    --profile ${PROFILE} \
    --query 'Role.Arn' \
    --output text)
  
  echo "✅ ${ENV}: ${ROLE_ARN}"
done

echo "All OIDC configurations complete!"
```

---

## Troubleshooting

### Issue: "InvalidIdentityToken" error

**Cause**: OIDC provider thumbprint incorrect or expired

**Solution**:
```bash
# Get current thumbprint
echo | openssl s_client -servername token.actions.githubusercontent.com \
  -connect token.actions.githubusercontent.com:443 2>/dev/null | \
  openssl x509 -fingerprint -noout | \
  sed 's/://g' | \
  awk -F= '{print tolower($2)}'

# Update OIDC provider with new thumbprint
```

### Issue: "AccessDenied" when assuming role

**Cause**: Trust policy doesn't match GitHub repository

**Solution**:
```bash
# Verify trust policy allows your repository
aws iam get-role \
  --role-name GitHubActionsDeploymentRole \
  --query 'Role.AssumeRolePolicyDocument'

# Update trust policy if needed
```

### Issue: Role has insufficient permissions

**Cause**: Permissions policy too restrictive

**Solution**:
```bash
# Review current policies
aws iam list-role-policies \
  --role-name GitHubActionsDeploymentRole

# Update permissions policy as needed
```

---

## Documentation

Create `infrastructure/github-oidc-roles.md`:

```markdown
# GitHub Actions OIDC Roles

## Role ARNs

| Account | Role ARN |
|---------|----------|
| Ops | arn:aws:iam::146072879609:role/GitHubActionsDeploymentRole |
| Dev | arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole |
| QA | arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole |
| Prod | arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole |

## Usage in GitHub Actions

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
    aws-region: us-east-1
```

## Permissions

- ECR: Push/pull images
- ECS: Update services, register task definitions
- IAM: Pass role to ECS tasks
- S3: Upload/download artifacts
- CloudWatch: Write logs
```

---

## Checklist

- [ ] Created OIDC provider in Ops account
- [ ] Created OIDC provider in dev account
- [ ] Created OIDC provider in qa account
- [ ] Created OIDC provider in prod account
- [ ] Created GitHubActionsDeploymentRole in Ops account
- [ ] Created GitHubActionsDeploymentRole in dev account
- [ ] Created GitHubActionsDeploymentRole in qa account
- [ ] Created GitHubActionsDeploymentRole in prod account
- [ ] Attached permissions policies to all roles
- [ ] Tested OIDC authentication with GitHub Actions
- [ ] Documented role ARNs
- [ ] Saved role ARNs for GitHub secrets (task 025)

---

## Next Steps

After OIDC configuration:
1. Proceed to task 023: Configure Amazon SES
2. Use role ARNs in task 025: Configure GitHub Environments and Secrets
3. Update CI/CD workflows to use OIDC authentication

---

## References

- [GitHub OIDC with AWS](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS IAM OIDC](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- specs/ci-cd-pipelines.md (Section: AWS OIDC Authentication)
- INFRASTRUCTURE_PLAN.md (Phase 2.2, 4.1)
