#!/bin/bash

set -e

# Verify Flyway Network Configuration for Dev Environment
# Purpose: Verify security group rules for CodeBuild to RDS access

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"

echo "=========================================="
echo "Flyway Network Configuration Verification"
echo "Environment: ${ENV}"
echo "=========================================="
echo ""

# Check AWS credentials
echo "Checking AWS credentials..."
if ! aws sts get-caller-identity --profile $ROOT_PROFILE &> /dev/null; then
    echo "❌ AWS credentials not configured for $ROOT_PROFILE profile"
    echo "   Run: aws sso login --profile $ROOT_PROFILE"
    exit 1
fi

echo "✅ AWS credentials configured"
echo ""

# Assume role in dev account
echo "Assuming role in dev account..."
CREDENTIALS=$(aws sts assume-role \
  --profile $ROOT_PROFILE \
  --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
  --role-session-name "verify-flyway-network" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="us-east-1"

echo "✅ Assumed role in dev account"
echo ""

# Get infrastructure details
echo "=========================================="
echo "Infrastructure Details"
echo "=========================================="
echo ""

# Get VPC ID (from standalone deployment)
VPC_ID="vpc-0eb73410956d368a8"

echo "VPC ID: $VPC_ID"

# Get CodeBuild Security Group (from standalone deployment)
CODEBUILD_SG_ID="sg-01b1f0d32cf32bd22"

echo "CodeBuild SG ID: $CODEBUILD_SG_ID"

# Get RDS Security Group (from standalone deployment)
RDS_SG_ID="sg-0700dfd644af580af"

echo "RDS SG ID: $RDS_SG_ID"
echo ""

# Verify CodeBuild Security Group Rules
echo "=========================================="
echo "CodeBuild Security Group Rules"
echo "=========================================="
echo ""

if [ "$CODEBUILD_SG_ID" != "N/A" ]; then
    echo "Egress Rules:"
    aws ec2 describe-security-groups \
      --group-ids $CODEBUILD_SG_ID \
      --query 'SecurityGroups[0].IpPermissionsEgress[*].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,Target:UserIdGroupPairs[0].GroupId||CidrIp,Description:UserIdGroupPairs[0].Description||""}' \
      --output table
    
    echo ""
    echo "Ingress Rules:"
    aws ec2 describe-security-groups \
      --group-ids $CODEBUILD_SG_ID \
      --query 'SecurityGroups[0].IpPermissions[*].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,Source:UserIdGroupPairs[0].GroupId||CidrIp,Description:UserIdGroupPairs[0].Description||""}' \
      --output table
else
    echo "⚠️  CodeBuild security group not found"
fi

echo ""

# Verify RDS Security Group Rules
echo "=========================================="
echo "RDS Security Group Rules"
echo "=========================================="
echo ""

if [ "$RDS_SG_ID" != "N/A" ]; then
    echo "Ingress Rules:"
    aws ec2 describe-security-groups \
      --group-ids $RDS_SG_ID \
      --query 'SecurityGroups[0].IpPermissions[*].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,Source:UserIdGroupPairs[0].GroupId||CidrIp,Description:UserIdGroupPairs[0].Description||""}' \
      --output table
    
    echo ""
    echo "Egress Rules:"
    aws ec2 describe-security-groups \
      --group-ids $RDS_SG_ID \
      --query 'SecurityGroups[0].IpPermissionsEgress[*].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,Target:UserIdGroupPairs[0].GroupId||CidrIp,Description:UserIdGroupPairs[0].Description||""}' \
      --output table
else
    echo "⚠️  RDS security group not found"
fi

echo ""

# Verification Checklist
echo "=========================================="
echo "Verification Checklist"
echo "=========================================="
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# Check 1: VPC exists
if [ "$VPC_ID" != "N/A" ]; then
    echo "✅ VPC exists: $VPC_ID"
    ((CHECKS_PASSED++))
else
    echo "❌ VPC not found"
    ((CHECKS_FAILED++))
fi

# Check 2: CodeBuild SG exists
if [ "$CODEBUILD_SG_ID" != "N/A" ]; then
    echo "✅ CodeBuild security group exists: $CODEBUILD_SG_ID"
    ((CHECKS_PASSED++))
else
    echo "❌ CodeBuild security group not found"
    ((CHECKS_FAILED++))
fi

# Check 3: RDS SG exists
if [ "$RDS_SG_ID" != "N/A" ]; then
    echo "✅ RDS security group exists: $RDS_SG_ID"
    ((CHECKS_PASSED++))
else
    echo "❌ RDS security group not found"
    ((CHECKS_FAILED++))
fi

# Check 4: CodeBuild can reach RDS
if [ "$CODEBUILD_SG_ID" != "N/A" ] && [ "$RDS_SG_ID" != "N/A" ]; then
    EGRESS_RULE=$(aws ec2 describe-security-groups \
      --group-ids $CODEBUILD_SG_ID \
      --query "SecurityGroups[0].IpPermissionsEgress[?ToPort==\`5432\` && UserIdGroupPairs[0].GroupId==\`$RDS_SG_ID\`]" \
      --output text)
    
    if [ -n "$EGRESS_RULE" ]; then
        echo "✅ CodeBuild has egress rule to RDS on port 5432"
        ((CHECKS_PASSED++))
    else
        echo "❌ CodeBuild missing egress rule to RDS on port 5432"
        ((CHECKS_FAILED++))
    fi
fi

# Check 5: RDS accepts from CodeBuild
if [ "$CODEBUILD_SG_ID" != "N/A" ] && [ "$RDS_SG_ID" != "N/A" ]; then
    INGRESS_RULE=$(aws ec2 describe-security-groups \
      --group-ids $RDS_SG_ID \
      --query "SecurityGroups[0].IpPermissions[?ToPort==\`5432\` && UserIdGroupPairs[0].GroupId==\`$CODEBUILD_SG_ID\`]" \
      --output text)
    
    if [ -n "$INGRESS_RULE" ]; then
        echo "✅ RDS has ingress rule from CodeBuild on port 5432"
        ((CHECKS_PASSED++))
    else
        echo "❌ RDS missing ingress rule from CodeBuild on port 5432"
        ((CHECKS_FAILED++))
    fi
fi

# Check 6: CodeBuild can reach internet (for package downloads)
if [ "$CODEBUILD_SG_ID" != "N/A" ]; then
    HTTPS_RULE=$(aws ec2 describe-security-groups \
      --group-ids $CODEBUILD_SG_ID \
      --query "SecurityGroups[0].IpPermissionsEgress[?ToPort==\`443\`]" \
      --output text)
    
    if [ -n "$HTTPS_RULE" ]; then
        echo "✅ CodeBuild has egress rule for HTTPS (443)"
        ((CHECKS_PASSED++))
    else
        echo "❌ CodeBuild missing egress rule for HTTPS (443)"
        ((CHECKS_FAILED++))
    fi
fi

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Checks Passed: $CHECKS_PASSED"
echo "Checks Failed: $CHECKS_FAILED"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo "✅ All network configuration checks passed!"
    echo ""
    echo "Ready for Task 028: Create CodeBuild Migration Projects"
else
    echo "❌ Some checks failed. Please review the configuration above."
    exit 1
fi

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN
