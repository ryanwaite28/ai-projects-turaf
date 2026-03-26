# Infrastructure Implementation Plan (Generalized Template)

This plan provides a comprehensive, step-by-step guide for setting up all infrastructure resources and configurations required to deploy this event-driven SaaS platform to AWS, applicable to any domain, AWS accounts, and GitHub repository.

---

## How to Use This Template

1. **Fill in the Configuration Variables** section below with your specific values
2. **Follow each phase sequentially**, replacing placeholders with your actual values
3. **Save your configuration** for reference and documentation
4. **Execute commands** after substituting variables

---

## Configuration Variables

Before starting, gather and document these values:

### Domain Configuration
```yaml
DOMAIN_NAME: "example.com"              # Your purchased domain name
DOMAIN_REGISTRAR: "route53"             # Where your domain is registered (route53, godaddy, namecheap, etc.)
EMAIL_PROVIDER: "gmail"                 # Email hosting provider (gmail, outlook, titan, etc.)
ADMIN_EMAIL: "admin@example.com"        # Primary admin email address
```

### AWS Configuration
```yaml
AWS_ORGANIZATION_ID: "o-xxxxxxxxxx"     # Your AWS Organization ID
AWS_ROOT_ACCOUNT_ID: "111111111111"     # Root/Management account ID
AWS_DEV_ACCOUNT_ID: "222222222222"      # Development account ID
AWS_QA_ACCOUNT_ID: "333333333333"       # QA/Staging account ID
AWS_PROD_ACCOUNT_ID: "444444444444"     # Production account ID
AWS_OPS_ACCOUNT_ID: "555555555555"      # DevOps/Operations account ID
AWS_PRIMARY_REGION: "us-east-1"         # Primary AWS region
```

### GitHub Configuration
```yaml
GITHUB_ORG_OR_USER: "your-username"     # GitHub organization or username
GITHUB_REPO_NAME: "your-repo-name"      # Repository name
GITHUB_REPO_URL: "https://github.com/your-username/your-repo-name"
```

### Project Configuration
```yaml
PROJECT_NAME: "myapp"                   # Short project name (lowercase, no spaces)
ENVIRONMENT_PREFIX: "myapp"             # Prefix for AWS resources
```

### Network Configuration
```yaml
VPC_CIDR: "10.0.0.0/16"                # VPC CIDR block
PUBLIC_SUBNET_1_CIDR: "10.0.1.0/24"    # Public subnet AZ1
PUBLIC_SUBNET_2_CIDR: "10.0.2.0/24"    # Public subnet AZ2
PRIVATE_SUBNET_1_CIDR: "10.0.11.0/24"  # Private subnet AZ1
PRIVATE_SUBNET_2_CIDR: "10.0.12.0/24"  # Private subnet AZ2
```

### Service Names (Microservices)
```yaml
SERVICES:
  - identity-service
  - organization-service
  - experiment-service
  - metrics-service
  - communications-service
  - bff-api
  - ws-gateway
```

---

## Prerequisites

**Required Resources**:
- Domain name (registered and accessible)
- AWS Organization with 5 accounts (root, dev, qa, prod, ops)
- Email hosting for your domain
- GitHub repository (public or private)
- Local development environment with:
  - AWS CLI v2
  - Terraform >= 1.5
  - Git
  - GitHub CLI (optional but recommended)

**Required Access**:
- AWS root account credentials
- Domain registrar access
- Email provider admin access
- GitHub repository admin access

---

## Phase 1: Domain and DNS Configuration

### 1.1 Configure Route 53 Hosted Zones

**Objective**: Set up DNS management in AWS Route 53

**Steps**:

1. **Create Public Hosted Zone** (in root account):
   ```bash
   aws route53 create-hosted-zone \
     --name ${DOMAIN_NAME} \
     --caller-reference $(date +%s) \
     --hosted-zone-config Comment="Public hosted zone for ${PROJECT_NAME} platform"
   ```

2. **Note the Name Servers** from the output:
   ```bash
   aws route53 get-hosted-zone --id <HOSTED_ZONE_ID> \
     --query 'DelegationSet.NameServers' --output table
   ```
   
   **Save these 4 nameservers** - you'll need them in the next step.

