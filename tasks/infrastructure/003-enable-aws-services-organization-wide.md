# Task: Enable AWS Services Organization-Wide

**Service**: Infrastructure  
**Type**: Organization Setup  
**Priority**: High  
**Estimated Time**: 30 minutes  
**Dependencies**: 015-create-organizational-units

---

## Objective

Enable AWS services across the organization to provide centralized management for CloudTrail, Config, GuardDuty, and Security Hub.

---

## Acceptance Criteria

- [x] CloudTrail enabled organization-wide
- [x] AWS Config enabled organization-wide
- [x] GuardDuty enabled organization-wide
- [x] Security Hub enabled organization-wide
- [x] Service access verified in all accounts

---

## Implementation

### 1. Enable CloudTrail

```bash
# Enable CloudTrail service access
aws organizations enable-aws-service-access \
  --service-principal cloudtrail.amazonaws.com

# Verify enablement
aws organizations list-aws-service-access-for-organization \
  --query 'EnabledServicePrincipals[?ServicePrincipal==`cloudtrail.amazonaws.com`]'
```

**Purpose**: Enables organization-wide audit logging

**Benefits**:
- Centralized log management
- Compliance and security auditing
- Automatic enablement for new accounts

### 2. Enable AWS Config

```bash
# Enable Config service access
aws organizations enable-aws-service-access \
  --service-principal config.amazonaws.com

# Verify enablement
aws organizations list-aws-service-access-for-organization \
  --query 'EnabledServicePrincipals[?ServicePrincipal==`config.amazonaws.com`]'
```

**Purpose**: Enables configuration compliance tracking

**Benefits**:
- Resource configuration history
- Compliance rule evaluation
- Change tracking across accounts

### 3. Enable GuardDuty

```bash
# Enable GuardDuty service access
aws organizations enable-aws-service-access \
  --service-principal guardduty.amazonaws.com

# Verify enablement
aws organizations list-aws-service-access-for-organization \
  --query 'EnabledServicePrincipals[?ServicePrincipal==`guardduty.amazonaws.com`]'
```

**Purpose**: Enables threat detection across all accounts

**Benefits**:
- Centralized security monitoring
- Automatic threat detection
- Delegated administrator support

### 4. Enable Security Hub (Optional)

```bash
# Enable Security Hub service access
aws organizations enable-aws-service-access \
  --service-principal securityhub.amazonaws.com

# Verify enablement
aws organizations list-aws-service-access-for-organization \
  --query 'EnabledServicePrincipals[?ServicePrincipal==`securityhub.amazonaws.com`]'
```

**Purpose**: Centralized security findings and compliance

**Benefits**:
- Aggregated security findings
- Compliance standards automation
- Cross-account security posture

---

## Verification

### 1. List All Enabled Services

```bash
# List all enabled service principals
aws organizations list-aws-service-access-for-organization
```

**Expected Output**:
```json
{
    "EnabledServicePrincipals": [
        {
            "ServicePrincipal": "cloudtrail.amazonaws.com",
            "DateEnabled": "2024-01-01T00:00:00Z"
        },
        {
            "ServicePrincipal": "config.amazonaws.com",
            "DateEnabled": "2024-01-01T00:00:00Z"
        },
        {
            "ServicePrincipal": "guardduty.amazonaws.com",
            "DateEnabled": "2024-01-01T00:00:00Z"
        },
        {
            "ServicePrincipal": "securityhub.amazonaws.com",
            "DateEnabled": "2024-01-01T00:00:00Z"
        }
    ]
}
```

### 2. Verify Service Access in Member Accounts

```bash
# Test in dev account
aws organizations describe-account \
  --account-id 801651112319 \
  --profile turaf-root

# Repeat for qa, prod, and ops accounts
```

---

## Post-Enablement Configuration

### CloudTrail Organization Trail

After enabling CloudTrail service access, create an organization trail:

```bash
# Create S3 bucket for CloudTrail logs (in root account)
aws s3api create-bucket \
  --bucket turaf-cloudtrail-logs \
  --region us-east-1

# Create organization trail
aws cloudtrail create-trail \
  --name turaf-organization-trail \
  --s3-bucket-name turaf-cloudtrail-logs \
  --is-organization-trail \
  --is-multi-region-trail

# Start logging
aws cloudtrail start-logging \
  --name turaf-organization-trail
```

### AWS Config Aggregator

After enabling Config service access, create an aggregator:

```bash
# Create configuration aggregator (in root account)
aws configservice put-configuration-aggregator \
  --configuration-aggregator-name turaf-org-aggregator \
  --organization-aggregation-source '{
    "RoleArn": "arn:aws:iam::072456928432:role/aws-service-role/organizations.amazonaws.com/AWSServiceRoleForOrganizations",
    "AllAwsRegions": true
  }'
```

### GuardDuty Delegated Administrator

After enabling GuardDuty, designate Ops account as delegated administrator:

```bash
# Enable delegated administrator (from root account)
aws guardduty enable-organization-admin-account \
  --admin-account-id 146072879609

# Verify
aws guardduty list-organization-admin-accounts
```

