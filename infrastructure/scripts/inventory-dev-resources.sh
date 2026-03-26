#!/bin/bash

# Inventory DEV Resources
# Captures current state before teardown

set -e

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"
OUTPUT_FILE="../../docs/pre-reset-inventory.md"

echo "=========================================="
echo "DEV Environment Resource Inventory"
echo "=========================================="
echo ""

# Assume role in DEV account
echo "🔐 Assuming role in DEV account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "inventory-session" \
    --duration-seconds 900 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in DEV account"
echo ""

# Create output file
cat > "${OUTPUT_FILE}" << 'EOF'
# DEV Environment Pre-Reset Inventory

**Date**: March 25, 2026  
**Account**: 801651112319 (dev)  
**Region**: us-east-1  
**Purpose**: Document resources before infrastructure reset

---

## VPC and Networking

EOF

# VPCs
echo "📊 Inventorying VPCs..."
aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | \
    jq -r '.Vpcs[] | "- VPC ID: \(.VpcId)\n  - CIDR: \(.CidrBlock)\n  - State: \(.State)\n  - DNS Hostnames: \(.EnableDnsHostnames)\n  - DNS Support: \(.EnableDnsSupport)\n"' >> "${OUTPUT_FILE}"

# Subnets
echo "📊 Inventorying Subnets..."
echo -e "\n## Subnets\n" >> "${OUTPUT_FILE}"
aws ec2 describe-subnets --filters "Name=tag:Environment,Values=dev" --output json | \
    jq -r '.Subnets[] | "- Subnet ID: \(.SubnetId)\n  - CIDR: \(.CidrBlock)\n  - AZ: \(.AvailabilityZone)\n  - Available IPs: \(.AvailableIpAddressCount)\n  - Type: \(.Tags[]? | select(.Key=="Name") | .Value)\n"' >> "${OUTPUT_FILE}"

# NAT Gateways
echo "📊 Inventorying NAT Gateways..."
echo -e "\n## NAT Gateways\n" >> "${OUTPUT_FILE}"
aws ec2 describe-nat-gateways --filter "Name=tag:Environment,Values=dev" --output json | \
    jq -r '.NatGateways[] | "- NAT Gateway ID: \(.NatGatewayId)\n  - State: \(.State)\n  - Subnet: \(.SubnetId)\n  - EIP: \(.NatGatewayAddresses[0].PublicIp)\n"' >> "${OUTPUT_FILE}"

# VPC Endpoints
echo "📊 Inventorying VPC Endpoints..."
echo -e "\n## VPC Endpoints\n" >> "${OUTPUT_FILE}"
aws ec2 describe-vpc-endpoints --filters "Name=tag:Environment,Values=dev" --output json | \
    jq -r '.VpcEndpoints[] | "- Endpoint ID: \(.VpcEndpointId)\n  - Service: \(.ServiceName)\n  - Type: \(.VpcEndpointType)\n  - State: \(.State)\n"' >> "${OUTPUT_FILE}"

# Security Groups
echo "📊 Inventorying Security Groups..."
echo -e "\n## Security Groups\n" >> "${OUTPUT_FILE}"
aws ec2 describe-security-groups --filters "Name=tag:Environment,Values=dev" --output json | \
    jq -r '.SecurityGroups[] | "- SG ID: \(.GroupId)\n  - Name: \(.GroupName)\n  - Description: \(.Description)\n  - Ingress Rules: \(.IpPermissions | length)\n  - Egress Rules: \(.IpPermissionsEgress | length)\n"' >> "${OUTPUT_FILE}"

# RDS
echo "📊 Inventorying RDS Instances..."
echo -e "\n## RDS Instances\n" >> "${OUTPUT_FILE}"
aws rds describe-db-instances --output json | \
    jq -r '.DBInstances[] | select(.DBInstanceIdentifier | contains("dev")) | "- Instance ID: \(.DBInstanceIdentifier)\n  - Engine: \(.Engine) \(.EngineVersion)\n  - Class: \(.DBInstanceClass)\n  - Storage: \(.AllocatedStorage) GB\n  - Multi-AZ: \(.MultiAZ)\n  - Status: \(.DBInstanceStatus)\n  - Endpoint: \(.Endpoint.Address):\(.Endpoint.Port)\n"' >> "${OUTPUT_FILE}"

