# Database Migration IAM Infrastructure Plan

This plan adds IAM roles, permissions, and infrastructure tasks to support the centralized Flyway migration service using AWS CodeBuild, including GitHub Actions integration and RDS network access configuration.

---

## Overview

**Objective**: Extend the infrastructure plan and tasks to support database migrations via the centralized flyway-service using AWS CodeBuild.

**Scope**:
1. Create new GitHub Actions IAM role for triggering migrations (`GitHubActionsFlywayRole`)
2. Create new CodeBuild IAM role for executing migrations (`CodeBuildFlywayRole`)
3. Update RDS security groups to allow CodeBuild access
4. Configure Secrets Manager permissions for database credentials
5. Update INFRASTRUCTURE_PLAN.md with migration-specific steps
6. Create new infrastructure tasks for migration infrastructure

---

## Architecture

### IAM Roles

**1. GitHubActionsFlywayRole** (per environment)
- **Purpose**: Allows GitHub Actions to trigger CodeBuild migration projects
- **Assumed by**: GitHub Actions via OIDC
- **Permissions**: 
  - Start CodeBuild builds
  - Get CodeBuild build status
  - Read CloudWatch Logs (for build output)

**2. CodeBuildFlywayRole** (per environment)
- **Purpose**: Allows CodeBuild to execute Flyway migrations
- **Assumed by**: CodeBuild service
- **Permissions**:
  - Read Secrets Manager (database credentials)
  - Write CloudWatch Logs
  - VPC network access (ENI creation)
  - Pull from ECR (if using custom Flyway image)

### Network Architecture

```
GitHub Actions (OIDC)
  ↓ (assumes GitHubActionsFlywayRole)
AWS CodeBuild Project
  ↓ (runs in VPC private subnet)
  ↓ (uses CodeBuildFlywayRole)
RDS PostgreSQL
  ↑ (security group allows CodeBuild)
```

### Security Groups

**CodeBuild Security Group**:
- Outbound: Allow PostgreSQL (5432) to RDS security group
- Outbound: Allow HTTPS (443) for package downloads

**RDS Security Group** (update):
- Inbound: Allow PostgreSQL (5432) from CodeBuild security group

---

## Implementation Plan

### Part 1: Update INFRASTRUCTURE_PLAN.md

**Location**: `/Users/ryanwaite28/Developer/portfolio-projects/Turaf/INFRASTRUCTURE_PLAN.md`

**Changes**:

#### 1. Add New Section: Phase 2.3 - Configure Database Migration IAM Roles

Insert after Phase 2.2 (around line 945):

```markdown
### 2.3 Configure Database Migration IAM Roles

**Objective**: Set up IAM roles for centralized database migrations via CodeBuild

**Accounts to configure**: dev, qa, prod

**Steps for EACH account**:

1. **Create GitHub Actions Flyway OIDC Role**:
   ```bash
   # Create trust policy for Flyway migrations
   cat > github-actions-flyway-trust-policy.json <<EOF
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
   EOF
   
   # Create role
   aws iam create-role \
     --role-name GitHubActionsFlywayRole \
     --assume-role-policy-document file://github-actions-flyway-trust-policy.json \
     --description "Role for GitHub Actions to trigger database migrations"
   
   # Create permissions policy
   cat > github-actions-flyway-permissions.json <<EOF
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
         "Resource": "arn:aws:codebuild:us-east-1:<ACCOUNT_ID>:project/turaf-flyway-migrations-*"
       },
       {
         "Sid": "CloudWatchLogs",
         "Effect": "Allow",
         "Action": [
           "logs:GetLogEvents",
           "logs:FilterLogEvents"
         ],
         "Resource": "arn:aws:logs:us-east-1:<ACCOUNT_ID>:log-group:/aws/codebuild/turaf-flyway-migrations-*"
       }
     ]
   }
   EOF
   
   # Attach permissions
   aws iam put-role-policy \
     --role-name GitHubActionsFlywayRole \
     --policy-name GitHubActionsFlywayPolicy \
     --policy-document file://github-actions-flyway-permissions.json
   ```

2. **Create CodeBuild Flyway Execution Role**:
   ```bash
   # Create trust policy for CodeBuild
   cat > codebuild-flyway-trust-policy.json <<EOF
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
     --assume-role-policy-document file://codebuild-flyway-trust-policy.json \
     --description "Role for CodeBuild to execute Flyway database migrations"
   
   # Create permissions policy
   cat > codebuild-flyway-permissions.json <<EOF
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
           "arn:aws:secretsmanager:us-east-1:<ACCOUNT_ID>:secret:turaf/db/master-*"
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
         "Resource": "arn:aws:logs:us-east-1:<ACCOUNT_ID>:log-group:/aws/codebuild/turaf-flyway-migrations-*"
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
     --policy-document file://codebuild-flyway-permissions.json
   ```

3. **Verify Roles Created**:
   ```bash
   # List roles
   aws iam list-roles --query 'Roles[?contains(RoleName, `Flyway`)].RoleName'
   
   # Get role ARNs
   aws iam get-role --role-name GitHubActionsFlywayRole --query 'Role.Arn'
   aws iam get-role --role-name CodeBuildFlywayRole --query 'Role.Arn'
   ```
```

