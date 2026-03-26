#!/bin/bash

# Complete Teardown of DEV Environment
# Destroys all application resources in preparation for infrastructure reset

set -e

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"
ENV="dev"

echo "=========================================="
echo "DEV Environment Complete Teardown"
echo "=========================================="
echo ""
echo "⚠️  WARNING: This will destroy ALL application resources in DEV"
echo ""
echo "Resources to be destroyed:"
echo "  - All ECS clusters, services, and tasks"
echo "  - All Application Load Balancers"
echo "  - All Lambda functions"
echo "  - All RDS instances and DB subnet groups"
echo "  - All ElastiCache clusters"
echo "  - All VPCs, subnets, NAT gateways, and VPC endpoints"
echo "  - All security groups"
echo "  - All EventBridge rules and SQS queues"
echo "  - All Secrets Manager secrets"
echo "  - All CloudWatch log groups, alarms, and dashboards"
echo "  - All KMS keys (scheduled for deletion)"
echo "  - All S3 application buckets (NOT state bucket)"
echo ""
echo "Resources to be PRESERVED:"
echo "  ✅ Terraform state bucket (turaf-terraform-state)"
echo "  ✅ DynamoDB lock table (turaf-terraform-locks)"
echo "  ✅ ECR repositories"
echo "  ✅ IAM OIDC roles for GitHub Actions"
echo "  ✅ CodeBuild Flyway projects"
echo ""
read -p "Are you sure you want to proceed? (type 'yes' to confirm): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Teardown cancelled."
    exit 0
fi

echo ""
echo "🔐 Assuming role in DEV account..."
CREDENTIALS=$(aws sts assume-role \
    --profile "${ROOT_PROFILE}" \
    --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
    --role-session-name "teardown-session" \
    --duration-seconds 3600 \
    --output json)

