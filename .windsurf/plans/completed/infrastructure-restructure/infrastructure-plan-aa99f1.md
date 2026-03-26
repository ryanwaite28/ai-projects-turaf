# Infrastructure Implementation Plan for Turaf Platform

This plan provides a comprehensive, step-by-step guide for setting up all infrastructure resources and configurations required to deploy the Turaf SaaS platform to AWS, from domain configuration to CI/CD automation.

---

## Prerequisites

**Required Resources**:
- Domain: `turafapp.com` (registered at whois.com)
- Email hosting: `admin@turafapp.com` via titan.email
- AWS Organization with 5 accounts (already created - see AWS_ACCOUNTS.md)
- GitHub repository: https://github.com/ryanwaite28/ai-projects-turaf
- Local development environment with AWS CLI, Terraform, and Git

**Required Access**:
- AWS root account credentials (072456928432)
- whois.com domain management access
- GitHub repository admin access
- AWS CLI configured with appropriate credentials

---

## Phase 1: Domain and DNS Configuration

### 1.1 Configure Route 53 Hosted Zones

**Objective**: Set up DNS management in AWS Route 53

**Steps**:

1. **Create Public Hosted Zone** (in root account 072456928432)
   ```bash
   aws route53 create-hosted-zone \
     --name turafapp.com \
     --caller-reference $(date +%s) \
     --hosted-zone-config Comment="Public hosted zone for Turaf platform"
   ```

2. **Note the Name Servers** from the output:
   ```bash
   aws route53 get-hosted-zone --id <HOSTED_ZONE_ID> \
     --query 'DelegationSet.NameServers' --output table
   ```

3. **Update Name Servers at whois.com**:
   - Log into whois.com domain management
   - Navigate to turafapp.com DNS settings
   - Replace existing nameservers with the 4 AWS Route 53 nameservers
   - Save changes (propagation takes 24-48 hours)

4. **Verify DNS Delegation** (after propagation):
   ```bash
   dig NS turafapp.com +short
   nslookup -type=NS turafapp.com
   ```

### 1.2 Request ACM Certificates

**Objective**: Obtain SSL/TLS certificates for all environments

**Steps**:

1. **Request Wildcard Certificate** (in us-east-1 for CloudFront compatibility):
   ```bash
   # In root account (or each environment account)
   aws acm request-certificate \
     --domain-name "*.turafapp.com" \
     --subject-alternative-names "turafapp.com" \
     --validation-method DNS \
     --region us-east-1
   ```

2. **Add DNS Validation Records**:
   ```bash
   # Get validation records
   aws acm describe-certificate \
     --certificate-arn <CERTIFICATE_ARN> \
     --region us-east-1 \
     --query 'Certificate.DomainValidationOptions[*].[ResourceRecord.Name,ResourceRecord.Value]' \
     --output table
   
   # Create validation records in Route 53
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch file://acm-validation-records.json
   ```

3. **Wait for Certificate Validation**:
   ```bash
   aws acm wait certificate-validated \
     --certificate-arn <CERTIFICATE_ARN> \
     --region us-east-1
   ```

4. **Repeat for Environment-Specific Wildcards** (optional):
   - `*.dev.turafapp.com`
   - `*.qa.turafapp.com`
   - `*.prod.turafapp.com`

### 1.3 Configure Email Forwarding Aliases

**Objective**: Set up email aliases for platform notifications

**Steps**:

1. **Configure at whois.com/titan.email**:
   - `notifications@turafapp.com` → forward to `admin@turafapp.com`
   - `support@turafapp.com` → forward to `admin@turafapp.com`
   - `noreply@turafapp.com` → forward to `admin@turafapp.com`
   - `aws@turafapp.com` → forward to `admin@turafapp.com`
   - `aws-ops@turafapp.com` → forward to `admin@turafapp.com`
   - `aws-dev@turafapp.com` → forward to `admin@turafapp.com`
   - `aws-qa@turafapp.com` → forward to `admin@turafapp.com`
   - `aws-prod@turafapp.com` → forward to `admin@turafapp.com`

