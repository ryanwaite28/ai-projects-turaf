locals {
  common_tags = merge(
    var.tags,
    {
      Project     = var.project_name
      Environment = var.environment
    }
  )
}

module "networking" {
  source = "./modules/networking"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = var.enable_nat_gateway
  enable_vpn_gateway = var.enable_vpn_gateway
  tags               = local.common_tags
}

module "security" {
  source = "./modules/security"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.networking.vpc_id
  tags         = local.common_tags
}

module "database" {
  source = "./modules/database"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  subnet_ids         = module.networking.database_subnet_ids
  security_group_ids = [module.security.database_security_group_id]
  instance_class     = var.db_instance_class
  allocated_storage  = var.db_allocated_storage
  engine_version     = var.db_engine_version
  tags               = local.common_tags
}

module "storage" {
  source = "./modules/storage"

  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}

module "messaging" {
  source = "./modules/messaging"

  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}

module "compute" {
  source = "./modules/compute"

  project_name              = var.project_name
  environment               = var.environment
  vpc_id                    = module.networking.vpc_id
  public_subnet_ids         = module.networking.public_subnet_ids
  private_subnet_ids        = module.networking.private_subnet_ids
  alb_security_group_id     = module.security.alb_security_group_id
  ecs_security_group_id     = module.security.ecs_security_group_id
  task_cpu                  = var.ecs_task_cpu
  task_memory               = var.ecs_task_memory
  desired_count             = var.ecs_desired_count
  enable_container_insights = var.enable_container_insights
  tags                      = local.common_tags
}

module "lambda" {
  source = "./modules/lambda"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.networking.vpc_id
  subnet_ids         = module.networking.private_subnet_ids
  security_group_ids = [module.security.lambda_security_group_id]
  tags               = local.common_tags
}

module "monitoring" {
  source = "./modules/monitoring"

  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}
