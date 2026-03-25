# Task: Setup Terraform State Backend

**Service**: Infrastructure  
**Type**: Terraform Configuration  
**Priority**: High  
**Estimated Time**: 1.5 hours  
**Dependencies**: 016-enable-aws-services-organization-wide

---

## Objective

Create S3 buckets and DynamoDB tables in each AWS account to store Terraform state files with locking, enabling safe multi-user Terraform operations.

---

## Acceptance Criteria

- [x] S3 bucket created in each account (Ops, dev, qa, prod)
- [x] DynamoDB table created in each account
- [x] Bucket versioning enabled
- [x] Bucket encryption enabled
- [x] Public access blocked
- [x] State backend configuration documented

---

## Implementation

### 1. Create State Backend in Ops Account

**S3 Bucket**:

```bash
# Switch to Ops account
export AWS_PROFILE=turaf-ops

# Create S3 bucket for Terraform state
aws s3api create-bucket \
  --bucket turaf-terraform-state-ops \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-ops \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-ops \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket turaf-terraform-state-ops \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Enable lifecycle policy (optional - keep 90 days of versions)
aws s3api put-bucket-lifecycle-configuration \
  --bucket turaf-terraform-state-ops \
  --lifecycle-configuration file://s3-lifecycle-policy.json
```

**DynamoDB Table**:

```bash
# Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name turaf-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 2. Create State Backend in Dev Account

```bash
# Switch to dev account
export AWS_PROFILE=turaf-dev

# Create S3 bucket
aws s3api create-bucket \
  --bucket turaf-terraform-state-dev \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-dev \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-dev \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket turaf-terraform-state-dev \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Create DynamoDB table
aws dynamodb create-table \
  --table-name turaf-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 3. Create State Backend in QA Account

```bash
# Switch to qa account
export AWS_PROFILE=turaf-qa

# Create S3 bucket
aws s3api create-bucket \
  --bucket turaf-terraform-state-qa \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-qa \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-qa \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket turaf-terraform-state-qa \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Create DynamoDB table
aws dynamodb create-table \
  --table-name turaf-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 4. Create State Backend in Prod Account

```bash
# Switch to prod account
export AWS_PROFILE=turaf-prod

# Create S3 bucket
aws s3api create-bucket \
  --bucket turaf-terraform-state-prod \
  --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket turaf-terraform-state-prod \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket turaf-terraform-state-prod \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket turaf-terraform-state-prod \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Create DynamoDB table
aws dynamodb create-table \
  --table-name turaf-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 5. S3 Lifecycle Policy (Optional)

Create `s3-lifecycle-policy.json`:

```json
{
  "Rules": [
    {
      "Id": "DeleteOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 90
      }
    }
  ]
}
```

---

## Terraform Backend Configuration

### Backend Configuration Files

Create backend config for each environment:

**`infrastructure/terraform/environments/dev/backend.tf`**:

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-dev"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
```

**`infrastructure/terraform/environments/qa/backend.tf`**:

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-qa"
    key            = "qa/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
```

**`infrastructure/terraform/environments/prod/backend.tf`**:

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-prod"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
```

**`infrastructure/terraform/environments/ops/backend.tf`**:

```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-ops"
    key            = "ops/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "turaf-terraform-locks"
    encrypt        = true
  }
}
```

---

## Verification

### 1. Verify S3 Buckets

```bash
# List all state buckets
for profile in turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws s3 ls --profile $profile | grep terraform-state
done

# Expected output:
# turaf-terraform-state-ops
# turaf-terraform-state-dev
# turaf-terraform-state-qa
# turaf-terraform-state-prod
```

### 2. Verify Bucket Configuration

```bash
# Check versioning
aws s3api get-bucket-versioning \
  --bucket turaf-terraform-state-dev \
  --profile turaf-dev

# Expected: Status: Enabled

# Check encryption
aws s3api get-bucket-encryption \
  --bucket turaf-terraform-state-dev \
  --profile turaf-dev

# Expected: SSEAlgorithm: AES256

# Check public access block
aws s3api get-public-access-block \
  --bucket turaf-terraform-state-dev \
  --profile turaf-dev

# Expected: All settings true
```

### 3. Verify DynamoDB Tables

```bash
# List DynamoDB tables in each account
for profile in turaf-ops turaf-dev turaf-qa turaf-prod; do
  echo "=== $profile ==="
  aws dynamodb list-tables --profile $profile --region us-east-1
done

# Expected: turaf-terraform-locks in each account
```

### 4. Test Terraform Backend

```bash
# Initialize Terraform with backend
cd infrastructure/terraform/environments/dev
terraform init

# Expected: Successfully configured the backend "s3"

# Verify state file created
aws s3 ls s3://turaf-terraform-state-dev/dev/ --profile turaf-dev

