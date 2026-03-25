# Monitoring Module

Terraform module for CloudWatch monitoring, alarms, dashboards, and X-Ray tracing for the Turaf platform.

## Features

- **CloudWatch Alarms**: CPU, memory, error rate, and response time monitoring
- **SNS Alerts**: Email notifications for alarm triggers
- **CloudWatch Dashboard**: Centralized metrics visualization
- **X-Ray Tracing**: Distributed tracing for microservices
- **Log Insights Queries**: Saved queries for error analysis
- **Cost Optimization**: All features disabled by default for demo

## Architecture

### Demo Configuration (Cost-Optimized)

**All monitoring features disabled by default** - Use AWS Console for manual monitoring.

```
Demo Approach:
├── CloudWatch Alarms: Disabled (save $0.20/month)
├── SNS Alerts: Disabled (save $0.50/month)
├── Dashboard: Disabled (free, but simplified)
├── X-Ray: Disabled (save $5/month)
└── Log Insights: Disabled (free, but simplified)

Cost: $0/month (manual monitoring via AWS Console)
```

**Rationale**:
- CloudWatch Logs already created by compute module
- AWS Console provides free metric viewing
- Alarms not critical for demo/portfolio
- Can enable selectively for production

### Production Configuration (Optional)

```
CloudWatch Monitoring
├── Alarms (6 alarms)
│   ├── ECS CPU High (>80%)
│   ├── ECS Memory High (>80%)
│   ├── ALB 5xx Errors (>10/5min)
│   ├── ALB Response Time (>2s)
│   ├── RDS CPU High (>80%)
│   └── RDS Storage Low (<5GB)
│
├── SNS Topic
│   └── Email Subscription → alerts@turaf.io
│
├── CloudWatch Dashboard
│   ├── ECS Metrics (CPU, Memory)
│   ├── ALB Metrics (Requests, Errors, Response Time)
│   └── RDS Metrics (CPU, Connections)
│
├── X-Ray Tracing
│   └── 5% sampling rate
│
└── Log Insights Queries
    ├── Error Logs
    └── Slow Requests

Cost: ~$5-10/month
```

## Usage

### Demo Configuration (All Disabled)

```hcl
module "monitoring" {
  source = "../../modules/monitoring"

  environment = "dev"
  region      = "us-east-1"
  
  # Resource identifiers (for future use)
  cluster_name      = module.compute.cluster_name
  alb_arn_suffix    = module.compute.alb_arn_suffix
  rds_instance_id   = module.database.rds_instance_id
  
  # All monitoring features disabled for demo
  enable_alarms       = false
  enable_dashboard    = false
  enable_sns_alerts   = false
  enable_xray         = false
  enable_log_insights = false
  
  tags = {
    Project     = "turaf"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}
```

### Production Configuration (All Enabled)

```hcl
module "monitoring" {
  source = "../../modules/monitoring"

  environment = "prod"
  region      = "us-east-1"
  
  # Resource identifiers
  cluster_name      = module.compute.cluster_name
  service_names     = ["identity-service", "organization-service", "experiment-service"]
  alb_arn_suffix    = module.compute.alb_arn_suffix
  rds_instance_id   = module.database.rds_instance_id
  
  # Enable all monitoring features
  enable_alarms       = true
  enable_dashboard    = true
  enable_sns_alerts   = true
  enable_xray         = true
  enable_log_insights = true
  
  # Alert configuration
  alarm_email = "alerts@turaf.io"
  
  # Alarm thresholds
  cpu_threshold           = 80
  memory_threshold        = 80
  error_rate_threshold    = 5
  response_time_threshold = 2
  
  # X-Ray configuration
  xray_sampling_rate  = 0.05
  xray_reservoir_size = 1
  
  tags = {
    Project     = "turaf"
    Environment = "prod"
    ManagedBy   = "terraform"
  }
}
```

### Selective Monitoring (Cost-Conscious Production)

