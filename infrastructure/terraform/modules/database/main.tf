resource "random_password" "admin_password" {
  length  = 32
  special = true
}

resource "random_password" "identity_user" {
  length  = 32
  special = true
}

resource "random_password" "organization_user" {
  length  = 32
  special = true
}

resource "random_password" "experiment_user" {
  length  = 32
  special = true
}

resource "random_password" "metrics_user" {
  length  = 32
  special = true
}

resource "random_password" "redis_auth_token" {
  count   = var.enable_redis ? 1 : 0
  length  = 32
  special = false
}

resource "random_password" "documentdb_password" {
  length  = 32
  special = true
}

resource "aws_db_subnet_group" "main" {
  name       = "turaf-db-subnet-group-${var.environment}"
  subnet_ids = var.database_subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "turaf-db-subnet-group-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_db_instance" "postgres" {
  identifier     = "turaf-db-${var.environment}"
  engine         = "postgres"
  engine_version = "15.17"
  instance_class = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = var.rds_kms_key_arn

  db_name  = "turaf"
  username = "turaf_admin"
  password = random_password.admin_password.result

  multi_az               = var.enable_multi_az
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.rds_security_group_id]

  backup_retention_period = var.backup_retention_days
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  deletion_protection      = var.deletion_protection
  skip_final_snapshot      = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "turaf-db-${var.environment}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  performance_insights_enabled    = var.enable_performance_insights
  performance_insights_kms_key_id = var.enable_performance_insights ? var.rds_kms_key_arn : null

  tags = merge(
    var.tags,
    {
      Name        = "turaf-db-${var.environment}"
      Environment = var.environment
    }
  )

  lifecycle {
    ignore_changes = [final_snapshot_identifier]
  }
}

resource "aws_secretsmanager_secret" "db_admin_password" {
  name        = "turaf/${var.environment}/db/admin-password"
  description = "Admin password for Turaf PostgreSQL database"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-db-admin-password-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_secretsmanager_secret_version" "db_admin_password" {
  secret_id = aws_secretsmanager_secret.db_admin_password.id
  secret_string = jsonencode({
    username = aws_db_instance.postgres.username
    password = random_password.admin_password.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
  })
}

resource "aws_secretsmanager_secret" "identity_user_password" {
  name        = "turaf/${var.environment}/db/identity-user"
  description = "Identity service database user credentials"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-identity-user-${var.environment}"
      Environment = var.environment
      Service     = "identity-service"
    }
  )
}

resource "aws_secretsmanager_secret_version" "identity_user_password" {
  secret_id = aws_secretsmanager_secret.identity_user_password.id
  secret_string = jsonencode({
    username = "identity_user"
    password = random_password.identity_user.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
    schema   = "identity_schema"
  })
}

resource "aws_secretsmanager_secret" "organization_user_password" {
  name        = "turaf/${var.environment}/db/organization-user"
  description = "Organization service database user credentials"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-organization-user-${var.environment}"
      Environment = var.environment
      Service     = "organization-service"
    }
  )
}

resource "aws_secretsmanager_secret_version" "organization_user_password" {
  secret_id = aws_secretsmanager_secret.organization_user_password.id
  secret_string = jsonencode({
    username = "organization_user"
    password = random_password.organization_user.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
    schema   = "organization_schema"
  })
}

resource "aws_secretsmanager_secret" "experiment_user_password" {
  name        = "turaf/${var.environment}/db/experiment-user"
  description = "Experiment service database user credentials"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-experiment-user-${var.environment}"
      Environment = var.environment
      Service     = "experiment-service"
    }
  )
}

resource "aws_secretsmanager_secret_version" "experiment_user_password" {
  secret_id = aws_secretsmanager_secret.experiment_user_password.id
  secret_string = jsonencode({
    username = "experiment_user"
    password = random_password.experiment_user.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
    schema   = "experiment_schema"
  })
}

resource "aws_secretsmanager_secret" "metrics_user_password" {
  name        = "turaf/${var.environment}/db/metrics-user"
  description = "Metrics service database user credentials"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-metrics-user-${var.environment}"
      Environment = var.environment
      Service     = "metrics-service"
    }
  )
}

resource "aws_secretsmanager_secret_version" "metrics_user_password" {
  secret_id = aws_secretsmanager_secret.metrics_user_password.id
  secret_string = jsonencode({
    username = "metrics_user"
    password = random_password.metrics_user.result
    host     = aws_db_instance.postgres.address
    port     = aws_db_instance.postgres.port
    database = aws_db_instance.postgres.db_name
    schema   = "metrics_schema"
  })
}