3. **Update Name Servers at Your Domain Registrar**:
   
   **For Route 53 Registrar**:
   ```bash
   aws route53domains update-domain-nameservers \
     --domain-name ${DOMAIN_NAME} \
     --nameservers \
       Name=ns-1234.awsdns-12.org \
       Name=ns-5678.awsdns-34.co.uk \
       Name=ns-9012.awsdns-56.com \
       Name=ns-3456.awsdns-78.net
   ```
   
   **For Other Registrars** (GoDaddy, Namecheap, etc.):
   - Log into your domain registrar's control panel
   - Navigate to DNS settings for ${DOMAIN_NAME}
   - Replace existing nameservers with the 4 AWS Route 53 nameservers
   - Save changes (propagation takes 24-48 hours)

4. **Verify DNS Delegation** (after propagation):
   ```bash
   dig NS ${DOMAIN_NAME} +short
   nslookup -type=NS ${DOMAIN_NAME}
   ```

### 1.2 Request ACM Certificates

**Objective**: Obtain SSL/TLS certificates for all environments

**Steps**:

1. **Request Wildcard Certificate** (in us-east-1 for CloudFront compatibility):
   ```bash
   # In root account (or each environment account)
   aws acm request-certificate \
     --domain-name "*.${DOMAIN_NAME}" \
     --subject-alternative-names "${DOMAIN_NAME}" \
     --validation-method DNS \
     --region ${AWS_PRIMARY_REGION}
   ```

2. **Add DNS Validation Records**:
   ```bash
   # Get validation records
   aws acm describe-certificate \
     --certificate-arn <CERTIFICATE_ARN> \
     --region ${AWS_PRIMARY_REGION} \
     --query 'Certificate.DomainValidationOptions[*].[ResourceRecord.Name,ResourceRecord.Value]' \
     --output table
   
   # Create validation records in Route 53
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch file://acm-validation-records.json
   ```
   
   **acm-validation-records.json template**:
   ```json
   {
     "Changes": [{
       "Action": "CREATE",
       "ResourceRecordSet": {
         "Name": "_<VALIDATION_NAME>.${DOMAIN_NAME}",
         "Type": "CNAME",
         "TTL": 300,
         "ResourceRecords": [{"Value": "<VALIDATION_VALUE>"}]
       }
     }]
   }
   ```

3. **Wait for Certificate Validation**:
   ```bash
   aws acm wait certificate-validated \
     --certificate-arn <CERTIFICATE_ARN> \
     --region ${AWS_PRIMARY_REGION}
   ```

4. **Repeat for Environment-Specific Wildcards** (optional):
   - `*.dev.${DOMAIN_NAME}`
   - `*.qa.${DOMAIN_NAME}`
   - `*.prod.${DOMAIN_NAME}`

### 1.3 Configure Email Forwarding Aliases

**Objective**: Set up email aliases for platform notifications

**Email Addresses to Configure**:
- `notifications@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `support@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `noreply@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `aws@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `aws-ops@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `aws-dev@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `aws-qa@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}
- `aws-prod@${DOMAIN_NAME}` → forward to ${ADMIN_EMAIL}

**Steps vary by email provider**:

**For Gmail/Google Workspace**:
- Configure email routing rules in Admin Console

**For Microsoft 365/Outlook**:
- Set up mail flow rules in Exchange Admin Center

**For cPanel/Email Hosting**:
- Use Email Forwarders feature in cPanel

**For Titan Email/whois.com**:
- Configure aliases in Titan email control panel

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
   # Get root ID
   ROOT_ID=$(aws organizations list-roots --query 'Roots[0].Id' --output text)
   
   # Create Workloads OU
   aws organizations create-organizational-unit \
     --parent-id ${ROOT_ID} \
     --name "Workloads"
   
   # Create Security OU
   aws organizations create-organizational-unit \
     --parent-id ${ROOT_ID} \
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

4. **Create Service Control Policies** (example):
   ```bash
   # Create SCP to deny CloudTrail deletion
   aws organizations create-policy \
     --name DenyCloudTrailDeletion \
     --description "Prevent deletion of CloudTrail logs" \
     --type SERVICE_CONTROL_POLICY \
     --content file://scp-deny-cloudtrail-deletion.json
   ```

### 2.2 Configure IAM in Each Account

**Accounts to configure**: dev, qa, prod, ops

**Steps for EACH account**:

1. **Switch to the target account**:
   ```bash
   # Configure AWS CLI profile for each account
   aws configure --profile ${PROJECT_NAME}-dev
   # Enter AWS Access Key ID, Secret Access Key, region, output format
   
   # Or use AWS SSO
   aws sso login --profile ${PROJECT_NAME}-dev
   ```

2. **Create GitHub Actions OIDC Provider**:
   ```bash
   aws iam create-open-id-connect-provider \
     --url https://token.actions.githubusercontent.com \
     --client-id-list sts.amazonaws.com \
     --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
     --profile ${PROJECT_NAME}-dev
   ```

3. **Create GitHub Actions IAM Role**:
   
   **github-actions-trust-policy.json**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Principal": {
         "Federated": "arn:aws:iam::${AWS_ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
       },
       "Action": "sts:AssumeRoleWithWebIdentity",
       "Condition": {
         "StringEquals": {
           "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
         },
         "StringLike": {
           "token.actions.githubusercontent.com:sub": "repo:${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}:*"
         }
       }
     }]
   }
   ```
   
   ```bash
   # Create role
   aws iam create-role \
     --role-name GitHubActionsRole-Dev \
     --assume-role-policy-document file://github-actions-trust-policy.json \
     --profile ${PROJECT_NAME}-dev
   
   # Attach policies (adjust permissions as needed)
   aws iam attach-role-policy \
     --role-name GitHubActionsRole-Dev \
     --policy-arn arn:aws:iam::aws:policy/AdministratorAccess \
     --profile ${PROJECT_NAME}-dev
   ```
   
   **Note**: For production, use least-privilege policies instead of AdministratorAccess.

