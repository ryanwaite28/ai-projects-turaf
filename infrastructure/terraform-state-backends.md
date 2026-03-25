# Terraform State Backends

**Configured**: 2024-03-23  
**Status**: ✅ Active in all accounts

---

## Configuration

| Account | Account ID | Bucket | DynamoDB Table | Region |
|---------|------------|--------|----------------|--------|
| Ops | 146072879609 | turaf-terraform-state-ops | turaf-terraform-locks | us-east-1 |
| Dev | 801651112319 | turaf-terraform-state-dev | turaf-terraform-locks | us-east-1 |
| QA | 965932217544 | turaf-terraform-state-qa | turaf-terraform-locks | us-east-1 |
| Prod | 811783768245 | turaf-terraform-state-prod | turaf-terraform-locks | us-east-1 |

---

## Features

### S3 Buckets
- ✅ **Versioning enabled** - Track all state file changes
- ✅ **Encryption at rest** - AES256 server-side encryption
- ✅ **Public access blocked** - All public access settings disabled
- ✅ **Regional deployment** - All buckets in us-east-1

### DynamoDB Tables
- ✅ **State locking** - Prevents concurrent Terraform operations
- ✅ **PAY_PER_REQUEST billing** - Cost-effective for low usage
- ✅ **LockID hash key** - Unique identifier for each lock

---

## Backend Configuration Files

Each environment has a dedicated `backend.tf` file:

- `infrastructure/terraform/environments/ops/backend.tf`
- `infrastructure/terraform/environments/dev/backend.tf`
- `infrastructure/terraform/environments/qa/backend.tf`
- `infrastructure/terraform/environments/prod/backend.tf`

---

## Usage

### Initialize Terraform with Backend

```bash
# Navigate to environment directory
cd infrastructure/terraform/environments/dev

# Initialize Terraform (will configure S3 backend)
terraform init

# Verify backend configuration
terraform show
```

### Switch Between Environments

```bash
# Dev environment
cd infrastructure/terraform/environments/dev
terraform init
terraform plan

# QA environment
cd infrastructure/terraform/environments/qa
terraform init
terraform plan

# Prod environment
cd infrastructure/terraform/environments/prod
terraform init
terraform plan
```

---

## State File Management

### View State

```bash
# List state resources
terraform state list

# Show specific resource
terraform state show <resource_name>
```

### State Locking

Terraform automatically locks the state during operations:
- Lock acquired before `terraform plan`, `apply`, `destroy`
- Lock released after operation completes
- DynamoDB table stores lock information
- Prevents concurrent modifications

### Manual Unlock (Emergency Only)

```bash
# If lock is stuck (use with caution)
terraform force-unlock <LOCK_ID>
```

---

## Backup and Recovery

### S3 Versioning

All state files are versioned in S3. To recover a previous version:

```bash
# List all versions
aws s3api list-object-versions \
  --bucket turaf-terraform-state-dev \
  --prefix dev/terraform.tfstate \
  --profile turaf-dev

# Download specific version
aws s3api get-object \
  --bucket turaf-terraform-state-dev \
  --key dev/terraform.tfstate \
  --version-id <VERSION_ID> \
  terraform.tfstate.backup \
  --profile turaf-dev
```

### State Backup Best Practices

1. **Automatic versioning** - S3 versioning enabled on all buckets
2. **Manual backups** - Run `terraform state pull > backup.tfstate` before major changes
3. **Version retention** - Consider lifecycle policy to retain versions for 90 days
4. **Cross-region replication** - Optional for production (not currently configured)

---

## Security

### Encryption

- **At rest**: AES256 encryption on all S3 buckets
- **In transit**: HTTPS enforced for all S3 operations
- **State file encryption**: Terraform `encrypt = true` in backend config

### Access Control

- **S3 bucket policies**: Restrict access to authorized IAM roles only
- **DynamoDB permissions**: Limited to state locking operations
- **Public access**: Completely blocked on all buckets

### IAM Permissions Required

Terraform needs the following permissions:

**S3 Permissions**:
- `s3:ListBucket`
- `s3:GetObject`
- `s3:PutObject`
- `s3:DeleteObject`

**DynamoDB Permissions**:
- `dynamodb:GetItem`
- `dynamodb:PutItem`
- `dynamodb:DeleteItem`

---

## Cost Estimation

### S3 Costs
- **Storage**: ~$0.023/GB/month (Standard tier)
- **Requests**: Minimal (only during Terraform operations)
- **Versioning**: Additional storage for old versions
- **Estimated**: <$1/month per account

### DynamoDB Costs
- **Billing mode**: PAY_PER_REQUEST
- **Write requests**: $1.25 per million requests
- **Read requests**: $0.25 per million requests
- **Terraform operations**: ~10-50 requests per apply
- **Estimated**: <$0.10/month per account

**Total Estimated Cost**: ~$5/month for all 4 accounts

---

## Troubleshooting

### Issue: "Error acquiring the state lock"

**Cause**: Another Terraform operation is running or a previous operation didn't release the lock

**Solution**:
```bash
# Wait for other operation to complete, or
# Force unlock (use with extreme caution)
terraform force-unlock <LOCK_ID>
```

### Issue: "Error loading state: AccessDenied"

**Cause**: Insufficient S3 permissions or wrong AWS profile

**Solution**:
```bash
# Verify AWS credentials
aws sts get-caller-identity --profile turaf-dev

# Check S3 bucket access
aws s3 ls s3://turaf-terraform-state-dev --profile turaf-dev
```

### Issue: "Backend configuration changed"

**Cause**: backend.tf was modified

**Solution**:
```bash
# Reinitialize Terraform
terraform init -reconfigure
```

### Issue: State file corruption

**Cause**: Concurrent modifications or interrupted operations

**Solution**:
```bash
# Restore from S3 version
aws s3api list-object-versions \
  --bucket turaf-terraform-state-dev \
  --prefix dev/terraform.tfstate

# Download previous version and restore
```

---

## Maintenance

### Regular Tasks

- **Weekly**: Verify state locks are released
- **Monthly**: Review S3 storage costs and version count
- **Quarterly**: Test state backup and recovery procedures
- **Annually**: Review and update IAM permissions

### Monitoring

Monitor the following metrics:
- S3 bucket size and version count
- DynamoDB read/write capacity usage
- Failed Terraform operations
- State lock duration

---

## Migration (If Needed)

### Migrate from Local to S3 Backend

```bash
# 1. Add backend.tf configuration
# 2. Run terraform init
terraform init -migrate-state

# 3. Confirm migration
# State will be copied to S3
```

### Migrate Between Accounts

```bash
# 1. Pull state from old backend
terraform state pull > backup.tfstate

# 2. Update backend.tf with new bucket
# 3. Reinitialize
terraform init -reconfigure

# 4. Push state to new backend
terraform state push backup.tfstate
```

---

## Automation Script

The setup script is available at `scripts/setup-terraform-backends.sh`:

```bash
# Run setup for all accounts
./scripts/setup-terraform-backends.sh

# Setup creates:
# - S3 buckets with versioning and encryption
# - DynamoDB tables for state locking
# - Public access blocking on all buckets
```

---

## References

- [Terraform S3 Backend Documentation](https://www.terraform.io/docs/language/settings/backends/s3.html)
- [S3 Versioning](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html)
- [DynamoDB State Locking](https://www.terraform.io/docs/language/settings/backends/s3.html#dynamodb-state-locking)
- [Terraform State Management](https://www.terraform.io/docs/language/state/index.html)
- specs/terraform-structure.md
- INFRASTRUCTURE_PLAN.md (Phase 3)
