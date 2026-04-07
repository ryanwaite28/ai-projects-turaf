# Task 004: Update IAM Permissions for GitHubActionsDeploymentRole

**Status**: ✅ Completed  
**Assignee**: AI Assistant  
**Estimated Time**: 2 hours  
**Actual Time**: < 1 hour  
**Completed**: 2026-03-31  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md)  
**Note**: IAM policy file already contained required permissions. Created verification script and documentation.

---

## Objective

Update the GitHubActionsDeploymentRole IAM permissions policy to include S3 and CloudFront permissions for architecture test reports.

---

## Prerequisites

- Access to AWS accounts (dev, qa, prod)
- Understanding of IAM policies
- AWS CLI configured

---

## Tasks

### 1. Update Permissions Policy JSON

Edit `infrastructure/iam-policies/github-actions-permissions-policy.json`:

Add two new statement blocks after the existing CloudWatchLogs statement:

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
},
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

### 2. Apply to DEV Account

```bash
# Get current policy
aws iam get-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --profile turaf-dev \
  --output json > current-policy-dev.json

# Update policy with new permissions
# Replace ${ACCOUNT_ID} with 801651112319
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://infrastructure/iam-policies/github-actions-permissions-policy.json \
  --profile turaf-dev
```

### 3. Apply to QA Account

```bash
# Replace ${ACCOUNT_ID} with 965932217544
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://infrastructure/iam-policies/github-actions-permissions-policy.json \
  --profile turaf-qa
```

### 4. Apply to PROD Account

```bash
# Replace ${ACCOUNT_ID} with 811783768245
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://infrastructure/iam-policies/github-actions-permissions-policy.json \
  --profile turaf-prod
```

### 5. Verify Permissions

Create test script `scripts/verify-iam-permissions.sh`:

```bash
#!/bin/bash

ROLE_NAME="GitHubActionsDeploymentRole"

for ENV in dev qa prod; do
  echo "Verifying permissions in $ENV..."
  
  aws iam get-role-policy \
    --role-name $ROLE_NAME \
    --policy-name GitHubActionsDeploymentPolicy \
    --profile turaf-$ENV \
    --query 'PolicyDocument.Statement[?Sid==`ArchitectureTestReports`]' \
    --output json
    
  aws iam get-role-policy \
    --role-name $ROLE_NAME \
    --policy-name GitHubActionsDeploymentPolicy \
    --profile turaf-$ENV \
    --query 'PolicyDocument.Statement[?Sid==`CloudFrontInvalidation`]' \
    --output json
done
```

### 6. Update Documentation

Update `infrastructure/github-oidc-roles.md`:

Add to the Permissions section:

```markdown
### S3 (Architecture Test Reports)
- `s3:PutObject` - Upload test reports
- `s3:PutObjectAcl` - Set object ACLs
- `s3:GetObject` - Download reports
- `s3:ListBucket` - List bucket contents
  - Resources: `turaf-architecture-test-reports-*` buckets

### CloudFront (Cache Invalidation)
- `cloudfront:CreateInvalidation` - Invalidate cache
- `cloudfront:GetInvalidation` - Check invalidation status
- `cloudfront:ListInvalidations` - List invalidations
  - Resources: All CloudFront distributions
```

---

## Acceptance Criteria

- [x] Permissions policy JSON file updated (already contained required permissions)
- [x] Verification script created (`scripts/verify-iam-permissions.sh`)
- [x] Documentation updated (`infrastructure/github-oidc-roles.md`)
- [x] Application guide created (`infrastructure/apply-iam-policy-updates.md`)
- [x] Policy applied to DEV account (801651112319)
- [x] Policy applied to QA account (965932217544)
- [x] Policy applied to PROD account (811783768245)
- [x] Permissions verified in all accounts - S3 and CloudFront permissions confirmed

---

## Verification

```bash
# Run verification script
bash scripts/verify-iam-permissions.sh

# Should show the new permission statements in all accounts
```

---

## Rollback Plan

If issues occur, restore previous policy:

```bash
aws iam put-role-policy \
  --role-name GitHubActionsDeploymentRole \
  --policy-name GitHubActionsDeploymentPolicy \
  --policy-document file://current-policy-dev.json \
  --profile turaf-dev
```

---

## Notes

- Keep backup of current policy before updating
- Test in DEV first before applying to QA/PROD
- CloudFront permissions use wildcard for distribution ARN
- S3 permissions use wildcard for bucket name pattern
