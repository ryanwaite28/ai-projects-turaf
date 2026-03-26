# Refactor Lambda Services from Java to Python

Complete refactoring of notification-service and reporting-service from Java/Spring Cloud Function to Python 3.11, including all specification files, task files, and infrastructure references.

## Scope

This refactoring affects:
- **2 service specification files** (notification-service.md, reporting-service.md)
- **18 task files** (8 for notification-service, 10 for reporting-service)
- **5+ infrastructure/reference files** (aws-infrastructure.md, ci-cd-pipelines.md, terraform-structure.md, architecture.md, README.md, event-schemas.md)

## Rationale

Python is better suited for these Lambda functions due to:
- **Performance**: 200-300ms cold starts vs Java's 1-3 seconds
- **Cost**: Lower memory requirements (256-512MB vs 512-1024MB)
- **Simplicity**: I/O-bound event processing doesn't need Java's complexity
- **AWS Integration**: Boto3 is first-class, well-documented
- **Development Velocity**: Less boilerplate, faster iteration

## Changes Required

### 1. Service Specifications (2 files)

#### notification-service.md
- **Technology Stack section**: Change from Java 17/Spring Cloud Function to Python 3.11
- **Dependencies**: Replace Maven/Spring with Python packages (boto3, jinja2, requests)
- **Code examples**: Convert all Java handler examples to Python
  - ExperimentCompletedNotificationHandler
  - ReportGeneratedNotificationHandler
  - MemberAddedNotificationHandler
- **Lambda Configuration**: Update handler reference, runtime, package format

#### reporting-service.md
- **Technology Stack section**: Change from Java 17/Spring Cloud Function to Python 3.11
- **Dependencies**: Replace Maven/Spring/PDFBox with Python packages (boto3, jinja2, reportlab/weasyprint)
- **Code examples**: Convert Java handler to Python
  - ExperimentCompletedHandler
- **Lambda Configuration**: Update handler reference, runtime, package format

### 2. Task Files (18 files)

#### notification-service tasks (8 files)
- **001-setup-lambda-project.md**: Replace Maven/pom.xml with Python/requirements.txt, update dependencies
- **002-implement-event-handlers.md**: Convert Java event handler patterns to Python
- **003-implement-email-service.md**: Replace Spring WebClient/SES SDK with boto3
- **004-create-email-templates.md**: Replace Thymeleaf with Jinja2
- **005-implement-webhook-service.md**: Replace Spring WebClient with requests library
- **006-implement-recipient-selection.md**: Convert Java service patterns to Python
- **007-add-idempotency.md**: Update idempotency implementation for Python
- **008-add-unit-tests.md**: Replace JUnit/Mockito with pytest/moto

#### reporting-service tasks (10 files)
- **001-setup-lambda-project.md**: Replace Maven/pom.xml with Python/requirements.txt
- **002-implement-event-handler.md**: Convert Java handler to Python
- **003-implement-data-fetching.md**: Replace Spring WebClient with requests/boto3
- **004-implement-aggregation-logic.md**: Convert Java aggregation to Python
- **005-create-report-templates.md**: Replace Thymeleaf with Jinja2
- **006-implement-pdf-generation.md**: Replace PDFBox/iText with reportlab or weasyprint
- **007-implement-s3-storage.md**: Replace AWS Java SDK with boto3
- **008-implement-event-publishing.md**: Replace EventBridge Java SDK with boto3
- **009-add-idempotency.md**: Update idempotency for Python
- **010-add-unit-tests.md**: Replace JUnit with pytest

### 3. Infrastructure & Reference Files (6+ files)

#### aws-infrastructure.md
- **Lambda section**: Update runtime from "java17" to "python3.11"
- **Handler**: Change from Spring Cloud Function handler to Python handler (e.g., "handler.lambda_handler")
- **Package format**: Change from .jar to .zip
- **Terraform examples**: Update lambda function configurations

#### ci-cd-pipelines.md
- **Lambda deployment section**: Replace Maven build with Python packaging
- **Build steps**: Change from `mvn package` to `pip install` + `zip`
- **Artifact format**: Change from .jar to .zip
- **Deploy command**: Update lambda update-function-code to use .zip

#### terraform-structure.md
- **Lambda module**: Update runtime, handler, and packaging references
- **Build process**: Update from Java build to Python packaging