#### 2. Update Phase 6 - Add Database Migration Infrastructure

Insert new subsection in Phase 6 (Terraform Infrastructure Deployment):

```markdown
### 6.X Deploy Database Migration Infrastructure

**Objective**: Create CodeBuild projects and security groups for database migrations

**Prerequisites**:
- Terraform database module deployed
- IAM roles created (GitHubActionsFlywayRole, CodeBuildFlywayRole)

**Steps**:

1. **Create CodeBuild Security Group** (via Terraform or manually):
   ```bash
   # Get VPC ID
   VPC_ID=$(aws ec2 describe-vpcs \
     --filters "Name=tag:Name,Values=turaf-vpc-dev" \
     --query 'Vpcs[0].VpcId' \
     --output text)
   
   # Create security group
   SG_ID=$(aws ec2 create-security-group \
     --group-name turaf-codebuild-flyway-dev \
     --description "Security group for CodeBuild Flyway migrations" \
     --vpc-id $VPC_ID \
     --query 'GroupId' \
     --output text)
   
   # Add outbound rule for RDS
   aws ec2 authorize-security-group-egress \
     --group-id $SG_ID \
     --protocol tcp \
     --port 5432 \
     --source-group <RDS_SECURITY_GROUP_ID>
   
   # Add outbound rule for HTTPS (package downloads)
   aws ec2 authorize-security-group-egress \
     --group-id $SG_ID \
     --protocol tcp \
     --port 443 \
     --cidr 0.0.0.0/0
   ```

2. **Update RDS Security Group**:
   ```bash
   # Add inbound rule from CodeBuild
   aws ec2 authorize-security-group-ingress \
     --group-id <RDS_SECURITY_GROUP_ID> \
     --protocol tcp \
     --port 5432 \
     --source-group $SG_ID
   ```

3. **Create CodeBuild Project**:
   ```bash
   # Get private subnet IDs
   SUBNET_IDS=$(aws ec2 describe-subnets \
     --filters "Name=tag:Name,Values=turaf-private-*-dev" \
     --query 'Subnets[*].SubnetId' \
     --output text | tr '\t' ',')
   
   # Create CodeBuild project
   aws codebuild create-project \
     --name turaf-flyway-migrations-dev \
     --source type=GITHUB,location=https://github.com/ryanwaite28/ai-projects-turaf.git,buildspec=services/flyway-service/buildspec.yml \
     --artifacts type=NO_ARTIFACTS \
     --environment type=LINUX_CONTAINER,image=aws/codebuild/standard:7.0,computeType=BUILD_GENERAL1_SMALL \
     --service-role arn:aws:iam::<ACCOUNT_ID>:role/CodeBuildFlywayRole \
     --vpc-config vpcId=$VPC_ID,subnets=[$SUBNET_IDS],securityGroupIds=[$SG_ID]
   ```

4. **Configure Environment Variables**:
   ```bash
   # Add environment variables to CodeBuild project
   aws codebuild update-project \
     --name turaf-flyway-migrations-dev \
     --environment environmentVariables='[
       {
         "name": "DB_HOST",
         "value": "<RDS_ENDPOINT>",
         "type": "PLAINTEXT"
       },
       {
         "name": "DB_NAME",
         "value": "turaf",
         "type": "PLAINTEXT"
       },
       {
         "name": "DB_USER",
         "value": "postgres",
         "type": "PLAINTEXT"
       },
       {
         "name": "DB_PASSWORD",
         "value": "turaf/db/master",
         "type": "SECRETS_MANAGER"
       }
     ]'
   ```
```

