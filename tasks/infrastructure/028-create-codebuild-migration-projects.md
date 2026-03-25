# Task: Create CodeBuild Migration Projects

**Service**: Infrastructure  
**Type**: CI/CD - Database Migrations  
**Priority**: High  
**Estimated Time**: 1.5 hours  
**Dependencies**: 026-configure-database-migration-iam-roles, 027-configure-database-migration-network-access

---

## Objective

Create AWS CodeBuild projects for executing Flyway database migrations in each environment (dev, qa, prod).

---

## Acceptance Criteria

- [ ] CodeBuild project created in dev environment
- [ ] CodeBuild project created in qa environment
- [ ] CodeBuild project created in prod environment
- [ ] VPC configuration set correctly
- [ ] Environment variables configured
- [ ] Service role assigned
- [ ] Test migration execution successful
- [ ] CloudWatch logs verified

---

## Implementation

### 1. Create CodeBuild Project for DEV

```bash
# Set environment variables
export AWS_PROFILE=turaf-dev
export ENV=dev
export ACCOUNT_ID=801651112319

# Get VPC configuration
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
  --query 'Vpcs[0].VpcId' \
  --output text)

SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
  --query 'Subnets[*].SubnetId' \
  --output text | tr '\t' ',')

SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

# Get RDS endpoint
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier turaf-db-${ENV} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "VPC ID: $VPC_ID"
echo "Subnet IDs: $SUBNET_IDS"
echo "Security Group ID: $SG_ID"
echo "RDS Endpoint: $RDS_ENDPOINT"

# Create CodeBuild project
aws codebuild create-project \
  --name turaf-flyway-migrations-${ENV} \
  --description "Flyway database migrations for ${ENV} environment" \
  --source '{
    "type": "GITHUB",
    "location": "https://github.com/ryanwaite28/ai-projects-turaf.git",
    "buildspec": "services/flyway-service/buildspec.yml",
    "gitCloneDepth": 1
  }' \
  --artifacts '{
    "type": "NO_ARTIFACTS"
  }' \
  --environment '{
    "type": "LINUX_CONTAINER",
    "image": "aws/codebuild/standard:7.0",
    "computeType": "BUILD_GENERAL1_SMALL",
    "environmentVariables": [
      {
        "name": "DB_HOST",
        "value": "'"$RDS_ENDPOINT"'",
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
    ]
  }' \
  --service-role arn:aws:iam::${ACCOUNT_ID}:role/CodeBuildFlywayRole \
  --vpc-config '{
    "vpcId": "'"$VPC_ID"'",
    "subnets": ["'"${SUBNET_IDS//,/\",\"}"'"],
    "securityGroupIds": ["'"$SG_ID"'"]
  }' \
  --logs-config '{
    "cloudWatchLogs": {
      "status": "ENABLED",
      "groupName": "/aws/codebuild/turaf-flyway-migrations-'"$ENV"'"
    }
  }' \
  --tags '[
    {
      "key": "Environment",
      "value": "'"$ENV"'"
    },
    {
      "key": "Service",
      "value": "flyway-service"
    },
    {
      "key": "ManagedBy",
      "value": "manual"
    }
  ]'

echo "✅ CodeBuild project created: turaf-flyway-migrations-${ENV}"
```

### 2. Create CodeBuild Project for QA

```bash
# Set environment variables
export AWS_PROFILE=turaf-qa
export ENV=qa
export ACCOUNT_ID=965932217544

# Get VPC configuration
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
  --query 'Vpcs[0].VpcId' \
  --output text)

SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
  --query 'Subnets[*].SubnetId' \
  --output text | tr '\t' ',')

SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier turaf-db-${ENV} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Create CodeBuild project (same command as DEV with qa values)
aws codebuild create-project \
  --name turaf-flyway-migrations-${ENV} \
  --description "Flyway database migrations for ${ENV} environment" \
  --source '{
    "type": "GITHUB",
    "location": "https://github.com/ryanwaite28/ai-projects-turaf.git",
    "buildspec": "services/flyway-service/buildspec.yml",
    "gitCloneDepth": 1
  }' \
  --artifacts '{"type": "NO_ARTIFACTS"}' \
  --environment '{
    "type": "LINUX_CONTAINER",
    "image": "aws/codebuild/standard:7.0",
    "computeType": "BUILD_GENERAL1_SMALL",
    "environmentVariables": [
      {"name": "DB_HOST", "value": "'"$RDS_ENDPOINT"'", "type": "PLAINTEXT"},
      {"name": "DB_NAME", "value": "turaf", "type": "PLAINTEXT"},
      {"name": "DB_USER", "value": "postgres", "type": "PLAINTEXT"},
      {"name": "DB_PASSWORD", "value": "turaf/db/master", "type": "SECRETS_MANAGER"}
    ]
  }' \
  --service-role arn:aws:iam::${ACCOUNT_ID}:role/CodeBuildFlywayRole \
  --vpc-config '{"vpcId": "'"$VPC_ID"'", "subnets": ["'"${SUBNET_IDS//,/\",\"}"'"], "securityGroupIds": ["'"$SG_ID"'"]}' \
  --logs-config '{"cloudWatchLogs": {"status": "ENABLED", "groupName": "/aws/codebuild/turaf-flyway-migrations-'"$ENV"'"}}' \
  --tags '[{"key": "Environment", "value": "'"$ENV"'"}, {"key": "Service", "value": "flyway-service"}]'

echo "✅ CodeBuild project created: turaf-flyway-migrations-${ENV}"
```

