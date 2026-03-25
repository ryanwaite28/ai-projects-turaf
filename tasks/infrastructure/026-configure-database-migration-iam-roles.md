# Task: Configure Database Migration IAM Roles

**Service**: Infrastructure  
**Type**: IAM Security  
**Priority**: High  
**Estimated Time**: 1.5 hours  
**Dependencies**: 009-configure-iam-oidc-github-actions, 016-create-database-module

---

## Objective

Create dedicated IAM roles for the centralized Flyway migration service:
1. GitHub Actions role to trigger migrations via CodeBuild
2. CodeBuild role to execute migrations with RDS and Secrets Manager access

---

## Acceptance Criteria

- [x] GitHubActionsFlywayRole created in dev, qa, prod accounts
- [x] CodeBuildFlywayRole created in dev, qa, prod accounts
- [x] Trust policies configured correctly
- [x] Permissions policies attached with least-privilege access
- [x] Automation script created (setup-flyway-iam-roles.sh)
- [x] Verification script created (verify-flyway-iam-roles.sh)
- [x] Documentation created (FLYWAY_IAM_README.md)
- [ ] Roles tested with CodeBuild project (requires Task 028)
- [x] Role ARNs documented for GitHub secrets

---

## Implementation

### 1. Create GitHub Actions Flyway Role

**For Each Account** (dev, qa, prod):

```bash
# Set environment variables
export AWS_PROFILE=turaf-dev  # or turaf-qa, turaf-prod
export ACCOUNT_ID=801651112319  # or 965932217544, 811783768245
export ENV=dev  # or qa, prod

# Create trust policy
cat > /tmp/github-actions-flyway-trust-${ENV}.json <<EOF
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
        "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
      }
    }
  }]
}
EOF

# Create role
aws iam create-role \
  --role-name GitHubActionsFlywayRole \
  --assume-role-policy-document file:///tmp/github-actions-flyway-trust-${ENV}.json \
  --description "Role for GitHub Actions to trigger Flyway database migrations" \
  --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service

# Create permissions policy
cat > /tmp/github-actions-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeBuildPermissions",
      "Effect": "Allow",
      "Action": [
        "codebuild:StartBuild",
        "codebuild:BatchGetBuilds"
      ],
      "Resource": "arn:aws:codebuild:us-east-1:${ACCOUNT_ID}:project/turaf-flyway-migrations-${ENV}"
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:GetLogEvents",
        "logs:FilterLogEvents"
      ],
      "Resource": "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
    }
  ]
}
EOF

# Attach permissions
aws iam put-role-policy \
  --role-name GitHubActionsFlywayRole \
  --policy-name GitHubActionsFlywayPolicy \
  --policy-document file:///tmp/github-actions-flyway-permissions-${ENV}.json

# Get role ARN
aws iam get-role \
  --role-name GitHubActionsFlywayRole \
  --query 'Role.Arn' \
  --output text
```

### 2. Create CodeBuild Flyway Execution Role

**For Each Account** (dev, qa, prod):

```bash
# Create trust policy
cat > /tmp/codebuild-flyway-trust-${ENV}.json <<EOF
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
EOF

# Create role
aws iam create-role \
  --role-name CodeBuildFlywayRole \
  --assume-role-policy-document file:///tmp/codebuild-flyway-trust-${ENV}.json \
  --description "Role for CodeBuild to execute Flyway database migrations" \
  --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service

# Create permissions policy
cat > /tmp/codebuild-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SecretsManagerAccess",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:turaf/db/master-*"
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
      "Resource": [
        "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}",
        "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
      ]
    },
    {
      "Sid": "VPCAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeDhcpOptions",
        "ec2:DescribeVpcs",
        "ec2:CreateNetworkInterfacePermission"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
EOF

# Attach permissions
aws iam put-role-policy \
  --role-name CodeBuildFlywayRole \
  --policy-name CodeBuildFlywayPolicy \
  --policy-document file:///tmp/codebuild-flyway-permissions-${ENV}.json

# Get role ARN
aws iam get-role \
  --role-name CodeBuildFlywayRole \
  --query 'Role.Arn' \
  --output text
```

---

## Verification

### 1. Verify Roles Created

