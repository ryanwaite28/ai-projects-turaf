# Standalone Terraform Configuration for Turaf Dev Environment
# VPC + RDS PostgreSQL Only (No Modules)
# Purpose: Minimal infrastructure for Task 027 (Database Migration Network Access)

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

  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Turaf"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# ============================================================================
# VPC and Networking
# ============================================================================

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "turaf-vpc-${var.environment}"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "turaf-igw-${var.environment}"
  }
}

# Public Subnets
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "turaf-public-subnet-${var.environment}-${count.index + 1}"
    Type = "public"
  }
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "turaf-private-subnet-${var.environment}-${count.index + 1}"
    Type = "private"
  }
}

# Database Subnets
resource "aws_subnet" "database" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "turaf-database-subnet-${var.environment}-${count.index + 1}"
    Type = "database"
  }
}

# Elastic IP for NAT Gateway
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "turaf-nat-eip-${var.environment}"
  }

  depends_on = [aws_internet_gateway.main]
}

# NAT Gateway (single for cost optimization)
resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "turaf-nat-${var.environment}"
  }

  depends_on = [aws_internet_gateway.main]
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "turaf-public-rt-${var.environment}"
  }
}

# Private Route Table
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "turaf-private-rt-${var.environment}"
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "database" {
  count          = 2
  subnet_id      = aws_subnet.database[count.index].id
  route_table_id = aws_route_table.private.id
}

# ============================================================================
# Security Groups
# ============================================================================

# RDS Security Group
resource "aws_security_group" "rds" {
  name_prefix = "turaf-rds-${var.environment}-"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "PostgreSQL from VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "turaf-rds-sg-${var.environment}"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# CodeBuild Security Group (for Flyway migrations)
resource "aws_security_group" "codebuild" {
  name_prefix = "turaf-codebuild-${var.environment}-"
  description = "Security group for CodeBuild (Flyway migrations)"
  vpc_id      = aws_vpc.main.id

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "turaf-codebuild-sg-${var.environment}"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Allow CodeBuild to access RDS
resource "aws_security_group_rule" "rds_from_codebuild" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.codebuild.id
  security_group_id        = aws_security_group.rds.id
  description              = "Allow CodeBuild to access RDS for migrations"
}

# ============================================================================
# KMS Key for RDS Encryption
# ============================================================================

resource "aws_kms_key" "rds" {
  description             = "KMS key for RDS encryption in ${var.environment}"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = {
    Name = "turaf-rds-kms-${var.environment}"
  }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/turaf-rds-${var.environment}"
  target_key_id = aws_kms_key.rds.key_id
}

# ============================================================================
# RDS PostgreSQL
# ============================================================================

# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "turaf-db-subnet-group-${var.environment}"
  subnet_ids = aws_subnet.database[*].id

  tags = {
    Name = "turaf-db-subnet-group-${var.environment}"
  }
}

# Master Password
resource "random_password" "db_master" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# RDS Instance
resource "aws_db_instance" "postgres" {
  identifier     = "turaf-postgres-${var.environment}"
  engine         = "postgres"
  engine_version = "15"
  
  instance_class        = var.db_instance_class
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = aws_kms_key.rds.arn

  db_name  = "turaf"
  username = "turaf_admin"
  password = random_password.db_master.result
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  multi_az               = var.enable_multi_az
  backup_retention_period = var.backup_retention_days
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "turaf-postgres-${var.environment}-final-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  performance_insights_enabled    = var.enable_performance_insights
  
  auto_minor_version_upgrade = true
  apply_immediately          = false

  tags = {
    Name = "turaf-postgres-${var.environment}"
  }

  lifecycle {
    ignore_changes = [final_snapshot_identifier]
  }
}

# Store master password in Secrets Manager
resource "aws_secretsmanager_secret" "db_master" {
  name_prefix             = "turaf/${var.environment}/rds/admin-"
  description             = "RDS master credentials for ${var.environment}"
  recovery_window_in_days = 7

  tags = {
    Name = "turaf-rds-admin-secret-${var.environment}"
  }
}

resource "aws_secretsmanager_secret_version" "db_master" {
  secret_id = aws_secretsmanager_secret.db_master.id
  secret_string = jsonencode({
    username = aws_db_instance.postgres.username
    password = random_password.db_master.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
    engine   = "postgres"
  })
}

# ============================================================================
# Data Sources
# ============================================================================

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