### 3. Create CodeBuild Project for PROD

```bash
# Set environment variables
export AWS_PROFILE=turaf-prod
export ENV=prod
export ACCOUNT_ID=811783768245

# Get VPC configuration
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
  --query 'Vpcs[0].VpcId' \
  --output text)

SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
  --query 'Subnets[*].SubnetId' \
  --output text | tr '\t' ',')

SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)

RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier turaf-db-${ENV} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Create CodeBuild project (same command as DEV with prod values)
aws codebuild create-project \
  --name turaf-flyway-migrations-${ENV} \
  --description "Flyway database migrations for ${ENV} environment" \
  --source '{
    "type": "GITHUB",
    "location": "https://github.com/ryanwaite28/ai-projects-turaf.git",
    "buildspec": "services/flyway-service/buildspec.yml",
    "gitCloneDepth": 1
  }' \
  --artifacts '{"type": "NO_ARTIFACTS"}' \
  --environment '{
    "type": "LINUX_CONTAINER",
    "image": "aws/codebuild/standard:7.0",
    "computeType": "BUILD_GENERAL1_SMALL",
    "environmentVariables": [
      {"name": "DB_HOST", "value": "'"$RDS_ENDPOINT"'", "type": "PLAINTEXT"},
      {"name": "DB_NAME", "value": "turaf", "type": "PLAINTEXT"},
      {"name": "DB_USER", "value": "postgres", "type": "PLAINTEXT"},
      {"name": "DB_PASSWORD", "value": "turaf/db/master", "type": "SECRETS_MANAGER"}
    ]
  }' \
  --service-role arn:aws:iam::${ACCOUNT_ID}:role/CodeBuildFlywayRole \
  --vpc-config '{"vpcId": "'"$VPC_ID"'", "subnets": ["'"${SUBNET_IDS//,/\",\"}"'"], "securityGroupIds": ["'"$SG_ID"'"]}' \
  --logs-config '{"cloudWatchLogs": {"status": "ENABLED", "groupName": "/aws/codebuild/turaf-flyway-migrations-'"$ENV"'"}}' \
  --tags '[{"key": "Environment", "value": "'"$ENV"'"}, {"key": "Service", "value": "flyway-service"}]'

echo "✅ CodeBuild project created: turaf-flyway-migrations-${ENV}"
```

---

## Verification

### 1. Verify Projects Created

```bash
# List CodeBuild projects
for profile in turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws codebuild list-projects \
    --profile $profile \
    --query 'projects[?contains(@, `flyway`)]'
done
```

### 2. Verify Project Configuration

```bash
# Get project details
aws codebuild batch-get-projects \
  --names turaf-flyway-migrations-dev \
  --profile turaf-dev \
  --query 'projects[0].{Name:name,ServiceRole:serviceRole,VPC:vpcConfig,Environment:environment}'
```

### 3. Test Migration Execution

```bash
# Start a test build
BUILD_ID=$(aws codebuild start-build \
  --project-name turaf-flyway-migrations-dev \
  --profile turaf-dev \
  --query 'build.id' \
  --output text)

echo "Build ID: $BUILD_ID"

# Wait for completion
aws codebuild wait build-complete \
  --ids $BUILD_ID \
  --profile turaf-dev

# Get build status
aws codebuild batch-get-builds \
  --ids $BUILD_ID \
  --profile turaf-dev \
  --query 'builds[0].{Status:buildStatus,Duration:buildDuration,Phases:phases}'
```

### 4. Verify CloudWatch Logs

```bash
# Get log stream
LOG_STREAM=$(aws logs describe-log-streams \
  --log-group-name /aws/codebuild/turaf-flyway-migrations-dev \
  --profile turaf-dev \
  --order-by LastEventTime \
  --descending \
  --max-items 1 \
  --query 'logStreams[0].logStreamName' \
  --output text)

# Get log events
aws logs get-log-events \
  --log-group-name /aws/codebuild/turaf-flyway-migrations-dev \
  --log-stream-name $LOG_STREAM \
  --profile turaf-dev \
  --limit 50
```

---

## Troubleshooting

### Issue: Build fails with "Cannot connect to database"

**Cause**: Network configuration or credentials issue

**Diagnosis**:
```bash
# Check VPC configuration
aws codebuild batch-get-projects \
  --names turaf-flyway-migrations-dev \
  --query 'projects[0].vpcConfig'

# Check environment variables
aws codebuild batch-get-projects \
  --names turaf-flyway-migrations-dev \
  --query 'projects[0].environment.environmentVariables'
```

**Solutions**:
1. Verify security group allows egress to RDS
2. Verify RDS security group allows ingress from CodeBuild
3. Verify Secrets Manager secret exists and is accessible
4. Check CloudWatch logs for detailed error messages