---

### Part 2: Create New Infrastructure Tasks

#### Task 026: Configure Database Migration IAM Roles

**File**: `tasks/infrastructure/026-configure-database-migration-iam-roles.md`

**Content**:

```markdown
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

- [ ] GitHubActionsFlywayRole created in dev, qa, prod accounts
- [ ] CodeBuildFlywayRole created in dev, qa, prod accounts
- [ ] Trust policies configured correctly
- [ ] Permissions policies attached with least-privilege access
- [ ] Roles tested with CodeBuild project
- [ ] Role ARNs documented for GitHub secrets

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

## Checklist

- [ ] GitHubActionsFlywayRole created in dev account
- [ ] GitHubActionsFlywayRole created in qa account
- [ ] GitHubActionsFlywayRole created in prod account
- [ ] CodeBuildFlywayRole created in dev account
- [ ] CodeBuildFlywayRole created in qa account
- [ ] CodeBuildFlywayRole created in prod account
- [ ] Trust policies verified
- [ ] Permissions policies verified
- [ ] Role ARNs documented
- [ ] Ready for Task 027 (network configuration)

---

## Next Steps

1. Proceed to **Task 027: Configure Database Migration Network Access**
2. Use CodeBuildFlywayRole ARN when creating CodeBuild project
3. Use GitHubActionsFlywayRole ARN in GitHub secrets (Task 012 update)
```

#### Task 027: Configure Database Migration Network Access

**File**: `tasks/infrastructure/027-configure-database-migration-network-access.md`

**Content**:

```markdown
# Task: Configure Database Migration Network Access

**Service**: Infrastructure  
**Type**: Network Security  
**Priority**: High  
**Estimated Time**: 1 hour  
**Dependencies**: 026-configure-database-migration-iam-roles, 016-create-database-module, 014-create-networking-module

---

## Objective

Configure VPC security groups to allow AWS CodeBuild to connect to RDS PostgreSQL for executing Flyway migrations.

---

## Acceptance Criteria

- [ ] CodeBuild security group created in each environment
- [ ] RDS security group updated to allow CodeBuild access
- [ ] Security group rules tested and verified
- [ ] Network access documented

---

## Implementation

### 1. Create CodeBuild Security Group

**For Each Environment** (dev, qa, prod):

```bash
# Set environment variables
export AWS_PROFILE=turaf-dev  # or turaf-qa, turaf-prod
export ENV=dev  # or qa, prod

# Get VPC ID
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
  --query 'Vpcs[0].VpcId' \
  --output text)

echo "VPC ID: $VPC_ID"

# Create security group for CodeBuild
SG_ID=$(aws ec2 create-security-group \
  --group-name turaf-codebuild-flyway-${ENV} \
  --description "Security group for CodeBuild Flyway migrations in ${ENV}" \
  --vpc-id $VPC_ID \
  --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=turaf-codebuild-flyway-${ENV}},{Key=Environment,Value=${ENV}},{Key=Service,Value=flyway-service}]" \
  --query 'GroupId' \
  --output text)

echo "CodeBuild Security Group ID: $SG_ID"

# Get RDS security group ID
RDS_SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

echo "RDS Security Group ID: $RDS_SG_ID"

# Remove default egress rule (allow all)
aws ec2 revoke-security-group-egress \
  --group-id $SG_ID \
  --ip-permissions IpProtocol=-1,IpRanges='[{CidrIp=0.0.0.0/0}]'

