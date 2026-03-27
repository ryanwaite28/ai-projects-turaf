# Infrastructure Implementation Plan for Turaf Platform

This plan provides a comprehensive, step-by-step guide for setting up all infrastructure resources and configurations required to deploy the Turaf SaaS platform to AWS, from domain configuration to CI/CD automation.

---

## Phase 0: Local Environment Setup

Before implementing any infrastructure, you must set up your local development environment with the necessary tools and credentials.

### 0.1 Install Required CLI Tools

**Objective**: Install all command-line tools needed for infrastructure management

#### macOS Installation (using Homebrew)

1. **Install Homebrew** (if not already installed):
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install AWS CLI**:
   ```bash
   brew install awscli
   
   # Verify installation
   aws --version
   # Expected output: aws-cli/2.x.x Python/3.x.x Darwin/xx.x.x
   ```

3. **Install Terraform**:
   ```bash
   brew tap hashicorp/tap
   brew install hashicorp/tap/terraform
   
   # Verify installation
   terraform --version
   # Expected output: Terraform v1.x.x
   ```

4. **Install GitHub CLI**:
   ```bash
   brew install gh
   
   # Verify installation
   gh --version
   # Expected output: gh version 2.x.x
   ```

5. **Install jq** (JSON processor for AWS CLI output):
   ```bash
   brew install jq
   
   # Verify installation
   jq --version
   # Expected output: jq-1.x
   ```

6. **Install Additional Utilities**:
   ```bash
   # For DNS verification
   brew install bind  # Provides dig and nslookup
   
   # For Git operations
   brew install git
   git --version
   ```

#### Linux Installation (Ubuntu/Debian)

```bash
# AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Terraform
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform

# GitHub CLI
type -p curl >/dev/null || sudo apt install curl -y
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update && sudo apt install gh -y

# jq and utilities
sudo apt install jq dnsutils git -y
```

### 0.2 Obtain AWS Credentials

**Objective**: Get access credentials for all AWS accounts

#### Option A: IAM User with Access Keys (Simpler, for individual use)

1. **Log into AWS Console** for the root account (072456928432):
   - Navigate to: https://console.aws.amazon.com/
   - Use root account email: `aws@turafapp.com`

2. **Create IAM User**:
   - Go to IAM → Users → Create user
   - Username: `infrastructure-admin` (or your name)
   - Enable "Provide user access to the AWS Management Console" (optional)
   - Click Next

3. **Attach Permissions**:
   - Select "Attach policies directly"
   - Attach: `AdministratorAccess` (for infrastructure setup)
   - Click Next → Create user

