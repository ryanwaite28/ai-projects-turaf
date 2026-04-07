# Apply IAM Policy Updates for Architecture Test Reports

**Created**: 2026-03-31  
**Status**: Ready for Execution  
**Related Task**: [004-update-iam-permissions.md](../tasks/architecture-tests/004-update-iam-permissions.md)

---

## Overview

This document provides step-by-step instructions to apply the updated IAM permissions policy to all AWS accounts. The policy has been updated to include S3 and CloudFront permissions for architecture test reports.

**Policy File**: `infrastructure/iam-policies/github-actions-permissions-policy.json`

---

## Prerequisites

- AWS CLI installed and configured
- AWS profiles configured for all accounts:
  - `turaf-dev` (801651112319)
  - `turaf-qa` (965932217544)
  - `turaf-prod` (811783768245)
- Admin access to IAM in each account

---

## Policy Changes

The following permissions have been added:

### S3 Permissions (Architecture Test Reports)
```json
{
  "Sid": "ArchitectureTestReports",
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:PutObjectAcl",
    "s3:GetObject",
    "s3:ListBucket"
  ],
  "Resource": [
    "arn:aws:s3:::turaf-architecture-test-reports-*",
    "arn:aws:s3:::turaf-architecture-test-reports-*/*"
  ]
}
```

### CloudFront Permissions
```json
{
  "Sid": "CloudFrontInvalidation",
  "Effect": "Allow",
  "Action": [
    "cloudfront:CreateInvalidation",
    "cloudfront:GetInvalidation",
    "cloudfront:ListInvalidations"
  ],
  "Resource": "arn:aws:cloudfront::*:distribution/*"
}
```

---

## Step 1: Backup Current Policies

Before applying updates, backup the current policies from all accounts:

```bash
# DEV Account
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-dev \
  --output json > backup-policy-dev-$(date +%Y%m%d).json

# QA Account
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-qa \
  --output json > backup-policy-qa-$(date +%Y%m%d).json

# PROD Account
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-prod \
  --output json > backup-policy-prod-$(date +%Y%m%d).json
```

---

## Step 2: Prepare Policy Document

The policy document uses a placeholder `${ACCOUNT_ID}` that needs to be replaced for each account.

### Option A: Manual Replacement

Create account-specific policy files:

```bash
# DEV
sed 's/${ACCOUNT_ID}/801651112319/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json \
  > /tmp/policy-dev.json

# QA
sed 's/${ACCOUNT_ID}/965932217544/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json \
  > /tmp/policy-qa.json

# PROD
sed 's/${ACCOUNT_ID}/811783768245/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json \
  > /tmp/policy-prod.json
```

### Option B: Inline Replacement

Use inline replacement during the `put-role-policy` command (shown in Step 3).

---

## Step 3: Apply Policy to DEV Account

**Test in DEV first before applying to QA/PROD**

```bash
# Replace ${ACCOUNT_ID} and apply
sed 's/${ACCOUNT_ID}/801651112319/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json | \
  aws iam put-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-name GitHubActionsDeploymentPolicy \
    --policy-document file:///dev/stdin \
    --profile turaf-dev

echo "✅ Policy applied to DEV account"
```

---

## Step 4: Verify DEV Policy

```bash
# Verify ArchitectureTestReports permissions
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-dev \
  --query 'PolicyDocument.Statement[?Sid==`ArchitectureTestReports`]' \
  --output json

# Verify CloudFrontInvalidation permissions
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-dev \
  --query 'PolicyDocument.Statement[?Sid==`CloudFrontInvalidation`]' \
  --output json
```

**Expected Output**: Should show the new S3 and CloudFront permission statements.

---

## Step 5: Apply Policy to QA Account

After verifying DEV works correctly:

```bash
sed 's/${ACCOUNT_ID}/965932217544/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json | \
  aws iam put-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-name GitHubActionsDeploymentPolicy \
    --policy-document file:///dev/stdin \
    --profile turaf-qa

echo "✅ Policy applied to QA account"
```

---

## Step 6: Verify QA Policy

```bash
bash scripts/verify-iam-permissions.sh
```

Or manually:

```bash
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-qa \
  --query 'PolicyDocument.Statement[?Sid==`ArchitectureTestReports` || Sid==`CloudFrontInvalidation`]' \
  --output json
```

---

## Step 7: Apply Policy to PROD Account

After verifying QA works correctly:

```bash
sed 's/${ACCOUNT_ID}/811783768245/g' \
  infrastructure/iam-policies/github-actions-permissions-policy.json | \
  aws iam put-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-name GitHubActionsDeploymentPolicy \
    --policy-document file:///dev/stdin \
    --profile turaf-prod

echo "✅ Policy applied to PROD account"
```

---

## Step 8: Verify All Accounts

Run the verification script to check all accounts:

```bash
bash scripts/verify-iam-permissions.sh
```

---

## Rollback Procedure

If issues occur, restore the backup policy:

```bash
# Extract policy document from backup
jq -r '.PolicyDocument | tostring' backup-policy-dev-YYYYMMDD.json > /tmp/rollback-policy.json

# Apply backup policy
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file:///tmp/rollback-policy.json \
  --profile turaf-dev
```

Repeat for QA and PROD if needed.

---

## Testing

After applying the policy, test the permissions:

### Test S3 Access

```bash
# Assume the role (requires GitHub Actions or manual STS assume-role)
# Then test S3 operations:

aws s3 ls s3://turaf-architecture-test-reports-dev/ --profile turaf-dev

# Upload test file
echo "test" > /tmp/test-report.txt
aws s3 cp /tmp/test-report.txt \
  s3://turaf-architecture-test-reports-dev/test-report.txt \
  --profile turaf-dev

# Download test file
aws s3 cp \
  s3://turaf-architecture-test-reports-dev/test-report.txt \
  /tmp/downloaded-report.txt \
  --profile turaf-dev
```

### Test CloudFront Access

```bash
# List CloudFront distributions
aws cloudfront list-distributions --profile turaf-dev

# Create invalidation (requires distribution ID)
aws cloudfront create-invalidation \
  --distribution-id E1234567890ABC \
  --paths "/*" \
  --profile turaf-dev
```

---

## Completion Checklist

- [ ] Backed up current policies from all accounts
- [ ] Applied policy to DEV account
- [ ] Verified DEV policy
- [ ] Tested S3 access in DEV
- [ ] Applied policy to QA account
- [ ] Verified QA policy
- [ ] Applied policy to PROD account
- [ ] Verified PROD policy
- [ ] Ran verification script for all accounts
- [ ] Tested permissions in at least one account
- [ ] Documented completion date in task file

---

## Notes

- The policy uses wildcard patterns for S3 bucket names (`turaf-architecture-test-reports-*`)
- CloudFront permissions use wildcard for all distributions
- No changes to trust policy are needed
- Existing permissions remain unchanged
- Policy changes take effect immediately

---

## References

- [IAM Permissions Policy](iam-policies/github-actions-permissions-policy.json)
- [GitHub OIDC Roles Documentation](github-oidc-roles.md)
- [Task 004: Update IAM Permissions](../tasks/architecture-tests/004-update-iam-permissions.md)
- [Verification Script](../scripts/verify-iam-permissions.sh)