---

## Phase 2: AWS Account Configuration

### 2.1 Configure AWS Organization (Root Account)

**Objective**: Set up organizational units and service control policies

**Steps**:

1. **Verify Organization Structure**:
   ```bash
   aws organizations describe-organization
   aws organizations list-accounts
   ```

2. **Create Organizational Units** (if not exists):
   ```bash
   # Create Workloads OU
   aws organizations create-organizational-unit \
     --parent-id r-gs6r \
     --name "Workloads"
   
   # Create Security OU
   aws organizations create-organizational-unit \
     --parent-id r-gs6r \
     --name "Security"
   ```

3. **Enable AWS Services** across organization:
   ```bash
   # Enable CloudTrail
   aws organizations enable-aws-service-access \
     --service-principal cloudtrail.amazonaws.com
   
   # Enable Config
   aws organizations enable-aws-service-access \
     --service-principal config.amazonaws.com
   
   # Enable GuardDuty
   aws organizations enable-aws-service-access \
     --service-principal guardduty.amazonaws.com
   ```

4. **Create Service Control Policies**:
   - Deny deletion of CloudTrail logs
   - Enforce encryption at rest
   - Restrict regions to us-east-1
   - Require MFA for sensitive operations

### 2.2 Configure IAM in Each Account

**Accounts to configure**: dev (801651112319), qa (965932217544), prod (811783768245), ops (146072879609)

**Steps for EACH account**:

1. **Create GitHub Actions OIDC Provider**:
   ```bash
   aws iam create-open-id-connect-provider \
     --url https://token.actions.githubusercontent.com \
     --client-id-list sts.amazonaws.com \
     --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
   ```

2. **Create GitHub Actions IAM Role**:
   ```bash
   # Create trust policy (github-actions-trust-policy.json)
   cat > github-actions-trust-policy.json <<EOF
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Principal": {
           "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
         },
         "Action": "sts:AssumeRoleWithWebIdentity",
         "Condition": {
           "StringEquals": {
             "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
           },
           "StringLike": {
             "token.actions.githubusercontent.com:sub": "repo:ryanwaite28/ai-projects-turaf:*"
           }
         }
       }
     ]
   }
   EOF
   
   # Create role
   aws iam create-role \
     --role-name GitHubActionsRole-<ENV> \
     --assume-role-policy-document file://github-actions-trust-policy.json
   
   # Attach policies
   aws iam attach-role-policy \
     --role-name GitHubActionsRole-<ENV> \
     --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
   ```

3. **Create ECS Task Execution Role**:
   ```bash
   aws iam create-role \
     --role-name ecsTaskExecutionRole \
     --assume-role-policy-document file://ecs-task-execution-trust-policy.json
   
   aws iam attach-role-policy \
     --role-name ecsTaskExecutionRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
   ```

4. **Create ECS Task Role** (for application permissions):
   ```bash
   aws iam create-role \
     --role-name ecsTaskRole \
     --assume-role-policy-document file://ecs-task-trust-policy.json
   
   # Attach custom policy for EventBridge, SQS, Secrets Manager, etc.
   aws iam put-role-policy \
     --role-name ecsTaskRole \
     --policy-name TurafApplicationPolicy \
     --policy-document file://turaf-app-policy.json
   ```

5. **Create Lambda Execution Role**:
   ```bash
   aws iam create-role \
     --role-name lambdaExecutionRole \
     --assume-role-policy-document file://lambda-trust-policy.json
   
   aws iam attach-role-policy \
     --role-name lambdaExecutionRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole
   ```

### 2.3 Set Up Amazon SES

**Objective**: Configure email sending for notifications (multi-account strategy: centralized in prod account)

**Steps** (in PROD account 811783768245):