#### architecture.md
- **Technology Stack**: Update Lambda runtime from Java 17 to Python 3.11
- **Compute section**: Reflect Python for serverless components

#### README.md
- **Service descriptions**: Update technology stack references for Lambda services
- **Technology overview**: Update Lambda runtime information

#### event-schemas.md
- **Source service references**: Ensure consistency (no code changes, just verification)

## Python Technology Stack

### notification-service
```
Runtime: Python 3.11
Dependencies:
  - boto3 (AWS SDK - SES, Secrets Manager)
  - jinja2 (email templates)
  - requests (HTTP webhooks)
  - python-json-logger (structured logging)
Handler: notification_handler.lambda_handler
Package: .zip file with dependencies
```

### reporting-service
```
Runtime: Python 3.11
Dependencies:
  - boto3 (AWS SDK - S3, EventBridge, Secrets Manager)
  - jinja2 (report templates)
  - requests (API calls)
  - reportlab or weasyprint (PDF generation)
  - python-json-logger (structured logging)
Handler: reporting_handler.lambda_handler
Package: .zip file with dependencies
```

## Code Example Patterns

### Python Lambda Handler Structure
```python
import json
import boto3
from typing import Dict, Any

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """Handle EventBridge event"""
    event_id = event['id']
    
    # Idempotency check
    if is_already_processed(event_id):
        return {'statusCode': 200, 'body': 'Already processed'}
    
    # Parse payload
    detail = event['detail']
    
    # Process event
    process_event(detail)
    
    # Mark as processed
    mark_as_processed(event_id)
    
    return {'statusCode': 200, 'body': 'Success'}
```

### Python Dependencies (requirements.txt)
```
boto3==1.34.0
jinja2==3.1.3
requests==2.31.0
reportlab==4.0.9  # for reporting-service
python-json-logger==2.0.7
```

## Implementation Order

1. ✅ Update notification-service.md specification
2. ✅ Update reporting-service.md specification
3. ✅ Update all 8 notification-service task files
4. ✅ Update all 10 reporting-service task files
5. ✅ Update aws-infrastructure.md
6. ✅ Update ci-cd-pipelines.md
7. ✅ Update terraform-structure.md
8. ✅ Update architecture.md
9. ✅ Update README.md
10. ✅ Verify event-schemas.md (no changes needed)

## Files to Modify

**Service Specs (2):**
- `/specs/notification-service.md`
- `/specs/reporting-service.md`

**Task Files (18):**
- `/tasks/notification-service/001-setup-lambda-project.md`
- `/tasks/notification-service/002-implement-event-handlers.md`
- `/tasks/notification-service/003-implement-email-service.md`
- `/tasks/notification-service/004-create-email-templates.md`
- `/tasks/notification-service/005-implement-webhook-service.md`
- `/tasks/notification-service/006-implement-recipient-selection.md`
- `/tasks/notification-service/007-add-idempotency.md`
- `/tasks/notification-service/008-add-unit-tests.md`
- `/tasks/reporting-service/001-setup-lambda-project.md`
- `/tasks/reporting-service/002-implement-event-handler.md`
- `/tasks/reporting-service/003-implement-data-fetching.md`
- `/tasks/reporting-service/004-implement-aggregation-logic.md`
- `/tasks/reporting-service/005-create-report-templates.md`
- `/tasks/reporting-service/006-implement-pdf-generation.md`
- `/tasks/reporting-service/007-implement-s3-storage.md`
- `/tasks/reporting-service/008-implement-event-publishing.md`
- `/tasks/reporting-service/009-add-idempotency.md`
- `/tasks/reporting-service/010-add-unit-tests.md`

**Infrastructure Specs (5):**
- `/specs/aws-infrastructure.md`
- `/specs/ci-cd-pipelines.md`
- `/specs/terraform-structure.md`
- `/specs/architecture.md`
- `/specs/README.md`

**Total: 25 files**

## Success Criteria

- ✅ All Java/Spring references replaced with Python equivalents
- ✅ All code examples converted to idiomatic Python
- ✅ All Maven/pom.xml references replaced with pip/requirements.txt
- ✅ All .jar references replaced with .zip
- ✅ All handler references updated to Python format
- ✅ All runtime references changed to python3.11
- ✅ Infrastructure configurations updated for Python
- ✅ CI/CD pipelines updated for Python builds
- ✅ Consistent terminology across all files