# Add egress rule for PostgreSQL to RDS
aws ec2 authorize-security-group-egress \
  --group-id $SG_ID \
  --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${RDS_SG_ID},Description='Allow PostgreSQL to RDS'}]"

# Add egress rule for HTTPS (package downloads, GitHub)
aws ec2 authorize-security-group-egress \
  --group-id $SG_ID \
  --ip-permissions IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTPS for package downloads'}]"

# Add egress rule for HTTP (package downloads)
aws ec2 authorize-security-group-egress \
  --group-id $SG_ID \
  --ip-permissions IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTP for package downloads'}]"

echo "✅ CodeBuild security group created: $SG_ID"
```

### 2. Update RDS Security Group

```bash
# Add ingress rule to RDS security group
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG_ID \
  --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${SG_ID},Description='Allow PostgreSQL from CodeBuild Flyway'}]"

echo "✅ RDS security group updated to allow CodeBuild access"
```

### 3. Verify Security Group Rules

```bash
# Describe CodeBuild security group
aws ec2 describe-security-groups \
  --group-ids $SG_ID \
  --query 'SecurityGroups[0].{Ingress:IpPermissions,Egress:IpPermissionsEgress}'

# Describe RDS security group
aws ec2 describe-security-groups \
  --group-ids $RDS_SG_ID \
  --query 'SecurityGroups[0].IpPermissions'

# Verify CodeBuild can reach RDS on port 5432
```

---

## Verification

### 1. Test Network Connectivity

Create a test CodeBuild project to verify connectivity:

```bash
# Create test buildspec
cat > /tmp/test-buildspec.yml <<EOF
version: 0.2
phases:
  build:
    commands:
      - echo "Testing RDS connectivity..."
      - apt-get update && apt-get install -y postgresql-client
      - psql -h \$DB_HOST -U \$DB_USER -d \$DB_NAME -c "SELECT version();"
EOF

# Create temporary test project
aws codebuild create-project \
  --name turaf-flyway-network-test-${ENV} \
  --source type=NO_SOURCE,buildspec=/tmp/test-buildspec.yml \
  --artifacts type=NO_ARTIFACTS \
  --environment type=LINUX_CONTAINER,image=aws/codebuild/standard:7.0,computeType=BUILD_GENERAL1_SMALL \
  --service-role arn:aws:iam::${ACCOUNT_ID}:role/CodeBuildFlywayRole \
  --vpc-config vpcId=${VPC_ID},subnets=[${SUBNET_IDS}],securityGroupIds=[${SG_ID}]

# Start build
BUILD_ID=$(aws codebuild start-build \
  --project-name turaf-flyway-network-test-${ENV} \
  --query 'build.id' \
  --output text)

# Wait for completion
aws codebuild wait build-complete --ids $BUILD_ID

# Check build status
aws codebuild batch-get-builds \
  --ids $BUILD_ID \
  --query 'builds[0].buildStatus'

# Delete test project
aws codebuild delete-project --name turaf-flyway-network-test-${ENV}
```

### 2. Verify Security Group Tags

```bash
# Verify CodeBuild security group tags
aws ec2 describe-security-groups \
  --group-ids $SG_ID \
  --query 'SecurityGroups[0].Tags'

# Expected tags:
# - Name: turaf-codebuild-flyway-{env}
# - Environment: {env}
# - Service: flyway-service
```

---

## Documentation

### Security Group IDs

Document the following security group IDs for use in CodeBuild project creation:

| Environment | CodeBuild SG ID | RDS SG ID | VPC ID |
|-------------|-----------------|-----------|--------|
| Dev | sg-xxxxx | sg-yyyyy | vpc-zzzzz |
| QA | sg-xxxxx | sg-yyyyy | vpc-zzzzz |
| Prod | sg-xxxxx | sg-yyyyy | vpc-zzzzz |

### Network Flow

```
CodeBuild (Private Subnet)
  ↓ (Security Group: turaf-codebuild-flyway-{env})
  ↓ Egress: TCP 5432 → RDS Security Group
  ↓