4. **Create ECS Task Execution Role**:
   ```bash
   aws iam create-role \
     --role-name ecsTaskExecutionRole \
     --assume-role-policy-document file://ecs-task-execution-trust-policy.json \
     --profile ${PROJECT_NAME}-dev
   
   aws iam attach-role-policy \
     --role-name ecsTaskExecutionRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy \
     --profile ${PROJECT_NAME}-dev
   ```

5. **Create ECS Task Role** (for application permissions):
   ```bash
   aws iam create-role \
     --role-name ecsTaskRole \
     --assume-role-policy-document file://ecs-task-trust-policy.json \
     --profile ${PROJECT_NAME}-dev
   
   # Attach custom policy for EventBridge, SQS, Secrets Manager, etc.
   aws iam put-role-policy \
     --role-name ecsTaskRole \
     --policy-name ${PROJECT_NAME}ApplicationPolicy \
     --policy-document file://app-policy.json \
     --profile ${PROJECT_NAME}-dev
   ```

6. **Create Lambda Execution Role**:
   ```bash
   aws iam create-role \
     --role-name lambdaExecutionRole \
     --assume-role-policy-document file://lambda-trust-policy.json \
     --profile ${PROJECT_NAME}-dev
   
   aws iam attach-role-policy \
     --role-name lambdaExecutionRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole \
     --profile ${PROJECT_NAME}-dev
   ```

**Repeat steps 1-6 for QA, PROD, and OPS accounts**, adjusting profile names accordingly.

### 2.3 Set Up Amazon SES

**Objective**: Configure email sending for notifications

**Recommended Strategy**: Centralize SES in PROD account, grant cross-account access

**Steps** (in PROD account):

1. **Verify Domain Identity**:
   ```bash
   aws ses verify-domain-identity \
     --domain ${DOMAIN_NAME} \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-prod
   ```

2. **Add DNS Records for Domain Verification**:
   ```bash
   # Get verification token
   VERIFICATION_TOKEN=$(aws ses get-identity-verification-attributes \
     --identities ${DOMAIN_NAME} \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-prod \
     --query "VerificationAttributes.\"${DOMAIN_NAME}\".VerificationToken" \
     --output text)
   
   # Create TXT record in Route 53
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "_amazonses.'${DOMAIN_NAME}'",
           "Type": "TXT",
           "TTL": 300,
           "ResourceRecords": [{"Value": "\"'${VERIFICATION_TOKEN}'\""}]
         }
       }]
     }'
   ```

3. **Configure DKIM**:
   ```bash
   aws ses verify-domain-dkim \
     --domain ${DOMAIN_NAME} \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-prod
   
   # Add 3 CNAME records returned by the command to Route 53
   # Format: <token>._domainkey.${DOMAIN_NAME} CNAME <token>.dkim.amazonses.com
   ```

4. **Add SPF Record**:
   ```bash
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "'${DOMAIN_NAME}'",
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
           "Name": "_dmarc.'${DOMAIN_NAME}'",
           "Type": "TXT",
           "TTL": 300,
           "ResourceRecords": [{"Value": "\"v=DMARC1; p=quarantine; rua=mailto:'${ADMIN_EMAIL}'\""}]
         }
       }]
     }'
   ```

