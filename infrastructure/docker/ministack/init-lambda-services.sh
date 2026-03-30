#!/bin/bash

echo "=========================================="
echo "Deploying Lambda Services to MiniStack"
echo "=========================================="

# Set AWS CLI to use MiniStack endpoint
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
ENDPOINT="http://localhost:4566"

# Check if MiniStack is running (skip if running inside container)
if [ ! -f "/.dockerenv" ]; then
    echo "Checking MiniStack availability..."
    if ! curl -s -f "$ENDPOINT/_localstack/health" > /dev/null 2>&1; then
        echo "Error: MiniStack is not running at $ENDPOINT"
        echo "Please start MiniStack first (e.g., docker-compose up -d ministack)"
        exit 1
    fi
    echo "✓ MiniStack is running"
    echo ""
else
    echo "Running inside container, skipping health check"
    echo ""
fi

# Script directory (POSIX-compliant)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/lambda-builds"

# Check for required tools
if ! command -v zip >/dev/null 2>&1; then
    echo "Error: 'zip' command not found. Installing..."
    if command -v apk >/dev/null 2>&1; then
        apk add --no-cache zip >/dev/null 2>&1 || {
            echo "Failed to install zip. Lambda deployment requires zip to be available."
            echo "Please install zip manually or run this script outside the container."
            exit 1
        }
    else
        echo "Error: Cannot install zip automatically. Please install it manually."
        exit 1
    fi
fi

if ! command -v pip >/dev/null 2>&1 && ! command -v pip3 >/dev/null 2>&1; then
    echo "Warning: pip not found. Lambda dependencies will not be installed."
    echo "Functions will be deployed with source code only."
fi

# Cleanup function
cleanup() {
    echo "Cleaning up temporary files..."
    rm -rf "$BUILD_DIR"
}
trap cleanup EXIT

# Create build directory
mkdir -p "$BUILD_DIR"

# ==========================================
# Helper Functions
# ==========================================

