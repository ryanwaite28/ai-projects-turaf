# Notification Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Notification Service, an event-driven Lambda function responsible for sending notifications via multiple channels.

---

## Service Overview

**Purpose**: Send notifications to users based on domain events

**Bounded Context**: Notifications and Communications

**Service Type**: Event-driven processor (AWS Lambda)

---

## Responsibilities

- Listen for domain events (ExperimentCompleted, ReportGenerated, etc.)
- Send email notifications via Amazon SES
- Send webhook notifications to external systems
- Manage notification preferences
- Track notification delivery status
- Handle notification failures and retries

---

## Technology Stack

**Runtime**: AWS Lambda (Python 3.11)  
**Framework**: Native Python Lambda  
**Email**: Amazon SES (via Boto3)  
**HTTP Client**: requests  
**Events**: AWS EventBridge  
**Build Tool**: pip  

**Key Dependencies**:
- `boto3` (AWS SDK for SES, Secrets Manager)
- `jinja2` (email templates)
- `requests` (HTTP webhooks)
- `python-json-logger` (structured logging)

---

## Event Handlers

### ExperimentCompleted Event Handler

**Trigger**: ExperimentCompleted event from EventBridge

**Notification Type**: Experiment completion notification

**Recipients**: Experiment creator and organization admins

**Handler Logic**:
```python
import json
from typing import Dict, Any
from handlers.experiment_completed import handle_experiment_completed

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle ExperimentCompleted event from EventBridge"""
    event_id = event['id']
    
    # Check idempotency
    if is_already_processed(event_id):
        return {'statusCode': 200, 'body': 'Already processed'}
    
    # Parse payload
    detail = event['detail']
    experiment_id = detail['experimentId']
    
    # Fetch experiment details
    experiment = experiment_client.get_experiment(experiment_id)
    
    # Get notification recipients
    recipients = get_recipients(experiment)
    
    # Send email notifications
    for user in recipients:
        send_experiment_completed_email(user, experiment)
    
    # Send webhook notifications
    send_webhook_notifications(
        experiment['organizationId'],
        'experiment.completed',
        detail
    )
    
    # Mark as processed
    mark_as_processed(event_id, 'ExperimentCompleted', len(recipients))
    
    return {'statusCode': 200, 'body': 'Success'}
```

---

### ReportGenerated Event Handler

**Trigger**: ReportGenerated event from EventBridge

**Notification Type**: Report ready notification

**Recipients**: Experiment creator and organization admins

**Handler Logic**:
```python
import json
from typing import Dict, Any

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle ReportGenerated event from EventBridge"""
    event_id = event['id']
    
    # Check idempotency
    if is_already_processed(event_id):
        return {'statusCode': 200, 'body': 'Already processed'}
    
    # Parse payload
    detail = event['detail']
    experiment_id = detail['experimentId']
    report_location = detail['reportLocation']
    
    # Fetch experiment details
    experiment = experiment_client.get_experiment(experiment_id)
    
    # Get notification recipients
    recipients = get_recipients(experiment)
    
    # Send email notifications with report link
    for user in recipients:
        send_report_ready_email(user, experiment, report_location)
    
    # Send webhook notifications
    send_webhook_notifications(
        experiment['organizationId'],
        'report.generated',
        detail
    )
    
    # Mark as processed
    mark_as_processed(event_id, 'ReportGenerated', len(recipients))
    
    return {'statusCode': 200, 'body': 'Success'}
```

---

### MemberAdded Event Handler

**Trigger**: MemberAdded event from EventBridge

**Notification Type**: Welcome email

**Recipients**: New member

**Handler Logic**:
```python
import json
from typing import Dict, Any

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle MemberAdded event from EventBridge"""
    event_id = event['id']
    
    # Check idempotency
    if is_already_processed(event_id):
        return {'statusCode': 200, 'body': 'Already processed'}
    
    # Parse payload
    detail = event['detail']
    user_id = detail['userId']
    organization_id = detail['organizationId']
    
    # Fetch user and organization details
    user = user_client.get_user(user_id)
    organization = organization_client.get_organization(organization_id)
    
    # Send welcome email
    send_welcome_email(user, organization)
    
    # Mark as processed
    mark_as_processed(event_id, 'MemberAdded', 1)
    
    return {'statusCode': 200, 'body': 'Success'}
```

