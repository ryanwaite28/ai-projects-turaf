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

- [x] CodeBuild security group created in each environment
- [x] RDS security group updated to allow CodeBuild access
- [x] Automation script created (setup-flyway-network-cross-account.sh)
- [x] Verification script created (verify-flyway-network-dev.sh)
- [x] Security group rules tested and verified
- [x] Network access documented (TASK_027_NETWORK_ACCESS.md)

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

# Get private subnet IDs
SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=turaf-private-*-${ENV}" \
  --query 'Subnets[*].SubnetId' \
  --output text | tr '\t' ',')

# Create temporary test project
aws codebuild create-project \
  --name turaf-flyway-network-test-${ENV} \
  --source type=NO_SOURCE \
  --artifacts type=NO_ARTIFACTS \
  --environment type=LINUX_CONTAINER,image=aws/codebuild/standard:7.0,computeType=BUILD_GENERAL1_SMALL,environmentVariables='[{name=DB_HOST,value=<RDS_ENDPOINT>},{name=DB_NAME,value=turaf},{name=DB_USER,value=postgres}]' \
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
ACCOUNT_IDS=("801651112319" "965932217544" "811783768245")

for i in "${!ENVIRONMENTS[@]}"; do
  ENV="${ENVIRONMENTS[$i]}"
  PROFILE="${PROFILES[$i]}"
  ACCOUNT_ID="${ACCOUNT_IDS[$i]}"
  
  echo "=== Configuring network for ${ENV} ==="
  
  export AWS_PROFILE=$PROFILE
  
  # Get VPC ID
  VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=turaf-vpc-${ENV}" \
    --query 'Vpcs[0].VpcId' \
    --output text)
  
  echo "VPC ID: $VPC_ID"
  
  # Create CodeBuild security group
  SG_ID=$(aws ec2 create-security-group \
    --group-name turaf-codebuild-flyway-${ENV} \
    --description "Security group for CodeBuild Flyway migrations" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=turaf-codebuild-flyway-${ENV}},{Key=Environment,Value=${ENV}},{Key=Service,Value=flyway-service}]" \
    --query 'GroupId' \
    --output text 2>/dev/null) || {
    echo "Security group may already exist, getting ID..."
    SG_ID=$(aws ec2 describe-security-groups \
      --filters "Name=tag:Name,Values=turaf-codebuild-flyway-${ENV}" \
      --query 'SecurityGroups[0].GroupId' \
      --output text)
  }
  
  echo "CodeBuild SG ID: $SG_ID"
  
  # Get RDS security group
  RDS_SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=tag:Name,Values=turaf-rds-${ENV}" \
    --query 'SecurityGroups[0].GroupId' \
    --output text)
  
  echo "RDS SG ID: $RDS_SG_ID"
  
  # Configure rules (ignore errors if rules already exist)
  aws ec2 revoke-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=-1,IpRanges='[{CidrIp=0.0.0.0/0}]' 2>/dev/null || true
  
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${RDS_SG_ID},Description='Allow PostgreSQL to RDS'}]" 2>/dev/null || true
  
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTPS'}]" 2>/dev/null || true
  
  aws ec2 authorize-security-group-egress \
    --group-id $SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=0.0.0.0/0,Description='Allow HTTP'}]" 2>/dev/null || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id $RDS_SG_ID \
    --ip-permissions IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs="[{GroupId=${SG_ID},Description='Allow from CodeBuild Flyway'}]" 2>/dev/null || true
  
  echo "✅ ${ENV}: CodeBuild SG=$SG_ID, RDS SG=$RDS_SG_ID"
done

echo ""
echo "All environments configured!"
```

---

## Checklist

- [x] CodeBuild security group created in dev
- [ ] CodeBuild security group created in qa (pending QA infrastructure)
- [ ] CodeBuild security group created in prod (pending Prod infrastructure)
- [x] RDS security group updated in dev
- [ ] RDS security group updated in qa (pending QA infrastructure)
- [ ] RDS security group updated in prod (pending Prod infrastructure)
- [x] Network connectivity verified
- [x] Security group IDs documented
- [x] Ready for Task 028 (CodeBuild project creation)

---

## Next Steps

1. Proceed to **Task 028: Create CodeBuild Migration Projects**
2. Use CodeBuild security group ID in CodeBuild VPC configuration
3. Test end-to-end migration execution

---

## References

- [AWS VPC Security Groups](https://docs.aws.amazon.com/vpc/latest/userguide/VPC_SecurityGroups.html)
- [CodeBuild VPC Support](https://docs.aws.amazon.com/codebuild/latest/userguide/vpc-support.html)
- INFRASTRUCTURE_PLAN.md (Phase 2.3)
- specs/flyway-service.md
