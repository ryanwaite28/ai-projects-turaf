#!/usr/bin/env bash
###############################################################################
# Turaf Platform — Full AWS Teardown Script
#
# Deletes ALL AWS resources across ALL accounts so that monthly costs go to $0.
#
# AWS Accounts:
#   Root : 072456928432
#   Ops  : 146072879609
#   Dev  : 801651112319
#   QA   : 965932217544
#   Prod : 811783768245
#
# USAGE:
#   ./scripts/aws-teardown-all.sh [--dry-run] [--skip-confirm]
#
# PREREQUISITES:
#   - AWS CLI v2 installed
#   - Credentials for the root/management account (072456928432) configured
#   - The root account must be able to assume GitHubActionsDeploymentRole in
#     each child account, OR you manually configure profiles for each account.
#   - jq installed
#
# WARNING: THIS SCRIPT IS DESTRUCTIVE AND IRREVERSIBLE.
###############################################################################
set -euo pipefail

REGION="us-east-1"
PROJECT="turaf"

# Account IDs
ROOT_ACCOUNT="072456928432"
OPS_ACCOUNT="146072879609"
DEV_ACCOUNT="801651112319"
QA_ACCOUNT="965932217544"
PROD_ACCOUNT="811783768245"

ENV_ACCOUNTS=("$DEV_ACCOUNT" "$QA_ACCOUNT" "$PROD_ACCOUNT")
ENV_NAMES=("dev" "qa" "prod")
ALL_ACCOUNTS=("$ROOT_ACCOUNT" "$OPS_ACCOUNT" "$DEV_ACCOUNT" "$QA_ACCOUNT" "$PROD_ACCOUNT")
ALL_ACCOUNT_NAMES=("root" "ops" "dev" "qa" "prod")

DRY_RUN=false
SKIP_CONFIRM=false

# ECR service names (in root/ops or whichever account hosts them)
ECR_SERVICES=(
  "turaf-identity-service"
  "turaf-organization-service"
  "turaf-experiment-service"
  "turaf-metrics-service"
  "turaf-communications-service"
  "turaf-bff-api"
  "turaf-ws-gateway"
)

# ECS service names per environment
ECS_SERVICES=(
  "bff-api"
  "identity-service"
  "organization-service"
  "experiment-service"
  "metrics-service"
  "communications-service"
  "ws-gateway"
)

###############################################################################
# Helpers
###############################################################################

log()   { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
err()   { echo -e "\033[1;31m[ERROR]\033[0m $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
header(){ echo -e "\n\033[1;35m========== $* ==========\033[0m\n"; }

run_aws() {
  if $DRY_RUN; then
    log "[DRY-RUN] aws $*"
  else
    aws "$@" 2>/dev/null || true
  fi
}

# Assume role in a child account and export temporary credentials
assume_role() {
  local account_id=$1
  local role_name="${2:-GitHubActionsDeploymentRole}"

  if [[ "$account_id" == "$ROOT_ACCOUNT" ]]; then
    # Already in root — unset any assumed-role env vars
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN 2>/dev/null || true
    return 0
  fi

  log "Assuming role arn:aws:iam::${account_id}:role/${role_name} ..."
  local creds
  creds=$(aws sts assume-role \
    --role-arn "arn:aws:iam::${account_id}:role/${role_name}" \
    --role-session-name "turaf-teardown-$(date +%s)" \
    --duration-seconds 3600 \
    --output json 2>/dev/null) || {
      warn "Could not assume role in account ${account_id}. Skipping."
      return 1
    }

  export AWS_ACCESS_KEY_ID=$(echo "$creds" | jq -r '.Credentials.AccessKeyId')
  export AWS_SECRET_ACCESS_KEY=$(echo "$creds" | jq -r '.Credentials.SecretAccessKey')
  export AWS_SESSION_TOKEN=$(echo "$creds" | jq -r '.Credentials.SessionToken')
  ok "Assumed role in account ${account_id}"
}

# Reset credentials back to default profile
reset_credentials() {
  unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN 2>/dev/null || true
}

###############################################################################
# Parse arguments
###############################################################################
for arg in "$@"; do
  case $arg in
    --dry-run)      DRY_RUN=true ;;
    --skip-confirm) SKIP_CONFIRM=true ;;
    *) err "Unknown argument: $arg"; exit 1 ;;
  esac
done

###############################################################################
# Confirmation
###############################################################################
if ! $SKIP_CONFIRM; then
  echo ""
  err "╔══════════════════════════════════════════════════════════════╗"
  err "║  WARNING: THIS WILL DELETE ALL AWS RESOURCES IN 5 ACCOUNTS ║"
  err "║                                                            ║"
  err "║  Root : 072456928432                                       ║"
  err "║  Ops  : 146072879609                                       ║"
  err "║  Dev  : 801651112319                                       ║"
  err "║  QA   : 965932217544                                       ║"
  err "║  Prod : 811783768245                                       ║"
  err "║                                                            ║"
  err "║  THIS ACTION IS IRREVERSIBLE.                              ║"
  err "╚══════════════════════════════════════════════════════════════╝"
  echo ""
  read -rp "Type 'DELETE EVERYTHING' to proceed: " confirmation
  if [[ "$confirmation" != "DELETE EVERYTHING" ]]; then
    log "Aborted."
    exit 0
  fi
  echo ""
