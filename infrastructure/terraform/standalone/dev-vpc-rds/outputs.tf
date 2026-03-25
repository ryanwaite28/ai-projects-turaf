# Outputs for Standalone VPC + RDS Configuration

# VPC Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = aws_subnet.database[*].id
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  description = "RDS address (hostname)"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS port"
  value       = aws_db_instance.postgres.port
}

output "rds_instance_id" {
  description = "RDS instance ID"
  value       = aws_db_instance.postgres.id
}

output "rds_database_name" {
  description = "RDS database name"
  value       = aws_db_instance.postgres.db_name
}

output "rds_master_username" {
  description = "RDS master username"
  value       = aws_db_instance.postgres.username
  sensitive   = true
}

# Security Group Outputs
output "rds_security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.rds.id
}

output "codebuild_security_group_id" {
  description = "CodeBuild security group ID"
  value       = aws_security_group.codebuild.id
}

# Secrets Manager Outputs
output "rds_master_secret_arn" {
  description = "ARN of the RDS master credentials secret"
  value       = aws_secretsmanager_secret.db_master.arn
}

output "rds_master_secret_name" {
  description = "Name of the RDS master credentials secret"
  value       = aws_secretsmanager_secret.db_master.name
}

# KMS Outputs
output "rds_kms_key_id" {
  description = "KMS key ID for RDS encryption"
  value       = aws_kms_key.rds.id
}

output "rds_kms_key_arn" {
  description = "KMS key ARN for RDS encryption"
  value       = aws_kms_key.rds.arn
}