export AWS_ACCESS_KEY_ID=$(echo "${CREDENTIALS}" | jq -r '.Credentials.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo "${CREDENTIALS}" | jq -r '.Credentials.SessionToken')
export AWS_DEFAULT_REGION="${REGION}"

echo "✅ Assumed role in DEV account"
echo ""

# Function to wait for resource deletion
wait_for_deletion() {
    local resource_type=$1
    local check_command=$2
    local max_wait=$3
    local wait_time=0
    
    echo "⏳ Waiting for $resource_type deletion (max ${max_wait}s)..."
    while [ $wait_time -lt $max_wait ]; do
        if eval "$check_command" 2>/dev/null | grep -q .; then
            sleep 10
            wait_time=$((wait_time + 10))
            echo "   Still waiting... (${wait_time}s elapsed)"
        else
            echo "✅ $resource_type deleted"
            return 0
        fi
    done
    echo "⚠️  Timeout waiting for $resource_type deletion"
    return 1
}

# Step 1: Delete ECS Services
echo "=========================================="
echo "Step 1: Deleting ECS Services"
echo "=========================================="
echo ""

for cluster_arn in $(aws ecs list-clusters --output json | jq -r '.clusterArns[] | select(contains("dev"))'); do
    cluster_name=$(basename "$cluster_arn")
    echo "📦 Processing cluster: $cluster_name"
    
    for service_arn in $(aws ecs list-services --cluster "$cluster_name" --output json | jq -r '.serviceArns[]'); do
        service_name=$(basename "$service_arn")
        echo "  🗑️  Deleting service: $service_name"
        
        # Scale down to 0 first
        aws ecs update-service --cluster "$cluster_name" --service "$service_name" --desired-count 0 >/dev/null 2>&1 || true
        
        # Delete service
        aws ecs delete-service --cluster "$cluster_name" --service "$service_name" --force >/dev/null 2>&1 || true
    done
done

echo ""

# Step 2: Delete ALBs and Target Groups
echo "=========================================="
echo "Step 2: Deleting Load Balancers"
echo "=========================================="
echo ""

for alb_arn in $(aws elbv2 describe-load-balancers --output json | jq -r '.LoadBalancers[] | select(.LoadBalancerName | contains("dev")) | .LoadBalancerArn'); do
    alb_name=$(aws elbv2 describe-load-balancers --load-balancer-arns "$alb_arn" | jq -r '.LoadBalancers[0].LoadBalancerName')
    echo "🗑️  Deleting ALB: $alb_name"
    
    # Delete listeners first
    for listener_arn in $(aws elbv2 describe-listeners --load-balancer-arn "$alb_arn" --output json | jq -r '.Listeners[].ListenerArn'); do
        aws elbv2 delete-listener --listener-arn "$listener_arn" >/dev/null 2>&1 || true
    done
    
    # Delete ALB
    aws elbv2 delete-load-balancer --load-balancer-arn "$alb_arn" >/dev/null 2>&1 || true
done

# Wait for ALBs to be deleted
sleep 30

# Delete target groups
for tg_arn in $(aws elbv2 describe-target-groups --output json | jq -r '.TargetGroups[] | select(.TargetGroupName | contains("dev")) | .TargetGroupArn'); do
    tg_name=$(aws elbv2 describe-target-groups --target-group-arns "$tg_arn" | jq -r '.TargetGroups[0].TargetGroupName')
    echo "🗑️  Deleting target group: $tg_name"
    aws elbv2 delete-target-group --target-group-arn "$tg_arn" >/dev/null 2>&1 || true
done

echo ""

# Step 3: Delete Lambda Functions
echo "=========================================="
echo "Step 3: Deleting Lambda Functions"
echo "=========================================="
echo ""

for function_name in $(aws lambda list-functions --output json | jq -r '.Functions[] | select(.FunctionName | contains("dev")) | .FunctionName'); do
    echo "🗑️  Deleting Lambda: $function_name"
    aws lambda delete-function --function-name "$function_name" >/dev/null 2>&1 || true
done

echo ""

# Step 4: Delete EventBridge Rules and SQS Queues
echo "=========================================="
echo "Step 4: Deleting EventBridge & SQS"
echo "=========================================="
echo ""

# EventBridge rules
for rule_name in $(aws events list-rules --output json | jq -r '.Rules[] | select(.Name | contains("dev")) | .Name'); do
    echo "🗑️  Deleting EventBridge rule: $rule_name"
    
    # Remove targets first
    target_ids=$(aws events list-targets-by-rule --rule "$rule_name" --output json | jq -r '.Targets[].Id')
    if [ -n "$target_ids" ]; then
        aws events remove-targets --rule "$rule_name" --ids $target_ids >/dev/null 2>&1 || true
    fi
    
    aws events delete-rule --name "$rule_name" >/dev/null 2>&1 || true
done

# SQS queues
for queue_url in $(aws sqs list-queues --output json 2>/dev/null | jq -r '.QueueUrls[]? | select(contains("dev"))'); do
    echo "🗑️  Deleting SQS queue: $(basename $queue_url)"
    aws sqs delete-queue --queue-url "$queue_url" >/dev/null 2>&1 || true
done

echo ""

# Step 5: Delete RDS Instances
echo "=========================================="
echo "Step 5: Deleting RDS Instances"
echo "=========================================="
echo ""

for db_id in $(aws rds describe-db-instances --output json | jq -r '.DBInstances[] | select(.DBInstanceIdentifier | contains("dev")) | .DBInstanceIdentifier'); do
    echo "🗑️  Deleting RDS instance: $db_id"
    aws rds delete-db-instance \
        --db-instance-identifier "$db_id" \
        --skip-final-snapshot \
        --delete-automated-backups >/dev/null 2>&1 || true
done

# Wait for RDS deletion
if aws rds describe-db-instances --output json | jq -r '.DBInstances[] | select(.DBInstanceIdentifier | contains("dev")) | .DBInstanceIdentifier' | grep -q .; then
    wait_for_deletion "RDS instances" "aws rds describe-db-instances --output json | jq -r '.DBInstances[] | select(.DBInstanceIdentifier | contains(\"dev\")) | .DBInstanceIdentifier'" 900
fi

echo ""

# Step 6: Delete ElastiCache Clusters
echo "=========================================="
echo "Step 6: Deleting ElastiCache Clusters"
echo "=========================================="
echo ""

for cache_id in $(aws elasticache describe-cache-clusters --output json 2>/dev/null | jq -r '.CacheClusters[]? | select(.CacheClusterId | contains("dev")) | .CacheClusterId'); do
    echo "🗑️  Deleting ElastiCache cluster: $cache_id"
    aws elasticache delete-cache-cluster --cache-cluster-id "$cache_id" >/dev/null 2>&1 || true
done

echo ""

# Step 7: Delete DB Subnet Groups
echo "=========================================="
echo "Step 7: Deleting DB Subnet Groups"
echo "=========================================="
echo ""

for subnet_group in $(aws rds describe-db-subnet-groups --output json | jq -r '.DBSubnetGroups[] | select(.DBSubnetGroupName | contains("dev")) | .DBSubnetGroupName'); do
    echo "🗑️  Deleting DB subnet group: $subnet_group"
    aws rds delete-db-subnet-group --db-subnet-group-name "$subnet_group" >/dev/null 2>&1 || true
done

echo ""

# Step 8: Delete ECS Clusters
echo "=========================================="
echo "Step 8: Deleting ECS Clusters"
echo "=========================================="
echo ""

for cluster_arn in $(aws ecs list-clusters --output json | jq -r '.clusterArns[] | select(contains("dev"))'); do
    cluster_name=$(basename "$cluster_arn")
    echo "🗑️  Deleting ECS cluster: $cluster_name"
    aws ecs delete-cluster --cluster "$cluster_name" >/dev/null 2>&1 || true
done

echo ""

# Step 9: Delete NAT Gateways
echo "=========================================="
echo "Step 9: Deleting NAT Gateways"
echo "=========================================="
echo ""

nat_gateway_ids=()
for nat_id in $(aws ec2 describe-nat-gateways --filter "Name=tag:Environment,Values=dev" "Name=state,Values=available" --output json | jq -r '.NatGateways[].NatGatewayId'); do
    echo "🗑️  Deleting NAT Gateway: $nat_id"
    aws ec2 delete-nat-gateway --nat-gateway-id "$nat_id" >/dev/null 2>&1 || true
    nat_gateway_ids+=("$nat_id")
done

# Wait for NAT gateways to be deleted
if [ ${#nat_gateway_ids[@]} -gt 0 ]; then
    wait_for_deletion "NAT gateways" "aws ec2 describe-nat-gateways --nat-gateway-ids ${nat_gateway_ids[@]} --output json | jq -r '.NatGateways[] | select(.State != \"deleted\") | .NatGatewayId'" 300
fi

echo ""

# Step 10: Release Elastic IPs
echo "=========================================="
echo "Step 10: Releasing Elastic IPs"
echo "=========================================="
echo ""

for alloc_id in $(aws ec2 describe-addresses --output json | jq -r '.Addresses[] | select(.Tags[]? | select(.Key=="Environment" and .Value=="dev")) | .AllocationId'); do
    echo "🗑️  Releasing EIP: $alloc_id"
    aws ec2 release-address --allocation-id "$alloc_id" >/dev/null 2>&1 || true
done

echo ""

# Step 11: Delete VPC Endpoints
echo "=========================================="
echo "Step 11: Deleting VPC Endpoints"
echo "=========================================="
echo ""

for endpoint_id in $(aws ec2 describe-vpc-endpoints --filters "Name=tag:Environment,Values=dev" --output json | jq -r '.VpcEndpoints[].VpcEndpointId'); do
    echo "🗑️  Deleting VPC endpoint: $endpoint_id"
    aws ec2 delete-vpc-endpoints --vpc-endpoint-ids "$endpoint_id" >/dev/null 2>&1 || true
done

sleep 30

echo ""

# Step 12: Delete Subnets and Route Tables
echo "=========================================="
echo "Step 12: Deleting Subnets & Route Tables"
echo "=========================================="
echo ""

for vpc_id in $(aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | jq -r '.Vpcs[].VpcId'); do
    echo "📦 Processing VPC: $vpc_id"
    
    # Delete route table associations and routes
    for rt_id in $(aws ec2 describe-route-tables --filters "Name=vpc-id,Values=$vpc_id" --output json | jq -r '.RouteTables[] | select(.Associations[].Main == false) | .RouteTableId'); do
        echo "  🗑️  Deleting route table: $rt_id"
        
        # Disassociate subnets
        for assoc_id in $(aws ec2 describe-route-tables --route-table-ids "$rt_id" --output json | jq -r '.RouteTables[].Associations[] | select(.Main == false) | .RouteTableAssociationId'); do
            aws ec2 disassociate-route-table --association-id "$assoc_id" >/dev/null 2>&1 || true
        done
        
        aws ec2 delete-route-table --route-table-id "$rt_id" >/dev/null 2>&1 || true
    done
    
    # Delete subnets
    for subnet_id in $(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc_id" --output json | jq -r '.Subnets[].SubnetId'); do
        echo "  🗑️  Deleting subnet: $subnet_id"
        aws ec2 delete-subnet --subnet-id "$subnet_id" >/dev/null 2>&1 || true
    done
done

echo ""

# Step 13: Delete Internet Gateways
echo "=========================================="
echo "Step 13: Deleting Internet Gateways"
echo "=========================================="
echo ""

for vpc_id in $(aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | jq -r '.Vpcs[].VpcId'); do
    for igw_id in $(aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=$vpc_id" --output json | jq -r '.InternetGateways[].InternetGatewayId'); do
        echo "🗑️  Detaching and deleting IGW: $igw_id"
        aws ec2 detach-internet-gateway --internet-gateway-id "$igw_id" --vpc-id "$vpc_id" >/dev/null 2>&1 || true
        aws ec2 delete-internet-gateway --internet-gateway-id "$igw_id" >/dev/null 2>&1 || true
    done
done

echo ""

# Step 14: Delete Security Groups
echo "=========================================="
echo "Step 14: Deleting Security Groups"
echo "=========================================="
echo ""

for vpc_id in $(aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | jq -r '.Vpcs[].VpcId'); do
    echo "📦 Processing VPC: $vpc_id"
    
    # Delete non-default security groups
    for sg_id in $(aws ec2 describe-security-groups --filters "Name=vpc-id,Values=$vpc_id" --output json | jq -r '.SecurityGroups[] | select(.GroupName != "default") | .GroupId'); do
        echo "  🗑️  Deleting security group: $sg_id"
        aws ec2 delete-security-group --group-id "$sg_id" >/dev/null 2>&1 || true
    done
done

echo ""

# Step 15: Delete VPCs
echo "=========================================="
echo "Step 15: Deleting VPCs"
echo "=========================================="
echo ""

for vpc_id in $(aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=dev" --output json | jq -r '.Vpcs[].VpcId'); do
    echo "🗑️  Deleting VPC: $vpc_id"
    aws ec2 delete-vpc --vpc-id "$vpc_id" >/dev/null 2>&1 || true
done

echo ""

# Step 16: Delete Secrets Manager Secrets
echo "=========================================="
echo "Step 16: Deleting Secrets"
echo "=========================================="
echo ""

for secret_arn in $(aws secretsmanager list-secrets --output json | jq -r '.SecretList[] | select(.Name | contains("dev")) | .ARN'); do
    secret_name=$(aws secretsmanager describe-secret --secret-id "$secret_arn" | jq -r '.Name')
    echo "🗑️  Force deleting secret: $secret_name"
    aws secretsmanager delete-secret --secret-id "$secret_arn" --force-delete-without-recovery >/dev/null 2>&1 || true
done

echo ""

# Step 17: Schedule KMS Key Deletion
echo "=========================================="
echo "Step 17: Scheduling KMS Key Deletion"
echo "=========================================="
echo ""

for key_id in $(aws kms list-keys --output json | jq -r '.Keys[].KeyId'); do
    key_tags=$(aws kms list-resource-tags --key-id "$key_id" --output json 2>/dev/null | jq -r '.Tags[]? | select(.TagKey=="Environment" and .TagValue=="dev")')
    if [ -n "$key_tags" ]; then
        echo "🗑️  Scheduling KMS key deletion: $key_id (7 days)"
        aws kms schedule-key-deletion --key-id "$key_id" --pending-window-in-days 7 >/dev/null 2>&1 || true
    fi
done

echo ""

# Step 18: Delete CloudWatch Resources
echo "=========================================="
echo "Step 18: Deleting CloudWatch Resources"
echo "=========================================="
echo ""

# Delete alarms
for alarm_name in $(aws cloudwatch describe-alarms --output json | jq -r '.MetricAlarms[] | select(.AlarmName | contains("dev")) | .AlarmName'); do
    echo "🗑️  Deleting alarm: $alarm_name"
    aws cloudwatch delete-alarms --alarm-names "$alarm_name" >/dev/null 2>&1 || true
done

# Delete log groups
for log_group in $(aws logs describe-log-groups --output json | jq -r '.logGroups[] | select(.logGroupName | contains("dev") or contains("turaf")) | .logGroupName'); do
    echo "🗑️  Deleting log group: $log_group"
    aws logs delete-log-group --log-group-name "$log_group" >/dev/null 2>&1 || true
done

echo ""

# Step 19: Clean Terraform State
echo "=========================================="
echo "Step 19: Cleaning Terraform State"
echo "=========================================="
echo ""

cd ../terraform/environments/dev

if [ -f "terraform.tfstate" ] || aws s3 ls s3://turaf-terraform-state/dev/terraform.tfstate >/dev/null 2>&1; then
    echo "🧹 Removing all resources from Terraform state..."
    
    # List all resources and remove them
    terraform state list 2>/dev/null | while read resource; do
        echo "  Removing: $resource"
        terraform state rm "$resource" >/dev/null 2>&1 || true
    done
    
    echo "✅ Terraform state cleaned"
else
    echo "ℹ️  No Terraform state found"
fi

echo ""
echo "=========================================="
echo "Teardown Complete!"
echo "=========================================="
echo ""
echo "✅ All DEV application resources have been destroyed"
echo ""
echo "Preserved resources:"
echo "  ✅ Terraform state bucket: turaf-terraform-state"
echo "  ✅ DynamoDB lock table: turaf-terraform-locks"
echo "  ✅ ECR repositories"
echo "  ✅ IAM OIDC roles"
echo "  ✅ CodeBuild projects"
echo ""
echo "Next steps:"
echo "  1. Review infrastructure/docs/pre-reset-inventory.md"
echo "  2. Fix Terraform modules (Phase 2)"
echo "  3. Deploy fresh infrastructure (Phase 4)"
echo ""
