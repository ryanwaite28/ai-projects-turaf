# Task: Create Networking Module

**Service**: Infrastructure  
**Phase**: 10  
**Estimated Time**: 3 hours  

## Objective

Create Terraform module for VPC, subnets, route tables, NAT gateways, and internet gateway.

## Prerequisites

- [x] Task 001: Terraform structure setup

## Scope

**Files to Create**:
- `infrastructure/terraform/modules/networking/main.tf`
- `infrastructure/terraform/modules/networking/variables.tf`
- `infrastructure/terraform/modules/networking/outputs.tf`

## Implementation Details

### VPC and Subnets

```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name        = "turaf-vpc-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_subnet" "public" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone = var.availability_zones[count.index]
  
  map_public_ip_on_launch = true
  
  tags = {
    Name = "turaf-public-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "turaf-private-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_subnet" "database" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "turaf-database-${var.availability_zones[count.index]}-${var.environment}"
  }
}
```

### Internet Gateway and NAT

```hcl
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = "turaf-igw-${var.environment}"
  }
}

resource "aws_eip" "nat" {
  count  = length(var.availability_zones)
  domain = "vpc"
  
  tags = {
    Name = "turaf-nat-eip-${var.availability_zones[count.index]}-${var.environment}"
  }
}

resource "aws_nat_gateway" "main" {
  count         = length(var.availability_zones)
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  
  tags = {
    Name = "turaf-nat-${var.availability_zones[count.index]}-${var.environment}"
  }
}
```

## Acceptance Criteria

- [x] VPC created
- [x] Public, private, and database subnets created
- [x] Internet gateway attached
- [x] NAT gateways created
- [x] Route tables configured
- [x] VPC endpoints created
- [x] Module documentation created
- [ ] terraform plan succeeds (requires environment configuration)

## Testing Requirements

**Validation**:
- Run `terraform plan`
- Verify subnet CIDR calculations
- Check route table associations

## Implementation Results (2024-03-23)

### ✅ Module Created

**Files Created**:
- ✅ `infrastructure/terraform/modules/networking/main.tf` (370 lines)
- ✅ `infrastructure/terraform/modules/networking/variables.tf` (80 lines)
- ✅ `infrastructure/terraform/modules/networking/outputs.tf` (110 lines)
- ✅ `infrastructure/terraform/modules/networking/README.md` (comprehensive documentation)

### 🏗️ Infrastructure Components

**VPC**:
- Configurable CIDR block
- DNS hostnames and support enabled
- Environment-specific naming

**Subnets** (3 tiers across multiple AZs):
- **Public subnets**: Internet-facing resources, auto-assign public IPs
- **Private subnets**: Application tier, NAT gateway access
- **Database subnets**: Data tier, isolated from internet

**Networking**:
- **Internet Gateway**: Public subnet internet access
- **NAT Gateways**: One per AZ for high availability
- **Route Tables**: Separate for public, private (per AZ), and database (per AZ)

**VPC Endpoints**:
- **S3** (Gateway): Free, for ECR and application data
- **ECR API** (Interface): Docker registry API
- **ECR DKR** (Interface): Docker image pulls
- **CloudWatch Logs** (Interface): Log streaming
- **Secrets Manager** (Interface): Secret retrieval
- **ECS** (Interface): Container orchestration
- **ECS Telemetry** (Interface): Container metrics

**Security**:
- Dedicated security group for VPC endpoints
- Optional VPC flow logs support
- Network isolation by subnet tier

### 📊 Features

- ✅ **Multi-AZ**: Resources distributed across 2+ availability zones
- ✅ **High Availability**: NAT gateway per AZ (no single point of failure)
- ✅ **Cost Optimization**: NAT gateways can be disabled for dev
- ✅ **Security**: VPC endpoints reduce internet routing
- ✅ **Automatic CIDR**: Subnet CIDRs calculated automatically
- ✅ **Flexible**: Configurable via variables
- ✅ **Tagged**: All resources properly tagged

### 💰 Cost Estimation

**Per Environment** (2 AZs):
- NAT Gateways: ~$65/month
- VPC Endpoints (6 interface): ~$43/month
- **Total**: ~$108/month

**Cost Optimization**:
- Dev: Disable NAT gateways (save $65/month)
- Dev: Use fewer VPC endpoints (save up to $43/month)

### 🎯 Module Inputs

| Variable | Description | Required |
|----------|-------------|----------|
| environment | Environment name (dev/qa/prod) | Yes |
| vpc_cidr | VPC CIDR block | Yes |
| availability_zones | List of AZs (min 2) | Yes |
| enable_nat_gateway | Enable NAT gateways | No (default: true) |
| enable_flow_logs | Enable VPC flow logs | No (default: false) |

### 📤 Module Outputs

- VPC ID and CIDR
- Subnet IDs (public, private, database)
- NAT gateway IDs and EIPs
- Route table IDs
- VPC endpoint IDs
- Security group ID for VPC endpoints

### 📋 Next Steps

1. Create environment-specific configurations (dev, qa, prod)
2. Run `terraform init` to initialize module
3. Run `terraform plan` to validate configuration
4. Apply to dev environment first
5. Proceed to Task 015: Create Security Modules

## References

- Specification: `specs/aws-infrastructure.md` (Networking section)
- Related Tasks: 015-create-security-modules, 019-create-compute-modules
- Module Documentation: `infrastructure/terraform/modules/networking/README.md`