```hcl
module "monitoring" {
  source = "../../modules/monitoring"

  environment = "prod"
  region      = "us-east-1"
  
  cluster_name    = module.compute.cluster_name
  alb_arn_suffix  = module.compute.alb_arn_suffix
  rds_instance_id = module.database.rds_instance_id
  
  # Enable only critical alarms
  enable_alarms     = true
  enable_sns_alerts = true
  alarm_email       = "alerts@turaf.io"
  
  # Disable optional features
  enable_dashboard    = false  # Use AWS Console
  enable_xray         = false  # Save $5/month
  enable_log_insights = false  # Use manual queries
  
  tags = {
    Project     = "turaf"
    Environment = "prod"
    ManagedBy   = "terraform"
  }
}
```

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| environment | Environment name | string | - | yes |
| region | AWS region | string | - | yes |
| cluster_name | ECS cluster name | string | "" | no |
| alb_arn_suffix | ALB ARN suffix | string | "" | no |
| rds_instance_id | RDS instance ID | string | "" | no |
| enable_alarms | Enable CloudWatch alarms | bool | false | no |
| enable_dashboard | Enable CloudWatch dashboard | bool | false | no |
| enable_sns_alerts | Enable SNS alerts | bool | false | no |
| enable_xray | Enable X-Ray tracing | bool | false | no |
| enable_log_insights | Enable Log Insights queries | bool | false | no |
| alarm_email | Email for alerts | string | "" | no |
| cpu_threshold | CPU alarm threshold (%) | number | 80 | no |
| memory_threshold | Memory alarm threshold (%) | number | 80 | no |

## Outputs

| Name | Description |
|------|-------------|
| alerts_topic_arn | SNS alerts topic ARN (null if disabled) |
| dashboard_name | CloudWatch dashboard name (null if disabled) |
| xray_sampling_rule_name | X-Ray sampling rule name (null if disabled) |
| monitoring_summary | Summary of monitoring configuration |

## Cost Estimation

### Demo Configuration (All Disabled)
- **CloudWatch Alarms**: $0/month (none created)
- **SNS**: $0/month (no topic)
- **Dashboard**: $0/month (not created)
- **X-Ray**: $0/month (disabled)
- **Total**: **$0/month**

### Production Configuration (All Enabled)

**CloudWatch Alarms**:
- 6 alarms × $0.10/month = $0.60/month

**SNS**:
- 1 topic: Free
- Email notifications: Free
- Total: $0/month

**CloudWatch Dashboard**:
- Up to 3 dashboards: Free
- Total: $0/month

**X-Ray**:
- 100K traces/month × $5.00/1M = $0.50/month
- Trace retrieval: ~$0.50/month
- Total: ~$1/month

**Log Insights**:
- Queries: Free
- Data scanned: $0.005/GB
- Estimated: ~$1/month

**Total**: ~$3-5/month

### Cost Optimization Tips

1. **Disable X-Ray for demo** - Save $1/month
2. **Reduce alarm count** - Only critical alarms
3. **Use AWS Console** - Free dashboard viewing
4. **Manual queries** - Skip saved Log Insights queries

## CloudWatch Alarms

### ECS Alarms

**CPU High**:
- Threshold: 80% (configurable)
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

**Memory High**:
- Threshold: 80% (configurable)
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

### ALB Alarms

**5xx Errors**:
- Threshold: 10 errors per 5 minutes
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

**Response Time**:
- Threshold: 2 seconds (configurable)
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

### RDS Alarms

**CPU High**:
- Threshold: 80% (configurable)
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

**Storage Low**:
- Threshold: 5 GB free space
- Period: 5 minutes
- Evaluation: 2 consecutive periods
- Action: SNS notification

## CloudWatch Dashboard

### Widgets

**ECS Cluster**:
- CPU Utilization (%)
- Memory Utilization (%)
- Service count

**Application Load Balancer**:
- Request count
- HTTP 2xx/4xx/5xx responses
- Target response time
- Active connections

