variable "environment" {
  description = "Environment name (dev, qa, prod)"
  type        = string

  validation {
    condition     = contains(["dev", "qa", "prod"], var.environment)
    error_message = "Environment must be dev, qa, or prod."
  }
}

variable "database_subnet_ids" {
  description = "List of subnet IDs for database resources"
  type        = list(string)
}

variable "rds_security_group_id" {
  description = "Security group ID for RDS"
  type        = string
}

variable "elasticache_security_group_id" {
  description = "Security group ID for ElastiCache"
  type        = string
}

variable "documentdb_security_group_id" {
  description = "Security group ID for DocumentDB"
  type        = string
}

variable "kms_key_id" {
  description = "KMS key ID for encryption"
  type        = string
}

variable "rds_kms_key_arn" {
  description = "KMS key ARN for RDS encryption"
  type        = string
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
  
  validation {
    condition     = can(regex("^db\\.(t3|t4g)\\.(micro|small)", var.db_instance_class))
    error_message = "For demo/cost optimization, use db.t3.micro or db.t3.small (Free Tier eligible)."
  }
}

variable "db_allocated_storage" {
  description = "Allocated storage for RDS in GB (Free Tier: 20 GB)"
  type        = number
  default     = 20
  
  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 100
    error_message = "Storage must be between 20 GB (Free Tier) and 100 GB."
  }
}

variable "db_max_allocated_storage" {
  description = "Maximum allocated storage for RDS autoscaling in GB"
  type        = number
  default     = 50
}

variable "backup_retention_days" {
  description = "Number of days to retain RDS backups (Free Tier: 1-7 days)"
  type        = number
  default     = 1
  
  validation {
    condition     = var.backup_retention_days >= 1 && var.backup_retention_days <= 35
    error_message = "Backup retention must be between 1 and 35 days."
  }
}

variable "enable_multi_az" {
  description = "Enable Multi-AZ for RDS (not recommended for demo - doubles cost)"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Enable deletion protection for databases (disable for demo)"
  type        = bool
  default     = false
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights for RDS (additional cost - disable for demo)"
  type        = bool
  default     = false
}

variable "enable_redis" {
  description = "Enable ElastiCache Redis (disable for demo to save ~$12/month)"
  type        = bool
  default     = false
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type (only used if enable_redis = true)"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_cache_nodes" {
  description = "Number of cache nodes in Redis cluster (use 1 for demo)"
  type        = number
  default     = 1

  validation {
    condition     = var.redis_num_cache_nodes >= 1 && var.redis_num_cache_nodes <= 6
    error_message = "Redis cluster must have between 1 and 6 nodes."
  }
}

variable "redis_snapshot_retention_days" {
  description = "Number of days to retain Redis snapshots"
  type        = number
  default     = 1
}

variable "enable_documentdb" {
  description = "Enable DocumentDB (disable for demo to save ~$54/month - use PostgreSQL JSON instead)"
  type        = bool
  default     = false
}

variable "documentdb_instance_class" {
  description = "DocumentDB instance class (only used if enable_documentdb = true)"
  type        = string
  default     = "db.t3.medium"
}

variable "documentdb_instance_count" {
  description = "Number of DocumentDB instances (use 1 for demo)"
  type        = number
  default     = 1

  validation {
    condition     = var.documentdb_instance_count >= 1 && var.documentdb_instance_count <= 16
    error_message = "DocumentDB cluster must have between 1 and 16 instances."
  }
}

variable "documentdb_backup_retention_days" {
  description = "Number of days to retain DocumentDB backups"
  type        = number
  default     = 1
}

variable "tags" {
  description = "Additional tags for all resources"
  type        = map(string)
  default     = {}
}