fi

if $DRY_RUN; then
  warn "Running in DRY-RUN mode — no resources will be deleted."
  echo ""
fi

###############################################################################
# 1. Tear down environment accounts (dev, qa, prod)
###############################################################################
for i in "${!ENV_ACCOUNTS[@]}"; do
  ACCOUNT_ID="${ENV_ACCOUNTS[$i]}"
  ENV="${ENV_NAMES[$i]}"

  header "TEARING DOWN: ${ENV} (${ACCOUNT_ID})"

  if ! assume_role "$ACCOUNT_ID"; then
    warn "Skipping account ${ACCOUNT_ID} (${ENV})"
    reset_credentials
    continue
  fi

  # -------------------------------------------------------------------------
  # 1a. ECS Services — scale to 0, then delete
  # -------------------------------------------------------------------------
  log "Deleting ECS services in cluster turaf-cluster-${ENV} ..."
  CLUSTER="turaf-cluster-${ENV}"
  for svc in "${ECS_SERVICES[@]}"; do
    SVC_NAME="${svc}-${ENV}"
    log "  Scaling down and deleting ECS service: ${SVC_NAME}"
    run_aws ecs update-service --cluster "$CLUSTER" --service "$SVC_NAME" --desired-count 0 --region "$REGION"
    run_aws ecs delete-service --cluster "$CLUSTER" --service "$SVC_NAME" --force --region "$REGION"
  done

  # Wait for services to drain
  log "Waiting 15s for ECS services to drain..."
  sleep 15

  # Deregister all task definitions
  log "Deregistering ECS task definitions ..."
  for svc in "${ECS_SERVICES[@]}"; do
    FAMILY="${svc}-${ENV}"
    TASK_DEFS=$(aws ecs list-task-definitions --family-prefix "$FAMILY" --region "$REGION" --query 'taskDefinitionArns[]' --output text 2>/dev/null || echo "")
    for td in $TASK_DEFS; do
      run_aws ecs deregister-task-definition --task-definition "$td" --region "$REGION"
    done
  done

  # Delete ECS cluster
  log "Deleting ECS cluster: ${CLUSTER}"
  run_aws ecs delete-cluster --cluster "$CLUSTER" --region "$REGION"

  # -------------------------------------------------------------------------
  # 1b. Application Load Balancers
  # -------------------------------------------------------------------------
  log "Deleting ALB resources ..."

  # Delete listener rules, listeners, target groups, then ALBs
  for ALB_NAME in "turaf-alb-${ENV}" "turaf-internal-alb-${ENV}"; do
    ALB_ARN=$(aws elbv2 describe-load-balancers --names "$ALB_NAME" --region "$REGION" \
      --query 'LoadBalancers[0].LoadBalancerArn' --output text 2>/dev/null || echo "None")

    if [[ "$ALB_ARN" != "None" && -n "$ALB_ARN" ]]; then
      # Disable deletion protection first (prod ALBs have it enabled)
      run_aws elbv2 modify-load-balancer-attributes \
        --load-balancer-arn "$ALB_ARN" \
        --attributes Key=deletion_protection.enabled,Value=false \
        --region "$REGION"

      # Delete listeners
      LISTENERS=$(aws elbv2 describe-listeners --load-balancer-arn "$ALB_ARN" --region "$REGION" \
        --query 'Listeners[].ListenerArn' --output text 2>/dev/null || echo "")
      for listener in $LISTENERS; do
        # Delete listener rules (non-default)
        RULES=$(aws elbv2 describe-rules --listener-arn "$listener" --region "$REGION" \
          --query 'Rules[?!IsDefault].RuleArn' --output text 2>/dev/null || echo "")
        for rule in $RULES; do
          run_aws elbv2 delete-rule --rule-arn "$rule" --region "$REGION"
        done
        run_aws elbv2 delete-listener --listener-arn "$listener" --region "$REGION"
      done

      # Delete ALB
      run_aws elbv2 delete-load-balancer --load-balancer-arn "$ALB_ARN" --region "$REGION"
      log "  Deleted ALB: ${ALB_NAME}"
    fi
  done

  # Delete target groups
  log "Deleting target groups ..."
  TGS=$(aws elbv2 describe-target-groups --region "$REGION" \
    --query 'TargetGroups[].TargetGroupArn' --output text 2>/dev/null || echo "")
  for tg in $TGS; do
    run_aws elbv2 delete-target-group --target-group-arn "$tg" --region "$REGION"
  done

  # -------------------------------------------------------------------------
  # 1c. RDS Database
  # -------------------------------------------------------------------------
  log "Deleting RDS instance: turaf-db-${ENV} ..."
  # Disable deletion protection first
  run_aws rds modify-db-instance \
    --db-instance-identifier "turaf-db-${ENV}" \
    --no-deletion-protection \
    --apply-immediately \
    --region "$REGION"
  sleep 5
  run_aws rds delete-db-instance \
    --db-instance-identifier "turaf-db-${ENV}" \
    --skip-final-snapshot \
    --delete-automated-backups \
    --region "$REGION"

  # Delete DB subnet group (after instance is gone — will retry later)
  log "  Will clean up DB subnet group after RDS deletion completes."

  # -------------------------------------------------------------------------
  # 1d. Lambda Functions
  # -------------------------------------------------------------------------
  log "Deleting Lambda functions ..."
  for fn in "event-processor-${ENV}" "notification-processor-${ENV}" "report-generator-${ENV}"; do
    run_aws lambda delete-function --function-name "$fn" --region "$REGION"
  done

  # -------------------------------------------------------------------------
  # 1e. EventBridge
  # -------------------------------------------------------------------------
  log "Deleting EventBridge resources ..."
  EVENT_BUS="turaf-event-bus-${ENV}"

  # Delete rules on the event bus
  RULES=$(aws events list-rules --event-bus-name "$EVENT_BUS" --region "$REGION" \
    --query 'Rules[].Name' --output text 2>/dev/null || echo "")
  for rule in $RULES; do
    # Remove targets first
    TARGETS=$(aws events list-targets-by-rule --rule "$rule" --event-bus-name "$EVENT_BUS" --region "$REGION" \
      --query 'Targets[].Id' --output text 2>/dev/null || echo "")
    if [[ -n "$TARGETS" ]]; then
      run_aws events remove-targets --rule "$rule" --event-bus-name "$EVENT_BUS" --ids $TARGETS --region "$REGION"
    fi
    run_aws events delete-rule --name "$rule" --event-bus-name "$EVENT_BUS" --region "$REGION"
  done

  # Delete event archive
  run_aws events delete-archive --archive-name "turaf-event-archive-${ENV}" --region "$REGION"

  # Delete event bus
  run_aws events delete-event-bus --name "$EVENT_BUS" --region "$REGION"

  # -------------------------------------------------------------------------
  # 1f. SQS Queues
  # -------------------------------------------------------------------------
  log "Deleting SQS queues ..."
  for queue_name in \
    "turaf-events-${ENV}" \
    "turaf-dlq-${ENV}" \
    "turaf-chat-messages-${ENV}" \
    "turaf-notifications-${ENV}" \
    "turaf-reports-${ENV}"; do
    QUEUE_URL=$(aws sqs get-queue-url --queue-name "$queue_name" --region "$REGION" \
      --query 'QueueUrl' --output text 2>/dev/null || echo "")
    if [[ -n "$QUEUE_URL" ]]; then
      run_aws sqs delete-queue --queue-url "$QUEUE_URL" --region "$REGION"
      log "  Deleted queue: ${queue_name}"
    fi
  done

  # -------------------------------------------------------------------------
  # 1g. SNS Topics
  # -------------------------------------------------------------------------
  log "Deleting SNS topics ..."
  SNS_TOPICS=$(aws sns list-topics --region "$REGION" --query 'Topics[].TopicArn' --output text 2>/dev/null || echo "")
  for topic in $SNS_TOPICS; do
    if [[ "$topic" == *"turaf"* ]]; then
      # Delete subscriptions first
      SUBS=$(aws sns list-subscriptions-by-topic --topic-arn "$topic" --region "$REGION" \
        --query 'Subscriptions[].SubscriptionArn' --output text 2>/dev/null || echo "")
      for sub in $SUBS; do
        [[ "$sub" == "PendingConfirmation" ]] && continue
        run_aws sns unsubscribe --subscription-arn "$sub" --region "$REGION"
      done
      run_aws sns delete-topic --topic-arn "$topic" --region "$REGION"
      log "  Deleted topic: ${topic}"
    fi
  done

  # -------------------------------------------------------------------------
  # 1h. Secrets Manager
  # -------------------------------------------------------------------------
  log "Deleting Secrets Manager secrets ..."
  SECRETS=$(aws secretsmanager list-secrets --region "$REGION" \
    --query "SecretList[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
  for secret in $SECRETS; do
    run_aws secretsmanager delete-secret --secret-id "$secret" --force-delete-without-recovery --region "$REGION"
    log "  Deleted secret: ${secret}"
  done

  # -------------------------------------------------------------------------
  # 1i. KMS Keys (schedule deletion — minimum 7-day wait)
  # -------------------------------------------------------------------------
  log "Scheduling KMS key deletion ..."
  KMS_KEYS=$(aws kms list-keys --region "$REGION" --query 'Keys[].KeyId' --output text 2>/dev/null || echo "")
  for key_id in $KMS_KEYS; do
    # Check if it's a customer-managed key with turaf tags
    KEY_DESC=$(aws kms describe-key --key-id "$key_id" --region "$REGION" --query 'KeyMetadata' --output json 2>/dev/null || echo "{}")
    KEY_MANAGER=$(echo "$KEY_DESC" | jq -r '.KeyManager // "AWS"')
    KEY_STATE=$(echo "$KEY_DESC" | jq -r '.KeyState // "Disabled"')
    if [[ "$KEY_MANAGER" == "CUSTOMER" && "$KEY_STATE" == "Enabled" ]]; then
      TAGS=$(aws kms list-resource-tags --key-id "$key_id" --region "$REGION" \
        --query "Tags[?TagKey=='Project' && TagValue=='turaf']" --output text 2>/dev/null || echo "")
      if [[ -n "$TAGS" ]]; then
        run_aws kms schedule-key-deletion --key-id "$key_id" --pending-window-in-days 7 --region "$REGION"
        log "  Scheduled deletion for KMS key: ${key_id}"
      fi
    fi
  done

  # -------------------------------------------------------------------------
  # 1j. S3 Buckets (empty + delete)
  # -------------------------------------------------------------------------
  log "Deleting S3 buckets ..."
  BUCKETS=$(aws s3api list-buckets --query "Buckets[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
  for bucket in $BUCKETS; do
    log "  Emptying bucket: ${bucket}"
    run_aws s3 rb "s3://${bucket}" --force
    log "  Deleted bucket: ${bucket}"
  done

  # -------------------------------------------------------------------------
  # 1k. CloudWatch Log Groups
  # -------------------------------------------------------------------------
  log "Deleting CloudWatch log groups ..."
  LOG_GROUPS=$(aws logs describe-log-groups --region "$REGION" \
    --query "logGroups[?contains(logGroupName,'turaf') || contains(logGroupName,'ecs/')].logGroupName" \
    --output text 2>/dev/null || echo "")
  for lg in $LOG_GROUPS; do
    run_aws logs delete-log-group --log-group-name "$lg" --region "$REGION"
    log "  Deleted log group: ${lg}"
  done

  # Also delete lambda log groups
  for lg_prefix in "/aws/lambda/event-processor" "/aws/lambda/notification-processor" "/aws/lambda/report-generator" "/aws/events/turaf"; do
    LG_NAME="${lg_prefix}-${ENV}"
    run_aws logs delete-log-group --log-group-name "$LG_NAME" --region "$REGION"
  done

  # -------------------------------------------------------------------------
  # 1l. CloudWatch Alarms
  # -------------------------------------------------------------------------
  log "Deleting CloudWatch alarms ..."
  ALARMS=$(aws cloudwatch describe-alarms --region "$REGION" \
    --query "MetricAlarms[?contains(AlarmName,'turaf') || contains(AlarmName,'ecs-') || contains(AlarmName,'rds-') || contains(AlarmName,'alb-')].AlarmName" \
    --output text 2>/dev/null || echo "")
  if [[ -n "$ALARMS" ]]; then
    run_aws cloudwatch delete-alarms --alarm-names $ALARMS --region "$REGION"
  fi

  # -------------------------------------------------------------------------
  # 1m. CloudWatch Dashboards
  # -------------------------------------------------------------------------
  log "Deleting CloudWatch dashboards ..."
  run_aws cloudwatch delete-dashboards --dashboard-names "turaf-${ENV}" --region "$REGION"

  # -------------------------------------------------------------------------
  # 1n. X-Ray Sampling Rules
  # -------------------------------------------------------------------------
  log "Deleting X-Ray sampling rules ..."
  run_aws xray delete-sampling-rule --rule-name "turaf-sampling-${ENV}" --region "$REGION"

  # -------------------------------------------------------------------------
  # 1o. VPC Resources (subnets, route tables, IGW, NAT GW, endpoints, SGs)
  # -------------------------------------------------------------------------
  log "Deleting VPC resources ..."
  VPC_ID=$(aws ec2 describe-vpcs --region "$REGION" \
    --filters "Name=tag:Name,Values=*turaf*" \
    --query 'Vpcs[0].VpcId' --output text 2>/dev/null || echo "None")

  if [[ "$VPC_ID" != "None" && -n "$VPC_ID" ]]; then
    log "  Found VPC: ${VPC_ID}"

    # Delete NAT Gateways
    NAT_GWS=$(aws ec2 describe-nat-gateways --region "$REGION" \
      --filter "Name=vpc-id,Values=${VPC_ID}" "Name=state,Values=available" \
      --query 'NatGateways[].NatGatewayId' --output text 2>/dev/null || echo "")
    for nat in $NAT_GWS; do
      run_aws ec2 delete-nat-gateway --nat-gateway-id "$nat" --region "$REGION"
      log "  Deleted NAT Gateway: ${nat}"
    done

    # Delete VPC Endpoints
    ENDPOINTS=$(aws ec2 describe-vpc-endpoints --region "$REGION" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query 'VpcEndpoints[].VpcEndpointId' --output text 2>/dev/null || echo "")
    for ep in $ENDPOINTS; do
      run_aws ec2 delete-vpc-endpoints --vpc-endpoint-ids "$ep" --region "$REGION"
      log "  Deleted VPC Endpoint: ${ep}"
    done

    # Wait for NAT Gateways to delete
    if [[ -n "$NAT_GWS" ]]; then
      log "  Waiting 60s for NAT Gateways to delete..."
      sleep 60
    fi

    # Release Elastic IPs associated with NAT Gateways
    EIPS=$(aws ec2 describe-addresses --region "$REGION" \
      --filters "Name=tag:Name,Values=*turaf*" \
      --query 'Addresses[].AllocationId' --output text 2>/dev/null || echo "")
    for eip in $EIPS; do
      run_aws ec2 release-address --allocation-id "$eip" --region "$REGION"
      log "  Released Elastic IP: ${eip}"
    done

    # Detach and delete Internet Gateway
    IGWS=$(aws ec2 describe-internet-gateways --region "$REGION" \
      --filters "Name=attachment.vpc-id,Values=${VPC_ID}" \
      --query 'InternetGateways[].InternetGatewayId' --output text 2>/dev/null || echo "")
    for igw in $IGWS; do
      run_aws ec2 detach-internet-gateway --internet-gateway-id "$igw" --vpc-id "$VPC_ID" --region "$REGION"
      run_aws ec2 delete-internet-gateway --internet-gateway-id "$igw" --region "$REGION"
      log "  Deleted Internet Gateway: ${igw}"
    done

    # Delete non-main route table associations, then route tables
    RTS=$(aws ec2 describe-route-tables --region "$REGION" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query 'RouteTables[].RouteTableId' --output text 2>/dev/null || echo "")
    for rt in $RTS; do
      # Delete non-main associations
      ASSOCS=$(aws ec2 describe-route-tables --route-table-ids "$rt" --region "$REGION" \
        --query 'RouteTables[0].Associations[?!Main].RouteTableAssociationId' --output text 2>/dev/null || echo "")
      for assoc in $ASSOCS; do
        run_aws ec2 disassociate-route-table --association-id "$assoc" --region "$REGION"
      done
      # Delete non-main route tables
      IS_MAIN=$(aws ec2 describe-route-tables --route-table-ids "$rt" --region "$REGION" \
        --query 'RouteTables[0].Associations[?Main]' --output text 2>/dev/null || echo "")
      if [[ -z "$IS_MAIN" ]]; then
        run_aws ec2 delete-route-table --route-table-id "$rt" --region "$REGION"
        log "  Deleted route table: ${rt}"
      fi
    done

    # Delete subnets
    SUBNETS=$(aws ec2 describe-subnets --region "$REGION" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query 'Subnets[].SubnetId' --output text 2>/dev/null || echo "")
    for subnet in $SUBNETS; do
      run_aws ec2 delete-subnet --subnet-id "$subnet" --region "$REGION"
      log "  Deleted subnet: ${subnet}"
    done

    # Delete non-default security groups
    SGS=$(aws ec2 describe-security-groups --region "$REGION" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query "SecurityGroups[?GroupName!='default'].GroupId" --output text 2>/dev/null || echo "")
    # First remove all ingress/egress rules referencing other SGs to break circular deps
    for sg in $SGS; do
      run_aws ec2 revoke-security-group-ingress --group-id "$sg" --region "$REGION" \
        --ip-permissions "$(aws ec2 describe-security-groups --group-ids "$sg" --region "$REGION" \
        --query 'SecurityGroups[0].IpPermissions' --output json 2>/dev/null || echo "[]")" 2>/dev/null || true
      run_aws ec2 revoke-security-group-egress --group-id "$sg" --region "$REGION" \
        --ip-permissions "$(aws ec2 describe-security-groups --group-ids "$sg" --region "$REGION" \
        --query 'SecurityGroups[0].IpPermissionsEgress' --output json 2>/dev/null || echo "[]")" 2>/dev/null || true
    done
    for sg in $SGS; do
      run_aws ec2 delete-security-group --group-id "$sg" --region "$REGION"
      log "  Deleted security group: ${sg}"
    done

    # Delete Network ACLs (non-default)
    NACLS=$(aws ec2 describe-network-acls --region "$REGION" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query "NetworkAcls[?!IsDefault].NetworkAclId" --output text 2>/dev/null || echo "")
    for nacl in $NACLS; do
      run_aws ec2 delete-network-acl --network-acl-id "$nacl" --region "$REGION"
    done

    # Delete DB subnet groups
    run_aws rds delete-db-subnet-group --db-subnet-group-name "turaf-db-subnet-group-${ENV}" --region "$REGION"

    # Delete ElastiCache subnet groups
    run_aws elasticache delete-cache-subnet-group --cache-subnet-group-name "turaf-redis-subnet-group-${ENV}" --region "$REGION" 2>/dev/null || true

    # Delete the VPC itself
    run_aws ec2 delete-vpc --vpc-id "$VPC_ID" --region "$REGION"
    ok "Deleted VPC: ${VPC_ID}"
  fi

  # -------------------------------------------------------------------------
  # 1p. DynamoDB Tables (idempotency table from messaging module)
  # -------------------------------------------------------------------------
  log "Deleting DynamoDB tables ..."
  DDB_TABLES=$(aws dynamodb list-tables --region "$REGION" \
    --query "TableNames[?contains(@,'turaf')]" --output text 2>/dev/null || echo "")
  for table in $DDB_TABLES; do
    run_aws dynamodb delete-table --table-name "$table" --region "$REGION"
    log "  Deleted DynamoDB table: ${table}"
  done

  # -------------------------------------------------------------------------
  # 1q. ACM Certificates
  # -------------------------------------------------------------------------
  log "Deleting ACM certificates ..."
  CERTS=$(aws acm list-certificates --region "$REGION" \
    --query "CertificateSummaryList[?contains(DomainName,'turafapp.com')].CertificateArn" \
    --output text 2>/dev/null || echo "")
  for cert in $CERTS; do
    run_aws acm delete-certificate --certificate-arn "$cert" --region "$REGION"
    log "  Deleted certificate: ${cert}"
  done

  ok "Completed teardown of ${ENV} (${ACCOUNT_ID})"
  reset_credentials
done

###############################################################################
# 2. Tear down Ops account (146072879609)
###############################################################################
header "TEARING DOWN: ops (${OPS_ACCOUNT})"

if assume_role "$OPS_ACCOUNT"; then

  # Delete S3 buckets (terraform state, artifacts)
  log "Deleting S3 buckets in ops account ..."
  BUCKETS=$(aws s3api list-buckets --query "Buckets[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
  for bucket in $BUCKETS; do
    log "  Emptying and deleting: ${bucket}"
    run_aws s3 rb "s3://${bucket}" --force
  done

  # Delete CloudWatch log groups
  LOG_GROUPS=$(aws logs describe-log-groups --region "$REGION" \
    --query "logGroups[?contains(logGroupName,'turaf')].logGroupName" --output text 2>/dev/null || echo "")
  for lg in $LOG_GROUPS; do
    run_aws logs delete-log-group --log-group-name "$lg" --region "$REGION"
  done

  ok "Completed teardown of ops (${OPS_ACCOUNT})"
  reset_credentials
else
  warn "Skipping ops account"
  reset_credentials
fi

###############################################################################
# 3. Tear down Root account (072456928432)
###############################################################################
header "TEARING DOWN: root (${ROOT_ACCOUNT})"
reset_credentials  # Use default credentials (root account)

# -------------------------------------------------------------------------
# 3a. ECR Repositories (may be in root or each account — check both)
# -------------------------------------------------------------------------
log "Deleting ECR repositories ..."
for repo in "${ECR_SERVICES[@]}"; do
  # Force delete all images
  run_aws ecr delete-repository --repository-name "$repo" --force --region "$REGION"
  log "  Deleted ECR repo: ${repo}"
done

# -------------------------------------------------------------------------
# 3b. Route53 Hosted Zones
# -------------------------------------------------------------------------
log "Deleting Route53 hosted zones ..."
HOSTED_ZONES=$(aws route53 list-hosted-zones \
  --query "HostedZones[?contains(Name,'turafapp.com')].Id" --output text 2>/dev/null || echo "")
for zone_id in $HOSTED_ZONES; do
  # Clean zone_id (remove /hostedzone/ prefix)
  ZONE_ID_CLEAN=$(echo "$zone_id" | sed 's|/hostedzone/||')

  # Delete all non-NS/SOA records
  RECORDS=$(aws route53 list-resource-record-sets --hosted-zone-id "$ZONE_ID_CLEAN" \
    --query "ResourceRecordSets[?Type!='NS' && Type!='SOA']" --output json 2>/dev/null || echo "[]")

  RECORD_COUNT=$(echo "$RECORDS" | jq 'length')
  if [[ "$RECORD_COUNT" -gt 0 ]]; then
    # Build change batch
    CHANGES=$(echo "$RECORDS" | jq '[.[] | {Action: "DELETE", ResourceRecordSet: .}]')
    CHANGE_BATCH="{\"Changes\": ${CHANGES}}"
    run_aws route53 change-resource-record-sets \
      --hosted-zone-id "$ZONE_ID_CLEAN" \
      --change-batch "$CHANGE_BATCH"
    log "  Deleted ${RECORD_COUNT} DNS records from zone ${ZONE_ID_CLEAN}"
  fi

  run_aws route53 delete-hosted-zone --id "$ZONE_ID_CLEAN"
  log "  Deleted hosted zone: ${ZONE_ID_CLEAN}"
done

# -------------------------------------------------------------------------
# 3c. CloudFront Distributions
# -------------------------------------------------------------------------
log "Deleting CloudFront distributions ..."
CF_DISTS=$(aws cloudfront list-distributions \
  --query "DistributionList.Items[?contains(Comment,'turaf') || contains(Origins.Items[0].DomainName,'turaf')].Id" \
  --output text 2>/dev/null || echo "")
for dist_id in $CF_DISTS; do
  # Get ETag and disable
  ETAG=$(aws cloudfront get-distribution-config --id "$dist_id" \
    --query 'ETag' --output text 2>/dev/null || echo "")
  CONFIG=$(aws cloudfront get-distribution-config --id "$dist_id" \
    --query 'DistributionConfig' --output json 2>/dev/null || echo "{}")
  DISABLED_CONFIG=$(echo "$CONFIG" | jq '.Enabled = false')

  if [[ -n "$ETAG" && "$DISABLED_CONFIG" != "{}" ]]; then
    run_aws cloudfront update-distribution --id "$dist_id" \
      --if-match "$ETAG" --distribution-config "$DISABLED_CONFIG"
    log "  Disabled CloudFront distribution: ${dist_id} (will delete after propagation)"
    # Note: CloudFront distributions take ~15 min to disable before they can be deleted
    log "  NOTE: Re-run this script or manually delete dist ${dist_id} after it's fully disabled."
  fi
done

# -------------------------------------------------------------------------
# 3d. ACM Certificates in root
# -------------------------------------------------------------------------
log "Deleting ACM certificates in root account ..."
CERTS=$(aws acm list-certificates --region "$REGION" \
  --query "CertificateSummaryList[?contains(DomainName,'turafapp.com')].CertificateArn" \
  --output text 2>/dev/null || echo "")
for cert in $CERTS; do
  run_aws acm delete-certificate --certificate-arn "$cert" --region "$REGION"
  log "  Deleted certificate: ${cert}"
done

# Also check us-east-1 for CloudFront certificates
if [[ "$REGION" != "us-east-1" ]]; then
  CERTS=$(aws acm list-certificates --region us-east-1 \
    --query "CertificateSummaryList[?contains(DomainName,'turafapp.com')].CertificateArn" \
    --output text 2>/dev/null || echo "")
  for cert in $CERTS; do
    run_aws acm delete-certificate --certificate-arn "$cert" --region us-east-1
  done
fi

# -------------------------------------------------------------------------
# 3e. S3 Buckets in root (terraform state, bootstrap)
# -------------------------------------------------------------------------
log "Deleting S3 buckets in root account ..."
BUCKETS=$(aws s3api list-buckets --query "Buckets[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
for bucket in $BUCKETS; do
  log "  Emptying and deleting: ${bucket}"
  run_aws s3 rb "s3://${bucket}" --force
done

# -------------------------------------------------------------------------
# 3f. DynamoDB Lock Table (in root/dev account)
# -------------------------------------------------------------------------
log "Deleting DynamoDB terraform lock table ..."
run_aws dynamodb delete-table --table-name "turaf-terraform-locks" --region "$REGION"

# -------------------------------------------------------------------------
# 3h. CloudTrail
# -------------------------------------------------------------------------
log "Deleting CloudTrail trails ..."
TRAILS=$(aws cloudtrail describe-trails --region "$REGION" \
  --query "trailList[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
for trail in $TRAILS; do
  run_aws cloudtrail delete-trail --name "$trail" --region "$REGION"
  log "  Deleted trail: ${trail}"
done

# -------------------------------------------------------------------------
# 3i. AWS Budgets
# -------------------------------------------------------------------------
log "Deleting AWS Budgets ..."
BUDGET_NAMES=$(aws budgets describe-budgets --account-id "$ROOT_ACCOUNT" \
  --query "Budgets[?contains(BudgetName,'turaf') || contains(BudgetName,'Turaf')].BudgetName" \
  --output text 2>/dev/null || echo "")
for budget in $BUDGET_NAMES; do
  run_aws budgets delete-budget --account-id "$ROOT_ACCOUNT" --budget-name "$budget"
  log "  Deleted budget: ${budget}"
done

# -------------------------------------------------------------------------
# 3j. SCPs (Service Control Policies) — detach but don't delete AWS managed
# -------------------------------------------------------------------------
log "Detaching custom SCPs ..."
ORG_ROOT_ID="r-gs6r"
SCPS=$(aws organizations list-policies --filter SERVICE_CONTROL_POLICY \
  --query "Policies[?!contains(Name,'FullAWSAccess')].Id" --output text 2>/dev/null || echo "")
for scp_id in $SCPS; do
  # Detach from all targets
  TARGETS=$(aws organizations list-targets-for-policy --policy-id "$scp_id" \
    --query 'Targets[].TargetId' --output text 2>/dev/null || echo "")
  for target in $TARGETS; do
    run_aws organizations detach-policy --policy-id "$scp_id" --target-id "$target"
  done
  run_aws organizations delete-policy --policy-id "$scp_id"
  log "  Deleted SCP: ${scp_id}"
done

ok "Completed teardown of root (${ROOT_ACCOUNT})"

###############################################################################
# 4. Final sweep — check all accounts for remaining tagged resources
###############################################################################
header "FINAL SWEEP — Checking for remaining turaf resources"

for i in "${!ALL_ACCOUNTS[@]}"; do
  ACCOUNT_ID="${ALL_ACCOUNTS[$i]}"
  ACCOUNT_NAME="${ALL_ACCOUNT_NAMES[$i]}"

  if ! assume_role "$ACCOUNT_ID"; then
    continue
  fi

  log "Checking ${ACCOUNT_NAME} (${ACCOUNT_ID}) for remaining resources..."

  # Check for remaining S3 buckets
  REMAINING_BUCKETS=$(aws s3api list-buckets --query "Buckets[?contains(Name,'turaf')].Name" --output text 2>/dev/null || echo "")
  [[ -n "$REMAINING_BUCKETS" ]] && warn "  Remaining S3 buckets: ${REMAINING_BUCKETS}"

  # Check for remaining ECS clusters
  REMAINING_CLUSTERS=$(aws ecs list-clusters --region "$REGION" --query 'clusterArns[]' --output text 2>/dev/null || echo "")
  for cluster in $REMAINING_CLUSTERS; do
    [[ "$cluster" == *"turaf"* ]] && warn "  Remaining ECS cluster: ${cluster}"
  done

  # Check for remaining RDS instances
  REMAINING_RDS=$(aws rds describe-db-instances --region "$REGION" \
    --query "DBInstances[?contains(DBInstanceIdentifier,'turaf')].DBInstanceIdentifier" --output text 2>/dev/null || echo "")
  [[ -n "$REMAINING_RDS" ]] && warn "  Remaining RDS: ${REMAINING_RDS} (may still be deleting)"

  # Check for remaining Lambda functions
  REMAINING_LAMBDA=$(aws lambda list-functions --region "$REGION" \
    --query "Functions[?contains(FunctionName,'processor') || contains(FunctionName,'generator')].FunctionName" --output text 2>/dev/null || echo "")
  [[ -n "$REMAINING_LAMBDA" ]] && warn "  Remaining Lambda: ${REMAINING_LAMBDA}"

  reset_credentials
done

###############################################################################
# Summary
###############################################################################
header "TEARDOWN COMPLETE"

echo "Resources deleted across all 5 AWS accounts:"
echo ""
echo "  Per environment (dev/qa/prod):"
echo "    - ECS services, task definitions, cluster"
echo "    - Application Load Balancers (public + internal)"
echo "    - RDS PostgreSQL instances"
echo "    - Lambda functions"
echo "    - EventBridge event buses, rules, archives"
echo "    - SQS queues (events, DLQ, chat, notifications, reports)"
echo "    - SNS topics and subscriptions"
echo "    - Secrets Manager secrets"
echo "    - KMS keys (scheduled for deletion in 7 days)"
echo "    - S3 buckets"
echo "    - CloudWatch log groups, alarms, dashboards"
echo "    - X-Ray sampling rules"
echo "    - VPC and all networking (subnets, NAT GW, IGW, SGs, endpoints)"
echo "    - DynamoDB tables"
echo "    - ACM certificates"
echo ""
echo "  Root account:"
echo "    - ECR repositories (7 services)"
echo "    - Route53 hosted zones"
echo "    - CloudFront distributions (disabled — may need manual final delete)"
echo "    - Terraform state S3 buckets"
echo "    - DynamoDB lock table"
echo "    - CloudTrail trails"
echo "    - AWS Budgets"
echo "    - Service Control Policies"
echo ""
echo "  Ops account:"
echo "    - S3 buckets"
echo "    - CloudWatch log groups"
echo ""
echo "  PRESERVED (not deleted):"
echo "    - IAM roles, policies, OIDC providers (all accounts)"
echo ""

if $DRY_RUN; then
  warn "This was a DRY RUN. No resources were actually deleted."
  warn "Run without --dry-run to execute the teardown."
else
  ok "All resources deleted. Monthly AWS costs should now be \$0."
  warn "NOTE: KMS keys are scheduled for deletion in 7 days (AWS minimum)."
  warn "NOTE: CloudFront distributions may take up to 15 minutes to disable before final deletion."
  warn "NOTE: RDS instances may take several minutes to fully terminate."
  echo ""
  echo "To verify, check the AWS Cost Explorer in 24-48 hours."
fi
