# Turaf Development Infrastructure - Deployment Details

**Deployment Date**: March 24, 2026  
**Environment**: Development (dev)  
**AWS Account**: 801651112319  
**Region**: us-east-1

## Infrastructure Components

### VPC and Networking

- **VPC ID**: `vpc-0eb73410956d368a8`
- **VPC CIDR**: `10.0.0.0/16`
- **Public Subnets**:
  - `subnet-0745ca1dadd4723b4` (us-east-1a)
  - `subnet-059a1a88c62c3e8c7` (us-east-1b)
- **Private Subnets**:
  - `subnet-0fbca1c0741c511bc` (us-east-1a)
  - `subnet-0a7e1733037f31e69` (us-east-1b)
- **Database Subnets**:
  - `subnet-0e5b26fc43803ca86` (us-east-1a)
  - `subnet-0ae5e2889493ca0ad` (us-east-1b)
- **NAT Gateway**: `nat-071665cb67dabc8f6`
- **Internet Gateway**: `igw-0f4091b162b12e4db`

### RDS PostgreSQL Database

- **Instance ID**: `db-3CXB74UPESYRYW7OVYSEE2VZOI`
- **Identifier**: `turaf-postgres-dev`
- **Endpoint**: `turaf-postgres-dev.cm7cimwey834.us-east-1.rds.amazonaws.com:5432`
- **Address**: `turaf-postgres-dev.cm7cimwey834.us-east-1.rds.amazonaws.com`
- **Port**: `5432`
- **Database Name**: `turaf`
- **Engine**: PostgreSQL 15
- **Instance Class**: db.t3.micro
- **Storage**: 20 GB (gp3, encrypted)
- **Multi-AZ**: Disabled (cost optimization)
- **Backup Retention**: 1 day

### Security Groups

- **RDS Security Group**: `sg-0700dfd644af580af`
  - Allows PostgreSQL (5432) from VPC CIDR
  - Allows PostgreSQL from CodeBuild security group
  
- **CodeBuild Security Group**: `sg-01b1f0d32cf32bd22`
  - For Flyway migration CodeBuild projects
  - Has egress to RDS security group

### Encryption

- **KMS Key ID**: `3f7c4bde-ae45-4a4e-bbad-298a51bc68a3`
- **KMS Key ARN**: `arn:aws:kms:us-east-1:801651112319:key/3f7c4bde-ae45-4a4e-bbad-298a51bc68a3`
- **KMS Alias**: `alias/turaf-rds-dev`

### Secrets Manager

- **Master Credentials Secret ARN**: `arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/admin-20260324134423738900000001-Wtw0q2`
- **Secret Name**: `turaf/dev/rds/admin-20260324134423738900000001`

## Retrieve Database Credentials

```bash
aws secretsmanager get-secret-value \
  --secret-id arn:aws:secretsmanager:us-east-1:801651112319:secret:turaf/dev/rds/admin-20260324134423738900000001-Wtw0q2 \
  --query SecretString --output text | jq .
```

## Task 027 Configuration

For **Task 027: Configure Database Migration Network Access**, use the following:

### CodeBuild Network Configuration

```yaml
VPC Configuration:
  VPC ID: vpc-0eb73410956d368a8
  Subnets: 
    - subnet-0fbca1c0741c511bc  # Private subnet 1
    - subnet-0a7e1733037f31e69  # Private subnet 2
  Security Groups:
    - sg-01b1f0d32cf32bd22      # CodeBuild security group
```

### Database Connection Details

```yaml
Database:
  Host: turaf-postgres-dev.cm7cimwey834.us-east-1.rds.amazonaws.com
  Port: 5432
  Database: turaf
  Username: turaf_admin
  Password: <retrieve from Secrets Manager>
  Security Group: sg-0700dfd644af580af
```

## Cost Estimate

**Monthly Cost**: ~$15/month
- RDS PostgreSQL (db.t3.micro): ~$12/month (Free Tier eligible for first 12 months)
- NAT Gateway: ~$3/month

## Terraform State

- **Backend**: Local (file-based)
- **State File**: `infrastructure/terraform/standalone/dev-vpc-rds/terraform.tfstate`

## Next Steps

1. ✅ VPC and RDS infrastructure deployed
2. Configure Flyway CodeBuild project for database migrations (Task 028)
3. Run initial database migrations to create schemas
4. Create database users for each microservice (Task 025)
5. Configure application connection strings using Secrets Manager

## Cleanup

To destroy this infrastructure:

```bash
cd infrastructure/terraform/standalone/dev-vpc-rds
terraform destroy
```

**Warning**: This will delete the RDS instance and all data. Ensure you have backups if needed.
