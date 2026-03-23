# Task: Verify AWS Organization Structure

**Service**: Infrastructure  
**Type**: Organization Setup  
**Priority**: High  
**Estimated Time**: 30 minutes  
**Dependencies**: None (prerequisite task)

---

## Objective

Verify that the AWS Organization is properly configured with all required accounts, organizational units, and service integrations before proceeding with infrastructure deployment.

---

## Acceptance Criteria

- [ ] AWS Organization exists and is accessible
- [ ] All 5 member accounts are present and active
- [ ] Root ID is verified
- [ ] Account emails are correct
- [ ] Organization-wide services are enabled
- [ ] Billing and cost allocation tags are configured

---

## Implementation

### 1. Verify Organization

```bash
# Verify organization structure
aws organizations describe-organization

# Expected output should show:
# - Organization ID: o-l3zk5a91yj
# - MasterAccountId: 072456928432
# - FeatureSet: ALL
```

### 2. List and Verify All Accounts

```bash
# List all accounts
aws organizations list-accounts

# Verify the following accounts exist:
# - 072456928432 (root) - aws@turafapp.com
# - 146072879609 (Ops) - aws-ops@turafapp.com
# - 801651112319 (dev) - aws-dev@turafapp.com
# - 965932217544 (qa) - aws-qa@turafapp.com
# - 811783768245 (prod) - aws-prod@turafapp.com
```

### 3. Verify Root ID

```bash
# Get root ID
aws organizations list-roots

# Expected: r-gs6r
```

### 4. Check Enabled Services

```bash
# List enabled AWS services
aws organizations list-aws-service-access-for-organization

# Verify the following services are enabled:
# - cloudtrail.amazonaws.com
# - config.amazonaws.com
# - guardduty.amazonaws.com
# - securityhub.amazonaws.com (if already configured)
```

### 5. Verify Account Access

```bash
# Test access to each account
for profile in turaf-root turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "Testing $profile..."
  aws sts get-caller-identity --profile $profile
done
```

### 6. Check Cost Allocation Tags

```bash
# List activated cost allocation tags
aws ce list-cost-allocation-tags

# Verify tags are activated:
# - Environment
# - Project
# - Service
# - CostCenter
```

---

## Verification

### Organization Structure

```bash
# Verify organization details
aws organizations describe-organization --query 'Organization.[Id,MasterAccountId,MasterAccountEmail]' --output table
```

**Expected Output**:
```
---------------------------------
|    DescribeOrganization       |
+-------------------------------+
|  o-l3zk5a91yj                |
|  072456928432                 |
|  aws@turafapp.com            |
+-------------------------------+
```

### Account List

```bash
# List accounts with details
aws organizations list-accounts --query 'Accounts[*].[Id,Name,Email,Status]' --output table
```

**Expected Output**:
```
-------------------------------------------------------------------------
|                           ListAccounts                                |
+---------------+--------+-------------------------+--------------------+
|  072456928432 |  root  |  aws@turafapp.com      |  ACTIVE           |
|  146072879609 |  Ops   |  aws-ops@turafapp.com  |  ACTIVE           |
|  801651112319 |  dev   |  aws-dev@turafapp.com  |  ACTIVE           |
|  965932217544 |  qa    |  aws-qa@turafapp.com   |  ACTIVE           |
|  811783768245 |  prod  |  aws-prod@turafapp.com |  ACTIVE           |
+---------------+--------+-------------------------+--------------------+
```

---

## Troubleshooting

### Issue: "AccessDeniedException"

**Cause**: Insufficient permissions to access Organizations API

**Solution**:
```bash
# Verify you're using the root account credentials
aws sts get-caller-identity

# Ensure the account ID is 072456928432
```

### Issue: Missing Accounts

**Cause**: Accounts not yet created or invited

**Solution**:
- Verify account creation in AWS Console
- Check invitation status
- Contact AWS support if accounts are missing

### Issue: Services Not Enabled

**Cause**: Organization-wide services not yet configured

**Solution**:
```bash
# Enable required services (covered in next task)
aws organizations enable-aws-service-access --service-principal cloudtrail.amazonaws.com
aws organizations enable-aws-service-access --service-principal config.amazonaws.com
aws organizations enable-aws-service-access --service-principal guardduty.amazonaws.com
```

---

## Checklist

- [ ] Organization ID verified: `o-l3zk5a91yj`
- [ ] Root account ID verified: `072456928432`
- [ ] Root ID verified: `r-gs6r`
- [ ] All 5 accounts present and ACTIVE
- [ ] Account emails correct
- [ ] AWS CLI access configured for all accounts
- [ ] Organization-wide services enabled
- [ ] Cost allocation tags activated

---

## Next Steps

After verification:
1. Proceed to task 015: Create Organizational Units
2. Configure Service Control Policies
3. Set up cross-account IAM roles

---

## References

- [AWS Organizations Documentation](https://docs.aws.amazon.com/organizations/)
- [Verifying Organization Setup](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_org_details.html)
- AWS_ACCOUNTS.md
- specs/aws-organization-setup.md