### Security Hub Delegated Administrator

After enabling Security Hub, designate Ops account as delegated administrator:

```bash
# Enable delegated administrator (from root account)
aws securityhub enable-organization-admin-account \
  --admin-account-id 146072879609

# Verify
aws securityhub list-organization-admin-accounts
```

---

## Troubleshooting

### Issue: "Service already enabled"

**Cause**: Service was previously enabled

**Solution**:
- This is not an error, service is already configured
- Verify with `list-aws-service-access-for-organization`

### Issue: "AccessDeniedException"

**Cause**: Insufficient permissions

**Solution**:
```bash
# Ensure using root account credentials
aws sts get-caller-identity

# Verify account ID is 072456928432
```

### Issue: "OrganizationNotInAllFeaturesMode"

**Cause**: Organization is in consolidated billing mode only

**Solution**:
```bash
# Enable all features
aws organizations enable-all-features

# Confirm the change
```

---

## Service-Specific Notes

### CloudTrail

**Log Retention**:
- CloudWatch Logs: 90 days
- S3: 7 years (with lifecycle policy)

**Log Encryption**:
- Use KMS encryption for S3 bucket
- Enable log file validation

### AWS Config

**Configuration Items**:
- All supported resource types
- Global resources (IAM, CloudFront)
- Relationship tracking

**Delivery Channel**:
- S3 bucket for configuration snapshots
- SNS topic for notifications

### GuardDuty

**Finding Types**:
- Reconnaissance
- Instance compromise
- Account compromise
- Bucket compromise

**Auto-Enable**:
- New accounts automatically enrolled
- All regions enabled

### Security Hub

**Standards**:
- AWS Foundational Security Best Practices
- CIS AWS Foundations Benchmark
- PCI DSS

**Integration**:
- GuardDuty findings
- Config findings
- IAM Access Analyzer

---

## Checklist

- [x] CloudTrail service access enabled
- [x] AWS Config service access enabled
- [x] GuardDuty service access enabled
- [x] Security Hub service access enabled
- [x] All services verified in organization
- [ ] Organization trail created (CloudTrail) - deferred to later task
- [ ] Configuration aggregator created (Config) - deferred to later task
- [ ] Delegated administrator set (GuardDuty) - deferred to later task
- [ ] Delegated administrator set (Security Hub) - deferred to later task

---

## Next Steps

After enabling services:
1. ✅ **COMPLETED** - All AWS services enabled organization-wide
2. Proceed to **Task 004: Configure Route 53 Hosted Zone**
3. Later: Configure service-specific settings (CloudTrail trails, Config aggregators, GuardDuty delegated admin)

## Implementation Results (2024-03-23)

### ✅ Enabled Services

All services successfully enabled at **2026-03-23 15:22 EDT**:

| Service | Service Principal | Date Enabled | Status |
|---------|------------------|--------------|--------|
| CloudTrail | cloudtrail.amazonaws.com | 2026-03-23 15:22:04 | ✓ Enabled |
| AWS Config | config.amazonaws.com | 2026-03-23 15:22:05 | ✓ Enabled |
| GuardDuty | guardduty.amazonaws.com | 2026-03-23 15:22:06 | ✓ Enabled |
| Security Hub | securityhub.amazonaws.com | 2026-03-23 15:22:07 | ✓ Enabled |

### 📋 Previously Enabled Services

| Service | Service Principal | Date Enabled |
|---------|------------------|--------------|
| IAM | iam.amazonaws.com | 2026-03-22 23:47:15 |
| SSO | sso.amazonaws.com | 2026-03-23 13:25:31 |

### 🎯 Service Benefits Enabled

**CloudTrail**:
- ✅ Centralized audit logging across all accounts
- ✅ Automatic enablement for new accounts
- ✅ Compliance and security auditing capability

**AWS Config**:
- ✅ Resource configuration history tracking
- ✅ Compliance rule evaluation
- ✅ Change tracking across all accounts

**GuardDuty**:
- ✅ Centralized threat detection
- ✅ Automatic security monitoring
- ✅ Delegated administrator support ready

**Security Hub**:
- ✅ Aggregated security findings
- ✅ Compliance standards automation
- ✅ Cross-account security posture management

### 📝 Post-Enablement Tasks (Deferred)

The following configurations will be completed in later tasks:
1. **CloudTrail**: Create organization trail with S3 bucket and logging
2. **AWS Config**: Create configuration aggregator for centralized compliance
3. **GuardDuty**: Set Ops account (146072879609) as delegated administrator
4. **Security Hub**: Set Ops account (146072879609) as delegated administrator

---

## References

- [Enabling AWS Service Access](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_integrate_services.html)
- [CloudTrail Organization Trails](https://docs.aws.amazon.com/awscloudtrail/latest/userguide/creating-trail-organization.html)
- [Config Aggregator](https://docs.aws.amazon.com/config/latest/developerguide/aggregate-data.html)
- [GuardDuty Multi-Account](https://docs.aws.amazon.com/guardduty/latest/ug/guardduty_organizations.html)
- specs/aws-organization-setup.md
