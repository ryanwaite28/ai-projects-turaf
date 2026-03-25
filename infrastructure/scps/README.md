# Service Control Policies (SCPs)

**Organization**: Turaf (o-l3zk5a91yj)  
**Root Account**: 072456928432  
**Configured**: 2024-03-23  
**Status**: ✅ Active and Enforced

---

## Active SCPs Attached to Workloads OU

AWS Organizations allows a maximum of **5 SCPs per target** (including the default FullAWSAccess policy).

| SCP Name | Policy ID | Attached To | Status | Purpose |
|----------|-----------|-------------|--------|---------|
| FullAWSAccess | p-FullAWSAccess | Workloads OU | ✅ Active | AWS default - allows all operations |
| DenyCloudTrailDeletion | p-5teci82m | Workloads OU | ✅ Active | Protect audit logs from deletion |
| RequireEncryption | p-arhnq0fv | Workloads OU | ✅ Active | Enforce encryption at rest |
| RestrictRegions | p-gbqomqvb | Workloads OU | ✅ Active | Limit resources to us-east-1 |
| RequireMFA | p-itwumsee | Workloads OU | ✅ Active | Require MFA for sensitive operations |

**Total Attached**: 5/5 (at AWS limit)

---

## Created but Not Attached

Due to the 5 SCP limit per target, the following policy was created but not attached:

| SCP Name | Policy ID | Status | Purpose |
|----------|-----------|--------|---------|
| BlockPublicS3 | p-tubel6y0 | ⚠️ Created, Not Attached | Block public S3 bucket access |

**Note**: S3 public access blocking can be enforced at the account level using S3 Block Public Access settings instead.

---

## SCP Inheritance Model

```
Root Account (072456928432)
└── Workloads OU (ou-gs6r-6qpsgd9n)
    ├── Ops Account (146072879609)
    ├── Dev Account (801651112319)
    ├── QA Account (965932217544)
    └── Prod Account (811783768245)

SCPs Applied to Workloads OU:
✅ FullAWSAccess (default, allows all)
✅ DenyCloudTrailDeletion
✅ RequireEncryption
✅ RestrictRegions
✅ RequireMFA

Effective Permissions = FullAWSAccess ∩ (All Deny SCPs)
```

All member accounts (Ops, Dev, QA, Prod) inherit these SCPs from the Workloads OU.

---

## SCP Details

### 1. DenyCloudTrailDeletion (p-5teci82m)

**Purpose**: Prevent deletion or modification of CloudTrail logs to maintain audit trail integrity.

**Denied Actions**:
- `cloudtrail:DeleteTrail`
- `cloudtrail:StopLogging`
- `cloudtrail:UpdateTrail`

**Impact**: CloudTrail trails cannot be deleted, stopped, or modified in any member account.

---

### 2. RequireEncryption (p-arhnq0fv)

**Purpose**: Enforce encryption at rest for all storage services.

**Denied Actions**:
- S3: `PutObject` without server-side encryption (AES256 or KMS)
- EC2: `RunInstances` with unencrypted EBS volumes
- RDS: `CreateDBInstance` without storage encryption

**Impact**: All S3 objects, EBS volumes, and RDS instances must be encrypted.

---

### 3. RestrictRegions (p-gbqomqvb)

**Purpose**: Limit resource creation to us-east-1 region only.

**Allowed Global Services** (exempt from region restriction):
- IAM
- AWS Organizations
- Route 53
- Budgets
- WAF
- CloudFront
- Global Accelerator
- Support
- STS

**Impact**: Resources can only be created in us-east-1, except for global services.

---

### 4. RequireMFA (p-itwumsee)

**Purpose**: Require Multi-Factor Authentication for sensitive destructive operations.

**MFA Required For**:
- `ec2:StopInstances`
- `ec2:TerminateInstances`
- `rds:DeleteDBInstance`
- `s3:DeleteBucket`
- `iam:DeleteUser`
- `iam:DeleteRole`

**Impact**: Users must authenticate with MFA before performing destructive operations.

---

### 5. BlockPublicS3 (p-tubel6y0) - NOT ATTACHED

**Purpose**: Prevent public S3 bucket access by default.

**Denied Actions**:
- Disabling S3 Block Public Access settings
- Setting public ACLs on buckets or objects

**Alternative**: Enable S3 Block Public Access at the account level in each member account.

---

## Policy Files

All SCP policy documents are stored in `infrastructure/scps/`:

