# Task: Implement S3 Storage

**Service**: Reporting Service  
**Phase**: 7  
**Estimated Time**: 2 hours  

## Objective

Implement S3 storage for generated PDF reports with proper naming and organization.

## Prerequisites

- [x] Task 006: PDF generation implemented

## Scope

**Files to Create**:
- `services/reporting-service/src/services/s3_storage.py`

## Implementation Details

### S3 Storage Service

```python
import boto3
import os
import logging
from datetime import datetime, timedelta
from typing import Optional
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)

class S3StorageService:
    """Service for storing reports in S3"""
    
    def __init__(self):
        self.s3_client = boto3.client('s3')
        self.bucket_name = os.environ.get('REPORTS_BUCKET_NAME', 'turaf-reports-dev')
        
        logger.info(f"S3 Storage initialized with bucket: {self.bucket_name}")
    
    def upload_report(self, pdf_bytes: bytes, organization_id: str, 
                     experiment_id: str, html_content: Optional[str] = None) -> str:
        """
        Upload PDF report to S3.
        
        Args:
            pdf_bytes: PDF content as bytes
            organization_id: Organization identifier
            experiment_id: Experiment identifier
            html_content: Optional HTML version of report
            
        Returns:
            S3 URL of uploaded report
        """
        try:
            # Generate S3 key
            pdf_key = self._generate_s3_key(organization_id, experiment_id, 'pdf')
            
            # Upload PDF
            self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=pdf_key,
                Body=pdf_bytes,
                ContentType='application/pdf',
                Metadata={
                    'organizationId': organization_id,
                    'experimentId': experiment_id,
                    'generatedAt': datetime.utcnow().isoformat()
                }
            )
            
            logger.info(f"Uploaded PDF report to s3://{self.bucket_name}/{pdf_key}")
            
            # Upload HTML version if provided
            if html_content:
                html_key = self._generate_s3_key(organization_id, experiment_id, 'html')
                self.s3_client.put_object(
                    Bucket=self.bucket_name,
                    Key=html_key,
                    Body=html_content.encode('utf-8'),
                    ContentType='text/html',
                    Metadata={
                        'organizationId': organization_id,
                        'experimentId': experiment_id,
                        'generatedAt': datetime.utcnow().isoformat()
                    }
                )
                logger.info(f"Uploaded HTML report to s3://{self.bucket_name}/{html_key}")
            
            return self._generate_s3_url(pdf_key)
            
        except ClientError as e:
            logger.error(f"Failed to upload report to S3: {str(e)}")
            raise Exception(f"S3 upload failed: {str(e)}")
    
    def _generate_s3_key(self, organization_id: str, experiment_id: str, 
                        file_type: str) -> str:
        """
        Generate S3 key for report file.
        
        Args:
            organization_id: Organization identifier
            experiment_id: Experiment identifier
            file_type: File extension (pdf or html)
            
        Returns:
            S3 key string
        """
        timestamp = datetime.utcnow().isoformat().replace(':', '-').replace('.', '-')
        return f"reports/{organization_id}/{experiment_id}/experiment-report-{timestamp}.{file_type}"
    
    def _generate_s3_url(self, key: str) -> str:
        """
        Generate S3 URL for key.
        
        Args:
            key: S3 object key
            
        Returns:
            S3 URL string
        """
        return f"s3://{self.bucket_name}/{key}"
    
    def generate_presigned_url(self, key: str, expiration: int = 3600) -> str:
        """
        Generate presigned URL for S3 object.
        
        Args:
            key: S3 object key
            expiration: URL expiration time in seconds (default 1 hour)
            
        Returns:
            Presigned URL string
        """
        try:
            url = self.s3_client.generate_presigned_url(
                'get_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': key
                },
                ExpiresIn=expiration
            )
            
            logger.info(f"Generated presigned URL for {key}, expires in {expiration}s")
            
            return url
            
        except ClientError as e:
            logger.error(f"Failed to generate presigned URL: {str(e)}")
            raise Exception(f"Presigned URL generation failed: {str(e)}")
```

## Acceptance Criteria

- [x] S3 upload works correctly
- [x] File naming convention followed (timestamp-based)
- [x] Metadata attached to objects
- [x] Both PDF and HTML uploaded
- [x] Presigned URLs generated
- [x] Error handling implemented
- [x] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test S3 upload (PDF)
- Test S3 upload (HTML)
- Test key generation
- Test presigned URL generation
- Test error handling
- Use moto for S3 mocking

**Test Files to Create**:
- `tests/test_s3_storage.py`

## References

- Specification: `specs/reporting-service.md` (S3 Storage section)
- Related Tasks: 008-implement-event-publishing