6. **Verify Email Addresses**:
   ```bash
   for email in noreply notifications support; do
     aws ses verify-email-identity \
       --email-address ${email}@${DOMAIN_NAME} \
       --region ${AWS_PRIMARY_REGION} \
       --profile ${PROJECT_NAME}-prod
   done
   ```

7. **Request Production Access** (exit sandbox):
   - Navigate to AWS Console → SES → Account dashboard
   - Click "Request production access"
   - Provide use case: "Transactional emails for SaaS platform"
   - Expected sending volume: estimate based on your needs
   - Describe bounce/complaint handling process

8. **Configure Sending Authorization** (allow other accounts to send):
   
   **ses-cross-account-policy.json**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Principal": {
         "AWS": [
           "arn:aws:iam::${AWS_DEV_ACCOUNT_ID}:root",
           "arn:aws:iam::${AWS_QA_ACCOUNT_ID}:root"
         ]
       },
       "Action": ["ses:SendEmail", "ses:SendRawEmail"],
       "Resource": "arn:aws:ses:${AWS_PRIMARY_REGION}:${AWS_PROD_ACCOUNT_ID}:identity/${DOMAIN_NAME}"
     }]
   }
   ```
   
   ```bash
   aws ses put-identity-policy \
     --identity ${DOMAIN_NAME} \
     --policy-name CrossAccountSending \
     --policy file://ses-cross-account-policy.json \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-prod
   ```

### 2.4 Set Up ECR Repositories

**Steps for EACH account** (dev, qa, prod, ops):

1. **Create ECR Repositories**:
   ```bash
   # For each microservice
   for service in ${SERVICES[@]}; do
     aws ecr create-repository \
       --repository-name ${PROJECT_NAME}/${service} \
       --image-scanning-configuration scanOnPush=true \
       --encryption-configuration encryptionType=AES256 \
       --region ${AWS_PRIMARY_REGION} \
       --profile ${PROJECT_NAME}-dev
   done
   ```

2. **Set Repository Policies** (optional - for cross-account pull):
   
   **ecr-cross-account-policy.json**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Sid": "AllowCrossAccountPull",
       "Effect": "Allow",
       "Principal": {
         "AWS": "arn:aws:iam::${AWS_OPS_ACCOUNT_ID}:root"
       },
       "Action": [
         "ecr:GetDownloadUrlForLayer",
         "ecr:BatchGetImage",
         "ecr:BatchCheckLayerAvailability"
       ]
     }]
   }
   ```
   
   ```bash
   aws ecr set-repository-policy \
     --repository-name ${PROJECT_NAME}/identity-service \
     --policy-text file://ecr-cross-account-policy.json \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-dev
   ```

3. **Configure Lifecycle Policies** (retain last N images):
   
   **ecr-lifecycle-policy.json**:
   ```json
   {
     "rules": [{
       "rulePriority": 1,
       "description": "Keep last 10 images",
       "selection": {
         "tagStatus": "any",
         "countType": "imageCountMoreThan",
         "countNumber": 10
       },
       "action": {
         "type": "expire"
       }
     }]
   }
   ```
   
   ```bash
   for service in ${SERVICES[@]}; do
     aws ecr put-lifecycle-policy \
       --repository-name ${PROJECT_NAME}/${service} \
       --lifecycle-policy-text file://ecr-lifecycle-policy.json \
       --region ${AWS_PRIMARY_REGION} \
       --profile ${PROJECT_NAME}-dev
   done
   ```

---

## Phase 3: Terraform State Backend Setup

### 3.1 Bootstrap Terraform State Infrastructure

**Objective**: Create S3 buckets and DynamoDB tables for Terraform state management

**Steps for EACH account** (dev, qa, prod, ops):