---

## Email Notifications

### Email Templates

**Template Engine**: Jinja2

**Template Location**: `templates/email/`

**Templates**:
- `experiment-completed.html`: Experiment completion notification
- `report-ready.html`: Report ready notification
- `welcome.html`: Welcome to organization
- `experiment-started.html`: Experiment started notification

---

### Experiment Completed Email

**Subject**: "Experiment Completed: {experiment.name}"

**Template Variables**:
```python
variables = {
    'user_name': user['name'],
    'experiment_name': experiment['name'],
    'outcome': experiment['result']['outcome'],
    'summary': experiment['result']['summary'],
    'experiment_url': build_experiment_url(experiment['id']),
    'organization_name': organization['name']
}
```

**Email Content**:
```html
<!DOCTYPE html>
<html>
<head>
    <title>Experiment Completed</title>
</head>
<body>
    <h1>Experiment Completed</h1>
    <p>Hi {{ user_name }},</p>
    
    <p>Your experiment "<strong>{{ experiment_name }}</strong>" has been completed.</p>
    
    <p><strong>Outcome:</strong> {{ outcome }}</p>
    <p><strong>Summary:</strong> {{ summary }}</p>
    
    <p>
        <a href="{{ experiment_url }}">View Experiment Details</a>
    </p>
    
    <p>Best regards,<br/>The Turaf Team</p>
</body>
</html>
```

---

### Report Ready Email

**Subject**: "Report Ready: {experiment.name}"

**Template Variables**:
```python
variables = {
    'user_name': user['name'],
    'experiment_name': experiment['name'],
    'report_url': build_report_download_url(report_location),
    'experiment_url': build_experiment_url(experiment['id'])
}
```

---

### Email Sending

**Amazon SES Integration**:
```python
import boto3
from jinja2 import Environment, FileSystemLoader
import logging

logger = logging.getLogger(__name__)
ses_client = boto3.client('ses')

def send_email(to: str, subject: str, template_name: str, variables: dict):
    """Send email using Amazon SES with Jinja2 template"""
    # Render template
    env = Environment(loader=FileSystemLoader('templates/email'))
    template = env.get_template(f'{template_name}.html')
    html_body = template.render(**variables)
    
    # Send email
    try:
        response = ses_client.send_email(
            Source='notifications@turaf.com',
            Destination={'ToAddresses': [to]},
            Message={
                'Subject': {'Data': subject},
                'Body': {'Html': {'Data': html_body}}
            }
        )
        message_id = response['MessageId']
        logger.info(f'Email sent to {to}, messageId: {message_id}')
        return message_id
    except Exception as e:
        logger.error(f'Failed to send email to {to}: {str(e)}')
        raise EmailSendException(f'Failed to send email: {str(e)}')
```

---

## Webhook Notifications

### Webhook Configuration

**Storage**: Organization settings in database

**Webhook Structure**:
```json
{
  "organizationId": "uuid",
  "webhookUrl": "https://example.com/webhooks/turaf",
  "events": ["experiment.completed", "report.generated"],
  "secret": "webhook-signing-secret",
  "enabled": true
}
```

---

### Webhook Delivery

**HTTP Request**:
```
POST {webhookUrl}
Content-Type: application/json
X-Turaf-Signature: sha256={signature}
X-Turaf-Event: {eventType}
X-Turaf-Delivery: {deliveryId}

{
  "eventId": "uuid",
  "eventType": "experiment.completed",
  "timestamp": "ISO-8601",
  "organizationId": "uuid",
  "data": {
    "experimentId": "uuid",
    "name": "Experiment Name",
    "outcome": "VALIDATED"
  }
}
```

