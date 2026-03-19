"""
S3 storage service for report persistence.

This module provides services for uploading and managing report files in
Amazon S3, following Clean Architecture principles.
"""

import boto3
import os
import logging
from datetime import datetime
from typing import Optional, Dict, Any
from botocore.exceptions import ClientError, BotoCoreError

logger = logging.getLogger(__name__)


class S3StorageService:
    """
    Service for storing and retrieving reports in Amazon S3.
    
    This service acts as an adapter between the application and AWS S3,
    following the Adapter pattern and Repository pattern from DDD. It
    provides a clean interface for report storage while handling AWS-specific
    details internally.
    
    Features:
    - PDF and HTML report upload
    - Organized folder structure by organization and experiment
    - Metadata attachment for tracking
    - Presigned URL generation for secure access
    - Comprehensive error handling
    
    S3 Key Structure:
        reports/{organizationId}/{experimentId}/experiment-report-{timestamp}.{ext}
    
    The service follows the Single Responsibility Principle (SOLID),
    focusing solely on S3 storage operations.
    
    Attributes:
        s3_client: Boto3 S3 client for AWS operations
        bucket_name: S3 bucket name for report storage
    """
    
    def __init__(self, bucket_name: str = None, s3_client=None):
        """
        Initialize S3 storage service.
        
        Args:
            bucket_name: Optional S3 bucket name override.
                        Defaults to REPORTS_BUCKET_NAME env var.
            s3_client: Optional boto3 S3 client for testing.
                      Defaults to creating a new client.
                      
        Example:
            >>> service = S3StorageService()
            >>> # Uses environment variable for bucket
            
            >>> service = S3StorageService(bucket_name='my-reports-bucket')
            >>> # Uses custom bucket name
        """
        self.s3_client = s3_client or boto3.client('s3')
        self.bucket_name = bucket_name or os.environ.get(
            'REPORTS_BUCKET_NAME',
            'turaf-reports-dev'
        )
        
        logger.info(f"S3StorageService initialized with bucket: {self.bucket_name}")
    
    def upload_report(
        self,
        pdf_bytes: bytes,
        organization_id: str,
        experiment_id: str,
        html_content: Optional[str] = None
    ) -> Dict[str, str]:
        """
        Upload PDF report to S3, optionally with HTML version.
        
        This method uploads the generated PDF report to S3 with proper
        organization and metadata. It can also upload an HTML version
        for web viewing.
        
        Args:
            pdf_bytes: PDF content as bytes
            organization_id: Organization identifier for folder organization
            experiment_id: Experiment identifier for folder organization
            html_content: Optional HTML version of the report
            
        Returns:
            Dictionary containing:
                - pdf_url: S3 URL of the PDF report
                - pdf_key: S3 key of the PDF report
                - html_url: S3 URL of HTML report (if provided)
                - html_key: S3 key of HTML report (if provided)
                
        Raises:
            ValueError: If required parameters are missing or invalid
            Exception: If S3 upload fails
            
        Example:
            >>> service = S3StorageService()
            >>> pdf_bytes = b'%PDF-1.4...'
            >>> result = service.upload_report(
            ...     pdf_bytes,
            ...     'org-123',
            ...     'exp-456'
            ... )
            >>> result['pdf_url']
            's3://turaf-reports-dev/reports/org-123/exp-456/...'
        """
        # Validate inputs
        if not pdf_bytes:
            raise ValueError("pdf_bytes is required and cannot be empty")
        if not organization_id:
            raise ValueError("organization_id is required")
        if not experiment_id:
            raise ValueError("experiment_id is required")
        
        try:
            logger.info(
                f"Uploading report for experiment {experiment_id} "
                f"in organization {organization_id}"
            )
            
            # Generate S3 key for PDF
            pdf_key = self._generate_s3_key(organization_id, experiment_id, 'pdf')
            
            # Prepare metadata
            metadata = {
                'organizationId': organization_id,
                'experimentId': experiment_id,
                'generatedAt': datetime.utcnow().isoformat()
            }
            
            # Upload PDF
            self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=pdf_key,
                Body=pdf_bytes,
                ContentType='application/pdf',
                Metadata=metadata
            )
            
            logger.info(
                f"Uploaded PDF report to s3://{self.bucket_name}/{pdf_key} "
                f"({len(pdf_bytes)} bytes)"
            )
            
            # Build result
            result = {
                'pdf_url': self._generate_s3_url(pdf_key),
                'pdf_key': pdf_key
            }
            
            # Upload HTML version if provided
            if html_content:
                html_key = self._generate_s3_key(organization_id, experiment_id, 'html')
                
                self.s3_client.put_object(
                    Bucket=self.bucket_name,
                    Key=html_key,
                    Body=html_content.encode('utf-8'),
                    ContentType='text/html',
                    Metadata=metadata
                )
                
                logger.info(
                    f"Uploaded HTML report to s3://{self.bucket_name}/{html_key} "
                    f"({len(html_content)} characters)"
                )
                
                result['html_url'] = self._generate_s3_url(html_key)
                result['html_key'] = html_key
            
            return result
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(
                f"S3 ClientError uploading report: {error_code} - {str(e)}",
                exc_info=True
            )
            raise Exception(f"S3 upload failed: {error_code} - {str(e)}")
            
        except BotoCoreError as e:
            logger.error(f"BotoCore error uploading report: {str(e)}", exc_info=True)
            raise Exception(f"S3 upload failed: {str(e)}")
            
        except Exception as e:
            logger.error(f"Unexpected error uploading report: {str(e)}", exc_info=True)
            raise
    
    def _generate_s3_key(
        self,
        organization_id: str,
        experiment_id: str,
        file_type: str
    ) -> str:
        """
        Generate S3 key for report file.
        
        Creates a hierarchical key structure for organizing reports:
        reports/{organizationId}/{experimentId}/experiment-report-{timestamp}.{ext}
        
        Args:
            organization_id: Organization identifier
            experiment_id: Experiment identifier
            file_type: File extension (pdf or html)
            
        Returns:
            S3 key string
            
        Example:
            >>> service = S3StorageService()
            >>> key = service._generate_s3_key('org-123', 'exp-456', 'pdf')
            >>> key.startswith('reports/org-123/exp-456/')
            True
        """
        # Generate timestamp-based filename to ensure uniqueness
        timestamp = datetime.utcnow().isoformat().replace(':', '-').replace('.', '-')
        
        key = (
            f"reports/{organization_id}/{experiment_id}/"
            f"experiment-report-{timestamp}.{file_type}"
        )
        
        logger.debug(f"Generated S3 key: {key}")
        
        return key
    
    def _generate_s3_url(self, key: str) -> str:
        """
        Generate S3 URL for object key.
        
        Creates an S3 protocol URL for the object. Note that this is not
        an HTTP URL - use generate_presigned_url() for HTTP access.
        
        Args:
            key: S3 object key
            
        Returns:
            S3 URL string (s3://bucket/key format)
            
        Example:
            >>> service = S3StorageService()
            >>> url = service._generate_s3_url('reports/org-123/exp-456/report.pdf')
            >>> url
            's3://turaf-reports-dev/reports/org-123/exp-456/report.pdf'
        """
        return f"s3://{self.bucket_name}/{key}"
    
    def generate_presigned_url(
        self,
        key: str,
        expiration: int = 3600
    ) -> str:
        """
        Generate presigned URL for secure S3 object access.
        
        Creates a temporary HTTP URL that allows access to the S3 object
        without requiring AWS credentials. The URL expires after the
        specified duration.
        
        Args:
            key: S3 object key
            expiration: URL expiration time in seconds (default: 1 hour)
            
        Returns:
            Presigned HTTPS URL string
            
        Raises:
            ValueError: If key is empty or expiration is invalid
            Exception: If presigned URL generation fails
            
        Example:
            >>> service = S3StorageService()
            >>> url = service.generate_presigned_url(
            ...     'reports/org-123/exp-456/report.pdf',
            ...     expiration=7200
            ... )
            >>> url.startswith('https://')
            True
        """
        if not key:
            raise ValueError("key is required and cannot be empty")
        if expiration <= 0:
            raise ValueError("expiration must be positive")
        
        try:
            logger.info(f"Generating presigned URL for {key}, expires in {expiration}s")
            
            url = self.s3_client.generate_presigned_url(
                'get_object',
                Params={
                    'Bucket': self.bucket_name,
                    'Key': key
                },
                ExpiresIn=expiration
            )
            
            logger.debug(f"Generated presigned URL: {url[:50]}...")
            
            return url
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(
                f"Failed to generate presigned URL: {error_code} - {str(e)}",
                exc_info=True
            )
            raise Exception(f"Presigned URL generation failed: {error_code}")
            
        except Exception as e:
            logger.error(f"Unexpected error generating presigned URL: {str(e)}")
            raise
    
    def get_object_metadata(self, key: str) -> Dict[str, Any]:
        """
        Retrieve metadata for an S3 object.
        
        Args:
            key: S3 object key
            
        Returns:
            Dictionary with object metadata
            
        Raises:
            Exception: If object doesn't exist or retrieval fails
        """
        try:
            response = self.s3_client.head_object(
                Bucket=self.bucket_name,
                Key=key
            )
            
            return {
                'content_type': response.get('ContentType'),
                'content_length': response.get('ContentLength'),
                'last_modified': response.get('LastModified'),
                'metadata': response.get('Metadata', {})
            }
            
        except ClientError as e:
            error_code = e.response.get('Error', {}).get('Code', 'Unknown')
            logger.error(f"Failed to get object metadata: {error_code}")
            raise Exception(f"Failed to get metadata: {error_code}")