RDS PostgreSQL (Private Subnet)
  ↑ (Security Group: turaf-rds-{env})
  ↑ Ingress: TCP 5432 ← CodeBuild Security Group
```

---

## Troubleshooting

### Issue: CodeBuild cannot connect to RDS

**Symptoms**: Connection timeout or "could not connect to server"

**Diagnosis**:
```bash
# Check security group rules
aws ec2 describe-security-groups --group-ids $SG_ID
aws ec2 describe-security-groups --group-ids $RDS_SG_ID

# Verify CodeBuild is in correct subnets
aws codebuild batch-get-projects \
  --names turaf-flyway-migrations-${ENV} \
  --query 'projects[0].vpcConfig'
```

**Solutions**:
1. Verify CodeBuild security group has egress to RDS on port 5432
2. Verify RDS security group has ingress from CodeBuild on port 5432
3. Verify CodeBuild is running in private subnets with route to RDS
4. Check VPC flow logs for dropped packets

### Issue: CodeBuild cannot download packages

**Symptoms**: "Could not resolve host" or download failures

**Diagnosis**:
```bash
# Check egress rules for HTTPS
aws ec2 describe-security-groups \
  --group-ids $SG_ID \
  --query 'SecurityGroups[0].IpPermissionsEgress'
```

**Solutions**:
1. Verify egress rule for HTTPS (443) to 0.0.0.0/0
2. Verify NAT Gateway exists in public subnet
3. Verify private subnet route table has route to NAT Gateway

---

## Automation Script

Create `scripts/setup-flyway-network.sh`:

```bash
#!/bin/bash

set -e

ENVIRONMENTS=("dev" "qa" "prod")
PROFILES=("turaf-dev" "turaf-qa" "turaf-prod")

for i in "${!ENVIRONMENTS[@]}"; do
  ENV="${ENVIRONMENTS[$i]}"
  PROFILE="${PROFILES[$i]}"
  
  echo "=== Configuring network for ${ENV} ==="
  
  export AWS_PROFILE=$PROFILE
  
  # Get VPC ID
  VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
    --query 'Vpcs[0].VpcId' \
    --output text)
  
  # Create CodeBuild security group
  SG_ID=$(aws ec2 create-security-group \
    --group-name turaf-codebuild-flyway-${ENV} \
    --description "Security group for CodeBuild Flyway migrations" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=turaf-codebuild-flyway-${ENV}},{Key=Environment,Value=${ENV}}]" \
    --query 'GroupId' \
    --output text) || echo "Security group may already exist"
  
  # Get RDS security group
  RDS_SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text)
  
  # Configure rules
  aws ec2 revoke-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=-1,IpRanges='[{CidrIp=0.0.0.0/0}]' || true
  
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${RDS_SG_ID}}]" || true
  
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges="[{CidrIp=0.0.0.0/0}]" || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id $RDS_SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${SG_ID}}]" || true
  
  echo "✅ ${ENV}: CodeBuild SG=$SG_ID, RDS SG=$RDS_SG_ID"
done

echo "All environments configured!"
```

---

## Checklist

- [ ] CodeBuild security group created in dev
- [ ] CodeBuild security group created in qa
- [ ] CodeBuild security group created in prod
- [ ] RDS security group updated in dev
- [ ] RDS security group updated in qa
- [ ] RDS security group updated in prod
- [ ] Network connectivity tested
- [ ] Security group IDs documented
- [ ] Ready for Task 028 (CodeBuild project creation)

---

## Next Steps

1. Proceed to **Task 028: Create CodeBuild Migration Projects**
2. Use CodeBuild security group ID in CodeBuild VPC configuration
3. Test end-to-end migration execution
```

#### Task 028: Create CodeBuild Migration Projects

**File**: `tasks/infrastructure/028-create-codebuild-migration-projects.md`

**Content Summary**:
- Create CodeBuild project for each environment
- Configure VPC settings with security groups from Task 027
- Configure environment variables (DB_HOST, DB_NAME, DB_USER, DB_PASSWORD from Secrets Manager)
- Set service role to CodeBuildFlywayRole from Task 026
- Configure source to GitHub repository
- Set buildspec path to `services/flyway-service/buildspec.yml`
- Test project execution