```bash
# List Flyway-related roles
for profile in turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws iam list-roles \
    --query 'Roles[?contains(RoleName, `Flyway`)].RoleName' \
    --profile $profile
done

# Expected output: GitHubActionsFlywayRole, CodeBuildFlywayRole in each account
```

### 2. Verify Trust Policies

```bash
# Check GitHub Actions role trust policy
aws iam get-role \
  --role-name GitHubActionsFlywayRole \
  --query 'Role.AssumeRolePolicyDocument' \
  --profile turaf-dev

# Verify OIDC provider is federated principal
# Verify repository condition is correct

# Check CodeBuild role trust policy
aws iam get-role \
  --role-name CodeBuildFlywayRole \
  --query 'Role.AssumeRolePolicyDocument' \
  --profile turaf-dev

# Verify codebuild.amazonaws.com is the principal
```

### 3. Verify Permissions Policies

```bash
# List attached policies for GitHub Actions role
aws iam list-role-policies \
  --role-name GitHubActionsFlywayRole \
  --profile turaf-dev

# Get policy document
aws iam get-role-policy \
  --role-name GitHubActionsFlywayRole \
  --policy-name GitHubActionsFlywayPolicy \
  --profile turaf-dev

# Verify CodeBuild permissions
# List attached policies for CodeBuild role
aws iam list-role-policies \
  --role-name CodeBuildFlywayRole \
  --profile turaf-dev

# Get policy document
aws iam get-role-policy \
  --role-name CodeBuildFlywayRole \
  --policy-name CodeBuildFlywayPolicy \
  --profile turaf-dev

# Verify Secrets Manager, CloudWatch, VPC, ECR permissions
```

---

## Documentation

### Role ARNs

Document the following ARNs for use in GitHub secrets and Terraform:

| Account | GitHubActionsFlywayRole ARN | CodeBuildFlywayRole ARN |
|---------|----------------------------|-------------------------|
| Dev | arn:aws:iam::801651112319:role/GitHubActionsFlywayRole | arn:aws:iam::801651112319:role/CodeBuildFlywayRole |
| QA | arn:aws:iam::965932217544:role/GitHubActionsFlywayRole | arn:aws:iam::965932217544:role/CodeBuildFlywayRole |
| Prod | arn:aws:iam::811783768245:role/GitHubActionsFlywayRole | arn:aws:iam::811783768245:role/CodeBuildFlywayRole |

### Permissions Summary

**GitHubActionsFlywayRole**:
- Start CodeBuild builds for migration projects
- Get CodeBuild build status
- Read CloudWatch Logs for migration output

**CodeBuildFlywayRole**:
- Read database credentials from Secrets Manager
- Create network interfaces in VPC
- Write logs to CloudWatch
- Pull images from ECR (if needed)

---

## Troubleshooting

### Issue: "AccessDenied" when GitHub Actions assumes role

**Cause**: OIDC provider not configured or trust policy incorrect

**Solution**:
```bash
# Verify OIDC provider exists
aws iam list-open-id-connect-providers --profile turaf-dev

# Verify trust policy
aws iam get-role \
  --role-name GitHubActionsFlywayRole \
  --query 'Role.AssumeRolePolicyDocument' \
  --profile turaf-dev
```

### Issue: CodeBuild cannot read Secrets Manager

**Cause**: Missing permissions or incorrect secret ARN

**Solution**:
```bash
# Verify secret exists
aws secretsmanager list-secrets \
  --query 'SecretList[?contains(Name, `turaf/db/master`)].ARN' \
  --profile turaf-dev

# Update permissions policy with correct ARN
```

### Issue: CodeBuild cannot connect to RDS

**Cause**: VPC configuration or security group rules

**Solution**: See Task 027 for network configuration

---

## Automation Script

Create `scripts/setup-flyway-iam-roles.sh`:

