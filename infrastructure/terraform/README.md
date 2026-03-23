# Turaf Terraform Infrastructure

This directory contains the Terraform infrastructure-as-code for the Turaf platform.

## Directory Structure

```
infrastructure/terraform/
├── main.tf                 # Main Terraform configuration
├── variables.tf            # Variable definitions
├── outputs.tf              # Output definitions
├── backend.tf              # S3 backend configuration
├── versions.tf             # Provider version constraints
├── modules/                # Reusable Terraform modules
│   ├── networking/         # VPC, subnets, NAT, IGW
│   ├── compute/            # ECS, ALB, Auto Scaling
│   ├── database/           # RDS PostgreSQL
│   ├── storage/            # S3 buckets
│   ├── messaging/          # EventBridge, SQS
│   ├── lambda/             # Lambda functions
│   ├── security/           # IAM, KMS, Secrets Manager
│   └── monitoring/         # CloudWatch, X-Ray
└── environments/           # Environment-specific configurations
    ├── dev/                # Development environment
    ├── qa/                 # QA environment
    └── prod/               # Production environment
```

## Prerequisites

1. **Terraform**: Version 1.5.0 or higher
   ```bash
   terraform --version
   ```

2. **AWS CLI**: Configured with appropriate credentials
   ```bash
   aws --version
   aws sts get-caller-identity
   ```

3. **Terraform State Backend**: S3 bucket and DynamoDB table must be created first
   - See task `021-setup-terraform-state-backend.md`
   - Required before running `terraform init`

## Usage

### Initialize Terraform

```bash
# Navigate to terraform directory
cd infrastructure/terraform

# Initialize Terraform (downloads providers, sets up backend)
terraform init
```

### Select Environment

```bash
# Development
terraform workspace select dev || terraform workspace new dev

# QA
terraform workspace select qa || terraform workspace new qa

# Production
terraform workspace select prod || terraform workspace new prod
```

### Plan Infrastructure Changes

```bash
# Development
terraform plan -var-file=environments/dev/terraform.tfvars

# QA
terraform plan -var-file=environments/qa/terraform.tfvars

# Production
terraform plan -var-file=environments/prod/terraform.tfvars
```

### Apply Infrastructure Changes

```bash
# Development
terraform apply -var-file=environments/dev/terraform.tfvars

# QA
terraform apply -var-file=environments/qa/terraform.tfvars

# Production (requires approval)
terraform apply -var-file=environments/prod/terraform.tfvars
```

### Destroy Infrastructure

```bash
# Development
terraform destroy -var-file=environments/dev/terraform.tfvars

# QA
terraform destroy -var-file=environments/qa/terraform.tfvars

# Production (use with extreme caution!)
terraform destroy -var-file=environments/prod/terraform.tfvars
```

## Environment Configuration

Each environment has its own `terraform.tfvars` file with environment-specific values:

### Development (dev)
- AWS Account: 801651112319
- VPC CIDR: 10.0.0.0/16
- Instance Size: Small (cost-optimized)
- Desired Count: 1

### QA (qa)
- AWS Account: 965932217544
- VPC CIDR: 10.1.0.0/16
- Instance Size: Medium
- Desired Count: 2

### Production (prod)
- AWS Account: 811783768245
- VPC CIDR: 10.2.0.0/16
- Instance Size: Large (performance-optimized)
- Desired Count: 3

## State Management

Terraform state is stored in S3 with DynamoDB locking:

- **S3 Bucket**: `turaf-terraform-state-<account-id>`
- **DynamoDB Table**: `turaf-terraform-locks-<account-id>`
- **Encryption**: Enabled (AES256)
- **Versioning**: Enabled

## Module Development

When creating new modules:

1. Create module directory under `modules/`
2. Add `main.tf`, `variables.tf`, `outputs.tf`
3. Document module in module's `README.md`
4. Reference module in root `main.tf`
5. Test module in dev environment first

## Best Practices

1. **Always run `terraform plan` before `apply`**
2. **Use workspaces for environment isolation**
3. **Never commit sensitive values to Git**
4. **Use variables for all configurable values**
5. **Tag all resources consistently**
6. **Enable state locking to prevent concurrent modifications**
7. **Review and approve all production changes**

## Validation

```bash
# Validate Terraform syntax
terraform validate

# Format Terraform files
terraform fmt -recursive

# Check for security issues (requires tfsec)
tfsec .
```

## Troubleshooting

### Backend Initialization Fails

**Issue**: `Error: Failed to get existing workspaces`

**Solution**: Ensure S3 bucket and DynamoDB table exist (run task 021 first)

### State Lock Errors

**Issue**: `Error acquiring the state lock`

**Solution**: 
```bash
# Force unlock (use with caution)
terraform force-unlock <LOCK_ID>
```

### Provider Download Fails

**Issue**: `Error installing provider`

**Solution**:
```bash
# Clear provider cache
rm -rf .terraform
terraform init
```

## References

- [Terraform Documentation](https://www.terraform.io/docs)
- [AWS Provider Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- Project Specifications: `../../specs/terraform-structure.md`
- Infrastructure Plan: `../../INFRASTRUCTURE_PLAN.md`