1. **Verify Domain Identity**:
   ```bash
   aws ses verify-domain-identity \
     --domain turafapp.com \
     --region us-east-1
   ```

2. **Add DNS Records for Domain Verification**:
   ```bash
   # Get verification token
   aws ses get-identity-verification-attributes \
     --identities turafapp.com \
     --region us-east-1
   
   # Create TXT record in Route 53
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "_amazonses.turafapp.com",
           "Type": "TXT",
           "TTL": 300,
           "ResourceRecords": [{"Value": "\"<VERIFICATION_TOKEN>\""}]
         }
       }]
     }'
   ```

3. **Configure DKIM**:
   ```bash
   aws ses verify-domain-dkim \
     --domain turafapp.com \
     --region us-east-1
   
   # Add 3 CNAME records returned by the command to Route 53
   ```

4. **Add SPF Record**:
   ```bash
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "turafapp.com",
           "Type": "TXT",
           "TTL": 300,
           "ResourceRecords": [{"Value": "\"v=spf1 include:amazonses.com ~all\""}]
         }
       }]
     }'
   ```

5. **Add DMARC Record**:
   ```bash
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "_dmarc.turafapp.com",
           "Type": "TXT",
           "TTL": 300,
           "ResourceRecords": [{"Value": "\"v=DMARC1; p=quarantine; rua=mailto:admin@turafapp.com\""}]
         }
       }]
     }'
   ```

6. **Verify Email Addresses**:
   ```bash
   aws ses verify-email-identity \
     --email-address noreply@turafapp.com \
     --region us-east-1
   
   aws ses verify-email-identity \
     --email-address notifications@turafapp.com \
     --region us-east-1
   
   aws ses verify-email-identity \
     --email-address support@turafapp.com \
     --region us-east-1
   ```

7. **Request Production Access** (exit sandbox):
   ```bash
   # Submit request via AWS Console
   # Navigate to SES → Account dashboard → Request production access
   # Provide use case: "Transactional emails for SaaS platform (experiment notifications, reports)"
   # Expected sending volume: 10,000 emails/day
   # Bounce/complaint handling: Automated via SNS
   ```

8. **Configure Sending Authorization** (allow other accounts to send):
   ```bash
   # Create SES sending authorization policy
   aws ses put-identity-policy \
     --identity turafapp.com \
     --policy-name CrossAccountSending \
     --policy file://ses-cross-account-policy.json \
     --region us-east-1
   ```

### 2.4 Set Up ECR Repositories

**Steps for EACH account** (dev, qa, prod, ops):

1. **Create ECR Repositories**:
   ```bash
   # For each microservice
   for service in identity-service organization-service experiment-service metrics-service communications-service bff-api ws-gateway; do
     aws ecr create-repository \
       --repository-name turaf/$service \
       --image-scanning-configuration scanOnPush=true \
       --encryption-configuration encryptionType=AES256 \
       --region us-east-1
   done
   ```

2. **Set Repository Policies** (allow cross-account pull from ops account):
   ```bash
   aws ecr set-repository-policy \
     --repository-name turaf/identity-service \
     --policy-text file://ecr-cross-account-policy.json \
     --region us-east-1
   ```

3. **Configure Lifecycle Policies** (retain last 10 images):
   ```bash
   aws ecr put-lifecycle-policy \
     --repository-name turaf/identity-service \
     --lifecycle-policy-text file://ecr-lifecycle-policy.json \
     --region us-east-1
   ```

---

## Phase 3: Terraform State Backend Setup

### 3.1 Bootstrap Terraform State Infrastructure

**Objective**: Create S3 buckets and DynamoDB tables for Terraform state management

**Steps for EACH account** (dev, qa, prod, ops):

