#!/bin/bash

# This script should be run from the host machine, not inside the container
# It deploys Lambda functions to MiniStack running in Docker

echo "=========================================="
echo "Deploying Lambda Services to MiniStack"
echo "=========================================="

# Set AWS CLI to use MiniStack endpoint
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
ENDPOINT="http://localhost:4566"

# Check if MiniStack is running
echo "Checking MiniStack availability..."
if ! curl -s -f "$ENDPOINT/_localstack/health" > /dev/null 2>&1; then
    echo "Error: MiniStack is not running at $ENDPOINT"
    echo "Please start MiniStack first: docker-compose up -d ministack"
    exit 1
fi
echo "✓ MiniStack is running"
echo ""

# Run the init-lambda-services.sh script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$SCRIPT_DIR/init-lambda-services.sh" ]; then
    echo "Running Lambda deployment script..."
    bash "$SCRIPT_DIR/init-lambda-services.sh"
else
    echo "Error: init-lambda-services.sh not found in $SCRIPT_DIR"
    exit 1
fi
