# Task: Configure ALB with ACM Certificates

**Service**: Infrastructure  
**Type**: Compute - Application Load Balancer  
**Priority**: High  
**Estimated Time**: 30 minutes  
**Dependencies**: 005-request-acm-certificates, 019-create-compute-modules

---

## Objective

Configure Application Load Balancer (ALB) HTTPS listeners with environment-specific ACM certificates for secure communication.

---

## Acceptance Criteria

- [ ] ALB HTTPS listener (port 443) configured with ACM certificate
- [ ] ALB HTTP listener (port 80) configured with redirect to HTTPS
- [ ] SSL policy set to `ELBSecurityPolicy-TLS-1-2-2017-01`
- [ ] Certificate ARN referenced via Terraform data source
- [ ] HTTP to HTTPS redirect tested and working
- [ ] HTTPS listener tested with valid SSL certificate

---

## Implementation

### 1. Update Compute Module for ACM Certificate

**File**: `infrastructure/terraform/modules/compute/alb.tf`

```hcl
# Data source to fetch environment-specific ACM certificate
data "aws_acm_certificate" "main" {
  domain      = "*.turafapp.com"
  statuses    = ["ISSUED"]
  most_recent = true
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "${var.project_name}-alb-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = var.environment == "prod" ? true : false
  enable_http2              = true
  enable_cross_zone_load_balancing = true

  tags = {
    Name        = "${var.project_name}-alb-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# HTTPS Listener (Port 443)
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = data.aws_acm_certificate.main.arn

  default_action {
    type = "fixed-response"
    
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }

  tags = {
    Name        = "${var.project_name}-alb-https-listener-${var.environment}"
    Environment = var.environment
  }
}

# HTTP Listener (Port 80) - Redirect to HTTPS
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  tags = {
    Name        = "${var.project_name}-alb-http-listener-${var.environment}"
    Environment = var.environment
  }
}
```

### 2. Update Compute Module Outputs

**File**: `infrastructure/terraform/modules/compute/outputs.tf`

```hcl
output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "alb_listener_https_arn" {
  description = "ARN of the HTTPS listener"
  value       = aws_lb_listener.https.arn
}

output "alb_listener_http_arn" {
  description = "ARN of the HTTP listener"
  value       = aws_lb_listener.http.arn
}

output "acm_certificate_arn" {
  description = "ARN of the ACM certificate used by ALB"
  value       = data.aws_acm_certificate.main.arn
}
```

### 3. Create Route 53 Alias Record for ALB

**File**: `infrastructure/terraform/modules/compute/route53.tf`

```hcl
# Data source for Route 53 hosted zone
data "aws_route53_zone" "main" {
  name         = var.domain_name
  private_zone = false
}

# API subdomain pointing to ALB
resource "aws_route53_record" "api" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "api.${var.environment == "prod" ? "" : "${var.environment}."}${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

# WebSocket subdomain pointing to ALB
resource "aws_route53_record" "ws" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "ws.${var.environment == "prod" ? "" : "${var.environment}."}${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}
```

### 4. Update Compute Module Variables

**File**: `infrastructure/terraform/modules/compute/variables.tf`

Add:
```hcl
variable "domain_name" {
  description = "Primary domain name"
  type        = string
  default     = "turafapp.com"
}
```

### 5. Deploy to DEV Environment

```bash
cd infrastructure/terraform/environments/dev

# Initialize Terraform
terraform init

# Plan changes
terraform plan

# Apply changes
terraform apply

# Verify ALB DNS
terraform output alb_dns_name

# Verify certificate ARN
terraform output acm_certificate_arn
```

---

## Verification

### 1. Verify HTTPS Listener

```bash
# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names turaf-alb-dev \
  --query 'LoadBalancers[0].DNSName' \
  --output text \
  --profile turaf-dev)

# Test HTTPS endpoint (should return 404 - no listener rules yet)
curl -v https://$ALB_DNS

# Verify SSL certificate
openssl s_client -connect $ALB_DNS:443 -servername $ALB_DNS < /dev/null 2>&1 | grep -A 2 "Certificate chain"
```