1. **Create S3 Bucket for State**:
   ```bash
   # Set environment variable
   ENV="dev"  # Change to qa, prod, or ops as needed
   
   aws s3api create-bucket \
     --bucket ${PROJECT_NAME}-terraform-state-${ENV} \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-${ENV}
   
   # Enable versioning
   aws s3api put-bucket-versioning \
     --bucket ${PROJECT_NAME}-terraform-state-${ENV} \
     --versioning-configuration Status=Enabled \
     --profile ${PROJECT_NAME}-${ENV}
   
   # Enable encryption
   aws s3api put-bucket-encryption \
     --bucket ${PROJECT_NAME}-terraform-state-${ENV} \
     --server-side-encryption-configuration '{
       "Rules": [{
         "ApplyServerSideEncryptionByDefault": {
           "SSEAlgorithm": "AES256"
         }
       }]
     }' \
     --profile ${PROJECT_NAME}-${ENV}
   
   # Block public access
   aws s3api put-public-access-block \
     --bucket ${PROJECT_NAME}-terraform-state-${ENV} \
     --public-access-block-configuration \
       BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
     --profile ${PROJECT_NAME}-${ENV}
   ```

2. **Create DynamoDB Table for State Locking**:
   ```bash
   aws dynamodb create-table \
     --table-name ${PROJECT_NAME}-terraform-locks-${ENV} \
     --attribute-definitions AttributeName=LockID,AttributeType=S \
     --key-schema AttributeName=LockID,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --region ${AWS_PRIMARY_REGION} \
     --profile ${PROJECT_NAME}-${ENV}
   ```

3. **Create Backend Configuration File**:
   ```bash
   # infrastructure/environments/${ENV}/backend.tf
   cat > infrastructure/environments/${ENV}/backend.tf <<EOF
   terraform {
     backend "s3" {
       bucket         = "${PROJECT_NAME}-terraform-state-${ENV}"
       key            = "${ENV}/terraform.tfstate"
       region         = "${AWS_PRIMARY_REGION}"
       dynamodb_table = "${PROJECT_NAME}-terraform-locks-${ENV}"
       encrypt        = true
     }
   }
   EOF
   ```

**Repeat for all environments**: dev, qa, prod, ops

---

## Phase 4: GitHub Repository Configuration

### 4.1 Configure GitHub Environments

**Steps**:

1. **Create Environments** (via GitHub CLI):
   ```bash
   gh api repos/${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}/environments/dev-environment -X PUT
   gh api repos/${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}/environments/qa-environment -X PUT
   gh api repos/${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}/environments/prod-environment -X PUT
   ```

2. **Configure Environment Protection Rules** (via GitHub UI):
   - **dev-environment**: No protection (auto-deploy)
   - **qa-environment**: 5-minute wait timer
   - **prod-environment**: Required reviewers (2 approvals), deployment branches: `main` only

### 4.2 Configure GitHub Secrets

**Repository Secrets** (available to all workflows):

```bash
# AWS Account IDs
gh secret set AWS_ACCOUNT_ID_DEV --body "${AWS_DEV_ACCOUNT_ID}" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set AWS_ACCOUNT_ID_QA --body "${AWS_QA_ACCOUNT_ID}" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set AWS_ACCOUNT_ID_PROD --body "${AWS_PROD_ACCOUNT_ID}" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set AWS_ACCOUNT_ID_OPS --body "${AWS_OPS_ACCOUNT_ID}" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}

# AWS Region
gh secret set AWS_REGION --body "${AWS_PRIMARY_REGION}" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}

# ECR Registry URLs
gh secret set ECR_REGISTRY_DEV --body "${AWS_DEV_ACCOUNT_ID}.dkr.ecr.${AWS_PRIMARY_REGION}.amazonaws.com" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set ECR_REGISTRY_QA --body "${AWS_QA_ACCOUNT_ID}.dkr.ecr.${AWS_PRIMARY_REGION}.amazonaws.com" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set ECR_REGISTRY_PROD --body "${AWS_PROD_ACCOUNT_ID}.dkr.ecr.${AWS_PRIMARY_REGION}.amazonaws.com" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
```

**Environment-Specific Secrets**:

```bash
# DEV Environment
gh secret set AWS_ROLE_ARN --env dev-environment --body "arn:aws:iam::${AWS_DEV_ACCOUNT_ID}:role/GitHubActionsRole-Dev" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set JWT_SECRET --env dev-environment --body "$(openssl rand -base64 32)" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}

# QA Environment
gh secret set AWS_ROLE_ARN --env qa-environment --body "arn:aws:iam::${AWS_QA_ACCOUNT_ID}:role/GitHubActionsRole-QA" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set JWT_SECRET --env qa-environment --body "$(openssl rand -base64 32)" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}

# PROD Environment
gh secret set AWS_ROLE_ARN --env prod-environment --body "arn:aws:iam::${AWS_PROD_ACCOUNT_ID}:role/GitHubActionsRole-Prod" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
gh secret set JWT_SECRET --env prod-environment --body "$(openssl rand -base64 32)" --repo ${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}
```