**Signature Calculation**:
```python
import hmac
import hashlib

def calculate_signature(payload: str, secret: str) -> str:
    """Calculate HMAC-SHA256 signature for webhook payload"""
    signature = hmac.new(
        secret.encode('utf-8'),
        payload.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    return signature
```

**Webhook Sending**:
```python
import requests
import json
import uuid
import logging

logger = logging.getLogger(__name__)

def send_webhook(webhook: dict, event_type: str, payload: dict):
    """Send webhook notification with signature"""
    payload_json = json.dumps(payload)
    signature = calculate_signature(payload_json, webhook['secret'])
    delivery_id = str(uuid.uuid4())
    
    headers = {
        'Content-Type': 'application/json',
        'X-Turaf-Signature': f'sha256={signature}',
        'X-Turaf-Event': event_type,
        'X-Turaf-Delivery': delivery_id
    }
    
    try:
        response = requests.post(
            webhook['url'],
            data=payload_json,
            headers=headers,
            timeout=10
        )
        response.raise_for_status()
        logger.info(f'Webhook delivered: {delivery_id}')
    except Exception as e:
        logger.error(f'Webhook delivery failed: {delivery_id}, error: {str(e)}')
        raise
```

---

### Webhook Retry Strategy

**Retry Policy**:
- Retry on 5xx errors and network failures
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maximum 5 retry attempts
- No retry on 4xx errors (client errors)

**Implementation**:
```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=1, min=1, max=16)
)
def send_webhook_with_retry(webhook: dict, event_type: str, payload: dict):
    """Send webhook with exponential backoff retry"""
    send_webhook(webhook, event_type, payload)
```

---

## Notification Preferences

### User Preferences

**Preference Structure**:
```json
{
  "userId": "uuid",
  "organizationId": "uuid",
  "emailNotifications": {
    "experimentCompleted": true,
    "reportGenerated": true,
    "experimentStarted": false
  },
  "webhookNotifications": {
    "enabled": true
  }
}
```

**Preference Checking**:
```python
def should_send_notification(user: dict, notification_type: str) -> bool:
    """Check if user wants to receive this notification type"""
    prefs = preferences_repository.find_by_user_id(user['id'])
    if not prefs:
        return True  # Default: send all notifications
    return prefs.get('emailNotifications', {}).get(notification_type, True)
```

---

## Recipient Selection

### Get Recipients Logic

**Rules**:
- Experiment creator always receives notifications
- Organization admins receive notifications
- Users can opt-out via preferences

**Implementation**:
```python
def get_recipients(experiment: dict) -> list:
    """Get list of users who should receive notifications"""
    recipients = []
    
    # Add experiment creator
    creator = user_client.get_user(experiment['createdBy'])
    if should_send_notification(creator, 'experimentCompleted'):
        recipients.append(creator)
    
    # Add organization admins
    admins = organization_client.get_admins(experiment['organizationId'])
    for admin in admins:
        if should_send_notification(admin, 'experimentCompleted'):
            recipients.append(admin)
    
    # Remove duplicates by user ID
    seen = set()
    unique_recipients = []
    for user in recipients:
        if user['id'] not in seen:
            seen.add(user['id'])
            unique_recipients.append(user)
    
    return unique_recipients
```

---

## Error Handling

### Email Delivery Failures

**Failure Scenarios**:
- Invalid email address
- SES rate limit exceeded
- SES service unavailable
- Email bounced or rejected

**Handling**:
```python
try:
    email_service.send_email(user['email'], subject, template, variables)
except EmailSendException as e:
    logger.error(f"Failed to send email to {user['email']}: {str(e)}")
    # Record failure in database
    record_notification_failure(user['id'], 'email', str(e))
    # Don't raise - continue processing other recipients
```

---

### Webhook Delivery Failures

**Failure Scenarios**:
- Webhook URL unreachable
- Timeout
- 5xx server errors
- Invalid webhook configuration