1. **Create S3 Bucket for State**:
   ```bash
   # Replace <ENV> with dev, qa, prod, or ops
   # Replace <ACCOUNT_ID> with the account ID
   
   aws s3api create-bucket \
     --bucket turaf-terraform-state-<ENV> \
     --region us-east-1
   
   # Enable versioning
   aws s3api put-bucket-versioning \
     --bucket turaf-terraform-state-<ENV> \
     --versioning-configuration Status=Enabled
   
   # Enable encryption
   aws s3api put-bucket-encryption \
     --bucket turaf-terraform-state-<ENV> \
     --server-side-encryption-configuration '{
       "Rules": [{
         "ApplyServerSideEncryptionByDefault": {
           "SSEAlgorithm": "AES256"
         }
       }]
     }'
   
   # Block public access
   aws s3api put-public-access-block \
     --bucket turaf-terraform-state-<ENV> \
     --public-access-block-configuration \
       BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

2. **Create DynamoDB Table for State Locking**:
   ```bash
   aws dynamodb create-table \
     --table-name turaf-terraform-locks-<ENV> \
     --attribute-definitions AttributeName=LockID,AttributeType=S \
     --key-schema AttributeName=LockID,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --region us-east-1
   ```

3. **Create Backend Configuration File**:
   ```bash
   # infrastructure/environments/<ENV>/backend.tf
   cat > infrastructure/environments/<ENV>/backend.tf <<EOF
   terraform {
     backend "s3" {
       bucket         = "turaf-terraform-state-<ENV>"
       key            = "<ENV>/terraform.tfstate"
       region         = "us-east-1"
       dynamodb_table = "turaf-terraform-locks-<ENV>"
       encrypt        = true
     }
   }
   EOF
   ```

---

## Phase 4: GitHub Repository Configuration

### 4.1 Configure GitHub Environments

**Steps**:

1. **Create Environments** (via GitHub UI or API):
   ```bash
   # Using GitHub CLI
   gh api repos/ryanwaite28/ai-projects-turaf/environments/dev-environment -X PUT
   gh api repos/ryanwaite28/ai-projects-turaf/environments/qa-environment -X PUT
   gh api repos/ryanwaite28/ai-projects-turaf/environments/prod-environment -X PUT
   ```

2. **Configure Environment Protection Rules**:
   - **dev-environment**: No protection (auto-deploy)
   - **qa-environment**: 5-minute wait timer
   - **prod-environment**: Required reviewers (2 approvals), deployment branches: `main` only

### 4.2 Configure GitHub Secrets

**Repository Secrets** (available to all workflows):

```bash
# AWS Account IDs
gh secret set AWS_ACCOUNT_ID_DEV --body "801651112319"
gh secret set AWS_ACCOUNT_ID_QA --body "965932217544"
gh secret set AWS_ACCOUNT_ID_PROD --body "811783768245"
gh secret set AWS_ACCOUNT_ID_OPS --body "146072879609"

# AWS Regions
gh secret set AWS_REGION --body "us-east-1"

# ECR Registry URLs
gh secret set ECR_REGISTRY_DEV --body "801651112319.dkr.ecr.us-east-1.amazonaws.com"
gh secret set ECR_REGISTRY_QA --body "965932217544.dkr.ecr.us-east-1.amazonaws.com"
gh secret set ECR_REGISTRY_PROD --body "811783768245.dkr.ecr.us-east-1.amazonaws.com"
```

**Environment-Specific Secrets**:

```bash
# DEV Environment
gh secret set AWS_ROLE_ARN --env dev-environment --body "arn:aws:iam::801651112319:role/GitHubActionsRole-Dev"
gh secret set DATABASE_URL --env dev-environment --body "<TO_BE_SET_AFTER_TERRAFORM>"
gh secret set JWT_SECRET --env dev-environment --body "<GENERATE_RANDOM_SECRET>"

# QA Environment
gh secret set AWS_ROLE_ARN --env qa-environment --body "arn:aws:iam::965932217544:role/GitHubActionsRole-QA"
gh secret set DATABASE_URL --env qa-environment --body "<TO_BE_SET_AFTER_TERRAFORM>"
gh secret set JWT_SECRET --env qa-environment --body "<GENERATE_RANDOM_SECRET>"