```bash
#!/bin/bash

set -e

ACCOUNTS=(
  "dev:turaf-dev:801651112319"
  "qa:turaf-qa:965932217544"
  "prod:turaf-prod:811783768245"
)

GITHUB_REPO="ryanwaite28/ai-projects-turaf"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE=$(echo "$account" | cut -d: -f2)
  ACCOUNT_ID=$(echo "$account" | cut -d: -f3)
  
  echo "=== Setting up IAM roles for ${ENV} account (${ACCOUNT_ID}) ==="
  
  export AWS_PROFILE=$PROFILE
  
  # Create GitHub Actions Flyway Role
  cat > /tmp/github-actions-flyway-trust-${ENV}.json <<EOF
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
  
  aws iam create-role \
    --role-name GitHubActionsFlywayRole \
    --assume-role-policy-document file:///tmp/github-actions-flyway-trust-${ENV}.json \
    --description "Role for GitHub Actions to trigger Flyway database migrations" \
    --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service \
    || echo "GitHubActionsFlywayRole already exists"
  
  cat > /tmp/github-actions-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeBuildPermissions",
      "Effect": "Allow",
      "Action": [
        "codebuild:StartBuild",
        "codebuild:BatchGetBuilds"
      ],
      "Resource": "arn:aws:codebuild:us-east-1:${ACCOUNT_ID}:project/turaf-flyway-migrations-${ENV}"
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:GetLogEvents",
        "logs:FilterLogEvents"
      ],
      "Resource": "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
    }
  ]
}
EOF
  
  aws iam put-role-policy \
    --role-name GitHubActionsFlywayRole \
    --policy-name GitHubActionsFlywayPolicy \
    --policy-document file:///tmp/github-actions-flyway-permissions-${ENV}.json
  
  # Create CodeBuild Flyway Role
  cat > /tmp/codebuild-flyway-trust-${ENV}.json <<EOF
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
EOF
  
  aws iam create-role \
    --role-name CodeBuildFlywayRole \
    --assume-role-policy-document file:///tmp/codebuild-flyway-trust-${ENV}.json \
    --description "Role for CodeBuild to execute Flyway database migrations" \
    --tags Key=Environment,Value=${ENV} Key=Service,Value=flyway-service \
    || echo "CodeBuildFlywayRole already exists"
  
  cat > /tmp/codebuild-flyway-permissions-${ENV}.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SecretsManagerAccess",
      "Effect": "Allow",
      "Action": ["secretsmanager:GetSecretValue"],
      "Resource": ["arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:turaf/db/master-*"]
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": "arn:aws:logs:us-east-1:${ACCOUNT_ID}:log-group:/aws/codebuild/turaf-flyway-migrations-${ENV}:*"
    },
    {
      "Sid": "VPCAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeDhcpOptions",
        "ec2:DescribeVpcs",
        "ec2:CreateNetworkInterfacePermission"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
EOF
  
  aws iam put-role-policy \
    --role-name CodeBuildFlywayRole \
    --policy-name CodeBuildFlywayPolicy \
    --policy-document file:///tmp/codebuild-flyway-permissions-${ENV}.json
  
  # Get role ARNs
  GH_ROLE_ARN=$(aws iam get-role \
    --role-name GitHubActionsFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  CB_ROLE_ARN=$(aws iam get-role \
    --role-name CodeBuildFlywayRole \
    --query 'Role.Arn' \
    --output text)
  
  echo "✅ ${ENV}:"
  echo "   GitHubActionsFlywayRole: ${GH_ROLE_ARN}"
  echo "   CodeBuildFlywayRole: ${CB_ROLE_ARN}"
done

echo ""
echo "All IAM roles configured successfully!"
```

---

## Checklist

- [x] GitHubActionsFlywayRole created in dev account
- [x] GitHubActionsFlywayRole created in qa account
- [x] GitHubActionsFlywayRole created in prod account
- [x] CodeBuildFlywayRole created in dev account
- [x] CodeBuildFlywayRole created in qa account
- [x] CodeBuildFlywayRole created in prod account
- [x] Trust policies verified
- [x] Permissions policies verified
- [x] Role ARNs documented
- [x] Ready for Task 027 (network configuration)

---

## Next Steps

1. Proceed to **Task 027: Configure Database Migration Network Access**
2. Use CodeBuildFlywayRole ARN when creating CodeBuild project
3. Use GitHubActionsFlywayRole ARN in GitHub secrets (Task 012 update)

---

## References

- [GitHub OIDC with AWS](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS IAM Roles](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html)
- [AWS CodeBuild IAM](https://docs.aws.amazon.com/codebuild/latest/userguide/auth-and-access-control-iam-identity-based-access-control.html)
- INFRASTRUCTURE_PLAN.md (Phase 2.3)
- specs/flyway-service.md