**Handling**:
- Retry with exponential backoff
- After max retries, record failure
- Send alert to organization admins
- Disable webhook after repeated failures

---

## Idempotency

### Deduplication Strategy

**Idempotency Key**: EventBridge eventId

**Implementation**:
```python
import boto3
from datetime import datetime
from typing import Optional

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('processed_notification_events')

def is_already_processed(event_id: str) -> bool:
    """Check if event has already been processed"""
    response = table.get_item(Key={'eventId': event_id})
    return 'Item' in response

def mark_as_processed(event_id: str, event_type: str, recipient_count: int):
    """Mark event as processed in DynamoDB"""
    table.put_item(
        Item={
            'eventId': event_id,
            'eventType': event_type,
            'processedAt': datetime.utcnow().isoformat(),
            'recipientCount': recipient_count
        }
    )
```

---

## Lambda Configuration

### Function Configuration

**Memory**: 512 MB  
**Timeout**: 30 seconds  
**Concurrency**: 20 (reserved concurrency)  
**Environment Variables**:
- `ENVIRONMENT`: dev/qa/prod
- `SES_FROM_EMAIL`: notifications@turaf.com
- `EXPERIMENT_SERVICE_URL`: https://api.{env}.turaf.com
- `ORGANIZATION_SERVICE_URL`: https://api.{env}.turaf.com
- `FRONTEND_URL`: https://app.{env}.turaf.com

### IAM Permissions

**Required Permissions**:
- `ses:SendEmail` for email notifications
- `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- `secretsmanager:GetSecretValue` for API credentials
- HTTP egress for webhook delivery

---

## Notification Channels

### Current Channels

1. **Email** (via Amazon SES)
   - Transactional emails
   - HTML templates
   - Delivery tracking

2. **Webhooks** (via HTTP POST)
   - Real-time event delivery
   - Signature verification
   - Retry logic

### Future Channels

- **Slack**: Direct messages and channel notifications
- **Microsoft Teams**: Team notifications
- **SMS**: Critical alerts via SNS
- **In-App**: Browser push notifications
- **Mobile Push**: iOS and Android push notifications

---

## Monitoring

### CloudWatch Metrics

**Custom Metrics**:
- `NotificationsSent`: Count by type (email, webhook)
- `NotificationFailures`: Count by type and reason
- `EmailDeliveryTime`: Duration to send emails
- `WebhookDeliveryTime`: Duration to deliver webhooks

**CloudWatch Logs**:
- Log all notification attempts
- Log delivery failures with details
- Include event ID and recipient info

### Alarms

**Alarm Conditions**:
- Email failure rate > 5%
- Webhook failure rate > 10%
- SES bounce rate > 5%
- Processing time > 15 seconds

---

## Testing Strategy

### Unit Tests
- Test email template rendering
- Test webhook signature calculation
- Test recipient selection logic
- Test preference checking

### Integration Tests
- Test with LocalStack SES
- Test webhook delivery to mock server
- Test idempotency
- Test error scenarios

### End-to-End Tests
- Publish domain events
- Verify emails sent
- Verify webhooks delivered
- Verify preferences honored

---

## Security Considerations

### Email Security
- SPF, DKIM, DMARC configured for domain
- No sensitive data in email bodies
- Secure links with time-limited tokens

### Webhook Security
- HMAC signature verification
- HTTPS only for webhook URLs
- Webhook secret rotation
- Rate limiting per organization

---

## Performance Optimization

### Batching
- Batch email sends when possible
- Parallel webhook delivery
- Async processing with reactive streams

### Caching
- Cache user preferences
- Cache organization settings
- Cache email templates

---

## Future Enhancements

- Notification scheduling (digest emails)
- Notification templates customization
- Multi-language support
- Notification analytics dashboard
- Delivery status tracking
- Read receipts for emails
- Webhook payload customization
- Notification rules engine

---

## References

- PROJECT.md: Notification Service specification
- event-flow.md: Event specifications
- Amazon SES Documentation
- Webhook Best Practices