# DB Subnet Groups
echo "📊 Inventorying DB Subnet Groups..."
echo -e "\n## DB Subnet Groups\n" >> "${OUTPUT_FILE}"
aws rds describe-db-subnet-groups --output json | \
    jq -r '.DBSubnetGroups[] | select(.DBSubnetGroupName | contains("dev")) | "- Name: \(.DBSubnetGroupName)\n  - VPC: \(.VpcId)\n  - Subnets: \(.Subnets | length)\n  - Status: \(.SubnetGroupStatus)\n"' >> "${OUTPUT_FILE}"

# ElastiCache
echo "📊 Inventorying ElastiCache Clusters..."
echo -e "\n## ElastiCache Clusters\n" >> "${OUTPUT_FILE}"
aws elasticache describe-cache-clusters --output json 2>/dev/null | \
    jq -r '.CacheClusters[]? | select(.CacheClusterId | contains("dev")) | "- Cluster ID: \(.CacheClusterId)\n  - Engine: \(.Engine) \(.EngineVersion)\n  - Node Type: \(.CacheNodeType)\n  - Nodes: \(.NumCacheNodes)\n  - Status: \(.CacheClusterStatus)\n"' >> "${OUTPUT_FILE}" || echo "No ElastiCache clusters found\n" >> "${OUTPUT_FILE}"

# ECS
echo "📊 Inventorying ECS Clusters..."
echo -e "\n## ECS Clusters\n" >> "${OUTPUT_FILE}"
aws ecs list-clusters --output json | jq -r '.clusterArns[]' | while read cluster_arn; do
    if [[ $cluster_arn == *"dev"* ]]; then
        aws ecs describe-clusters --clusters "$cluster_arn" --output json | \
            jq -r '.clusters[] | "- Cluster: \(.clusterName)\n  - Status: \(.status)\n  - Running Tasks: \(.runningTasksCount)\n  - Pending Tasks: \(.pendingTasksCount)\n  - Registered Instances: \(.registeredContainerInstancesCount)\n"' >> "${OUTPUT_FILE}"
    fi
done

# ECS Services
echo "📊 Inventorying ECS Services..."
echo -e "\n## ECS Services\n" >> "${OUTPUT_FILE}"
aws ecs list-clusters --output json | jq -r '.clusterArns[]' | while read cluster_arn; do
    if [[ $cluster_arn == *"dev"* ]]; then
        cluster_name=$(basename "$cluster_arn")
        aws ecs list-services --cluster "$cluster_name" --output json | jq -r '.serviceArns[]' | while read service_arn; do
            aws ecs describe-services --cluster "$cluster_name" --services "$service_arn" --output json | \
                jq -r '.services[] | "- Service: \(.serviceName)\n  - Status: \(.status)\n  - Desired: \(.desiredCount)\n  - Running: \(.runningCount)\n  - Launch Type: \(.launchType)\n"' >> "${OUTPUT_FILE}"
        done
    fi
done

# ALB
echo "📊 Inventorying Load Balancers..."
echo -e "\n## Application Load Balancers\n" >> "${OUTPUT_FILE}"
aws elbv2 describe-load-balancers --output json | \
    jq -r '.LoadBalancers[] | select(.LoadBalancerName | contains("dev")) | "- ALB: \(.LoadBalancerName)\n  - DNS: \(.DNSName)\n  - Scheme: \(.Scheme)\n  - State: \(.State.Code)\n  - Type: \(.Type)\n"' >> "${OUTPUT_FILE}"

# Lambda
echo "📊 Inventorying Lambda Functions..."
echo -e "\n## Lambda Functions\n" >> "${OUTPUT_FILE}"
aws lambda list-functions --output json | \
    jq -r '.Functions[] | select(.FunctionName | contains("dev")) | "- Function: \(.FunctionName)\n  - Runtime: \(.Runtime)\n  - Memory: \(.MemorySize) MB\n  - Timeout: \(.Timeout)s\n  - Last Modified: \(.LastModified)\n"' >> "${OUTPUT_FILE}"