---

### Part 3: Update TASK_ORDER.md

**File**: `tasks/infrastructure/TASK_ORDER.md`

**Changes**:
1. Add three new tasks after Task 025:
   - Task 026: Configure Database Migration IAM Roles
   - Task 027: Configure Database Migration Network Access
   - Task 028: Create CodeBuild Migration Projects

2. Update task count from 25 to 28

3. Add dependencies:
   - Task 026 depends on: 009, 016
   - Task 027 depends on: 026, 016, 014
   - Task 028 depends on: 026, 027

---

### Part 4: Update Task 012 (GitHub Secrets)

**File**: `tasks/infrastructure/012-configure-github-environments-secrets.md`

**Changes**:
Add new secrets for Flyway migration role ARNs:

```markdown
### Flyway Migration Secrets

| Secret Name | Value | Environment |
|-------------|-------|-------------|
| AWS_FLYWAY_ROLE_DEV | arn:aws:iam::801651112319:role/GitHubActionsFlywayRole | dev |
| AWS_FLYWAY_ROLE_QA | arn:aws:iam::965932217544:role/GitHubActionsFlywayRole | qa |
| AWS_FLYWAY_ROLE_PROD | arn:aws:iam::811783768245:role/GitHubActionsFlywayRole | prod |
```

---

## Summary of Changes

### Files to Create (3):
1. `tasks/infrastructure/026-configure-database-migration-iam-roles.md`
2. `tasks/infrastructure/027-configure-database-migration-network-access.md`
3. `tasks/infrastructure/028-create-codebuild-migration-projects.md`

### Files to Modify (3):
1. `INFRASTRUCTURE_PLAN.md` - Add Phase 2.3 and Phase 6.X
2. `tasks/infrastructure/TASK_ORDER.md` - Add tasks 026-028
3. `tasks/infrastructure/012-configure-github-environments-secrets.md` - Add Flyway role ARNs

### IAM Roles Created (6 total, 2 per environment):
- **GitHubActionsFlywayRole** (dev, qa, prod) - Triggers migrations
- **CodeBuildFlywayRole** (dev, qa, prod) - Executes migrations

### Security Groups Created (3):
- **turaf-codebuild-flyway-dev**
- **turaf-codebuild-flyway-qa**
- **turaf-codebuild-flyway-prod**

### CodeBuild Projects Created (3):
- **turaf-flyway-migrations-dev**
- **turaf-flyway-migrations-qa**
- **turaf-flyway-migrations-prod**

---

## Implementation Order

1. Create Task 026 file (IAM roles)
2. Create Task 027 file (Network access)
3. Create Task 028 file (CodeBuild projects)
4. Update INFRASTRUCTURE_PLAN.md (add Phase 2.3 and 6.X)
5. Update TASK_ORDER.md (add tasks 026-028)
6. Update Task 012 (add Flyway secrets)

---

## Benefits

1. **Separation of Concerns**: Dedicated roles for migration vs deployment
2. **Least Privilege**: Each role has only necessary permissions
3. **Auditability**: Clear separation in CloudTrail logs
4. **Security**: Network isolation via security groups
5. **Maintainability**: Clear task breakdown for implementation
6. **Testability**: Each component can be tested independently

---

## Security Considerations

1. **Read-Only Database Access**: CodeBuild uses master password but only for migrations
2. **VPC Isolation**: CodeBuild runs in private subnets with no internet access (except via NAT)
3. **Secrets Manager**: Database credentials never exposed in code or logs
4. **OIDC Authentication**: No long-lived credentials for GitHub Actions
5. **Security Group Restrictions**: Only specific ports and sources allowed

---

## Testing Strategy

1. **IAM Role Testing**: Use AWS STS to verify role assumptions
2. **Network Testing**: Create temporary CodeBuild project to test RDS connectivity
3. **End-to-End Testing**: Run actual migration via GitHub Actions workflow
4. **Rollback Testing**: Verify migration failures don't break infrastructure
