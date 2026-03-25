output "rds_instance_id" {
  description = "ID of the RDS instance"
  value       = aws_db_instance.postgres.id
}

output "rds_instance_arn" {
  description = "ARN of the RDS instance"
  value       = aws_db_instance.postgres.arn
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  description = "RDS instance address"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.postgres.port
}

output "rds_database_name" {
  description = "RDS database name"
  value       = aws_db_instance.postgres.db_name
}

output "redis_primary_endpoint" {
  description = "Redis primary endpoint address (null if Redis disabled)"
  value       = var.enable_redis ? aws_elasticache_replication_group.redis[0].primary_endpoint_address : null
}

output "redis_reader_endpoint" {
  description = "Redis reader endpoint address (null if Redis disabled)"
  value       = var.enable_redis ? aws_elasticache_replication_group.redis[0].reader_endpoint_address : null
}

output "redis_port" {
  description = "Redis port (null if Redis disabled)"
  value       = var.enable_redis ? aws_elasticache_replication_group.redis[0].port : null
}

output "redis_enabled" {
  description = "Whether Redis is enabled"
  value       = var.enable_redis
}

output "documentdb_endpoint" {
  description = "DocumentDB cluster endpoint (null if DocumentDB disabled)"
  value       = var.enable_documentdb ? aws_docdb_cluster.documentdb[0].endpoint : null
}

output "documentdb_reader_endpoint" {
  description = "DocumentDB cluster reader endpoint (null if DocumentDB disabled)"
  value       = var.enable_documentdb ? aws_docdb_cluster.documentdb[0].reader_endpoint : null
}

output "documentdb_port" {
  description = "DocumentDB port (null if DocumentDB disabled)"
  value       = var.enable_documentdb ? aws_docdb_cluster.documentdb[0].port : null
}

output "documentdb_enabled" {
  description = "Whether DocumentDB is enabled"
  value       = var.enable_documentdb
}

output "db_admin_secret_arn" {
  description = "ARN of the database admin password secret"
  value       = aws_secretsmanager_secret.db_admin_password.arn
}

output "identity_user_secret_arn" {
  description = "ARN of the identity user password secret"
  value       = aws_secretsmanager_secret.identity_user_password.arn
}

output "organization_user_secret_arn" {
  description = "ARN of the organization user password secret"
  value       = aws_secretsmanager_secret.organization_user_password.arn
}

output "experiment_user_secret_arn" {
  description = "ARN of the experiment user password secret"
  value       = aws_secretsmanager_secret.experiment_user_password.arn
}

output "metrics_user_secret_arn" {
  description = "ARN of the metrics user password secret"
  value       = aws_secretsmanager_secret.metrics_user_password.arn
}

output "redis_auth_secret_arn" {
  description = "ARN of the Redis auth token secret (null if Redis disabled)"
  value       = var.enable_redis ? aws_secretsmanager_secret.redis_auth_token[0].arn : null
}

output "documentdb_secret_arn" {
  description = "ARN of the DocumentDB password secret (null if DocumentDB disabled)"
  value       = var.enable_documentdb ? aws_secretsmanager_secret.documentdb_password[0].arn : null
}

output "schema_names" {
  description = "List of database schema names"
  value = [
    "identity_schema",
    "organization_schema",
    "experiment_schema",
    "metrics_schema"
  ]
}

output "service_users" {
  description = "Map of service names to database users"
  value = {
    identity     = "identity_user"
    organization = "organization_user"
    experiment   = "experiment_user"
    metrics      = "metrics_user"
  }
}