**Note**: Database URLs will be set after Terraform deployment.

### 4.3 Configure Branch Protection Rules

**Steps**:

1. **Protect `main` branch**:
   ```bash
   gh api repos/${GITHUB_ORG_OR_USER}/${GITHUB_REPO_NAME}/branches/main/protection -X PUT --input - <<EOF
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

2. **Update Terraform Variables**:
   
   **environments/dev/terraform.tfvars**:
   ```hcl
   environment    = "dev"
   aws_region     = "${AWS_PRIMARY_REGION}"
   aws_account_id = "${AWS_DEV_ACCOUNT_ID}"
   
   # Networking
   vpc_cidr           = "${VPC_CIDR}"
   availability_zones = ["${AWS_PRIMARY_REGION}a", "${AWS_PRIMARY_REGION}b"]
   public_subnets     = ["${PUBLIC_SUBNET_1_CIDR}", "${PUBLIC_SUBNET_2_CIDR}"]
   private_subnets    = ["${PRIVATE_SUBNET_1_CIDR}", "${PRIVATE_SUBNET_2_CIDR}"]
   
   # Compute
   ecs_cluster_name = "${PROJECT_NAME}-cluster-dev"
   
   # Database
   db_instance_class = "db.t3.micro"
   db_name           = "${PROJECT_NAME}"
   db_username       = "${PROJECT_NAME}_admin"
   
   # Domain
   domain_name         = "${DOMAIN_NAME}"
   hosted_zone_id      = "<YOUR_ROUTE53_HOSTED_ZONE_ID>"
   acm_certificate_arn = "<YOUR_ACM_CERTIFICATE_ARN>"
   
   # Project
   project_name = "${PROJECT_NAME}"
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
   export AWS_PROFILE=${PROJECT_NAME}-dev  # or qa, prod
   ```

2. **Plan Infrastructure**:
   ```bash
   cd environments/dev  # or qa, prod
   terraform plan -out=tfplan
   ```

3. **Review Plan** and verify:
   - VPC and subnets configuration
   - Security groups rules
   - RDS instance specifications
   - ECS cluster and services
   - ALB listeners and target groups
   - Lambda functions
   - EventBridge rules
   - S3 buckets
   - IAM roles and policies

4. **Apply Infrastructure**:
   ```bash
   terraform apply tfplan
   ```

5. **Save Outputs**:
   ```bash
   terraform output -json > outputs.json
   ```

### 5.3 Configure DNS Records

**Steps** (after infrastructure deployment):

1. **Create DNS Records for ALB**:
   ```bash
   # Get ALB DNS name from Terraform output
   ALB_DNS=$(terraform output -raw public_alb_dns_name)
   ALB_ZONE_ID=$(terraform output -raw public_alb_zone_id)
   
   # Create A record (alias to ALB)
   aws route53 change-resource-record-sets \
     --hosted-zone-id <YOUR_HOSTED_ZONE_ID> \
     --change-batch '{
       "Changes": [{
         "Action": "CREATE",
         "ResourceRecordSet": {
           "Name": "api.dev.'${DOMAIN_NAME}'",
           "Type": "A",
           "AliasTarget": {
             "HostedZoneId": "'${ALB_ZONE_ID}'",
             "DNSName": "'${ALB_DNS}'",
             "EvaluateTargetHealth": true
           }
         }
       }]
     }'
   ```

2. **Create DNS Records for CloudFront** (after frontend deployment):
   ```bash
   # Similar process for app.dev.${DOMAIN_NAME}
   ```

---

## Phase 6: Database Initialization

### 6.1 Run Database Migrations

**Steps for EACH environment**:

1. **Get RDS Endpoint** from Terraform output:
   ```bash
   RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
   ```

2. **Connect to RDS Instance** (via bastion host or VPN):
   ```bash
   psql -h ${RDS_ENDPOINT} -U ${PROJECT_NAME}_admin -d ${PROJECT_NAME}
   ```

3. **Run Schema Migrations**:
   ```bash
   # Using Flyway
   cd services/identity-service
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://${RDS_ENDPOINT}/${PROJECT_NAME}
   
   # Repeat for each service
   ```

4. **Verify Schema Creation**:
   ```sql
   \dt  -- List all tables
   ```

---

## Phase 7: CI/CD Pipeline Deployment

### 7.1 Deploy GitHub Actions Workflows

**Steps**:

1. **Update Workflow Files** with your values:
   - Replace repository references
   - Update AWS account IDs
   - Update ECR registry URLs
   - Update environment names

2. **Test CI Pipeline**:
   ```bash
   # Create feature branch
   git checkout -b feature/test-ci
   git commit -am "Test CI pipeline"
   git push origin feature/test-ci
   
   # Create PR
   gh pr create --title "Test CI" --body "Testing CI pipeline"
   ```

3. **Test CD Pipeline to DEV**:
   ```bash
   git checkout develop
   git merge feature/test-ci
   git push origin develop
   ```

4. **Test CD Pipeline to QA**:
   ```bash
   git checkout -b release/v1.0.0
   git push origin release/v1.0.0
   ```

5. **Test CD Pipeline to PROD**:
   ```bash
   git checkout main
   git merge release/v1.0.0
   git push origin main
   
   # Manually trigger PROD deployment
   gh workflow run cd-prod.yml
   ```

---

## Phase 8: Monitoring and Observability

### 8.1 Configure CloudWatch Dashboards

**Steps**:

1. **Create Dashboard**:
   ```bash
   aws cloudwatch put-dashboard \
     --dashboard-name ${PROJECT_NAME}-dev-dashboard \
     --dashboard-body file://cloudwatch-dashboard.json \
     --profile ${PROJECT_NAME}-dev
   ```

2. **Configure Alarms**:
   ```bash
   # ECS CPU Utilization
   aws cloudwatch put-metric-alarm \
     --alarm-name ${PROJECT_NAME}-dev-ecs-cpu-high \
     --alarm-description "ECS CPU utilization above 80%" \
     --metric-name CPUUtilization \
     --namespace AWS/ECS \
     --statistic Average \
     --period 300 \
     --threshold 80 \
     --comparison-operator GreaterThanThreshold \
     --evaluation-periods 2 \
     --profile ${PROJECT_NAME}-dev
   ```

3. **Configure SNS Topics** for alerts:
   ```bash
   aws sns create-topic --name ${PROJECT_NAME}-dev-alerts --profile ${PROJECT_NAME}-dev
   aws sns subscribe \
     --topic-arn arn:aws:sns:${AWS_PRIMARY_REGION}:${AWS_DEV_ACCOUNT_ID}:${PROJECT_NAME}-dev-alerts \
     --protocol email \
     --notification-endpoint ${ADMIN_EMAIL} \
     --profile ${PROJECT_NAME}-dev
   ```

### 8.2 Enable X-Ray Tracing

**Steps**:

1. **Verify X-Ray is enabled** in ECS task definitions (configured via Terraform)

2. **View Traces**:
   ```bash
   aws xray get-service-graph \
     --start-time $(date -u -d '1 hour ago' +%s) \
     --end-time $(date -u +%s) \
     --profile ${PROJECT_NAME}-dev
   ```

---

## Phase 9: Security Hardening

### 9.1 Enable AWS Security Services

**Steps for EACH account**:

1. **Enable GuardDuty**:
   ```bash
   aws guardduty create-detector --enable --profile ${PROJECT_NAME}-dev
   ```

2. **Enable Security Hub**:
   ```bash
   aws securityhub enable-security-hub --profile ${PROJECT_NAME}-dev
   ```

3. **Enable AWS Config**:
   ```bash
   aws configservice put-configuration-recorder \
     --configuration-recorder file://config-recorder.json \
     --profile ${PROJECT_NAME}-dev
   
   aws configservice put-delivery-channel \
     --delivery-channel file://delivery-channel.json \
     --profile ${PROJECT_NAME}-dev
   
   aws configservice start-configuration-recorder \
     --configuration-recorder-name default \
     --profile ${PROJECT_NAME}-dev
   ```

4. **Enable CloudTrail** (organization-wide from root account):
   ```bash
   aws cloudtrail create-trail \
     --name ${PROJECT_NAME}-organization-trail \
     --s3-bucket-name ${PROJECT_NAME}-cloudtrail-logs \
     --is-organization-trail \
     --is-multi-region-trail
   
   aws cloudtrail start-logging --name ${PROJECT_NAME}-organization-trail
   ```

### 9.2 Rotate Secrets

**Steps**:

1. **Enable Automatic Rotation** for RDS passwords:
   ```bash
   aws secretsmanager rotate-secret \
     --secret-id ${PROJECT_NAME}/dev/db-password \
     --rotation-lambda-arn <ROTATION_LAMBDA_ARN> \
     --rotation-rules AutomaticallyAfterDays=30 \
     --profile ${PROJECT_NAME}-dev
   ```

---

## Phase 10: Validation and Testing

### 10.1 End-to-End Testing

**Steps**:

1. **Verify Frontend Access**:
   ```bash
   curl -I https://app.dev.${DOMAIN_NAME}
   # Should return 200 OK
   ```

2. **Verify API Access**:
   ```bash
   curl -I https://api.dev.${DOMAIN_NAME}/actuator/health
   # Should return 200 OK
   ```

3. **Test User Registration**:
   ```bash
   curl -X POST https://api.dev.${DOMAIN_NAME}/api/v1/auth/register \
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