# PROD Environment
gh secret set AWS_ROLE_ARN --env prod-environment --body "arn:aws:iam::811783768245:role/GitHubActionsRole-Prod"
gh secret set DATABASE_URL --env prod-environment --body "<TO_BE_SET_AFTER_TERRAFORM>"
gh secret set JWT_SECRET --env prod-environment --body "<GENERATE_RANDOM_SECRET>"
```

### 4.3 Configure Branch Protection Rules

**Steps**:

1. **Protect `main` branch**:
   ```bash
   gh api repos/ryanwaite28/ai-projects-turaf/branches/main/protection -X PUT --input - <<EOF
   {
     "required_status_checks": {
       "strict": true,
       "contexts": ["lint-java", "lint-angular", "test-java", "test-angular", "security-scan"]
     },
     "enforce_admins": true,
     "required_pull_request_reviews": {
       "required_approving_review_count": 2,
       "dismiss_stale_reviews": true
     },
     "restrictions": null,
     "required_linear_history": true,
     "allow_force_pushes": false,
     "allow_deletions": false
   }
   EOF
   ```

2. **Protect `develop` branch** (similar but 1 approval):
   ```bash
   # Similar configuration with required_approving_review_count: 1
   ```

---

## Phase 5: Terraform Infrastructure Implementation

### 5.1 Initialize Terraform Structure

**Objective**: Set up Terraform modules and environment configurations

**Steps**:

1. **Navigate to infrastructure directory**:
   ```bash
   cd infrastructure/terraform
   ```

2. **Create Module Structure** (if not exists):
   ```bash
   mkdir -p modules/{networking,compute,database,storage,messaging,lambda,monitoring,security}
   mkdir -p environments/{dev,qa,prod}
   ```

3. **Initialize Terraform** for each environment:
   ```bash
   # DEV
   cd environments/dev
   terraform init
   
   # QA
   cd ../qa
   terraform init
   
   # PROD
   cd ../prod
   terraform init
   ```

### 5.2 Deploy Infrastructure by Environment

**Deployment Order**: DEV → QA → PROD

**Steps for EACH environment**:

1. **Configure AWS Credentials**:
   ```bash
   # Assume GitHub Actions role or use AWS SSO
   aws sts assume-role \
     --role-arn arn:aws:iam::<ACCOUNT_ID>:role/GitHubActionsRole-<ENV> \
     --role-session-name terraform-deployment
   ```

2. **Create terraform.tfvars**:
   ```hcl
   # environments/<ENV>/terraform.tfvars
   environment = "<ENV>"
   aws_region  = "us-east-1"
   aws_account_id = "<ACCOUNT_ID>"
   
   # Networking
   vpc_cidr = "10.0.0.0/16"
   availability_zones = ["us-east-1a", "us-east-1b"]
   
   # Compute
   ecs_cluster_name = "turaf-cluster-<ENV>"
   
   # Database
   db_instance_class = "db.t3.micro"  # dev/qa
   # db_instance_class = "db.r5.large"  # prod
   db_name = "turaf"
   db_username = "turaf_admin"
   
   # Domain
   domain_name = "turafapp.com"
   hosted_zone_id = "<ROUTE53_HOSTED_ZONE_ID>"
   acm_certificate_arn = "<ACM_CERTIFICATE_ARN>"
   ```

3. **Plan Infrastructure**:
   ```bash
   cd environments/<ENV>
   terraform plan -out=tfplan
   ```

4. **Review Plan** and verify:
   - VPC and subnets configuration
   - Security groups rules
   - RDS instance specifications
   - ECS cluster and services
   - ALB listeners and target groups
   - Lambda functions
   - EventBridge rules
   - S3 buckets
   - IAM roles and policies

5. **Apply Infrastructure**:
   ```bash
   terraform apply tfplan
   ```

6. **Save Outputs**:
   ```bash
   terraform output -json > outputs.json
   
   # Key outputs to save:
   # - vpc_id
   # - private_subnet_ids
   # - public_subnet_ids
   # - rds_endpoint
   # - alb_dns_name
   # - ecr_repository_urls
   ```

### 5.3 Configure DNS Records

**Steps** (after infrastructure deployment):

1. **Create DNS Records for ALB**:
   ```bash
   # Get ALB DNS name from Terraform output
   ALB_DNS=$(terraform output -raw alb_dns_name)
   
   # Create A record (alias to ALB)
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "api.<ENV>.turafapp.com",
           "Type": "A",
           "AliasTarget": {
             "HostedZoneId": "<ALB_HOSTED_ZONE_ID>",
             "DNSName": "'"$ALB_DNS"'",
             "EvaluateTargetHealth": true
           }
         }
       }]
     }'
   ```

2. **Create DNS Records for CloudFront** (after frontend deployment):
   ```bash
   # Similar process for app.<ENV>.turafapp.com
   ```

---

## Phase 6: Database Initialization

### 6.1 Run Database Migrations

**Steps for EACH environment**:

1. **Connect to RDS Instance** (via bastion host or VPN):
   ```bash
   # Get RDS endpoint from Terraform output
   RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
   
   # Connect using psql
   psql -h $RDS_ENDPOINT -U turaf_admin -d turaf
   ```

2. **Run Schema Migrations**:
   ```bash
   # Using Flyway or Liquibase
   cd services/identity-service
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://$RDS_ENDPOINT/turaf
   
   # Repeat for each service
   ```

3. **Verify Schema Creation**:
   ```sql
   \dt  -- List all tables
   SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema');
   ```

---

## Phase 7: CI/CD Pipeline Deployment

### 7.1 Deploy GitHub Actions Workflows

**Steps**:

1. **Create Workflow Files** (if not exists):
   ```bash
   mkdir -p .github/workflows
   ```

2. **Test CI Pipeline**:
   ```bash
   # Create feature branch
   git checkout -b feature/test-ci
   
   # Make a small change and push
   git commit -am "Test CI pipeline"
   git push origin feature/test-ci
   
   # Create PR and verify CI runs
   gh pr create --title "Test CI" --body "Testing CI pipeline"
   ```

3. **Test CD Pipeline to DEV**:
   ```bash
   # Merge to develop branch
   git checkout develop
   git merge feature/test-ci
   git push origin develop
   
   # Verify automatic deployment to DEV
   gh run list --workflow=cd-dev.yml
   ```

4. **Test CD Pipeline to QA**:
   ```bash
   # Create release branch
   git checkout -b release/v1.0.0
   git push origin release/v1.0.0
   
   # Verify automatic deployment to QA
   gh run list --workflow=cd-qa.yml
   ```

5. **Test CD Pipeline to PROD**:
   ```bash
   # Merge to main
   git checkout main
   git merge release/v1.0.0
   git push origin main
   
   # Manually trigger PROD deployment
   gh workflow run cd-prod.yml
   
   # Approve deployment in GitHub UI
   ```

---

## Phase 8: Monitoring and Observability

### 8.1 Configure CloudWatch Dashboards

**Steps**:

1. **Create Dashboard**:
   ```bash
   aws cloudwatch put-dashboard \
     --dashboard-name turaf-<ENV>-dashboard \
     --dashboard-body file://cloudwatch-dashboard.json
   ```

2. **Configure Alarms**:
   ```bash
   # ECS CPU Utilization
   aws cloudwatch put-metric-alarm \
     --alarm-name turaf-<ENV>-ecs-cpu-high \
     --alarm-description "ECS CPU utilization above 80%" \
     --metric-name CPUUtilization \
     --namespace AWS/ECS \
     --statistic Average \
     --period 300 \
     --threshold 80 \
     --comparison-operator GreaterThanThreshold \
     --evaluation-periods 2
   
   # RDS Connections
   # Lambda Errors
   # ALB 5xx Errors
   ```

3. **Configure SNS Topics** for alerts:
   ```bash
   aws sns create-topic --name turaf-<ENV>-alerts
   aws sns subscribe \
     --topic-arn arn:aws:sns:us-east-1:<ACCOUNT_ID>:turaf-<ENV>-alerts \
     --protocol email \
     --notification-endpoint admin@turafapp.com
   ```

### 8.2 Enable X-Ray Tracing

**Steps**:

1. **Enable X-Ray in ECS Task Definitions** (via Terraform):
   ```hcl
   # Already configured in task definitions
   ```

2. **Verify Traces**:
   ```bash
   aws xray get-service-graph \
     --start-time $(date -u -d '1 hour ago' +%s) \
     --end-time $(date -u +%s)
   ```

---

## Phase 9: Security Hardening

### 9.1 Enable AWS Security Services

**Steps**:

1. **Enable GuardDuty** (in each account):
   ```bash
   aws guardduty create-detector --enable
   ```

2. **Enable Security Hub**:
   ```bash
   aws securityhub enable-security-hub
   ```

3. **Enable AWS Config**:
   ```bash
   aws configservice put-configuration-recorder \
     --configuration-recorder file://config-recorder.json
   
   aws configservice put-delivery-channel \
     --delivery-channel file://delivery-channel.json
   
   aws configservice start-configuration-recorder \
     --configuration-recorder-name default
   ```

4. **Enable CloudTrail** (organization-wide):
   ```bash
   aws cloudtrail create-trail \
     --name turaf-organization-trail \
     --s3-bucket-name turaf-cloudtrail-logs \
     --is-organization-trail \
     --is-multi-region-trail
   
   aws cloudtrail start-logging --name turaf-organization-trail
   ```

### 9.2 Rotate Secrets

**Steps**:

1. **Enable Automatic Rotation** for RDS passwords:
   ```bash
   aws secretsmanager rotate-secret \
     --secret-id turaf/<ENV>/db-password \
     --rotation-lambda-arn <ROTATION_LAMBDA_ARN> \
     --rotation-rules AutomaticallyAfterDays=30
   ```

2. **Rotate JWT Secrets** periodically (manual process with zero-downtime deployment)

---

## Phase 10: Validation and Testing

### 10.1 End-to-End Testing

**Steps**:

1. **Verify Frontend Access**:
   ```bash
   curl -I https://app.dev.turafapp.com
   # Should return 200 OK
   ```

2. **Verify API Access**:
   ```bash
   curl -I https://api.dev.turafapp.com/actuator/health
   # Should return 200 OK with health status
   ```

3. **Test User Registration**:
   ```bash
   curl -X POST https://api.dev.turafapp.com/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "SecurePassword123!",
       "firstName": "Test",
       "lastName": "User"
     }'
   ```

4. **Test Event Flow**:
   - Create experiment
   - Record metrics
   - Complete experiment
   - Verify report generation
   - Verify notification sent

5. **Test WebSocket Connection**:
   ```bash
   # Use wscat or similar tool
   wscat -c wss://ws.dev.turafapp.com
   ```

### 10.2 Performance Testing

**Steps**:

1. **Load Test API Endpoints**:
   ```bash
   # Using Apache Bench
   ab -n 1000 -c 10 https://api.dev.turafapp.com/api/v1/experiments
   ```

2. **Monitor Metrics** during load test:
   - ECS CPU/Memory utilization
   - RDS connections
   - ALB response times
   - Lambda invocations

---

## Appendix A: Resource Inventory

### A.1 AWS Resources by Environment

**DEV (801651112319)**:
- VPC: 10.0.0.0/16
- ECS Cluster: turaf-cluster-dev
- RDS: turaf-dev.xxxxx.us-east-1.rds.amazonaws.com
- ALB: turaf-alb-dev-xxxxx.us-east-1.elb.amazonaws.com
- S3: turaf-reports-dev, turaf-terraform-state-dev
- ECR: 7 repositories

**QA (965932217544)**:
- Similar structure with -qa suffix

**PROD (811783768245)**:
- Similar structure with -prod suffix
- Enhanced monitoring and auto-scaling

**Ops (146072879609)**:
- Centralized logging
- CI/CD artifacts
- Terraform state for ops tooling

### A.2 Email Addresses

**AWS Account Management**:
- aws@turafapp.com (root)
- aws-ops@turafapp.com (ops)
- aws-dev@turafapp.com (dev)
- aws-qa@turafapp.com (qa)
- aws-prod@turafapp.com (prod)

**Application Emails** (SES):
- noreply@turafapp.com (transactional emails)
- notifications@turafapp.com (experiment notifications)
- support@turafapp.com (user support)

### A.3 DNS Records

**Public Records**:
- turafapp.com (A → CloudFront)
- api.dev.turafapp.com (A → ALB)
- api.qa.turafapp.com (A → ALB)
- api.turafapp.com (A → ALB)
- app.dev.turafapp.com (A → CloudFront)
- app.qa.turafapp.com (A → CloudFront)
- app.turafapp.com (A → CloudFront)

**Email Records**:
- _amazonses.turafapp.com (TXT for SES verification)
- 3x CNAME for DKIM
- TXT for SPF
- _dmarc.turafapp.com (TXT for DMARC)

---

## Appendix B: Troubleshooting Guide

### B.1 Common Issues

**Issue**: Terraform state lock timeout
**Solution**: 
```bash
# Force unlock (use with caution)
terraform force-unlock <LOCK_ID>
```

**Issue**: ECS tasks failing health checks
**Solution**:
```bash
# Check task logs
aws logs tail /ecs/turaf-identity-service --follow

