terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# S3 Bucket for Terraform State
resource "aws_s3_bucket" "terraform_state" {
  bucket = "turaf-terraform-state-${var.environment}"

  tags = {
    Name        = "Terraform State - ${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform-Bootstrap"
  }
}

# Enable versioning for state bucket
resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Enable encryption for state bucket
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Block public access to state bucket
resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# NOTE: DynamoDB lock table (turaf-terraform-locks) is created via AWS CLI in bootstrap.yml
# before this Terraform runs. Since bootstrap runs with -backend=false (no Terraform state),
# Terraform cannot manage this shared resource idempotently across environment runs.
# The AWS CLI step in the workflow handles the "create if not exists" pattern.

# ECR Repositories for all services
locals {
  services = [
    "identity-service",
    "organization-service",
    "experiment-service",
    "metrics-service",
    "communications-service",
    "bff-api",
    "ws-gateway"
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.services)

  name                 = "turaf-${each.value}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name        = "turaf-${each.value}"
    Service     = each.value
    Environment = var.environment
    ManagedBy   = "Terraform-Bootstrap"
  }
}

# Lifecycle policy for ECR repositories - keep last 10 images
resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus     = "any"
        countType     = "imageCountMoreThan"
        countNumber   = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
