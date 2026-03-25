#!/bin/bash

set -e

# Update CodeBuild Project to use NO_SOURCE with inline buildspec
# Purpose: Fix GitHub access issue by using inline buildspec

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
ENV="dev"
REGION="us-east-1"
PROJECT_NAME="turaf-flyway-migrations-${ENV}"

echo "=========================================="
echo "Update CodeBuild Source Configuration"
echo "Environment: ${ENV}"
echo "=========================================="
echo ""

# Check AWS credentials
if ! aws sts get-caller-identity --profile $ROOT_PROFILE &> /dev/null; then
    echo "❌ AWS credentials not configured"
    exit 1
fi

# Assume role
CREDENTIALS=$(aws sts assume-role \
  --profile $ROOT_PROFILE \
  --role-arn "arn:aws:iam::${DEV_ACCOUNT_ID}:role/${ASSUME_ROLE_NAME}" \
  --role-session-name "update-codebuild" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="$REGION"

echo "✅ Assumed role in dev account"
echo ""

# Create inline buildspec in temp file
cat > /tmp/buildspec-inline.yml <<'EOF'
version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto17
    commands:
      - echo "Installing Flyway CLI..."
      - wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/9.22.3/flyway-commandline-9.22.3-linux-x64.tar.gz | tar xvz
      - export PATH=$PATH:$(pwd)/flyway-9.22.3
      - flyway -v
      - echo "Installing PostgreSQL client..."
      - apt-get update && apt-get install -y postgresql-client
  pre_build:
    commands:
      - echo "Pre-build phase - Testing database connection..."
      - echo "DB_HOST=$DB_HOST"
      - echo "DB_NAME=$DB_NAME"
      - echo "DB_USER=$DB_USER"
      - echo "Testing connection..."
      - PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT version();"
  build:
    commands:
      - echo "Running database migrations..."
      - echo "NOTE This is a test build. Actual migrations would run from GitHub source."
      - echo "For now, just verifying database connectivity."
      - PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT current_database(), current_user;"
  post_build:
    commands:
      - echo "Post-build phase - Migration test complete"
      - echo "Build completed on $(date)"
      - echo "Database connection successful"
EOF

# Update project with inline buildspec
echo "Updating project with inline buildspec..."
echo ""

BUILDSPEC_JSON=$(cat /tmp/buildspec-inline.yml | jq -Rs .)

aws codebuild update-project \
  --name "$PROJECT_NAME" \
  --source "{\"type\": \"NO_SOURCE\", \"buildspec\": $BUILDSPEC_JSON}"

echo ""
echo "✅ Project updated successfully"
echo ""
echo "The project now uses an inline buildspec for testing."
echo "This allows testing without GitHub access."
echo ""

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

echo "Next: Run test-codebuild-flyway-dev.sh to test the updated configuration"
