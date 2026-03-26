#!/bin/bash

# Verify Environment Deployment
# Usage: ./verify-environment.sh <env>

set -e

ENV=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -z "$ENV" ]; then
    echo "Usage: ./verify-environment.sh <env>"
    echo "  env: dev, qa, or prod"
    exit 1
fi

echo "=========================================="
echo "Verifying $ENV Environment"
echo "=========================================="
echo ""

# Assume role
source "${SCRIPT_DIR}/assume-role.sh" "$ENV"

# Initialize counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# Helper function to check resource
check_resource() {
    local name=$1
    local command=$2
    local expected=$3
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -n "Checking $name... "
    
    if eval "$command" &> /dev/null; then
        echo "✅"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo "❌"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

# VPC
echo "📦 Networking"
check_resource "VPC" "aws ec2 describe-vpcs --filters 'Name=tag:Environment,Values=$ENV' --query 'Vpcs[0].VpcId' --output text | grep -q vpc-"

check_resource "Public Subnets" "aws ec2 describe-subnets --filters 'Name=tag:Environment,Values=$ENV' 'Name=tag:Type,Values=public' --query 'Subnets[*].SubnetId' --output text | grep -q subnet-"

check_resource "Private Subnets" "aws ec2 describe-subnets --filters 'Name=tag:Environment,Values=$ENV' 'Name=tag:Type,Values=private' --query 'Subnets[*].SubnetId' --output text | grep -q subnet-"

check_resource "Database Subnets" "aws ec2 describe-subnets --filters 'Name=tag:Environment,Values=$ENV' 'Name=tag:Type,Values=database' --query 'Subnets[*].SubnetId' --output text | grep -q subnet-"

check_resource "Internet Gateway" "aws ec2 describe-internet-gateways --filters 'Name=tag:Environment,Values=$ENV' --query 'InternetGateways[0].InternetGatewayId' --output text | grep -q igw-"

# Check NAT Gateway (optional)
if aws ec2 describe-nat-gateways --filter "Name=tag:Environment,Values=$ENV" "Name=state,Values=available" --query 'NatGateways[0].NatGatewayId' --output text | grep -q nat-; then
    check_resource "NAT Gateway" "aws ec2 describe-nat-gateways --filter 'Name=tag:Environment,Values=$ENV' 'Name=state,Values=available' --query 'NatGateways[0].NatGatewayId' --output text | grep -q nat-"
else
    echo "Checking NAT Gateway... ⚠️  (Not deployed - using VPC endpoints)"
fi

echo ""

# Security Groups
echo "🔒 Security"
check_resource "ALB Security Group" "aws ec2 describe-security-groups --filters 'Name=tag:Environment,Values=$ENV' 'Name=group-name,Values=*alb*' --query 'SecurityGroups[0].GroupId' --output text | grep -q sg-"

check_resource "ECS Security Group" "aws ec2 describe-security-groups --filters 'Name=tag:Environment,Values=$ENV' 'Name=group-name,Values=*ecs*' --query 'SecurityGroups[0].GroupId' --output text | grep -q sg-"

check_resource "RDS Security Group" "aws ec2 describe-security-groups --filters 'Name=tag:Environment,Values=$ENV' 'Name=group-name,Values=*rds*' --query 'SecurityGroups[0].GroupId' --output text | grep -q sg-"

echo ""

# RDS
echo "🗄️  Database"
check_resource "RDS Instance" "aws rds describe-db-instances --query 'DBInstances[?contains(DBInstanceIdentifier, \`$ENV\`)] | [0].DBInstanceStatus' --output text | grep -q available"

check_resource "DB Subnet Group" "aws rds describe-db-subnet-groups --query 'DBSubnetGroups[?contains(DBSubnetGroupName, \`$ENV\`)] | [0].DBSubnetGroupName' --output text | grep -q turaf"

# Check Redis (optional)
if aws elasticache describe-cache-clusters --query "CacheClusters[?contains(CacheClusterId, '$ENV')] | [0].CacheClusterStatus" --output text 2>/dev/null | grep -q available; then
    check_resource "Redis Cluster" "aws elasticache describe-cache-clusters --query \"CacheClusters[?contains(CacheClusterId, '$ENV')] | [0].CacheClusterStatus\" --output text | grep -q available"
else
    echo "Checking Redis Cluster... ⚠️  (Not deployed)"
fi

echo ""

# ECS
echo "🐳 Compute"
check_resource "ECS Cluster" "aws ecs describe-clusters --clusters turaf-cluster-$ENV --query 'clusters[0].status' --output text | grep -q ACTIVE"

check_resource "ALB" "aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName, \`$ENV\`)] | [0].State.Code' --output text | grep -q active"

# Check ECS Services
SERVICES=("identity-service" "organization-service" "experiment-service")
for service in "${SERVICES[@]}"; do
    if aws ecs describe-services --cluster turaf-cluster-$ENV --services ${service}-$ENV --query 'services[0].status' --output text 2>/dev/null | grep -q ACTIVE; then
        check_resource "ECS Service: $service" "aws ecs describe-services --cluster turaf-cluster-$ENV --services ${service}-$ENV --query 'services[0].status' --output text | grep -q ACTIVE"
    else
        echo "Checking ECS Service: $service... ⚠️  (Not deployed or no tasks running)"
    fi
done

echo ""

# Lambda (optional)
echo "⚡ Lambda"
if aws lambda list-functions --query "Functions[?contains(FunctionName, '$ENV')].FunctionName" --output text | grep -q .; then
    check_resource "Lambda Functions" "aws lambda list-functions --query \"Functions[?contains(FunctionName, '$ENV')].FunctionName\" --output text | grep -q ."
else
    echo "Checking Lambda Functions... ⚠️  (Not deployed)"
fi

echo ""

# Storage
echo "📦 Storage"
check_resource "S3 Bucket" "aws s3 ls | grep -q turaf.*$ENV"

check_resource "ECR Repositories" "aws ecr describe-repositories --query 'repositories[?contains(repositoryName, \`turaf\`)].repositoryName' --output text | grep -q turaf"

echo ""

# Secrets
echo "🔐 Secrets"
check_resource "Secrets Manager" "aws secretsmanager list-secrets --query \"SecretList[?contains(Name, '$ENV')].Name\" --output text | grep -q ."

echo ""

# Messaging (optional)
echo "📨 Messaging"
if aws sqs list-queues --query "QueueUrls[?contains(@, '$ENV')]" --output text 2>/dev/null | grep -q .; then
    check_resource "SQS Queues" "aws sqs list-queues --query \"QueueUrls[?contains(@, '$ENV')]\" --output text | grep -q ."
else
    echo "Checking SQS Queues... ⚠️  (Not deployed)"
fi

if aws events list-rules --query "Rules[?contains(Name, '$ENV')].Name" --output text | grep -q .; then
    check_resource "EventBridge Rules" "aws events list-rules --query \"Rules[?contains(Name, '$ENV')].Name\" --output text | grep -q ."
else
    echo "Checking EventBridge Rules... ⚠️  (Not deployed)"
fi

echo ""

# CloudWatch
echo "📊 Monitoring"
check_resource "CloudWatch Log Groups" "aws logs describe-log-groups --query \"logGroups[?contains(logGroupName, '$ENV') || contains(logGroupName, 'turaf')].logGroupName\" --output text | grep -q ."

if aws cloudwatch describe-alarms --query "MetricAlarms[?contains(AlarmName, '$ENV')].AlarmName" --output text | grep -q .; then
    check_resource "CloudWatch Alarms" "aws cloudwatch describe-alarms --query \"MetricAlarms[?contains(AlarmName, '$ENV')].AlarmName\" --output text | grep -q ."
else
    echo "Checking CloudWatch Alarms... ⚠️  (Not deployed)"
fi

echo ""
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo ""
echo "Total Checks: $TOTAL_CHECKS"
echo "Passed: ✅ $PASSED_CHECKS"
echo "Failed: ❌ $FAILED_CHECKS"
echo "Warnings: ⚠️  $((TOTAL_CHECKS - PASSED_CHECKS - FAILED_CHECKS))"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
    echo "✅ Environment verification PASSED"
    echo ""
    exit 0
else
    echo "⚠️  Environment verification completed with failures"
    echo ""
    echo "Review failed checks above and ensure all required resources are deployed."
    exit 1
fi
