# Terraform Bootstrap

This directory contains the Terraform configuration for bootstrapping the AWS infrastructure prerequisites.

## What It Creates

1. **S3 Bucket for Terraform State**
   - Versioning enabled
   - Encryption at rest (AES256)
   - Public access blocked
   - Bucket name: `turaf-terraform-state-{environment}`

2. **DynamoDB Table for State Locking** (DEV only, shared across environments)
   - Table name: `turaf-terraform-locks`
   - Pay-per-request billing
   - Used by all environments

3. **ECR Repositories** (per environment)
   - One repository per service
   - Image scanning enabled
   - Lifecycle policy: keep last 10 images
   - Repository names: `turaf-{service-name}`

## Services

- identity-service
- organization-service
- experiment-service
- metrics-service
- communications-service
- bff-api
- ws-gateway

## Usage

### Initial Bootstrap (Local)

**Important:** The first time you run this, you cannot use a remote backend because the S3 bucket doesn't exist yet. Use local state for the initial bootstrap.

```bash
# DEV
cd infrastructure/terraform/bootstrap
terraform init
terraform apply -var="environment=dev"

# QA
terraform init
terraform apply -var="environment=qa"

# PROD
terraform init
terraform apply -var="environment=prod"
```

### Migrate to Remote State (After Bootstrap)

After the S3 bucket is created, you can migrate the bootstrap state to the remote backend:

1. Create `backend.tf`:
```hcl
terraform {
  backend "s3" {
    bucket         = "turaf-terraform-state-{environment}"
    key            = "bootstrap/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "turaf-terraform-locks"
  }
}
```

2. Run `terraform init -migrate-state`

### Via GitHub Actions

The bootstrap is automatically run as part of the infrastructure deployment workflow.

## Outputs

- `s3_bucket_name`: Name of the Terraform state bucket
- `s3_bucket_arn`: ARN of the Terraform state bucket
- `dynamodb_table_name`: Name of the DynamoDB locks table
- `ecr_repositories`: Map of service names to ECR repository URLs
- `ecr_repository_arns`: Map of service names to ECR repository ARNs

## Notes

- The DynamoDB table is only created in the DEV environment and shared across all environments
- Each environment gets its own S3 bucket for state isolation
- Each environment gets its own set of ECR repositories
- The bootstrap must be run before deploying the main infrastructure
