output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of private subnets"
  value       = aws_subnet.private[*].id
}

output "database_subnet_ids" {
  description = "IDs of database subnets"
  value       = aws_subnet.database[*].id
}

output "public_subnet_cidrs" {
  description = "CIDR blocks of public subnets"
  value       = aws_subnet.public[*].cidr_block
}

output "private_subnet_cidrs" {
  description = "CIDR blocks of private subnets"
  value       = aws_subnet.private[*].cidr_block
}

output "database_subnet_cidrs" {
  description = "CIDR blocks of database subnets"
  value       = aws_subnet.database[*].cidr_block
}

output "internet_gateway_id" {
  description = "ID of the internet gateway"
  value       = aws_internet_gateway.main.id
}

output "nat_gateway_ids" {
  description = "IDs of NAT gateways"
  value       = var.enable_nat_gateway ? aws_nat_gateway.main[*].id : []
}

output "nat_gateway_eips" {
  description = "Elastic IPs of NAT gateways"
  value       = var.enable_nat_gateway ? aws_eip.nat[*].public_ip : []
}

output "public_route_table_id" {
  description = "ID of public route table"
  value       = aws_route_table.public.id
}

output "private_route_table_ids" {
  description = "IDs of private route tables"
  value       = aws_route_table.private[*].id
}

output "database_route_table_ids" {
  description = "IDs of database route tables"
  value       = aws_route_table.database[*].id
}

output "vpc_endpoints_security_group_id" {
  description = "Security group ID for VPC endpoints"
  value       = aws_security_group.vpc_endpoints.id
}

output "s3_vpc_endpoint_id" {
  description = "ID of S3 VPC endpoint"
  value       = aws_vpc_endpoint.s3.id
}

output "ecr_api_vpc_endpoint_id" {
  description = "ID of ECR API VPC endpoint"
  value       = aws_vpc_endpoint.ecr_api.id
}

output "ecr_dkr_vpc_endpoint_id" {
  description = "ID of ECR DKR VPC endpoint"
  value       = aws_vpc_endpoint.ecr_dkr.id
}

output "logs_vpc_endpoint_id" {
  description = "ID of CloudWatch Logs VPC endpoint"
  value       = aws_vpc_endpoint.logs.id
}

output "secretsmanager_vpc_endpoint_id" {
  description = "ID of Secrets Manager VPC endpoint"
  value       = aws_vpc_endpoint.secretsmanager.id
}

output "ecs_vpc_endpoint_id" {
  description = "ID of ECS VPC endpoint"
  value       = aws_vpc_endpoint.ecs.id
}

output "ecs_telemetry_vpc_endpoint_id" {
  description = "ID of ECS Telemetry VPC endpoint"
  value       = aws_vpc_endpoint.ecs_telemetry.id
}

output "availability_zones" {
  description = "List of availability zones used"
  value       = var.availability_zones
}
