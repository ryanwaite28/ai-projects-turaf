# Task: Create Organizational Units

**Service**: Infrastructure  
**Type**: Organization Setup  
**Priority**: High  
**Estimated Time**: 30 minutes  
**Dependencies**: 014-verify-aws-organization

---

## Objective

Create organizational units (OUs) to logically group AWS accounts by function (Workloads vs Security) and enable OU-level policy management.

---

## Acceptance Criteria

- [ ] Workloads OU created
- [ ] Security OU created
- [ ] dev, qa, prod accounts moved to Workloads OU
- [ ] Ops account moved to Security OU
- [ ] OU structure verified

---

## Implementation

### 1. Create Workloads OU

```bash
# Create Workloads OU
aws organizations create-organizational-unit \
  --parent-id r-gs6r \
  --name "Workloads"

# Save the OU ID from the output
# Expected format: ou-xxxx-xxxxxxxx
```

**Expected Output**:
```json
{
    "OrganizationalUnit": {
        "Id": "ou-xxxx-xxxxxxxx",
        "Arn": "arn:aws:organizations::072456928432:ou/o-l3zk5a91yj/ou-xxxx-xxxxxxxx",
        "Name": "Workloads"
    }
}
```

### 2. Move All Accounts to Workloads OU

```bash
# Set variable
WORKLOADS_OU_ID="<OU_ID_FROM_STEP_1>"

# Move Ops account
aws organizations move-account \
  --account-id 146072879609 \
  --source-parent-id r-gs6r \
  --destination-parent-id $WORKLOADS_OU_ID

# Move dev account
aws organizations move-account \
  --account-id 801651112319 \
  --source-parent-id r-gs6r \
  --destination-parent-id $WORKLOADS_OU_ID

# Move qa account
aws organizations move-account \
  --account-id 965932217544 \
  --source-parent-id r-gs6r \
  --destination-parent-id $WORKLOADS_OU_ID

# Move prod account
aws organizations move-account \
  --account-id 811783768245 \
  --source-parent-id r-gs6r \
  --destination-parent-id $WORKLOADS_OU_ID
```

---

## Verification

### 1. List All OUs

```bash
# List OUs under root
aws organizations list-organizational-units-for-parent \
  --parent-id r-gs6r

# Expected: 1 OU (Workloads)
```

### 2. Verify Workloads OU Members

```bash
# List accounts in Workloads OU
aws organizations list-accounts-for-parent \
  --parent-id $WORKLOADS_OU_ID

# Expected: 4 accounts (Ops, dev, qa, prod)
# - 146072879609 (Ops)
# - 801651112319 (dev)
# - 965932217544 (qa)
# - 811783768245 (prod)
```

### 4. Verify Organization Structure

```bash
# Get full organization structure
aws organizations describe-organization

# List all accounts and their parent OUs
aws organizations list-accounts --query 'Accounts[*].[Id,Name]' --output table
```

---

## OU Structure Diagram

```
AWS Organization (o-l3zk5a91yj)
└── Root (r-gs6r) - Management Account: 072456928432
    └── Workloads OU
        ├── Ops (146072879609)
        ├── dev (801651112319)
        ├── qa (965932217544)
        └── prod (811783768245)
```

---

## Troubleshooting

### Issue: "OU already exists"

**Cause**: OU with the same name already exists

**Solution**:
```bash
# List existing OUs
aws organizations list-organizational-units-for-parent --parent-id r-gs6r

# Use existing OU ID instead of creating new one
```

### Issue: "Account not found in source parent"

**Cause**: Account is already in a different OU

**Solution**:
```bash
# Find current parent of account
aws organizations list-parents --child-id 801651112319

# Use the correct source-parent-id in move-account command
```

### Issue: "AccessDeniedException"

**Cause**: Insufficient permissions

**Solution**:
- Ensure you're using root account credentials
- Verify IAM permissions include `organizations:*`

---

## Documentation

### Save OU IDs

Create a file to store OU IDs for future reference:

```bash
# Save to infrastructure/ou-ids.txt
cat > infrastructure/ou-ids.txt <<EOF
Workloads OU: $WORKLOADS_OU_ID
EOF
```

### Update Terraform Variables

If using Terraform for organization management:

```hcl
# infrastructure/terraform/organization/terraform.tfvars
workloads_ou_id = "<WORKLOADS_OU_ID>"
```

---

## Checklist

- [ ] Workloads OU created successfully
- [ ] Ops account (146072879609) in Workloads OU
- [ ] dev account (801651112319) in Workloads OU
- [ ] qa account (965932217544) in Workloads OU
- [ ] prod account (811783768245) in Workloads OU
- [ ] OU ID documented
- [ ] Organization structure verified

---

## Next Steps

After creating OUs:
1. Proceed to task 016: Enable AWS Services Organization-Wide
2. Create and attach Service Control Policies
3. Configure OU-level policies

---

## References

- [Managing Organizational Units](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_ous.html)
- [Moving Accounts](https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_accounts_move.html)
- specs/aws-organization-setup.md