# Verify security group rules
aws ec2 describe-security-groups --group-ids <SG_ID>
```

**Issue**: SES emails not sending
**Solution**:
- Verify domain and email identities
- Check SES sending limits
- Review bounce/complaint rates
- Ensure production access granted

**Issue**: GitHub Actions OIDC authentication failing
**Solution**:
- Verify OIDC provider thumbprint
- Check IAM role trust policy
- Ensure repository name matches exactly

---

## Appendix C: Cost Optimization

### C.1 Cost-Saving Strategies

**DEV Environment**:
- Use Fargate Spot for non-critical services
- Single-AZ deployment
- Smaller RDS instance (db.t3.micro)
- Scheduled shutdown during off-hours

**QA Environment**:
- Multi-AZ for production-like testing
- Medium RDS instance (db.t3.small)
- On-demand capacity

**PROD Environment**:
- Reserved instances for predictable workloads
- Auto-scaling for variable load
- Multi-AZ for high availability
- CloudWatch cost anomaly detection

### C.2 Monthly Cost Estimates

**DEV**: ~$150-200/month
**QA**: ~$300-400/month
**PROD**: ~$800-1200/month (varies with usage)

---

## Summary

This infrastructure plan provides a complete, reproducible guide for deploying the Turaf platform to AWS. Follow the phases sequentially, starting with domain/DNS configuration, then AWS account setup, Terraform infrastructure deployment, and finally CI/CD automation. Each step includes both high-level context and detailed CLI commands for execution.

**Key Success Criteria**:
- ✅ All DNS records configured and propagating
- ✅ SSL certificates validated and attached to ALBs
- ✅ SES domain verified and in production mode
- ✅ All AWS accounts configured with IAM roles
- ✅ Terraform state backends operational
- ✅ Infrastructure deployed to all environments
- ✅ CI/CD pipelines functional
- ✅ End-to-end tests passing
- ✅ Monitoring and alerting active

**Next Steps After Completion**:
1. Deploy application code via CI/CD
2. Run database migrations
3. Configure application secrets
4. Perform load testing
5. Document runbooks for operations team