# Expected: terraform.tfstate
```

---

## Automation Script

Create `scripts/setup-terraform-backends.sh`:

```bash
#!/bin/bash

set -e

ACCOUNTS=("ops:turaf-ops" "dev:turaf-dev" "qa:turaf-qa" "prod:turaf-prod")
REGION="us-east-1"

for account in "${ACCOUNTS[@]}"; do
  ENV="${account%%:*}"
  PROFILE="${account##*:}"
  BUCKET="turaf-terraform-state-${ENV}"
  
  echo "Setting up Terraform backend for ${ENV} account..."
  
  # Create S3 bucket
  aws s3api create-bucket \
    --bucket ${BUCKET} \
    --region ${REGION} \
    --profile ${PROFILE} || echo "Bucket already exists"
  
  # Enable versioning
  aws s3api put-bucket-versioning \
    --bucket ${BUCKET} \
    --versioning-configuration Status=Enabled \
    --profile ${PROFILE}
  
  # Enable encryption
  aws s3api put-bucket-encryption \
    --bucket ${BUCKET} \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }]
    }' \
    --profile ${PROFILE}
  
  # Block public access
  aws s3api put-public-access-block \
    --bucket ${BUCKET} \
    --public-access-block-configuration \
      BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    --profile ${PROFILE}
  
  # Create DynamoDB table
  aws dynamodb create-table \
    --table-name turaf-terraform-locks \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region ${REGION} \
    --profile ${PROFILE} || echo "Table already exists"
  
  echo "✅ ${ENV} backend setup complete"
done

echo "All Terraform backends configured successfully!"
```

**Run script**:

```bash
chmod +x scripts/setup-terraform-backends.sh
./scripts/setup-terraform-backends.sh
```

---

## Troubleshooting

### Issue: "BucketAlreadyExists"

**Cause**: Bucket name already taken globally

**Solution**:
```bash
# Use account ID in bucket name for uniqueness
BUCKET="turaf-terraform-state-dev-801651112319"
```

### Issue: "AccessDenied" when creating bucket

**Cause**: Insufficient S3 permissions

**Solution**:
```bash
# Verify IAM permissions include:
# - s3:CreateBucket
# - s3:PutBucketVersioning
# - s3:PutBucketEncryption
# - s3:PutPublicAccessBlock
```

### Issue: DynamoDB table creation fails

**Cause**: Table already exists or insufficient permissions

**Solution**:
```bash
# Check if table exists
aws dynamodb describe-table \
  --table-name turaf-terraform-locks \
  --region us-east-1

# Delete and recreate if needed
aws dynamodb delete-table \
  --table-name turaf-terraform-locks \
  --region us-east-1
```

### Issue: Terraform init fails with backend error

**Cause**: Incorrect backend configuration or credentials

**Solution**:
```bash
# Verify AWS credentials
aws sts get-caller-identity --profile turaf-dev

# Check backend.tf configuration
# Ensure bucket name and region match
```

---

## Cost Optimization

### S3 Costs

- **Storage**: ~$0.023/GB/month (Standard)
- **Versioning**: Additional cost for old versions
- **Lifecycle policy**: Automatically delete old versions after 90 days

**Estimated**: <$1/month per account (state files are small)

### DynamoDB Costs

- **PAY_PER_REQUEST**: $1.25 per million write requests
- **Terraform locks**: ~10-50 requests per `terraform apply`

**Estimated**: <$0.10/month per account

**Total**: ~$5/month for all 4 accounts

---

## Security Best Practices

### Bucket Policies

Add bucket policy to restrict access:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnencryptedObjectUploads",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::turaf-terraform-state-dev/*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": "AES256"
        }
      }
    }
  ]
}
```

### IAM Permissions

Create IAM policy for Terraform state access:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": [
        "arn:aws:s3:::turaf-terraform-state-*",
        "arn:aws:s3:::turaf-terraform-state-*/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/turaf-terraform-locks"
    }
  ]
}
```

---

## Documentation

Create `infrastructure/terraform-state-backends.md`:

```markdown
# Terraform State Backends

## Configuration

| Account | Bucket | DynamoDB Table | Region |
|---------|--------|----------------|--------|
| Ops | turaf-terraform-state-ops | turaf-terraform-locks | us-east-1 |
| Dev | turaf-terraform-state-dev | turaf-terraform-locks | us-east-1 |
| QA | turaf-terraform-state-qa | turaf-terraform-locks | us-east-1 |
| Prod | turaf-terraform-state-prod | turaf-terraform-locks | us-east-1 |

## Features

- ✅ Versioning enabled
- ✅ Encryption at rest (AES256)
- ✅ Public access blocked
- ✅ State locking via DynamoDB
- ✅ Lifecycle policy (90-day retention)