- `deny-cloudtrail-deletion.json`
- `require-encryption.json`
- `restrict-regions.json`
- `require-mfa.json`
- `block-public-s3.json`

---

## Testing

**Last Tested**: 2024-03-23  
**Status**: ✅ SCPs enabled and attached

### Verification Commands

```bash
# List all SCPs
aws organizations list-policies \
  --filter SERVICE_CONTROL_POLICY \
  --profile turaf-root

# List SCPs attached to Workloads OU
aws organizations list-policies-for-target \
  --target-id ou-gs6r-6qpsgd9n \
  --filter SERVICE_CONTROL_POLICY \
  --profile turaf-root

# Check if SCP policy type is enabled
aws organizations describe-organization \
  --profile turaf-root \
  --query 'Organization.AvailablePolicyTypes'
```

### Test SCP Effectiveness (in member accounts)

**Test CloudTrail Protection**:
```bash
# Should FAIL with Access Denied
aws cloudtrail delete-trail --name test-trail --region us-east-1
```

**Test Region Restriction**:
```bash
# Should FAIL with Access Denied
aws ec2 describe-instances --region us-west-2
```

**Test Encryption Requirement**:
```bash
# Should FAIL with Access Denied (no encryption specified)
aws s3api put-object \
  --bucket test-bucket \
  --key test-file.txt \
  --body test.txt
```

---

## AWS Limits

- **Maximum SCPs per target**: 5 (including FullAWSAccess)
- **Maximum SCP size**: 5,120 bytes
- **Maximum SCPs in organization**: 1,000

**Current Usage**:
- SCPs attached to Workloads OU: 5/5 (at limit)
- Total SCPs in organization: 6/1,000

---

## Managing SCPs

### Attach Additional SCP (if space available)

```bash
aws organizations attach-policy \
  --policy-id <POLICY_ID> \
  --target-id ou-gs6r-6qpsgd9n \
  --profile turaf-root
```

### Detach SCP

```bash
aws organizations detach-policy \
  --policy-id <POLICY_ID> \
  --target-id ou-gs6r-6qpsgd9n \
  --profile turaf-root
```

### Update SCP Content

```bash
aws organizations update-policy \
  --policy-id <POLICY_ID> \
  --content file://infrastructure/scps/<policy-file>.json \
  --profile turaf-root
```

### Delete SCP (must detach first)

```bash
# Detach from all targets first
aws organizations detach-policy \
  --policy-id <POLICY_ID> \
  --target-id ou-gs6r-6qpsgd9n \
  --profile turaf-root

# Then delete
aws organizations delete-policy \
  --policy-id <POLICY_ID> \
  --profile turaf-root
```

---

## Recommendations

### Option 1: Keep Current Configuration
- Accept the 5 SCP limit
- Implement S3 Block Public Access at the account level
- Monitor and adjust SCPs as needed

### Option 2: Consolidate SCPs
- Combine multiple policies into fewer SCPs
- Example: Merge RequireEncryption and BlockPublicS3 into one policy
- This would free up space for additional policies

### Option 3: Use Account-Level Controls
- Implement some controls (like S3 public access blocking) at the account level
- Reserve SCPs for organization-wide critical controls

**Current Approach**: Option 1 - Keep current configuration and use account-level S3 Block Public Access.

---

## Security Benefits

✅ **Audit Trail Protection**: CloudTrail logs cannot be deleted or modified  
✅ **Data Encryption**: All storage must be encrypted at rest  
✅ **Geographic Compliance**: Resources restricted to us-east-1  
✅ **MFA Enforcement**: Destructive operations require MFA  
✅ **Centralized Governance**: Policies enforced across all member accounts  

---

## Monitoring

### CloudTrail Events to Monitor

- `DenyCloudTrailDeletion` denials: Monitor for attempts to delete CloudTrail
- `RequireEncryption` denials: Track unencrypted resource creation attempts
- `RestrictRegions` denials: Identify attempts to create resources outside us-east-1
- `RequireMFA` denials: Monitor destructive operations without MFA

### CloudWatch Alarms (Optional)

Set up CloudWatch alarms for SCP denial events to detect policy violations.

---

## References

- [AWS SCPs Documentation](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html)
- [SCP Examples](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps_examples.html)
- [SCP Syntax](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps_syntax.html)
- specs/aws-organization-setup.md (Section: Service Control Policies)
- INFRASTRUCTURE_PLAN.md (Phase 2.1)
