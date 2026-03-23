# Task: Create Service Control Policies

**Service**: Infrastructure  
**Type**: AWS Organization Security  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 015-create-organizational-units

---

## Objective

Create and attach Service Control Policies (SCPs) to the AWS Organization to enforce security guardrails across all member accounts.

---

## Acceptance Criteria

- [ ] 5 SCPs created in root account
- [ ] SCPs attached to appropriate OUs and accounts
- [ ] SCP effectiveness verified
- [ ] SCP policies documented

---

## Implementation

### 1. Deny CloudTrail Deletion SCP

**Purpose**: Prevent deletion or modification of CloudTrail logs

**Policy Document** (`scps/deny-cloudtrail-deletion.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyCloudTrailDeletion",
      "Effect": "Deny",
      "Action": [
        "cloudtrail:DeleteTrail",
        "cloudtrail:StopLogging",
        "cloudtrail:UpdateTrail"
      ],
      "Resource": "*"
    }
  ]
}
```

**Create and Attach**:

```bash
# Create SCP
aws organizations create-policy \
  --name "DenyCloudTrailDeletion" \
  --description "Prevent deletion or modification of CloudTrail logs" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scps/deny-cloudtrail-deletion.json

# Save policy ID
CLOUDTRAIL_SCP_ID="<POLICY_ID_FROM_OUTPUT>"

# Attach to Workloads OU
aws organizations attach-policy \
  --policy-id $CLOUDTRAIL_SCP_ID \
  --target-id <WORKLOADS_OU_ID>
```

### 2. Require Encryption SCP

**Purpose**: Enforce encryption at rest for all storage services

**Policy Document** (`scps/require-encryption.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnencryptedS3",
      "Effect": "Deny",
      "Action": "s3:PutObject",
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": ["AES256", "aws:kms"]
        }
      }
    },
    {
      "Sid": "DenyUnencryptedEBS",
      "Effect": "Deny",
      "Action": "ec2:RunInstances",
      "Resource": "arn:aws:ec2:*:*:volume/*",
      "Condition": {
        "Bool": {
          "ec2:Encrypted": "false"
        }
      }
    },
    {
      "Sid": "DenyUnencryptedRDS",
      "Effect": "Deny",
      "Action": "rds:CreateDBInstance",
      "Resource": "*",
      "Condition": {
        "Bool": {
          "rds:StorageEncrypted": "false"
        }
      }
    }
  ]
}
```

**Create and Attach**:

```bash
# Create SCP
aws organizations create-policy \
  --name "RequireEncryption" \
  --description "Enforce encryption at rest for all storage" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scps/require-encryption.json

# Save policy ID
ENCRYPTION_SCP_ID="<POLICY_ID_FROM_OUTPUT>"

# Attach to Workloads OU
aws organizations attach-policy \
  --policy-id $ENCRYPTION_SCP_ID \
  --target-id <WORKLOADS_OU_ID>
```

### 3. Restrict Regions SCP

**Purpose**: Limit resource creation to us-east-1 region

**Policy Document** (`scps/restrict-regions.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyAllOutsideUSEast1",
      "Effect": "Deny",
      "NotAction": [
        "iam:*",
        "organizations:*",
        "route53:*",
        "budgets:*",
        "waf:*",
        "cloudfront:*",
        "globalaccelerator:*",
        "importexport:*",
        "support:*",
        "sts:*"
      ],
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "aws:RequestedRegion": "us-east-1"
        }
      }
    }
  ]
}
```

**Create and Attach**:

```bash
# Create SCP
aws organizations create-policy \
  --name "RestrictRegions" \
  --description "Restrict resource creation to us-east-1" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scps/restrict-regions.json

# Save policy ID
REGIONS_SCP_ID="<POLICY_ID_FROM_OUTPUT>"

# Attach to Workloads OU
aws organizations attach-policy \
  --policy-id $REGIONS_SCP_ID \
  --target-id <WORKLOADS_OU_ID>
```

### 4. Require MFA SCP

**Purpose**: Require MFA for sensitive operations

**Policy Document** (`scps/require-mfa.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyAllWithoutMFA",
      "Effect": "Deny",
      "Action": [
        "ec2:StopInstances",
        "ec2:TerminateInstances",
        "rds:DeleteDBInstance",
        "s3:DeleteBucket",
        "iam:DeleteUser",
        "iam:DeleteRole"
      ],
      "Resource": "*",
      "Condition": {
        "BoolIfExists": {
          "aws:MultiFactorAuthPresent": "false"
        }
      }
    }
  ]
}
```

**Create and Attach**:

```bash
# Create SCP
aws organizations create-policy \
  --name "RequireMFA" \
  --description "Require MFA for sensitive operations" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scps/require-mfa.json

# Save policy ID
MFA_SCP_ID="<POLICY_ID_FROM_OUTPUT>"

# Attach to Workloads OU
aws organizations attach-policy \
  --policy-id $MFA_SCP_ID \
  --target-id <WORKLOADS_OU_ID>
```

### 5. Block Public S3 Buckets SCP

**Purpose**: Prevent public S3 bucket access by default

**Policy Document** (`scps/block-public-s3.json`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyPublicS3Buckets",
      "Effect": "Deny",
      "Action": [
        "s3:PutBucketPublicAccessBlock",
        "s3:PutAccountPublicAccessBlock"
      ],
      "Resource": "*",
      "Condition": {
        "Bool": {
          "s3:BlockPublicAcls": "false",
          "s3:BlockPublicPolicy": "false",
          "s3:IgnorePublicAcls": "false",
          "s3:RestrictPublicBuckets": "false"
        }
      }
    },
    {
      "Sid": "DenyPublicACLs",
      "Effect": "Deny",
      "Action": [
        "s3:PutObjectAcl",
        "s3:PutBucketAcl"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "s3:x-amz-acl": [
            "public-read",
            "public-read-write",
            "authenticated-read"
          ]
        }
      }
    }
  ]
}
```

**Create and Attach**:

```bash
# Create SCP
aws organizations create-policy \
  --name "BlockPublicS3" \
  --description "Block public S3 bucket access" \
  --type SERVICE_CONTROL_POLICY \
  --content file://scps/block-public-s3.json

# Save policy ID
S3_SCP_ID="<POLICY_ID_FROM_OUTPUT>"

# Attach to Workloads OU
aws organizations attach-policy \
  --policy-id $S3_SCP_ID \
  --target-id <WORKLOADS_OU_ID>
```

---

## Verification

### 1. List All SCPs

```bash
# List all policies
aws organizations list-policies \
  --filter SERVICE_CONTROL_POLICY

# Expected: 5 SCPs + FullAWSAccess (default)
```

### 2. Verify SCP Attachments

```bash
# List policies attached to Workloads OU
aws organizations list-policies-for-target \
  --target-id <WORKLOADS_OU_ID> \
  --filter SERVICE_CONTROL_POLICY

# Expected: 5 custom SCPs + FullAWSAccess
```

### 3. Test SCP Effectiveness

**Test CloudTrail Protection** (in dev account):
```bash
# This should FAIL due to SCP
aws cloudtrail delete-trail --name test-trail --region us-east-1

# Expected error: "Access Denied"
```

**Test Region Restriction** (in dev account):
```bash
# This should FAIL due to SCP
aws ec2 describe-instances --region us-west-2

# Expected error: "Access Denied"
```

**Test Encryption Requirement** (in dev account):
```bash
# This should FAIL due to SCP
aws s3api put-object \
  --bucket test-bucket \
  --key test-file.txt \
  --body test.txt
  # (without --server-side-encryption)

# Expected error: "Access Denied"
```

---

## SCP Inheritance Model

```
Root Account (072456928432)
└── Workloads OU
    ├── Ops Account (146072879609)
    ├── Dev Account (801651112319)
    ├── QA Account (965932217544)
    └── Prod Account (811783768245)

SCPs Applied:
- FullAWSAccess (default, allows all)
- DenyCloudTrailDeletion
- RequireEncryption
- RestrictRegions
- RequireMFA
- BlockPublicS3

Effective Permissions = FullAWSAccess ∩ (All Deny SCPs)
```

---

## Troubleshooting

### Issue: "AccessDeniedException" when creating SCP

**Cause**: Insufficient permissions in root account

**Solution**:
```bash
# Verify you're using root account credentials
aws sts get-caller-identity

# Ensure you have organizations:CreatePolicy permission
```

### Issue: SCP not taking effect

**Cause**: SCP propagation delay or incorrect attachment

**Solution**:
- Wait 5-10 minutes for SCP propagation
- Verify SCP is attached to correct OU/account
- Check SCP syntax is valid JSON

### Issue: Legitimate operations blocked by SCP

**Cause**: SCP too restrictive

**Solution**:
- Review SCP conditions
- Add exceptions for specific use cases
- Update SCP policy document
- Detach and reattach updated SCP

---

## Documentation

### Save SCP Configuration

Create `infrastructure/scps/README.md`:

```markdown
# Service Control Policies

## Active SCPs

| SCP Name | Policy ID | Attached To | Purpose |
|----------|-----------|-------------|---------|
| DenyCloudTrailDeletion | p-xxxxx | Workloads OU | Protect audit logs |
| RequireEncryption | p-xxxxx | Workloads OU | Enforce encryption |
| RestrictRegions | p-xxxxx | Workloads OU | Limit to us-east-1 |
| RequireMFA | p-xxxxx | Workloads OU | Require MFA for sensitive ops |
| BlockPublicS3 | p-xxxxx | Workloads OU | Prevent public buckets |

## Testing

Last tested: 2024-01-01  
Status: ✅ All SCPs active and enforced
```

---

## Checklist

- [ ] Created scps/ directory
- [ ] Created deny-cloudtrail-deletion.json
- [ ] Created require-encryption.json
- [ ] Created restrict-regions.json
- [ ] Created require-mfa.json
- [ ] Created block-public-s3.json
- [ ] Created all 5 SCPs in AWS Organizations
- [ ] Attached all SCPs to Workloads OU
- [ ] Verified SCP attachments
- [ ] Tested SCP effectiveness
- [ ] Documented SCP configuration

---

## Next Steps

After SCPs are configured:
1. Proceed to task 021: Setup Terraform State Backend
2. Verify SCPs don't block Terraform operations
3. Monitor CloudTrail for SCP denials

---

## References

- [AWS SCPs Documentation](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html)
- [SCP Examples](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps_examples.html)
- specs/aws-organization-setup.md (Section: Service Control Policies)
- INFRASTRUCTURE_PLAN.md (Phase 2.1)