## Usage

```bash
cd infrastructure/terraform/environments/dev
terraform init
terraform plan
terraform apply
```
```

---

## Checklist

- [x] Created S3 bucket in Ops account
- [x] Created S3 bucket in dev account
- [x] Created S3 bucket in qa account
- [x] Created S3 bucket in prod account
- [x] Enabled versioning on all buckets
- [x] Enabled encryption on all buckets
- [x] Blocked public access on all buckets
- [x] Created DynamoDB table in Ops account
- [x] Created DynamoDB table in dev account
- [x] Created DynamoDB table in qa account
- [x] Created DynamoDB table in prod account
- [x] Created backend.tf for each environment
- [ ] Tested terraform init in dev environment (ready to test)
- [x] Documented state backend configuration

---

## Next Steps

After Terraform state backends are configured:
1. ✅ **COMPLETED** - Terraform state backends configured in all 4 accounts
2. Proceed to **Task 009: Configure IAM OIDC for GitHub Actions**
3. Initialize Terraform in each environment when ready to deploy
4. Begin infrastructure deployment with Terraform modules

## Implementation Results (2024-03-23)

### ✅ S3 Buckets Created

All buckets created with versioning, encryption, and public access blocking:

| Account | Bucket Name | ARN | Status |
|---------|-------------|-----|--------|
| Ops (146072879609) | turaf-terraform-state-ops | arn:aws:s3:::turaf-terraform-state-ops | ✅ Active |
| Dev (801651112319) | turaf-terraform-state-dev | arn:aws:s3:::turaf-terraform-state-dev | ✅ Active |
| QA (965932217544) | turaf-terraform-state-qa | arn:aws:s3:::turaf-terraform-state-qa | ✅ Active |
| Prod (811783768245) | turaf-terraform-state-prod | arn:aws:s3:::turaf-terraform-state-prod | ✅ Active |

### ✅ S3 Bucket Features

- **Versioning**: Enabled on all buckets
- **Encryption**: AES256 server-side encryption
- **Public Access**: Completely blocked (all 4 settings enabled)
- **Region**: us-east-1

### ✅ DynamoDB Tables Created

State locking tables created in all accounts:

| Account | Table Name | ARN | Billing Mode | Status |
|---------|------------|-----|--------------|--------|
| Ops | turaf-terraform-locks | arn:aws:dynamodb:us-east-1:146072879609:table/turaf-terraform-locks | PAY_PER_REQUEST | ✅ Active |
| Dev | turaf-terraform-locks | arn:aws:dynamodb:us-east-1:801651112319:table/turaf-terraform-locks | PAY_PER_REQUEST | ✅ Active |
| QA | turaf-terraform-locks | arn:aws:dynamodb:us-east-1:965932217544:table/turaf-terraform-locks | PAY_PER_REQUEST | ✅ Active |
| Prod | turaf-terraform-locks | arn:aws:dynamodb:us-east-1:811783768245:table/turaf-terraform-locks | PAY_PER_REQUEST | ✅ Active |

### ✅ Backend Configuration Files

Created `backend.tf` for each environment:
- ✅ `infrastructure/terraform/environments/ops/backend.tf`
- ✅ `infrastructure/terraform/environments/dev/backend.tf`
- ✅ `infrastructure/terraform/environments/qa/backend.tf`
- ✅ `infrastructure/terraform/environments/prod/backend.tf`

### 📁 Documentation Created

- ✅ `infrastructure/terraform-state-backends.md` - Complete backend documentation
- ✅ `scripts/setup-terraform-backends.sh` - Automation script for backend setup

### 🎯 Benefits

- ✅ **Remote state storage** - State files stored securely in S3
- ✅ **State locking** - Prevents concurrent Terraform operations
- ✅ **Version control** - All state changes tracked via S3 versioning
- ✅ **Encryption** - State files encrypted at rest
- ✅ **Multi-environment** - Separate backends for each environment
- ✅ **Cost-effective** - PAY_PER_REQUEST billing for low usage

### 💰 Cost Estimation

- **S3 Storage**: <$1/month per account
- **DynamoDB**: <$0.10/month per account
- **Total**: ~$5/month for all 4 accounts

### 🔐 Security Features

- AES256 encryption on all S3 buckets
- Public access completely blocked
- State locking prevents concurrent modifications
- Versioning enables state recovery

---

## References

- [Terraform S3 Backend](https://www.terraform.io/docs/language/settings/backends/s3.html)
- [S3 Versioning](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html)
- [DynamoDB for State Locking](https://www.terraform.io/docs/language/settings/backends/s3.html#dynamodb-state-locking)
- specs/terraform-structure.md
- INFRASTRUCTURE_PLAN.md (Phase 3)
