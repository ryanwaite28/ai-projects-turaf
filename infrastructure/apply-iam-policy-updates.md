# Apply IAM Policy Updates

**Created**: 2026-03-31  
**Last Updated**: 2026-04-09  
**Status**: Ready for Execution  
**Related Task**: [004-update-iam-permissions.md](../tasks/architecture-tests/004-update-iam-permissions.md)

---

## Overview

The `GitHubActionsDeploymentRole` uses **two separate inline policies** — split for size (AWS inline policy limit: 10,240 non-whitespace characters) and concern separation:

| Policy Name | File | Purpose | Size |
|-------------|------|---------|------|
| `GitHubActionsDeploymentPolicy` | `github-actions-permissions-policy.json` | ECR, ECS, S3 artifacts, CloudWatch, CloudFront invalidation, Lambda deployment | ~2,100 chars |
| `GitHubActionsTerraformPolicy` | `github-actions-terraform-policy.json` | Full Terraform provisioning (KMS, EC2 VPC, RDS, IAM roles, SQS, EventBridge, DynamoDB, ELB, Lambda, S3 buckets) | ~7,400 chars |

Apply both policies to all environments.

---

## Policy History

### 2026-04-09 — Terraform Infrastructure Permissions (CURRENT)

Added comprehensive Terraform provisioning permissions after CI/CD failures:

- **Problem**: `GitHubActionsDeploymentRole` was provisioned only for deployment (ECR push, ECS update). Running Terraform for full infrastructure management required additional permissions.
- **Errors fixed**:
  - `KMSKeyNotAccessibleFault` on RDS — added full `kms:*` management permissions including `kms:CreateGrant` (required when RDS uses a CMK)
  - `AccessDeniedException: Access to KMS is not allowed` on Secrets Manager — same KMS fix
  - `UnauthorizedOperation: ec2:ModifyVpcEndpoint` — added EC2 VPC management permissions
- **Statements added**: `KMSTerraformManagement`, `SecretsManagerTerraformManagement`, `EC2TerraformManagement`, `RDSTerraformManagement`, `ElastiCacheTerraformManagement`, `DocumentDBTerraformManagement`, `S3TerraformManagement`, `SQSTerraformManagement`, `EventBridgeTerraformManagement`, `DynamoDBTerraformManagement`, `ELBTerraformManagement`, `LambdaTerraformManagement`, `CloudWatchTerraformManagement`

### 2026-03-31 — Architecture Test Reports + CloudFront

Added S3 (`ArchitectureTestReports`) and CloudFront (`CloudFrontInvalidation`) permissions.

---

## Prerequisites

- AWS CLI installed and configured
- AWS profiles configured for all accounts:
  - `turaf-dev` (801651112319)
  - `turaf-qa` (965932217544)
  - `turaf-prod` (811783768245)
- Admin access to IAM in each account

---

## Policy Changes (Current)

The policy now includes full Terraform infrastructure provisioning permissions. Key additions:

### KMS (required for RDS encryption + Secrets Manager CMK)
- Full key lifecycle management: `kms:CreateKey`, `kms:CreateAlias`, `kms:ScheduleKeyDeletion`, etc.
- **Critical**: `kms:CreateGrant` — required when Terraform creates an RDS instance or Secrets Manager secret with a CMK; Terraform must grant the AWS service access to the key

### EC2 VPC Management (required for networking module)
- VPC, subnet, route table, internet gateway, NAT gateway lifecycle
- **Critical**: `ec2:ModifyVpcEndpoint` — required when associating route tables with existing VPC endpoints

### Secrets Manager
- `secretsmanager:CreateSecret`, `PutSecretValue`, `DescribeSecret`, `TagResource`
- Resources scoped to `arn:aws:secretsmanager:*:${ACCOUNT_ID}:secret:turaf/*`

### Broad Terraform permissions
- RDS, ElastiCache, DocumentDB, S3, SQS, EventBridge, DynamoDB, ELB, Lambda, CloudWatch, ECS (full), ECR (full), IAM (turaf-* roles)

---

## Applying Policies

All policies are applied by a single script:

```bash
# Preview what will be applied
./scripts/apply-iam-policies.sh --list

# Apply to all accounts (dev, qa, prod)
./scripts/apply-iam-policies.sh --all

# Apply to one account only
./scripts/apply-iam-policies.sh --env dev

# Verify all policies are present (no changes)
./scripts/apply-iam-policies.sh --verify --all
```

The script substitutes `${ACCOUNT_ID}` in each policy file with the actual account ID at runtime. On first run it automatically migrates the old teardown managed policies (`TurafTeardownPolicy`, `TurafTeardownPolicy2`) to inline policies.

---

## Backup Before Updating

```bash
for env in dev qa prod; do
  for policy in GitHubActionsDeploymentPolicy GitHubActionsTerraformPolicy \
                TurafTeardownPolicy TurafTeardownPolicy2; do
    aws iam get-role-policy \
      --role-name GitHubActionsDeploymentRole \
      --policy-name "$policy" \
      --profile "turaf-${env}" \
      --output json > "backup-${policy}-${env}-$(date +%Y%m%d).json" 2>/dev/null || true
  done
done
```

---

## Rollback

Restore a backed-up policy:

```bash
jq -r '.PolicyDocument | tostring' backup-GitHubActionsDeploymentPolicy-dev-YYYYMMDD.json | \
  aws iam put-role-policy \
    --role-name GitHubActionsDeploymentRole \
    --policy-name GitHubActionsDeploymentPolicy \
    --policy-document file:///dev/stdin \
    --profile turaf-dev
```

Repeat for each policy and environment as needed.

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