### 10.2 Performance Testing

**Steps**:

1. **Load Test API Endpoints**:
   ```bash
   # Using Apache Bench
   ab -n 1000 -c 10 https://api.dev.${DOMAIN_NAME}/api/v1/experiments
   ```

2. **Monitor Metrics** during load test

---

## Appendix A: Configuration Template

Save this as `infrastructure-config.yml` in your project root:

```yaml
# Infrastructure Configuration Template
# Fill in your specific values

domain:
  name: "example.com"
  registrar: "route53"  # route53, godaddy, namecheap, etc.
  hosted_zone_id: "Z1234567890ABC"

email:
  provider: "gmail"  # gmail, outlook, titan, etc.
  admin: "admin@example.com"

aws:
  organization_id: "o-xxxxxxxxxx"
  primary_region: "us-east-1"
  accounts:
    root:
      id: "111111111111"
      email: "aws@example.com"
    dev:
      id: "222222222222"
      email: "aws-dev@example.com"
    qa:
      id: "333333333333"
      email: "aws-qa@example.com"
    prod:
      id: "444444444444"
      email: "aws-prod@example.com"
    ops:
      id: "555555555555"
      email: "aws-ops@example.com"

github:
  org_or_user: "your-username"
  repo_name: "your-repo-name"
  repo_url: "https://github.com/your-username/your-repo-name"

project:
  name: "myapp"
  prefix: "myapp"

network:
  vpc_cidr: "10.0.0.0/16"
  public_subnets:
    - "10.0.1.0/24"
    - "10.0.2.0/24"
  private_subnets:
    - "10.0.11.0/24"
    - "10.0.12.0/24"

services:
  - identity-service
  - organization-service
  - experiment-service
  - metrics-service
  - communications-service
  - bff-api
  - ws-gateway
```