package_lambda() {
    local service_name=$1
    local service_path=$2
    local handler_module=$3
    local output_zip=$4
    
    echo "Packaging $service_name..."
    
    local build_path="$BUILD_DIR/$service_name"
    mkdir -p "$build_path"
    
    # Install dependencies
    if [ -f "$service_path/requirements.txt" ]; then
        echo "  Installing dependencies..."
        pip install -q -r "$service_path/requirements.txt" -t "$build_path" 2>/dev/null || {
            echo "  Warning: Some dependencies may not have installed correctly"
        }
    fi
    
    # Copy source code
    echo "  Copying source code..."
    if [ -d "$service_path/src" ]; then
        cp -r "$service_path/src" "$build_path/"
    fi
    
    # Copy handler files at root level for notification-service
    if [ "$service_name" = "notification-service" ]; then
        cp -r "$service_path"/*.py "$build_path/" 2>/dev/null || true
        cp -r "$service_path/handlers" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/services" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/clients" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/models" "$build_path/" 2>/dev/null || true
        cp -r "$service_path/templates" "$build_path/" 2>/dev/null || true
    fi
    
    # Copy templates for reporting-service
    if [ "$service_name" = "reporting-service" ]; then
        if [ -d "$service_path/src/templates" ]; then
            mkdir -p "$build_path/templates"
            cp -r "$service_path/src/templates"/* "$build_path/templates/" 2>/dev/null || true
        fi
    fi
    
    # Create ZIP archive
    echo "  Creating ZIP archive..."
    cd "$build_path"
    zip -q -r "$output_zip" . || {
        echo "  Error: Failed to create ZIP archive"
        return 1
    }
    cd - > /dev/null
    
    echo "  ✓ Package created: $output_zip ($(du -h "$output_zip" | cut -f1))"
    return 0
}

create_lambda_role() {
    local role_name=$1
    local service_type=$2
    
    echo "Creating IAM role: $role_name..."
    
    # Trust policy for Lambda
    local trust_policy='{
      "Version": "2012-10-17",
      "Statement": [{
        "Effect": "Allow",
        "Principal": {"Service": "lambda.amazonaws.com"},
        "Action": "sts:AssumeRole"
      }]
    }'
    
    # Create role
    aws --endpoint-url=$ENDPOINT iam create-role \
        --role-name "$role_name" \
        --assume-role-policy-document "$trust_policy" 2>/dev/null || \
        echo "  Role $role_name already exists"
    
    # Attach basic Lambda execution policy
    local basic_policy='{
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "logs:CreateLogGroup",
            "logs:CreateLogStream",
            "logs:PutLogEvents"
          ],
          "Resource": "arn:aws:logs:*:*:*"
        },
        {
          "Effect": "Allow",
          "Action": [
            "sqs:ReceiveMessage",
            "sqs:DeleteMessage",
            "sqs:GetQueueAttributes"
          ],
          "Resource": "*"
        }
      ]
    }'
    
    aws --endpoint-url=$ENDPOINT iam put-role-policy \
        --role-name "$role_name" \
        --policy-name "${role_name}-basic-policy" \
        --policy-document "$basic_policy" 2>/dev/null || true
    
    # Service-specific policies
    if [ "$service_type" = "reporting" ]; then
        local reporting_policy='{
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:PutObject", "s3:GetObject"],
              "Resource": "arn:aws:s3:::turaf-reports-local/*"
            },
            {
              "Effect": "Allow",
              "Action": ["events:PutEvents"],
              "Resource": "*"
            },
            {
              "Effect": "Allow",
              "Action": ["dynamodb:GetItem", "dynamodb:PutItem"],
              "Resource": "arn:aws:dynamodb:*:*:table/processed_events"
            }
          ]
        }'
        
        aws --endpoint-url=$ENDPOINT iam put-role-policy \
            --role-name "$role_name" \
            --policy-name "${role_name}-service-policy" \
            --policy-document "$reporting_policy" 2>/dev/null || true
    fi
    
    if [ "$service_type" = "notification" ]; then
        local notification_policy='{
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["ses:SendEmail", "ses:SendRawEmail"],
              "Resource": "*"
            },
            {
              "Effect": "Allow",
              "Action": ["dynamodb:GetItem", "dynamodb:PutItem"],
              "Resource": "arn:aws:dynamodb:*:*:table/processed_notification_events"
            }
          ]
        }'
        
        aws --endpoint-url=$ENDPOINT iam put-role-policy \
            --role-name "$role_name" \
            --policy-name "${role_name}-service-policy" \
            --policy-document "$notification_policy" 2>/dev/null || true
    fi
    
    echo "  ✓ IAM role created"
}

deploy_lambda() {
    local function_name=$1
    local zip_file=$2
    local handler=$3
    local role_arn=$4
    local memory=$5
    local timeout=$6
    local env_vars=$7
    
    echo "Deploying Lambda function: $function_name..."
    
    # Check if function exists
    if aws --endpoint-url=$ENDPOINT lambda get-function --function-name "$function_name" 2>/dev/null; then
        echo "  Updating existing function..."
        aws --endpoint-url=$ENDPOINT lambda update-function-code \
            --function-name "$function_name" \
            --zip-file "fileb://$zip_file" 2>/dev/null || {
            echo "  Error: Failed to update function code"
            return 1
        }
        
        aws --endpoint-url=$ENDPOINT lambda update-function-configuration \
            --function-name "$function_name" \
            --timeout "$timeout" \
            --memory-size "$memory" \
            --environment "$env_vars" 2>/dev/null || true
    else
        echo "  Creating new function..."
        aws --endpoint-url=$ENDPOINT lambda create-function \
            --function-name "$function_name" \
            --runtime python3.11 \
            --role "$role_arn" \
            --handler "$handler" \
            --zip-file "fileb://$zip_file" \
            --timeout "$timeout" \
            --memory-size "$memory" \
            --environment "$env_vars" 2>/dev/null || {
            echo "  Error: Failed to create function"
            return 1
        }
    fi
    
    echo "  ✓ Lambda function deployed"
    return 0
}

create_event_source_mapping() {
    local function_name=$1
    local queue_name=$2
    
    echo "Creating event source mapping for $function_name..."
    
    # Get queue ARN
    local queue_arn="arn:aws:sqs:us-east-1:000000000000:$queue_name"
    
    # Check if mapping already exists
    local existing_mapping=$(aws --endpoint-url=$ENDPOINT lambda list-event-source-mappings \
        --function-name "$function_name" \
        --query "EventSourceMappings[?EventSourceArn=='$queue_arn'].UUID" \
        --output text 2>/dev/null)
    
    if [ -n "$existing_mapping" ]; then
        echo "  Event source mapping already exists: $existing_mapping"
        return 0
    fi
    
    # Create event source mapping
    aws --endpoint-url=$ENDPOINT lambda create-event-source-mapping \
        --function-name "$function_name" \
        --event-source-arn "$queue_arn" \
        --batch-size 1 \
        --enabled 2>/dev/null || {
        echo "  Warning: Failed to create event source mapping (may not be supported in MiniStack)"
        return 1
    }
    
    echo "  ✓ Event source mapping created"
    return 0
}

# ==========================================
# Deploy Reporting Service
# ==========================================
echo ""
echo "=========================================="
echo "Deploying Reporting Service"
echo "=========================================="

REPORTING_SERVICE_PATH="$PROJECT_ROOT/services/reporting-service"
REPORTING_ZIP="$BUILD_DIR/reporting-service.zip"

# Package Lambda
package_lambda "reporting-service" "$REPORTING_SERVICE_PATH" "src.lambda_handler" "$REPORTING_ZIP" || {
    echo "Failed to package reporting-service"
    exit 1
}

# Create IAM role
create_lambda_role "lambda-reporting-role" "reporting"

# Deploy Lambda function
REPORTING_ENV_VARS='Variables={
    ENVIRONMENT=local,
    S3_BUCKET_NAME=turaf-reports-local,
    EVENT_BUS_NAME=turaf-events,
    EXPERIMENT_SERVICE_URL=http://host.docker.internal:8080,
    METRICS_SERVICE_URL=http://host.docker.internal:8080,
    IDEMPOTENCY_TABLE_NAME=processed_events,
    AWS_ENDPOINT_URL=http://localhost:4566
}'

deploy_lambda \
    "turaf-reporting-service-local" \
    "$REPORTING_ZIP" \
    "src.lambda_handler.lambda_handler" \
    "arn:aws:iam::000000000000:role/lambda-reporting-role" \
    1024 \
    60 \
    "$REPORTING_ENV_VARS" || {
    echo "Failed to deploy reporting-service Lambda"
    exit 1
}

# Create event source mapping
create_event_source_mapping "turaf-reporting-service-local" "turaf-report-events"

echo "✓ Reporting Service deployed successfully"

# ==========================================
# Deploy Notification Service
# ==========================================
echo ""
echo "=========================================="
echo "Deploying Notification Service"
echo "=========================================="

NOTIFICATION_SERVICE_PATH="$PROJECT_ROOT/services/notification-service"
NOTIFICATION_ZIP="$BUILD_DIR/notification-service.zip"

# Package Lambda
package_lambda "notification-service" "$NOTIFICATION_SERVICE_PATH" "notification_handler" "$NOTIFICATION_ZIP" || {
    echo "Failed to package notification-service"
    exit 1
}

# Create IAM role
create_lambda_role "lambda-notification-role" "notification"

# Deploy Lambda function
NOTIFICATION_ENV_VARS='Variables={
    ENVIRONMENT=local,
    SES_FROM_EMAIL=notifications@turaf.com,
    EXPERIMENT_SERVICE_URL=http://host.docker.internal:8080,
    ORGANIZATION_SERVICE_URL=http://host.docker.internal:8080,
    FRONTEND_URL=http://localhost:4200,
    IDEMPOTENCY_TABLE_NAME=processed_notification_events,
    AWS_ENDPOINT_URL=http://localhost:4566,
    LOG_LEVEL=INFO
}'

deploy_lambda \
    "turaf-notification-service-local" \
    "$NOTIFICATION_ZIP" \
    "notification_handler.lambda_handler" \
    "arn:aws:iam::000000000000:role/lambda-notification-role" \
    512 \
    30 \
    "$NOTIFICATION_ENV_VARS" || {
    echo "Failed to deploy notification-service Lambda"
    exit 1
}

# Create event source mapping
create_event_source_mapping "turaf-notification-service-local" "turaf-notification-events"

echo "✓ Notification Service deployed successfully"

# ==========================================
# Verification
# ==========================================
echo ""
echo "=========================================="
echo "Verifying Deployment"
echo "=========================================="

echo "Listing deployed Lambda functions..."
aws --endpoint-url=$ENDPOINT lambda list-functions \
    --query 'Functions[?starts_with(FunctionName, `turaf-`)].{Name:FunctionName,Runtime:Runtime,Memory:MemorySize,Timeout:Timeout}' \
    --output table 2>/dev/null || echo "Could not list functions"

echo ""
echo "Listing event source mappings..."
aws --endpoint-url=$ENDPOINT lambda list-event-source-mappings \
    --query 'EventSourceMappings[].{Function:FunctionArn,Queue:EventSourceArn,State:State}' \
    --output table 2>/dev/null || echo "Could not list event source mappings (may not be supported)"

# ==========================================
# Summary
# ==========================================
echo ""
echo "=========================================="
echo "Lambda Deployment Complete!"
echo "=========================================="
echo ""
echo "Deployed Functions:"
echo "  - turaf-reporting-service-local (1024 MB, 60s timeout)"
echo "  - turaf-notification-service-local (512 MB, 30s timeout)"
echo ""
echo "Event Source Mappings:"
echo "  - turaf-report-events → turaf-reporting-service-local"
echo "  - turaf-notification-events → turaf-notification-service-local"
echo ""
echo "Test Commands:"
echo "  # List functions"
echo "  aws --endpoint-url=$ENDPOINT lambda list-functions"
echo ""
echo "  # Invoke reporting service manually"
echo "  aws --endpoint-url=$ENDPOINT lambda invoke \\"
echo "    --function-name turaf-reporting-service-local \\"
echo "    --payload '{\"Records\":[{\"body\":\"{}\"}]}' \\"
echo "    response.json"
echo ""
echo "  # View logs (if CloudWatch Logs is supported)"
echo "  aws --endpoint-url=$ENDPOINT logs tail /aws/lambda/turaf-reporting-service-local"
echo ""
echo "Note: Event source mappings may not be fully functional in MiniStack."
echo "You may need to invoke Lambda functions manually for testing."
echo "=========================================="