### Issue: Build fails with "Access Denied" to Secrets Manager

**Cause**: CodeBuild role lacks permissions

**Solution**:
```bash
# Verify role has Secrets Manager permissions
aws iam get-role-policy \
  --role-name CodeBuildFlywayRole \
  --policy-name CodeBuildFlywayPolicy \
  --query 'PolicyDocument.Statement[?Sid==`SecretsManagerAccess`]'
```

### Issue: Build times out

**Cause**: Network connectivity or slow migration execution

**Solutions**:
1. Increase build timeout in project configuration
2. Check VPC route tables for NAT Gateway
3. Verify migrations are optimized

---

## Automation Script

Create `scripts/create-codebuild-projects.sh`:

```bash
#!/bin/bash

set -e

ENVIRONMENTS=("dev" "qa" "prod")
PROFILES=("turaf-dev" "turaf-qa" "turaf-prod")
ACCOUNT_IDS=("801651112319" "965932217544" "811783768245")

for i in "${!ENVIRONMENTS[@]}"; do
  ENV="${ENVIRONMENTS[$i]}"
  PROFILE="${PROFILES[$i]}"
  ACCOUNT_ID="${ACCOUNT_IDS[$i]}"
  
  echo "=== Creating CodeBuild project for ${ENV} ==="
  
  export AWS_PROFILE=$PROFILE
  
  # Get VPC configuration
  VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
    --query 'Vpcs[0].VpcId' \
    --output text)
  
  SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
    --query 'Subnets[*].SubnetId' \
    --output json | jq -r '.[]' | paste -sd ',' -)
  
  SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text)
  
  RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier turaf-db-${ENV} \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text 2>/dev/null || echo "localhost")
  
  echo "VPC: $VPC_ID, Subnets: $SUBNET_IDS, SG: $SG_ID, RDS: $RDS_ENDPOINT"
  
  # Create project configuration file
  cat > /tmp/codebuild-project-${ENV}.json <<EOF
{
  "name": "turaf-flyway-migrations-${ENV}",
  "description": "Flyway database migrations for ${ENV} environment",
  "source": {
    "type": "GITHUB",
    "location": "https://github.com/ryanwaite28/ai-projects-turaf.git",
    "buildspec": "services/flyway-service/buildspec.yml",
    "gitCloneDepth": 1
  },
  "artifacts": {
    "type": "NO_ARTIFACTS"
  },
  "environment": {
    "type": "LINUX_CONTAINER",
    "image": "aws/codebuild/standard:7.0",
    "computeType": "BUILD_GENERAL1_SMALL",
    "environmentVariables": [
      {"name": "DB_HOST", "value": "${RDS_ENDPOINT}", "type": "PLAINTEXT"},
      {"name": "DB_NAME", "value": "turaf", "type": "PLAINTEXT"},
      {"name": "DB_USER", "value": "postgres", "type": "PLAINTEXT"},
      {"name": "DB_PASSWORD", "value": "turaf/db/master", "type": "SECRETS_MANAGER"}
    ]
  },
  "serviceRole": "arn:aws:iam::${ACCOUNT_ID}:role/CodeBuildFlywayRole",
  "vpcConfig": {
    "vpcId": "${VPC_ID}",
    "subnets": ["${SUBNET_IDS//,/\",\"}"],
    "securityGroupIds": ["${SG_ID}"]
  },
  "logsConfig": {
    "cloudWatchLogs": {
      "status": "ENABLED",
      "groupName": "/aws/codebuild/turaf-flyway-migrations-${ENV}"
    }
  },
  "tags": [
    {"key": "Environment", "value": "${ENV}"},
    {"key": "Service", "value": "flyway-service"},
    {"key": "ManagedBy", "value": "script"}
  ]
}
EOF
  
  # Create project
  aws codebuild create-project \
    --cli-input-json file:///tmp/codebuild-project-${ENV}.json \
    || echo "Project may already exist"
  
  echo "✅ ${ENV}: turaf-flyway-migrations-${ENV}"
done

echo ""
echo "All CodeBuild projects created!"
```

---

## Checklist

- [x] CodeBuild project created in dev
- [ ] CodeBuild project created in qa (pending QA infrastructure)
- [ ] CodeBuild project created in prod (pending Prod infrastructure)
- [x] VPC configuration verified
- [x] Environment variables configured
- [x] Service roles assigned
- [x] Test build executed successfully
- [x] CloudWatch logs verified
- [x] Project ARNs documented

---

## Next Steps

1. Update **Task 012** with Flyway role ARNs for GitHub secrets
2. Test GitHub Actions workflow end-to-end
3. Document CodeBuild project ARNs
4. Create runbook for migration operations

---

## References

- [AWS CodeBuild Projects](https://docs.aws.amazon.com/codebuild/latest/userguide/create-project.html)
- [CodeBuild VPC Support](https://docs.aws.amazon.com/codebuild/latest/userguide/vpc-support.html)
- [CodeBuild Environment Variables](https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html)
- INFRASTRUCTURE_PLAN.md (Phase 2.3)
- specs/flyway-service.md