---

## Appendix B: Troubleshooting Guide

### Common Issues

**Issue**: Terraform state lock timeout  
**Solution**:
```bash
terraform force-unlock <LOCK_ID>
```

**Issue**: ECS tasks failing health checks  
**Solution**:
```bash
aws logs tail /ecs/${PROJECT_NAME}-identity-service --follow --profile ${PROJECT_NAME}-dev
aws ec2 describe-security-groups --group-ids <SG_ID> --profile ${PROJECT_NAME}-dev
```

**Issue**: SES emails not sending  
**Solution**:
- Verify domain and email identities
- Check SES sending limits
- Ensure production access granted

**Issue**: GitHub Actions OIDC authentication failing  
**Solution**:
- Verify OIDC provider thumbprint
- Check IAM role trust policy
- Ensure repository name matches exactly

---

## Appendix C: Cost Optimization

### Cost-Saving Strategies

**DEV Environment**:
- Use Fargate Spot
- Single-AZ deployment
- Smaller RDS instance (db.t3.micro)
- Scheduled shutdown during off-hours

**QA Environment**:
- Multi-AZ for production-like testing
- Medium RDS instance (db.t3.small)

**PROD Environment**:
- Reserved instances for predictable workloads
- Auto-scaling for variable load
- Multi-AZ for high availability

### Estimated Monthly Costs

**DEV**: ~$150-200/month  
**QA**: ~$300-400/month  
**PROD**: ~$800-1200/month (varies with usage)

---

## Summary

This generalized infrastructure plan provides a complete, reproducible guide for deploying this event-driven SaaS platform to AWS with your own domain, AWS accounts, and GitHub repository. Follow the phases sequentially, substituting your specific values for the placeholders throughout.

**Key Success Criteria**:
- ✅ All DNS records configured
- ✅ SSL certificates validated
- ✅ SES domain verified
- ✅ All AWS accounts configured
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
5. Document runbooks for operations
