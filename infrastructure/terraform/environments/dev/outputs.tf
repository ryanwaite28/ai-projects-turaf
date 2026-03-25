# Minimal Outputs - Networking and Database Only

# Networking Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = module.networking.vpc_cidr
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.networking.private_subnet_ids
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = module.networking.database_subnet_ids
}

# Database Outputs
output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.database.rds_endpoint
}

output "rds_instance_id" {
  description = "RDS instance ID"
  value       = module.database.rds_instance_id
}

output "rds_security_group_id" {
  description = "RDS security group ID"
  value       = module.security.rds_security_group_id
}

output "rds_master_secret_arn" {
  description = "RDS master password secret ARN"
  value       = module.database.rds_master_secret_arn
}