resource "aws_elasticache_subnet_group" "redis" {
  count      = var.enable_redis ? 1 : 0
  name       = "turaf-redis-subnet-group-${var.environment}"
  subnet_ids = var.database_subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "turaf-redis-subnet-group-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_elasticache_replication_group" "redis" {
  count                = var.enable_redis ? 1 : 0
  replication_group_id = "turaf-redis-${var.environment}"
  description          = "Redis cluster for Turaf ${var.environment}"
  
  engine               = "redis"
  engine_version       = "7.0"
  node_type            = var.redis_node_type
  num_cache_clusters   = var.redis_num_cache_nodes
  parameter_group_name = "default.redis7"
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.redis[0].name
  security_group_ids = [var.elasticache_security_group_id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.redis_auth_token[0].result
  kms_key_id                 = var.kms_key_id

  automatic_failover_enabled = var.redis_num_cache_nodes > 1
  multi_az_enabled           = var.redis_num_cache_nodes > 1

  snapshot_retention_limit = var.redis_snapshot_retention_days
  snapshot_window          = "03:00-05:00"
  maintenance_window       = "mon:05:00-mon:07:00"

  tags = merge(
    var.tags,
    {
      Name        = "turaf-redis-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_secretsmanager_secret" "redis_auth_token" {
  count       = var.enable_redis ? 1 : 0
  name        = "turaf/${var.environment}/redis/auth-token"
  description = "Redis authentication token"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-redis-auth-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_secretsmanager_secret_version" "redis_auth_token" {
  count     = var.enable_redis ? 1 : 0
  secret_id = aws_secretsmanager_secret.redis_auth_token[0].id
  secret_string = jsonencode({
    auth_token = random_password.redis_auth_token[0].result
    host       = aws_elasticache_replication_group.redis[0].primary_endpoint_address
    port       = aws_elasticache_replication_group.redis[0].port
  })
}

resource "aws_docdb_subnet_group" "documentdb" {
  count      = var.enable_documentdb ? 1 : 0
  name       = "turaf-documentdb-subnet-group-${var.environment}"
  subnet_ids = var.database_subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "turaf-documentdb-subnet-group-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_docdb_cluster" "documentdb" {
  count                   = var.enable_documentdb ? 1 : 0
  cluster_identifier      = "turaf-documentdb-${var.environment}"
  engine                  = "docdb"
  engine_version          = "5.0.0"
  master_username         = "turaf_admin"
  master_password         = random_password.documentdb_password.result
  db_subnet_group_name    = aws_docdb_subnet_group.documentdb[0].name
  vpc_security_group_ids  = [var.documentdb_security_group_id]
  
  backup_retention_period = var.documentdb_backup_retention_days
  preferred_backup_window = "03:00-05:00"
  preferred_maintenance_window = "mon:05:00-mon:07:00"
  
  storage_encrypted = true
  kms_key_id        = var.kms_key_id
  
  skip_final_snapshot       = !var.deletion_protection
  final_snapshot_identifier = var.deletion_protection ? "turaf-documentdb-${var.environment}-final-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null
  
  enabled_cloudwatch_logs_exports = ["audit", "profiler"]

  tags = merge(
    var.tags,
    {
      Name        = "turaf-documentdb-${var.environment}"
      Environment = var.environment
    }
  )

  lifecycle {
    ignore_changes = [final_snapshot_identifier]
  }
}

resource "aws_docdb_cluster_instance" "documentdb" {
  count              = var.enable_documentdb ? var.documentdb_instance_count : 0
  identifier         = "turaf-documentdb-${var.environment}-${count.index + 1}"
  cluster_identifier = aws_docdb_cluster.documentdb[0].id
  instance_class     = var.documentdb_instance_class

  tags = merge(
    var.tags,
    {
      Name        = "turaf-documentdb-${var.environment}-${count.index + 1}"
      Environment = var.environment
    }
  )
}

resource "aws_secretsmanager_secret" "documentdb_password" {
  count       = var.enable_documentdb ? 1 : 0
  name        = "turaf/${var.environment}/documentdb/admin"
  description = "DocumentDB admin credentials"
  kms_key_id  = var.kms_key_id

  tags = merge(
    var.tags,
    {
      Name        = "turaf-documentdb-admin-${var.environment}"
      Environment = var.environment
    }
  )
}

resource "aws_secretsmanager_secret_version" "documentdb_password" {
  count     = var.enable_documentdb ? 1 : 0
  secret_id = aws_secretsmanager_secret.documentdb_password[0].id
  secret_string = jsonencode({
    username = aws_docdb_cluster.documentdb[0].master_username
    password = random_password.documentdb_password.result
    host     = aws_docdb_cluster.documentdb[0].endpoint
    port     = aws_docdb_cluster.documentdb[0].port
  })
}