4. **Create Access Keys**:
   - Click on the newly created user
   - Go to "Security credentials" tab
   - Under "Access keys", click "Create access key"
   - Select use case: "Command Line Interface (CLI)"
   - Acknowledge the recommendation
   - Click "Create access key"
   - **IMPORTANT**: Download the CSV or copy both:
     - Access key ID (e.g., `AKIAIOSFODNN7EXAMPLE`)
     - Secret access key (e.g., `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`)
   - Store these securely (you won't be able to see the secret again)

5. **Repeat for Other Accounts** (Ops, Dev, QA, Prod):
   - Log into each account using the account-specific email
   - Create IAM user with AdministratorAccess
   - Generate access keys for each

#### Option B: IAM Identity Center (SSO) - Recommended for Organizations

This option is more secure and scalable, especially for managing access across multiple AWS accounts in an organization.

**Prerequisites**:
- AWS Organizations must be enabled (already done - see AWS_ACCOUNTS.md)
- Access to the management/root account (072456928432)

##### Step 1: Enable IAM Identity Center

1. **Log into AWS Console** (root account 072456928432):
   - Navigate to: https://console.aws.amazon.com/
   - Use root account email: `aws@turafapp.com`

2. **Access IAM Identity Center**:
   - In the AWS Console search bar, type "IAM Identity Center"
   - Click on "IAM Identity Center" service
   - Or navigate directly to: https://console.aws.amazon.com/singlesignon

3. **Enable IAM Identity Center**:
   - Click "Enable" button
   - **Region Selection**: Choose `us-east-1` (N. Virginia) - this is where IAM Identity Center will be managed
   - Wait for enablement (takes 1-2 minutes)
   - You'll see a success message with your AWS access portal URL

4. **Note Your Access Portal URL**:
   - Format: `https://d-xxxxxxxxxx.awsapps.com/start`
   - **IMPORTANT**: Save this URL - you'll need it for CLI configuration
   - Example: `https://d-9067f4a123.awsapps.com/start`

##### Step 2: Choose Identity Source

1. **Select Identity Source**:
   - In IAM Identity Center dashboard, go to "Settings" in left menu
   - Under "Identity source", you'll see the current source (default: "Identity Center directory")
   
2. **Choose One of Three Options**:

   **Option 2a: Identity Center Directory (Recommended for getting started)**
   - This is the default AWS-managed directory
   - Best for small teams or getting started quickly
   - Click "Continue" to use this option

   **Option 2b: Active Directory (For enterprise environments)**
   - If you have AWS Managed Microsoft AD or AD Connector
   - Click "Actions" → "Change identity source"
   - Select "Active Directory"
   - Choose your directory

   **Option 2c: External Identity Provider (For existing IdP)**
   - If you use Okta, Azure AD, Google Workspace, etc.
   - Click "Actions" → "Change identity source"
   - Select "External identity provider"
   - Configure SAML 2.0 metadata

   **For this guide, we'll use Identity Center Directory (Option 2a)**

##### Step 3: Create Users

1. **Navigate to Users**:
   - In IAM Identity Center, click "Users" in the left menu
   - Click "Add user" button

2. **Add Your Primary User**:
   - **Username**: Your preferred username (e.g., `ryan.waite` or `admin`)
   - **Email address**: Your work email (e.g., `ryan@turafapp.com`)
   - **First name**: Your first name
   - **Last name**: Your last name
   - **Display name**: Full name (auto-populated)
   - Click "Next"

3. **Add to Groups (Optional but Recommended)**:
   - Click "Create group" if you want to organize users
   - Group name: `InfrastructureAdmins`
   - Description: "Full access to all accounts for infrastructure management"
   - Click "Create group"
   - Select the group checkbox
   - Click "Next"

4. **Review and Create**:
   - Review user details
   - Click "Add user"
   - **IMPORTANT**: User will receive an email with activation instructions

5. **Activate User Account**:
   - Check email inbox for "Invitation to join AWS IAM Identity Center"
   - Click "Accept invitation" link
   - Set up password (must meet complexity requirements)
   - Optionally set up MFA (highly recommended):
     - Choose "Authenticator app" or "Security key"
     - Follow setup instructions
   - Click "Continue"

6. **Repeat for Additional Users** (if needed):
   - Add team members who need infrastructure access
   - Assign them to appropriate groups

##### Step 4: Create Permission Sets

Permission sets define what users can do in each AWS account.

1. **Navigate to Permission Sets**:
   - In IAM Identity Center, click "Permission sets" in the left menu
   - Click "Create permission set"

2. **Create Administrator Permission Set**:
   
   **Step 4a: Select Type**
   - Choose "Predefined permission set"
   - Select "AdministratorAccess" from dropdown
   - Click "Next"
   
   **Step 4b: Specify Details**
   - **Permission set name**: `AdministratorAccess`
   - **Description**: "Full administrative access to AWS services"
   - **Session duration**: `12 hours` (adjust as needed)
   - Click "Next"
   
   **Step 4c: Review and Create**
   - Review settings
   - Click "Create"

3. **Create Additional Permission Sets** (Optional):
   
   **Developer Permission Set**:
   - Click "Create permission set"
   - Choose "Custom permission set"
   - Click "Next"
   - **Attach AWS managed policies**:
     - `PowerUserAccess` (full access except IAM/Organizations)
   - Permission set name: `DeveloperAccess`
   - Click "Next" → "Create"
   
   **ReadOnly Permission Set**:
   - Click "Create permission set"
   - Choose "Predefined permission set"
   - Select "ViewOnlyAccess"
   - Permission set name: `ReadOnlyAccess`
   - Click "Next" → "Create"

##### Step 5: Assign Users to AWS Accounts

Now assign users/groups to specific AWS accounts with specific permission sets.

1. **Navigate to AWS Accounts**:
   - In IAM Identity Center, click "AWS accounts" in the left menu
   - You should see your AWS Organization structure with all 5 accounts:
     - root (072456928432)
     - Ops (146072879609)
     - dev (801651112319)
     - qa (965932217544)
     - prod (811783768245)

2. **Assign to Root Account**:
   - Check the box next to "root (072456928432)"
   - Click "Assign users or groups" button
   
   **Step 5a: Select Users/Groups**
   - **Users** tab: Select your user (e.g., `ryan.waite`)
   - OR **Groups** tab: Select `InfrastructureAdmins` group
   - Click "Next"
   
   **Step 5b: Select Permission Sets**
   - Check "AdministratorAccess"
   - Click "Next"
   
   **Step 5c: Review and Submit**
   - Review the assignment
   - Click "Submit"
   - Wait for "Successfully assigned" message

3. **Assign to Ops Account**:
   - Check the box next to "Ops (146072879609)"
   - Click "Assign users or groups"
   - Select your user/group
   - Click "Next"
   - Select "AdministratorAccess"
   - Click "Next" → "Submit"

4. **Assign to Dev Account**:
   - Check the box next to "dev (801651112319)"
   - Click "Assign users or groups"
   - Select your user/group
   - Click "Next"
   - Select "AdministratorAccess"
   - Click "Next" → "Submit"

5. **Assign to QA Account**:
   - Check the box next to "qa (965932217544)"
   - Click "Assign users or groups"
   - Select your user/group
   - Click "Next"
   - Select "AdministratorAccess"
   - Click "Next" → "Submit"

6. **Assign to Prod Account**:
   - Check the box next to "prod (811783768245)"
   - Click "Assign users or groups"
   - Select your user/group
   - Click "Next"
   - Select "AdministratorAccess"
   - Click "Next" → "Submit"

**Bulk Assignment Alternative**:
- Select multiple accounts at once (Shift+Click or Cmd+Click)
- Click "Assign users or groups"
- Assign the same user/group and permission set to all selected accounts

##### Step 6: Configure AWS CLI for SSO

1. **Get SSO Configuration Details**:
   - In IAM Identity Center dashboard, note:
     - **AWS access portal URL**: `https://d-xxxxxxxxxx.awsapps.com/start`
     - **SSO Region**: `us-east-1` (where IAM Identity Center is enabled)

2. **Configure SSO Profile** (see section 0.3 for full `~/.aws/config` setup):
   ```ini
   [profile turaf-root]
   sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
   sso_region = us-east-1
   sso_account_id = 072456928432
   sso_role_name = AdministratorAccess
   region = us-east-1
   output = json
   ```

3. **Initial SSO Login**:
   ```bash
   aws sso login --profile turaf-root
   ```
   
   **What happens**:
   - Browser opens automatically
   - You'll see a code to verify (e.g., `QWER-TYUI`)
   - Confirm the code matches what's shown in terminal
   - Click "Confirm and continue"
   - Sign in with your IAM Identity Center credentials
   - Approve the request
   - You'll see "Request approved" message
   - Return to terminal - login successful!

4. **Verify Access**:
   ```bash
   aws sts get-caller-identity --profile turaf-root
   ```
   
   **Expected output**:
   ```json
   {
       "UserId": "AROAXXXXXXXXXXXXXXXXX:ryan.waite",
       "Account": "072456928432",
       "Arn": "arn:aws:sts::072456928432:assumed-role/AWSReservedSSO_AdministratorAccess_xxxxx/ryan.waite"
   }
   ```

##### Step 7: Test Access to All Accounts

```bash
# Test each account
aws sts get-caller-identity --profile turaf-root
aws sts get-caller-identity --profile turaf-ops
aws sts get-caller-identity --profile turaf-dev
aws sts get-caller-identity --profile turaf-qa
aws sts get-caller-identity --profile turaf-prod
```

**Note**: SSO sessions expire based on the session duration set in the permission set (default 12 hours). When expired, run:
```bash
aws sso login --profile turaf-root
```

##### Step 8: Enable MFA (Highly Recommended)

1. **Access User Portal**:
   - Go to your AWS access portal URL: `https://d-xxxxxxxxxx.awsapps.com/start`
   - Sign in with your credentials

2. **Register MFA Device**:
   - Click your username in top-right
   - Click "My profile" or "Security credentials"
   - Under "Multi-factor authentication (MFA)", click "Register device"
   - Choose device type:
     - **Authenticator app** (recommended): Use Google Authenticator, Authy, 1Password, etc.
     - **Security key**: Use hardware key like YubiKey
   - Follow the setup wizard
   - Scan QR code with authenticator app
   - Enter two consecutive MFA codes
   - Click "Assign MFA"

3. **Test MFA**:
   - Sign out
   - Sign back in
   - You should now be prompted for MFA code after password

##### Troubleshooting SSO

**Issue**: `aws sso login` fails with "Invalid grant"
- **Solution**: Delete cached credentials and re-login:
  ```bash
  rm -rf ~/.aws/sso/cache/
  aws sso login --profile turaf-root
  ```

**Issue**: Browser doesn't open automatically
- **Solution**: Manually copy the URL shown in terminal and paste in browser

**Issue**: "Access denied" when running AWS commands
- **Solution**: 
  - Verify user is assigned to the account in IAM Identity Center
  - Check permission set includes necessary permissions
  - Ensure SSO session hasn't expired - run `aws sso login` again

**Issue**: Can't see all accounts in IAM Identity Center
- **Solution**: 
  - Ensure you're logged into the management/root account (072456928432)
  - Verify AWS Organizations is enabled
  - Check that accounts are part of the organization

### 0.3 Configure AWS CLI Profiles

**Objective**: Set up `~/.aws/config` and `~/.aws/credentials` for all accounts

#### Create Directory Structure

```bash
mkdir -p ~/.aws
touch ~/.aws/config
touch ~/.aws/credentials
chmod 600 ~/.aws/credentials  # Secure the credentials file
```

#### Configure `~/.aws/config`

Edit `~/.aws/config` and add profiles for each account:

```ini
# Root/Management Account
[profile turaf-root]
region = us-east-1
output = json

# Operations Account
[profile turaf-ops]
region = us-east-1
output = json

# Development Account
[profile turaf-dev]
region = us-east-1
output = json

# QA Account
[profile turaf-qa]
region = us-east-1
output = json

# Production Account
[profile turaf-prod]
region = us-east-1
output = json
```

#### Configure `~/.aws/credentials`

**If using IAM Users (Option A)**, edit `~/.aws/credentials`:

```ini
[turaf-root]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

[turaf-ops]
aws_access_key_id = AKIAI44QH8DHBEXAMPLE
aws_secret_access_key = je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY

[turaf-dev]
aws_access_key_id = AKIAI44QH8DHBEXAMPLE
aws_secret_access_key = je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY

[turaf-qa]
aws_access_key_id = AKIAI44QH8DHBEXAMPLE
aws_secret_access_key = je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY

[turaf-prod]
aws_access_key_id = AKIAI44QH8DHBEXAMPLE
aws_secret_access_key = je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY
```

**If using SSO (Option B)**, update `~/.aws/config`:

```ini
[profile turaf-root]
sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
sso_region = us-east-1
sso_account_id = 072456928432
sso_role_name = AdministratorAccess
region = us-east-1
output = json

[profile turaf-ops]
sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
sso_region = us-east-1
sso_account_id = 146072879609
sso_role_name = AdministratorAccess
region = us-east-1
output = json

[profile turaf-dev]
sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
sso_region = us-east-1
sso_account_id = 801651112319
sso_role_name = AdministratorAccess
region = us-east-1
output = json

[profile turaf-qa]
sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
sso_region = us-east-1
sso_account_id = 965932217544
sso_role_name = AdministratorAccess
region = us-east-1
output = json

[profile turaf-prod]
sso_start_url = https://d-xxxxxxxxxx.awsapps.com/start
sso_region = us-east-1
sso_account_id = 811783768245
sso_role_name = AdministratorAccess
region = us-east-1
output = json
```

### 0.4 Verify AWS CLI Configuration

**Objective**: Confirm all profiles are working correctly

1. **Test Each Profile**:
   ```bash
   # Root account
   aws sts get-caller-identity --profile turaf-root
   
   # Ops account
   aws sts get-caller-identity --profile turaf-ops
   
   # Dev account
   aws sts get-caller-identity --profile turaf-dev
   
   # QA account
   aws sts get-caller-identity --profile turaf-qa
   
   # Prod account
   aws sts get-caller-identity --profile turaf-prod
   ```

2. **Expected Output** (example for root):
   ```json
   {
       "UserId": "AIDAI23HXS2EXAMPLE",
       "Account": "072456928432",
       "Arn": "arn:aws:iam::072456928432:user/infrastructure-admin"
   }
   ```

3. **If using SSO**, login first:
   ```bash
   aws sso login --profile turaf-root
   # Opens browser for authentication
   ```

### 0.5 Configure GitHub CLI

**Objective**: Authenticate with GitHub for repository access

1. **Authenticate**:
   ```bash
   gh auth login
   ```

2. **Follow the prompts**:
   - What account do you want to log into? **GitHub.com**
   - What is your preferred protocol? **HTTPS** or **SSH**
   - Authenticate Git with your GitHub credentials? **Yes**
   - How would you like to authenticate? **Login with a web browser** (recommended)
   - Copy the one-time code and press Enter
   - Complete authentication in browser

3. **Verify**:
   ```bash
   gh auth status
   # Should show: Logged in to github.com as <your-username>
   ```

4. **Clone Repository** (if not already done):
   ```bash
   cd ~/Developer/portfolio-projects
   gh repo clone ryanwaite28/ai-projects-turaf Turaf
   cd Turaf
   ```

### 0.6 Set Up Environment Variables (Optional)

**Objective**: Create helper aliases and environment variables

Create or edit `~/.zshrc` (macOS) or `~/.bashrc` (Linux):

```bash
# AWS Profile Aliases
alias aws-root='aws --profile turaf-root'
alias aws-ops='aws --profile turaf-ops'
alias aws-dev='aws --profile turaf-dev'
alias aws-qa='aws --profile turaf-qa'
alias aws-prod='aws --profile turaf-prod'

# Default AWS Profile
export AWS_PROFILE=turaf-root

# Terraform Workspace Helpers
export TF_VAR_project_name="turaf"
export TF_VAR_domain_name="turafapp.com"
```

Reload your shell:
```bash
source ~/.zshrc  # macOS
# or
source ~/.bashrc  # Linux
```

### 0.7 Verify Complete Setup

**Objective**: Final verification checklist

Run this verification script:

```bash
#!/bin/bash
echo "=== Turaf Infrastructure Setup Verification ==="
echo ""

echo "1. AWS CLI:"
aws --version && echo "✓ AWS CLI installed" || echo "✗ AWS CLI missing"
echo ""

echo "2. Terraform:"
terraform --version && echo "✓ Terraform installed" || echo "✗ Terraform missing"
echo ""

echo "3. GitHub CLI:"
gh --version && echo "✓ GitHub CLI installed" || echo "✗ GitHub CLI missing"
echo ""

echo "4. jq:"
jq --version && echo "✓ jq installed" || echo "✗ jq missing"
echo ""

echo "5. AWS Profiles:"
for profile in turaf-root turaf-ops turaf-dev turaf-qa turaf-prod; do
  if aws sts get-caller-identity --profile $profile &>/dev/null; then
    echo "✓ $profile configured"
  else
    echo "✗ $profile not configured or credentials invalid"
  fi
done
echo ""

echo "6. GitHub Authentication:"
gh auth status &>/dev/null && echo "✓ GitHub authenticated" || echo "✗ GitHub not authenticated"
echo ""

echo "=== Setup Verification Complete ==="
```

Save as `verify-setup.sh`, make executable, and run:
```bash
chmod +x verify-setup.sh
./verify-setup.sh
```

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

**Multi-Account Strategy**: Each AWS account (DEV, QA, PROD) requires its own ACM certificate for ALB HTTPS listeners. Certificates cannot be shared across accounts.

**Steps for Each Account**:

1. **Root Account Certificate** (072456928432):
   ```bash
   aws sso login --profile turaf-root
   
   aws acm request-certificate \
     --domain-name "*.turafapp.com" \
     --subject-alternative-names "turafapp.com" \
     --validation-method DNS \
     --region us-east-1 \
     --profile turaf-root
   ```

2. **DEV Account Certificate** (801651112319):
   ```bash
   aws sso login --profile turaf-dev
   
   aws acm request-certificate \
     --domain-name "*.turafapp.com" \
     --subject-alternative-names "turafapp.com" \
     --validation-method DNS \
     --region us-east-1 \
     --profile turaf-dev
   ```

3. **QA Account Certificate** (965932217544):
   ```bash
   aws sso login --profile turaf-qa
   
   aws acm request-certificate \
     --domain-name "*.turafapp.com" \
     --subject-alternative-names "turafapp.com" \
     --validation-method DNS \
     --region us-east-1 \
     --profile turaf-qa
   ```

4. **PROD Account Certificate** (811783768245):
   ```bash
   aws sso login --profile turaf-prod
   
   aws acm request-certificate \
     --domain-name "*.turafapp.com" \
     --subject-alternative-names "turafapp.com" \
     --validation-method DNS \
     --region us-east-1 \
     --profile turaf-prod
   ```

5. **Add DNS Validation Records** (in root account Route 53):
   ```bash
   # For each certificate, get validation records
   aws acm describe-certificate \
     --certificate-arn <CERTIFICATE_ARN> \
     --region us-east-1 \
     --profile <ACCOUNT_PROFILE> \
     --query 'Certificate.DomainValidationOptions[*].ResourceRecord'
   
   # Add CNAME validation records to Route 53 (root account)
   aws route53 change-resource-record-sets \
     --hosted-zone-id <HOSTED_ZONE_ID> \
     --change-batch file://acm-validation-<env>.json \
     --profile turaf-root
   ```

6. **Wait for Certificate Validation** (in each account):
   ```bash
   aws acm wait certificate-validated \
     --certificate-arn <CERTIFICATE_ARN> \
     --region us-east-1 \
     --profile <ACCOUNT_PROFILE>
   ```

**Note**: All certificates use the same wildcard domain but are validated via separate CNAME records in the shared Route 53 hosted zone.

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
     --role-name GitHubActionsDeploymentRole \
     --assume-role-policy-document file://github-actions-trust-policy.json
   
   # Attach policies
   aws iam attach-role-policy \
     --role-name GitHubActionsDeploymentRole \
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

### 2.3 Configure Database Migration IAM Roles

**Objective**: Set up IAM roles for centralized database migrations via CodeBuild

**Accounts to configure**: dev, qa, prod

**Steps for EACH account**:

1. **Create GitHub Actions Flyway OIDC Role**:
   ```bash
   # Create trust policy for Flyway migrations
   cat > github-actions-flyway-trust-policy.json <<EOF
   {
     "Version": "2012-10-17",
     "Statement": [{
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
     }]
   }
   EOF
   
   # Create role
   aws iam create-role \
     --role-name GitHubActionsFlywayRole \
     --assume-role-policy-document file://github-actions-flyway-trust-policy.json \
     --description "Role for GitHub Actions to trigger database migrations"
   
   # Create permissions policy
   cat > github-actions-flyway-permissions.json <<EOF
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Sid": "CodeBuildPermissions",
         "Effect": "Allow",
         "Action": [
           "codebuild:StartBuild",
           "codebuild:BatchGetBuilds"
         ],
         "Resource": "arn:aws:codebuild:us-east-1:<ACCOUNT_ID>:project/turaf-flyway-migrations-*"
       },
       {
         "Sid": "CloudWatchLogs",
         "Effect": "Allow",
         "Action": [
           "logs:GetLogEvents",
           "logs:FilterLogEvents"
         ],
         "Resource": "arn:aws:logs:us-east-1:<ACCOUNT_ID>:log-group:/aws/codebuild/turaf-flyway-migrations-*"
       }
     ]
   }
   EOF
   
   # Attach permissions
   aws iam put-role-policy \
     --role-name GitHubActionsFlywayRole \
     --policy-name GitHubActionsFlywayPolicy \
     --policy-document file://github-actions-flyway-permissions.json
   ```

2. **Create CodeBuild Flyway Execution Role**:
   ```bash
   # Create trust policy for CodeBuild
   cat > codebuild-flyway-trust-policy.json <<EOF
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Principal": {
         "Service": "codebuild.amazonaws.com"
       },
       "Action": "sts:AssumeRole"
     }]
   }
   EOF
   
   # Create role
   aws iam create-role \
     --role-name CodeBuildFlywayRole \
     --assume-role-policy-document file://codebuild-flyway-trust-policy.json \
     --description "Role for CodeBuild to execute Flyway database migrations"
   
   # Create permissions policy
   cat > codebuild-flyway-permissions.json <<EOF
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Sid": "SecretsManagerAccess",
         "Effect": "Allow",
         "Action": [
           "secretsmanager:GetSecretValue"
         ],
         "Resource": [
           "arn:aws:secretsmanager:us-east-1:<ACCOUNT_ID>:secret:turaf/db/master-*"
         ]
       },
       {
         "Sid": "CloudWatchLogs",
         "Effect": "Allow",
         "Action": [
           "logs:CreateLogGroup",
           "logs:CreateLogStream",
           "logs:PutLogEvents"
         ],
         "Resource": "arn:aws:logs:us-east-1:<ACCOUNT_ID>:log-group:/aws/codebuild/turaf-flyway-migrations-*"
       },
       {
         "Sid": "VPCAccess",
         "Effect": "Allow",
         "Action": [
           "ec2:CreateNetworkInterface",
           "ec2:DescribeNetworkInterfaces",
           "ec2:DeleteNetworkInterface",
           "ec2:DescribeSubnets",
           "ec2:DescribeSecurityGroups",
           "ec2:DescribeDhcpOptions",
           "ec2:DescribeVpcs",
           "ec2:CreateNetworkInterfacePermission"
         ],
         "Resource": "*"
       },
       {
         "Sid": "ECRAccess",
         "Effect": "Allow",
         "Action": [
           "ecr:GetAuthorizationToken",
           "ecr:BatchCheckLayerAvailability",
           "ecr:GetDownloadUrlForLayer",
           "ecr:BatchGetImage"
         ],
         "Resource": "*"
       }
     ]
   }
   EOF
   
   # Attach permissions
   aws iam put-role-policy \
     --role-name CodeBuildFlywayRole \
     --policy-name CodeBuildFlywayPolicy \
     --policy-document file://codebuild-flyway-permissions.json
   ```

3. **Verify Roles Created**:
   ```bash
   # List roles
   aws iam list-roles --query 'Roles[?contains(RoleName, `Flyway`)].RoleName'
   
   # Get role ARNs
   aws iam get-role --role-name GitHubActionsFlywayRole --query 'Role.Arn'
   aws iam get-role --role-name CodeBuildFlywayRole --query 'Role.Arn'
   ```

### 2.4 Set Up Amazon SES

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
gh secret set AWS_ROLE_ARN --env dev-environment --body "arn:aws:iam::801651112319:role/GitHubActionsDeploymentRole"
gh secret set DATABASE_URL --env dev-environment --body "<TO_BE_SET_AFTER_TERRAFORM>"
gh secret set JWT_SECRET --env dev-environment --body "<GENERATE_RANDOM_SECRET>"

# QA Environment
gh secret set AWS_ROLE_ARN --env qa-environment --body "arn:aws:iam::965932217544:role/GitHubActionsDeploymentRole"
gh secret set DATABASE_URL --env qa-environment --body "<TO_BE_SET_AFTER_TERRAFORM>"
gh secret set JWT_SECRET --env qa-environment --body "<GENERATE_RANDOM_SECRET>"

# PROD Environment
gh secret set AWS_ROLE_ARN --env prod-environment --body "arn:aws:iam::811783768245:role/GitHubActionsDeploymentRole"
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
     --role-arn arn:aws:iam::<ACCOUNT_ID>:role/GitHubActionsDeploymentRole \
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