**RDS Database**:
- CPU Utilization (%)
- Database connections
- Read/Write IOPS
- Free storage space

### Dashboard URL

After creation, access at:
```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=turaf-{environment}
```

## X-Ray Tracing

### Sampling Configuration

**Default Sampling**:
- Reservoir: 1 trace/second (minimum)
- Fixed Rate: 5% of requests
- Total: ~100K traces/month for moderate traffic

**Cost Impact**:
- First 100K traces/month: Free
- Additional traces: $5/1M traces
- Trace retrieval: $0.50/1M traces

### Integration

**ECS Services**:
1. Add X-Ray daemon sidecar to task definitions
2. Install X-Ray SDK in application
3. Enable tracing in application code

**Example Task Definition**:
```json
{
  "name": "xray-daemon",
  "image": "amazon/aws-xray-daemon",
  "cpu": 32,
  "memoryReservation": 256,
  "portMappings": [{
    "containerPort": 2000,
    "protocol": "udp"
  }]
}
```

## Log Insights Queries

### Error Logs Query

Finds all ERROR level logs across services:
```
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100
```

### Slow Requests Query

Finds requests taking >1 second:
```
fields @timestamp, @message, duration
| filter duration > 1000
| sort duration desc
| limit 50
```

### Custom Queries

Create additional queries in AWS Console or via Terraform.

## Monitoring Best Practices

### For Demo/Development

1. **Use AWS Console** - Free metric viewing
2. **Manual monitoring** - Check metrics periodically
3. **CloudWatch Logs** - Already created by services
4. **No alarms needed** - Not critical for demo

### For Production

1. **Enable critical alarms** - CPU, memory, errors
2. **Set up SNS alerts** - Email notifications
3. **Create dashboard** - Centralized monitoring
4. **Enable X-Ray** - For debugging issues
5. **Review metrics weekly** - Identify trends

## Troubleshooting

### Alarms Not Triggering

**Problem**: Alarms created but not firing

**Solutions**:
1. Check metric data is being published
2. Verify alarm threshold is appropriate
3. Check evaluation period settings
4. Verify SNS topic subscription confirmed

### SNS Email Not Received

**Problem**: Alarm triggered but no email

**Solutions**:
1. Confirm SNS subscription via email
2. Check spam folder
3. Verify email address is correct
4. Check SNS topic has subscription

### Dashboard Not Showing Data

**Problem**: Dashboard widgets empty

**Solutions**:
1. Verify resources are running
2. Check metric namespace and dimensions
3. Adjust time range
4. Verify region is correct

### X-Ray Traces Not Appearing

**Problem**: No traces in X-Ray console

**Solutions**:
1. Verify X-Ray daemon is running
2. Check application has X-Ray SDK
3. Verify sampling rule is active
4. Check IAM permissions for X-Ray

## Migration from Manual to Automated Monitoring

### Step 1: Enable Alarms Only

```hcl
enable_alarms     = true
enable_sns_alerts = true
alarm_email       = "your-email@example.com"
```

### Step 2: Add Dashboard

```hcl
enable_dashboard = true
```

### Step 3: Enable X-Ray (Optional)

```hcl
enable_xray = true
```

### Step 4: Add Log Insights Queries

```hcl
enable_log_insights = true
```

## Accessing Monitoring Data

### CloudWatch Console

**Metrics**: 
```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#metricsV2:
```

**Alarms**:
```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#alarmsV2:
```

**Logs**:
```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups
```

**X-Ray**:
```
https://console.aws.amazon.com/xray/home?region=us-east-1#/service-map
```

## References

- [CloudWatch Pricing](https://aws.amazon.com/cloudwatch/pricing/)
- [CloudWatch Alarms](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html)
- [CloudWatch Dashboards](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Dashboards.html)
- [X-Ray Pricing](https://aws.amazon.com/xray/pricing/)
- [X-Ray Developer Guide](https://docs.aws.amazon.com/xray/latest/devguide/aws-xray.html)
