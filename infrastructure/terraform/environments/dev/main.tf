# Turaf Platform - Development Environment (Minimal - Networking + Database Only)
# This is a simplified configuration to deploy only the infrastructure needed for Task 027

terraform {
  required_version = ">= 1.5.7"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }

  # Using local backend for initial deployment
  # After infrastructure is stable, migrate to S3 backend
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = var.tags
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

# Networking Module
module "networking" {
  source = "../../modules/networking"

  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = var.enable_nat_gateway
  single_nat_gateway = var.single_nat_gateway
  enable_flow_logs   = var.enable_flow_logs

  tags = var.tags
}

# Security Module
module "security" {
  source = "../../modules/security"

  environment = var.environment
  region      = var.aws_region
  account_id  = data.aws_caller_identity.current.account_id
  vpc_id      = module.networking.vpc_id
  vpc_cidr    = var.vpc_cidr

  tags = var.tags
}

# Database Module
module "database" {
  source = "../../modules/database"

  environment = var.environment

  # Networking
  database_subnet_ids = module.networking.database_subnet_ids

  # Security
  rds_security_group_id         = module.security.rds_security_group_id
  elasticache_security_group_id = module.security.elasticache_security_group_id
  documentdb_security_group_id  = module.security.documentdb_security_group_id
  kms_key_id                    = module.security.rds_kms_key_id
  rds_kms_key_arn               = module.security.rds_kms_key_arn

  # RDS Configuration
  db_instance_class             = var.db_instance_class
  db_allocated_storage          = var.db_allocated_storage
  db_max_allocated_storage      = var.db_max_allocated_storage
  backup_retention_days         = var.backup_retention_days
  enable_multi_az               = var.enable_multi_az
  deletion_protection           = var.deletion_protection
  enable_performance_insights   = var.enable_performance_insights

  # Cost Optimization - Disable Redis and DocumentDB
  enable_redis      = false
  enable_documentdb = false

  tags = var.tags
}
