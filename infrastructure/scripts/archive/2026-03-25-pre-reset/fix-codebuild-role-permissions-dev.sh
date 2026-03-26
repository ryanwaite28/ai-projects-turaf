#!/bin/bash

set -e

# Fix CodeBuildFlywayRole Secrets Manager Permissions
# Purpose: Add missing Secrets Manager permissions to CodeBuildFlywayRole

ROOT_PROFILE="turaf-root"
DEV_ACCOUNT_ID="801651112319"
ASSUME_ROLE_NAME="OrganizationAccountAccessRole"
REGION="us-east-1"
ROLE_NAME="CodeBuildFlywayRole"

echo "=========================================="
echo "Fix CodeBuildFlywayRole Permissions"
echo "Environment: dev"
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
  --role-session-name "fix-codebuild-role" \
  --query 'Credentials' \
  --output json)

export AWS_ACCESS_KEY_ID=$(echo $CREDENTIALS | jq -r '.AccessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo $CREDENTIALS | jq -r '.SecretAccessKey')
export AWS_SESSION_TOKEN=$(echo $CREDENTIALS | jq -r '.SessionToken')
export AWS_DEFAULT_REGION="$REGION"

echo "✅ Assumed role in dev account"
echo ""

# Get current inline policies
echo "Checking current inline policies..."
POLICIES=$(aws iam list-role-policies --role-name $ROLE_NAME --query 'PolicyNames' --output json)
echo "Current policies: $POLICIES"
echo ""

# Check if CodeBuildFlywayPolicy exists
POLICY_EXISTS=$(echo $POLICIES | jq -r '.[] | select(. == "CodeBuildFlywayPolicy")')

if [ -z "$POLICY_EXISTS" ]; then
    echo "⚠️  CodeBuildFlywayPolicy not found, will create new policy"
else
    echo "✅ CodeBuildFlywayPolicy exists, will update it"
fi

echo ""

# Create updated policy with Secrets Manager permissions
cat > /tmp/codebuild-flyway-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SecretsManagerAccess",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/*"
      ]
    },
    {
      "Sid": "VPCAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeDhcpOptions",
        "ec2:DescribeVpcs",
        "ec2:CreateNetworkInterfacePermission"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CloudWatchLogsAccess",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": [
        "arn:aws:logs:us-east-1:801651112319:log-group:/aws/codebuild/turaf-flyway-migrations-*"
      ]
    },
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    },
    {
      "Sid": "S3Access",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": [
        "arn:aws:s3:::codepipeline-us-east-1-*"
      ]
    }
  ]
}
EOF

echo "Updating CodeBuildFlywayPolicy..."
aws iam put-role-policy \
  --role-name $ROLE_NAME \
  --policy-name CodeBuildFlywayPolicy \
  --policy-document file:///tmp/codebuild-flyway-policy.json

echo ""
echo "✅ Policy updated successfully"
echo ""

# Verify the policy
echo "Verifying updated policy..."
POLICY_DOC=$(aws iam get-role-policy \
  --role-name $ROLE_NAME \
  --policy-name CodeBuildFlywayPolicy \
  --query 'PolicyDocument' \
  --output json)

echo "Updated policy document:"
echo "$POLICY_DOC" | jq '.'
echo ""

# Check Secrets Manager permissions specifically
SECRETS_PERMS=$(echo "$POLICY_DOC" | jq -r '.Statement[] | select(.Sid == "SecretsManagerAccess") | .Action[]')
echo "Secrets Manager permissions:"
echo "$SECRETS_PERMS"
echo ""

# Cleanup
rm -f /tmp/codebuild-flyway-policy.json

# Clear credentials
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

echo "=========================================="
echo "✅ Permissions Fixed"
echo "=========================================="
echo ""
echo "Next: Run test-codebuild-flyway-dev.sh to verify the fix"
