# Networking Module

Terraform module for creating VPC, subnets, NAT gateways, route tables, and VPC endpoints for the Turaf platform.

## Features

- **VPC**: Isolated network with DNS support
- **Subnets**: Public, private, and database subnets across multiple AZs
- **High Availability**: Resources distributed across 2+ availability zones
- **NAT Gateways**: One per AZ for private subnet internet access
- **Route Tables**: Separate routing for public, private, and database subnets
- **VPC Endpoints**: Interface and gateway endpoints for AWS services
- **Flow Logs**: Optional VPC flow logging for network monitoring
- **Security Groups**: Dedicated security group for VPC endpoints

## Architecture

### Subnet Layout

```
VPC (10.x.0.0/16)
├── Public Subnets (10.x.0.0/24, 10.x.1.0/24, ...)
│   ├── Internet Gateway access
│   ├── Public IP assignment
│   └── Load balancers, bastion hosts
├── Private Subnets (10.x.10.0/24, 10.x.11.0/24, ...)
│   ├── NAT Gateway access
│   ├── No public IPs
│   └── Application servers, ECS tasks
└── Database Subnets (10.x.20.0/24, 10.x.21.0/24, ...)
    ├── Isolated from internet
    ├── No NAT Gateway
    └── RDS, ElastiCache, DocumentDB
```

### VPC Endpoints

**Gateway Endpoints** (no cost):
- S3: For ECR image layers and application data

**Interface Endpoints** ($0.01/hour each):
- ECR API: Docker registry API
- ECR DKR: Docker image pulls
- CloudWatch Logs: Log streaming
- Secrets Manager: Secret retrieval
- ECS: Container orchestration
- ECS Telemetry: Container metrics

## Usage

```hcl
module "networking" {
  source = "../../modules/networking"

  environment        = "dev"
  region             = "us-east-1"
  vpc_cidr           = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b"]

  enable_nat_gateway = true
  enable_flow_logs   = false

  tags = {
    Project   = "turaf"
    ManagedBy = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name (dev, qa, prod) | string | - | yes |
| region | AWS region | string | us-east-1 | no |
| vpc_cidr | CIDR block for VPC | string | - | yes |
| availability_zones | List of availability zones | list(string) | - | yes |
| enable_nat_gateway | Enable NAT gateways | bool | true | no |
| enable_flow_logs | Enable VPC flow logs | bool | false | no |
| flow_log_role_arn | IAM role ARN for flow logs | string | "" | no |
| flow_log_destination | Flow log destination ARN | string | "" | no |
| flow_log_destination_type | Flow log destination type | string | cloud-watch-logs | no |
| tags | Additional tags | map(string) | {} | no |

## Outputs

| Name | Description |
|------|-------------|
| vpc_id | VPC ID |
| vpc_cidr | VPC CIDR block |
| public_subnet_ids | Public subnet IDs |
| private_subnet_ids | Private subnet IDs |
| database_subnet_ids | Database subnet IDs |
| internet_gateway_id | Internet gateway ID |
| nat_gateway_ids | NAT gateway IDs |
| nat_gateway_eips | NAT gateway elastic IPs |
| vpc_endpoints_security_group_id | VPC endpoints security group ID |
| s3_vpc_endpoint_id | S3 VPC endpoint ID |
| ecr_api_vpc_endpoint_id | ECR API VPC endpoint ID |
| ecr_dkr_vpc_endpoint_id | ECR DKR VPC endpoint ID |

## CIDR Calculation

Subnets are automatically calculated using `cidrsubnet()`:

- **Public subnets**: `cidrsubnet(vpc_cidr, 8, 0-9)`
  - Example: 10.0.0.0/24, 10.0.1.0/24
- **Private subnets**: `cidrsubnet(vpc_cidr, 8, 10-19)`
  - Example: 10.0.10.0/24, 10.0.11.0/24
- **Database subnets**: `cidrsubnet(vpc_cidr, 8, 20-29)`
  - Example: 10.0.20.0/24, 10.0.21.0/24

## Cost Estimation

### Per Environment

**NAT Gateways** (2 AZs):
- $0.045/hour × 2 = $0.09/hour
- Monthly: ~$65

**VPC Endpoints** (6 interface endpoints):
- $0.01/hour × 6 = $0.06/hour
- Monthly: ~$43

**Data Transfer**:
- NAT Gateway: $0.045/GB
- VPC Endpoints: $0.01/GB

**Total Monthly Cost**: ~$108/environment

### Cost Optimization

**Development**:
- Disable NAT gateways: Save $65/month
- Use fewer VPC endpoints: Save up to $43/month
- Use public subnets for testing

**Production**:
- Keep all NAT gateways for HA
- Keep all VPC endpoints for security
- Monitor data transfer costs

## Security

### Network Isolation

- **Public subnets**: Internet-facing resources only
- **Private subnets**: Application tier, no direct internet access
- **Database subnets**: Data tier, completely isolated

### VPC Endpoints

- Reduces data transfer costs
- Improves security (no internet routing)
- Reduces latency to AWS services
- Private DNS enabled for seamless integration

### Flow Logs

Optional VPC flow logs for:
- Network traffic analysis
- Security monitoring
- Troubleshooting connectivity issues
- Compliance requirements

## High Availability

- Resources span multiple availability zones
- NAT gateways in each AZ (no single point of failure)
- Separate route tables per AZ for private subnets
- Database subnets for RDS Multi-AZ deployments

## Examples

### Development Environment

```hcl
module "networking" {
  source = "../../modules/networking"

  environment        = "dev"
  vpc_cidr           = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b"]

  enable_nat_gateway = false  # Cost savings
  enable_flow_logs   = false

  tags = {
    Environment = "dev"
    CostCenter  = "engineering"
  }
}
```

### Production Environment

```hcl
module "networking" {
  source = "../../modules/networking"

  environment        = "prod"
  vpc_cidr           = "10.2.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]

  enable_nat_gateway         = true
  enable_flow_logs           = true
  flow_log_role_arn          = aws_iam_role.flow_logs.arn
  flow_log_destination       = aws_cloudwatch_log_group.flow_logs.arn
  flow_log_destination_type  = "cloud-watch-logs"

  tags = {
    Environment = "prod"
    Compliance  = "required"
  }
}
```

## Troubleshooting

### NAT Gateway Issues

**Problem**: Private subnet instances can't reach internet

**Solutions**:
1. Verify NAT gateway is in public subnet
2. Check route table has route to NAT gateway
3. Verify security groups allow outbound traffic
4. Check NACL rules

### VPC Endpoint Issues

**Problem**: Can't pull ECR images

**Solutions**:
1. Verify ECR endpoints are created
2. Check security group allows port 443
3. Verify private DNS is enabled
4. Check route tables

### Subnet CIDR Conflicts

**Problem**: Terraform plan fails with CIDR overlap

**Solutions**:
1. Verify VPC CIDR is large enough (/16 recommended)
2. Check no manual subnets conflict with calculated ranges
3. Adjust cidrsubnet calculations if needed

## References

- [AWS VPC Documentation](https://docs.aws.amazon.com/vpc/)
- [VPC Endpoints](https://docs.aws.amazon.com/vpc/latest/privatelink/vpc-endpoints.html)
- [NAT Gateways](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-nat-gateway.html)
- [VPC Flow Logs](https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html)
