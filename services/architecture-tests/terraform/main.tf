terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "turaf-terraform-state"
    key    = "architecture-tests/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Turaf"
      Component   = "ArchitectureTests"
      ManagedBy   = "Terraform"
      Environment = var.environment
    }
  }
}
