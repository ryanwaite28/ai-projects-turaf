"""
Unit tests for S3StorageService.

Tests cover S3 upload, key generation, presigned URLs, and error handling.
Uses moto for S3 mocking.
"""

import pytest
import boto3
from moto import mock_aws
from src.services.s3_storage import S3StorageService


class TestS3StorageService:
    """Test suite for S3StorageService."""
    
    @pytest.fixture
    def aws_credentials(self, monkeypatch):
        """Set up fake AWS credentials for moto."""
        monkeypatch.setenv('AWS_ACCESS_KEY_ID', 'testing')
        monkeypatch.setenv('AWS_SECRET_ACCESS_KEY', 'testing')
        monkeypatch.setenv('AWS_SECURITY_TOKEN', 'testing')
        monkeypatch.setenv('AWS_SESSION_TOKEN', 'testing')
        monkeypatch.setenv('AWS_DEFAULT_REGION', 'us-east-1')
    
    @pytest.fixture
    def s3_bucket(self, aws_credentials):
        """Create a mock S3 bucket."""
        with mock_aws():
            s3 = boto3.client('s3', region_name='us-east-1')
            bucket_name = 'test-reports-bucket'
            s3.create_bucket(Bucket=bucket_name)
            yield bucket_name
    
    @pytest.fixture
    def service(self, s3_bucket):
        """Create service instance with mock S3."""
        with mock_aws():
            s3_client = boto3.client('s3', region_name='us-east-1')
            s3_client.create_bucket(Bucket=s3_bucket)
            return S3StorageService(bucket_name=s3_bucket, s3_client=s3_client)
    
    @pytest.fixture
    def sample_pdf_bytes(self):
        """Create sample PDF bytes for testing."""
        return b'%PDF-1.4\nSample PDF content'
    
    @pytest.fixture
    def sample_html_content(self):
        """Create sample HTML content for testing."""
        return '<html><body><h1>Test Report</h1></body></html>'
    
    def test_service_initialization(self):
        """Test that service initializes correctly."""
        # When
        service = S3StorageService(bucket_name='test-bucket')
        
        # Then
        assert service is not None
        assert service.bucket_name == 'test-bucket'
        assert service.s3_client is not None
    
    def test_service_initialization_with_env_var(self, monkeypatch):
        """Test service initialization with environment variable."""
        # Given
        monkeypatch.setenv('REPORTS_BUCKET_NAME', 'env-bucket')
        
        # When
        service = S3StorageService()
        
        # Then
        assert service.bucket_name == 'env-bucket'
    
    def test_upload_report_pdf_only(self, service, sample_pdf_bytes):
        """Test uploading PDF report only."""
        # When
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # Then
        assert 'pdf_url' in result
        assert 'pdf_key' in result
        assert result['pdf_url'].startswith('s3://')
        assert 'org-123' in result['pdf_key']
        assert 'exp-456' in result['pdf_key']
        assert result['pdf_key'].endswith('.pdf')
    
    def test_upload_report_pdf_and_html(
        self,
        service,
        sample_pdf_bytes,
        sample_html_content
    ):
        """Test uploading both PDF and HTML reports."""
        # When
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456',
            html_content=sample_html_content
        )
        
        # Then
        assert 'pdf_url' in result
        assert 'pdf_key' in result
        assert 'html_url' in result
        assert 'html_key' in result
        assert result['html_key'].endswith('.html')
    
    def test_upload_report_raises_error_for_empty_pdf(self, service):
        """Test that upload raises error for empty PDF bytes."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.upload_report(
                pdf_bytes=b'',
                organization_id='org-123',
                experiment_id='exp-456'
            )
        assert 'pdf_bytes is required' in str(exc_info.value)
    
    def test_upload_report_raises_error_for_missing_organization_id(
        self,
        service,
        sample_pdf_bytes
    ):
        """Test that upload raises error for missing organization ID."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.upload_report(
                pdf_bytes=sample_pdf_bytes,
                organization_id='',
                experiment_id='exp-456'
            )
        assert 'organization_id is required' in str(exc_info.value)
    
    def test_upload_report_raises_error_for_missing_experiment_id(
        self,
        service,
        sample_pdf_bytes
    ):
        """Test that upload raises error for missing experiment ID."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.upload_report(
                pdf_bytes=sample_pdf_bytes,
                organization_id='org-123',
                experiment_id=''
            )
        assert 'experiment_id is required' in str(exc_info.value)
    
    def test_generate_s3_key_format(self, service):
        """Test S3 key generation format."""
        # When
        key = service._generate_s3_key('org-123', 'exp-456', 'pdf')
        
        # Then
        assert key.startswith('reports/org-123/exp-456/')
        assert key.endswith('.pdf')
        assert 'experiment-report-' in key
    
    def test_generate_s3_key_includes_timestamp(self, service):
        """Test that S3 key includes timestamp."""
        # When
        key1 = service._generate_s3_key('org-123', 'exp-456', 'pdf')
        key2 = service._generate_s3_key('org-123', 'exp-456', 'pdf')
        
        # Then
        # Keys should be different due to timestamp
        assert key1 != key2
    
    def test_generate_s3_key_for_html(self, service):
        """Test S3 key generation for HTML files."""
        # When
        key = service._generate_s3_key('org-123', 'exp-456', 'html')
        
        # Then
        assert key.endswith('.html')
    
    def test_generate_s3_url_format(self, service):
        """Test S3 URL generation format."""
        # When
        url = service._generate_s3_url('reports/org-123/exp-456/report.pdf')
        
        # Then
        assert url == 's3://test-reports-bucket/reports/org-123/exp-456/report.pdf'
    
    def test_generate_presigned_url(self, service, sample_pdf_bytes):
        """Test presigned URL generation."""
        # Given
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # When
        presigned_url = service.generate_presigned_url(result['pdf_key'])
        
        # Then
        assert presigned_url is not None
        assert len(presigned_url) > 0
    
    def test_generate_presigned_url_with_custom_expiration(
        self,
        service,
        sample_pdf_bytes
    ):
        """Test presigned URL with custom expiration."""
        # Given
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # When
        presigned_url = service.generate_presigned_url(
            result['pdf_key'],
            expiration=7200
        )
        
        # Then
        assert presigned_url is not None
    
    def test_generate_presigned_url_raises_error_for_empty_key(self, service):
        """Test that presigned URL generation raises error for empty key."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_presigned_url('')
        assert 'key is required' in str(exc_info.value)
    
    def test_generate_presigned_url_raises_error_for_invalid_expiration(
        self,
        service
    ):
        """Test that presigned URL generation raises error for invalid expiration."""
        # When/Then
        with pytest.raises(ValueError) as exc_info:
            service.generate_presigned_url('some-key', expiration=0)
        assert 'expiration must be positive' in str(exc_info.value)
    
    def test_upload_report_includes_metadata(self, service, sample_pdf_bytes):
        """Test that uploaded objects include metadata."""
        # When
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # Then
        metadata = service.get_object_metadata(result['pdf_key'])
        assert metadata['metadata']['organizationid'] == 'org-123'
        assert metadata['metadata']['experimentid'] == 'exp-456'
        assert 'generatedat' in metadata['metadata']
    
    def test_upload_report_sets_correct_content_type_for_pdf(
        self,
        service,
        sample_pdf_bytes
    ):
        """Test that PDF uploads have correct content type."""
        # When
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # Then
        metadata = service.get_object_metadata(result['pdf_key'])
        assert metadata['content_type'] == 'application/pdf'
    
    def test_upload_report_sets_correct_content_type_for_html(
        self,
        service,
        sample_pdf_bytes,
        sample_html_content
    ):
        """Test that HTML uploads have correct content type."""
        # When
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456',
            html_content=sample_html_content
        )
        
        # Then
        metadata = service.get_object_metadata(result['html_key'])
        assert metadata['content_type'] == 'text/html'
    
    def test_get_object_metadata(self, service, sample_pdf_bytes):
        """Test retrieving object metadata."""
        # Given
        result = service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-123',
            experiment_id='exp-456'
        )
        
        # When
        metadata = service.get_object_metadata(result['pdf_key'])
        
        # Then
        assert 'content_type' in metadata
        assert 'content_length' in metadata
        assert 'last_modified' in metadata
        assert 'metadata' in metadata
    
    def test_upload_report_logs_activity(
        self,
        service,
        sample_pdf_bytes,
        caplog
    ):
        """Test that upload logs activity."""
        # When
        with caplog.at_level('INFO'):
            service.upload_report(
                pdf_bytes=sample_pdf_bytes,
                organization_id='org-123',
                experiment_id='exp-456'
            )
        
        # Then
        assert 'Uploading report for experiment exp-456' in caplog.text
        assert 'Uploaded PDF report' in caplog.text