# S3 Buckets
echo "📊 Inventorying S3 Buckets..."
echo -e "\n## S3 Buckets\n" >> "${OUTPUT_FILE}"
aws s3api list-buckets --output json | \
    jq -r '.Buckets[] | select(.Name | contains("dev")) | "- Bucket: \(.Name)\n  - Created: \(.CreationDate)\n"' >> "${OUTPUT_FILE}"

# Secrets Manager
echo "📊 Inventorying Secrets..."
echo -e "\n## Secrets Manager\n" >> "${OUTPUT_FILE}"
aws secretsmanager list-secrets --output json | \
    jq -r '.SecretList[] | select(.Name | contains("dev")) | "- Secret: \(.Name)\n  - ARN: \(.ARN)\n  - Last Changed: \(.LastChangedDate)\n"' >> "${OUTPUT_FILE}"

# EventBridge
echo "📊 Inventorying EventBridge Rules..."
echo -e "\n## EventBridge Rules\n" >> "${OUTPUT_FILE}"
aws events list-rules --output json | \
    jq -r '.Rules[] | select(.Name | contains("dev")) | "- Rule: \(.Name)\n  - State: \(.State)\n  - Event Bus: \(.EventBusName // "default")\n"' >> "${OUTPUT_FILE}"

# SQS
echo "📊 Inventorying SQS Queues..."
echo -e "\n## SQS Queues\n" >> "${OUTPUT_FILE}"
aws sqs list-queues --output json 2>/dev/null | \
    jq -r '.QueueUrls[]? | select(. | contains("dev")) | "- Queue: \(.)\n"' >> "${OUTPUT_FILE}" || echo "No SQS queues found\n" >> "${OUTPUT_FILE}"

# CloudWatch Log Groups
echo "📊 Inventorying CloudWatch Log Groups..."
echo -e "\n## CloudWatch Log Groups\n" >> "${OUTPUT_FILE}"
aws logs describe-log-groups --output json | \
    jq -r '.logGroups[] | select(.logGroupName | contains("dev") or contains("turaf")) | "- Log Group: \(.logGroupName)\n  - Retention: \(.retentionInDays // "Never expire") days\n  - Stored Bytes: \(.storedBytes)\n"' >> "${OUTPUT_FILE}"

# Cost Estimate
echo "📊 Calculating current costs..."
echo -e "\n## Estimated Monthly Costs\n" >> "${OUTPUT_FILE}"
echo "Based on current running resources:\n" >> "${OUTPUT_FILE}"

# Count resources for cost estimate
vpc_count=$(aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | jq '.Vpcs | length')
nat_count=$(aws ec2 describe-nat-gateways --filter "Name=tag:Environment,Values=dev" "Name=state,Values=available" --output json | jq '.NatGateways | length')
rds_count=$(aws rds describe-db-instances --output json | jq '[.DBInstances[] | select(.DBInstanceIdentifier | contains("dev"))] | length')
alb_count=$(aws elbv2 describe-load-balancers --output json | jq '[.LoadBalancers[] | select(.LoadBalancerName | contains("dev"))] | length')

echo "- NAT Gateways: $nat_count × \$32/month = \$$(($nat_count * 32))" >> "${OUTPUT_FILE}"
echo "- RDS Instances: $rds_count × \$15/month (db.t3.micro) = \$$(($rds_count * 15))" >> "${OUTPUT_FILE}"
echo "- ALB: $alb_count × \$16/month = \$$(($alb_count * 16))" >> "${OUTPUT_FILE}"
echo "- VPC Endpoints: ~\$14/month (estimated)" >> "${OUTPUT_FILE}"
echo "- Other services: ~\$10/month (estimated)" >> "${OUTPUT_FILE}"
echo "" >> "${OUTPUT_FILE}"
total_estimate=$((nat_count * 32 + rds_count * 15 + alb_count * 16 + 24))
echo "**Estimated Total: ~\$${total_estimate}/month**" >> "${OUTPUT_FILE}"

echo ""
echo "=========================================="
echo "Inventory Complete!"
echo "=========================================="
echo ""
echo "📄 Report saved to: ${OUTPUT_FILE}"
echo ""