### 2. Verify HTTP to HTTPS Redirect

```bash
# Test HTTP redirect
curl -v http://$ALB_DNS

# Should return HTTP 301 with Location: https://$ALB_DNS
```

### 3. Verify Route 53 DNS Records

```bash
# Check API subdomain
dig api.dev.turafapp.com

# Check WebSocket subdomain
dig ws.dev.turafapp.com

# Verify HTTPS via subdomain
curl -v https://api.dev.turafapp.com
```

### 4. Verify Certificate Details

```bash
# Check certificate in ALB listener
aws elbv2 describe-listeners \
  --load-balancer-arn $(aws elbv2 describe-load-balancers \
    --names turaf-alb-dev \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text \
    --profile turaf-dev) \
  --query 'Listeners[?Port==`443`].Certificates[0].CertificateArn' \
  --output text \
  --profile turaf-dev

# Should match: arn:aws:acm:us-east-1:801651112319:certificate/8b83b688-7458-4627-9fd4-ff3b2801bf70
```

---

## Troubleshooting

### Issue: Certificate not found

**Cause**: ACM certificate not in same region or not issued

**Solution**:
```bash
# Verify certificate exists and is issued
aws acm list-certificates \
  --region us-east-1 \
  --profile turaf-dev \
  --query 'CertificateSummaryList[?DomainName==`*.turafapp.com`]'

# Check certificate status
aws acm describe-certificate \
  --certificate-arn <CERT_ARN> \
  --region us-east-1 \
  --profile turaf-dev \
  --query 'Certificate.Status'
```

### Issue: SSL handshake fails

**Cause**: Incorrect SSL policy or certificate mismatch

**Solution**:
```bash
# Test SSL connection
openssl s_client -connect api.dev.turafapp.com:443 -servername api.dev.turafapp.com

# Check supported protocols
nmap --script ssl-enum-ciphers -p 443 api.dev.turafapp.com
```

### Issue: HTTP redirect not working

**Cause**: HTTP listener not configured or security group blocking port 80

**Solution**:
```bash
# Verify HTTP listener exists
aws elbv2 describe-listeners \
  --load-balancer-arn <ALB_ARN> \
  --query 'Listeners[?Port==`80`]' \
  --profile turaf-dev

# Check security group allows port 80
aws ec2 describe-security-groups \
  --group-ids <ALB_SG_ID> \
  --query 'SecurityGroups[0].IpPermissions[?FromPort==`80`]' \
  --profile turaf-dev
```

---

## Rollout Plan

### Phase 1: DEV Environment
1. Deploy ALB with ACM certificate
2. Verify HTTPS listener and certificate
3. Test HTTP to HTTPS redirect
4. Verify Route 53 DNS records

### Phase 2: QA Environment
1. Repeat Phase 1 steps for QA account
2. Use QA certificate ARN: `arn:aws:acm:us-east-1:965932217544:certificate/906b4a44-11e3-4ee7-b10d-9f715ffc0ee6`

### Phase 3: PROD Environment
1. Repeat Phase 1 steps for PROD account
2. Use PROD certificate ARN: `arn:aws:acm:us-east-1:811783768245:certificate/779b5c14-8fc0-44fe-80b4-090bdee1ef62`
3. Enable deletion protection
4. Configure access logging to S3

---

## Documentation

Update the following files after completion:
- `infrastructure/terraform/modules/compute/README.md` - Document ALB ACM configuration
- `specs/aws-infrastructure.md` - Mark ALB ACM configuration as implemented
- `infrastructure/acm-certificates.md` - Add ALB usage notes

---

## Related Tasks

- Task 005: Request ACM Certificates (prerequisite)
- Task 019: Create Compute Modules (prerequisite)
- Task 022-024: Configure DEV/QA/PROD Environments (implementation)
- Service deployment tasks: Will use ALB HTTPS listener for routing
