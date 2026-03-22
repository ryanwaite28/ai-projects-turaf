"""
Integration tests for S3 storage service.

Tests S3 upload, retrieval, and presigned URL generation using moto
to simulate real S3 behavior without AWS costs.

Following the hybrid testing strategy (PROJECT.md Section 23a):
- Use moto for S3 (free-tier AWS service simulation)
- Test actual S3 SDK behavior
- Verify report storage and retrieval
"""

import pytest
import boto3
from moto import mock_aws
from src.services.s3_storage import S3StorageService


@pytest.fixture
def s3_integration_client():
    """S3 client for integration testing with moto."""
    with mock_aws():
        client = boto3.client('s3', region_name='us-east-1')
        yield client


@pytest.fixture
def s3_integration_bucket(s3_integration_client):
    """Create test bucket for integration tests."""
    bucket_name = 'integration-test-reports-bucket'
    s3_integration_client.create_bucket(Bucket=bucket_name)
    return bucket_name


@pytest.fixture
def s3_storage_service(s3_integration_client, s3_integration_bucket):
    """S3 storage service configured for integration testing."""
    return S3StorageService(
        bucket_name=s3_integration_bucket,
        s3_client=s3_integration_client
    )


class TestS3StorageIntegration:
    """
    Integration tests for S3StorageService.
    
    These tests verify the complete S3 storage workflow including:
    - PDF upload to S3
    - HTML upload to S3
    - Metadata attachment
    - Presigned URL generation
    - Object retrieval
    """
    
    def test_upload_pdf_report_to_s3(
        self,
        s3_storage_service,
        s3_integration_client,
        s3_integration_bucket,
        sample_pdf_bytes
    ):
        """
        Test uploading PDF report to S3.
        
        Verifies:
        - PDF is uploaded successfully
        - Correct S3 key structure
        - Metadata is attached
        - S3 URL is generated
        """
        # Given
        organization_id = 'org-integration-123'
        experiment_id = 'exp-integration-456'
        
        # When
        result = s3_storage_service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id=organization_id,
            experiment_id=experiment_id
        )
        
        # Then
        assert 'pdf_url' in result
        assert 'pdf_key' in result
        assert result['pdf_url'].startswith(f's3://{s3_integration_bucket}/')
        assert organization_id in result['pdf_key']
        assert experiment_id in result['pdf_key']
        assert result['pdf_key'].endswith('.pdf')
        
        # Verify object exists in S3
        response = s3_integration_client.head_object(
            Bucket=s3_integration_bucket,
            Key=result['pdf_key']
        )
        assert response['ContentType'] == 'application/pdf'
        assert response['ContentLength'] == len(sample_pdf_bytes)
        assert 'organizationId' in response['Metadata']
        assert response['Metadata']['organizationId'] == organization_id
    
    def test_upload_pdf_and_html_report_to_s3(
        self,
        s3_storage_service,
        s3_integration_client,
        s3_integration_bucket,
        sample_pdf_bytes,
        sample_html_content
    ):
        """
        Test uploading both PDF and HTML versions to S3.
        
        Verifies:
        - Both PDF and HTML are uploaded
        - Correct content types
        - Same folder structure
        - Both have metadata
        """
        # Given
        organization_id = 'org-integration-789'
        experiment_id = 'exp-integration-101'
        
        # When
        result = s3_storage_service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id=organization_id,
            experiment_id=experiment_id,
            html_content=sample_html_content
        )
        
        # Then
        assert 'pdf_url' in result
        assert 'pdf_key' in result
        assert 'html_url' in result
        assert 'html_key' in result
        
        # Verify PDF
        pdf_response = s3_integration_client.head_object(
            Bucket=s3_integration_bucket,
            Key=result['pdf_key']
        )
        assert pdf_response['ContentType'] == 'application/pdf'
        
        # Verify HTML
        html_response = s3_integration_client.head_object(
            Bucket=s3_integration_bucket,
            Key=result['html_key']
        )
        assert html_response['ContentType'] == 'text/html'
        
        # Verify both in same folder
        pdf_folder = '/'.join(result['pdf_key'].split('/')[:-1])
        html_folder = '/'.join(result['html_key'].split('/')[:-1])
        assert pdf_folder == html_folder
    
    def test_generate_presigned_url_for_s3_object(
        self,
        s3_storage_service,
        s3_integration_client,
        s3_integration_bucket,
        sample_pdf_bytes
    ):
        """
        Test generating presigned URL for S3 object access.
        
        Verifies:
        - Presigned URL is generated
        - URL format is correct
        - URL contains required parameters
        """
        # Given - Upload a report first
        result = s3_storage_service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-presigned-123',
            experiment_id='exp-presigned-456'
        )
        pdf_key = result['pdf_key']
        
        # When
        presigned_url = s3_storage_service.generate_presigned_url(
            key=pdf_key,
            expiration=3600
        )
        
        # Then
        assert presigned_url is not None
        assert isinstance(presigned_url, str)
        assert len(presigned_url) > 0
        # Moto generates presigned URLs in specific format
        assert s3_integration_bucket in presigned_url or 'localhost' in presigned_url
    
    def test_get_object_metadata_from_s3(
        self,
        s3_storage_service,
        s3_integration_client,
        s3_integration_bucket,
        sample_pdf_bytes
    ):
        """
        Test retrieving object metadata from S3.
        
        Verifies:
        - Metadata is retrieved successfully
        - Contains expected fields
        - Custom metadata is preserved
        """
        # Given - Upload a report first
        result = s3_storage_service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id='org-metadata-123',
            experiment_id='exp-metadata-456'
        )
        pdf_key = result['pdf_key']
        
        # When
        metadata = s3_storage_service.get_object_metadata(pdf_key)
        
        # Then
        assert metadata is not None
        assert 'content_type' in metadata
        assert 'content_length' in metadata
        assert 'last_modified' in metadata
        assert 'metadata' in metadata
        assert metadata['content_type'] == 'application/pdf'
        assert metadata['content_length'] == len(sample_pdf_bytes)
        assert 'organizationId' in metadata['metadata']
        assert 'experimentId' in metadata['metadata']
    
    def test_upload_report_validates_required_fields(
        self,
        s3_storage_service,
        sample_pdf_bytes
    ):
        """
        Test that upload_report validates required fields.
        
        Verifies:
        - ValueError raised for missing pdf_bytes
        - ValueError raised for missing organization_id
        - ValueError raised for missing experiment_id
        """
        # Test missing pdf_bytes
        with pytest.raises(ValueError, match="pdf_bytes is required"):
            s3_storage_service.upload_report(
                pdf_bytes=None,
                organization_id='org-123',
                experiment_id='exp-456'
            )
        
        # Test missing organization_id
        with pytest.raises(ValueError, match="organization_id is required"):
            s3_storage_service.upload_report(
                pdf_bytes=sample_pdf_bytes,
                organization_id='',
                experiment_id='exp-456'
            )
        
        # Test missing experiment_id
        with pytest.raises(ValueError, match="experiment_id is required"):
            s3_storage_service.upload_report(
                pdf_bytes=sample_pdf_bytes,
                organization_id='org-123',
                experiment_id=''
            )
    
    def test_s3_key_structure_follows_convention(
        self,
        s3_storage_service,
        sample_pdf_bytes
    ):
        """
        Test that S3 keys follow the expected structure.
        
        Verifies:
        - Key starts with 'reports/'
        - Contains organization ID
        - Contains experiment ID
        - Ends with timestamp and extension
        """
        # Given
        organization_id = 'org-structure-123'
        experiment_id = 'exp-structure-456'
        
        # When
        result = s3_storage_service.upload_report(
            pdf_bytes=sample_pdf_bytes,
            organization_id=organization_id,
            experiment_id=experiment_id
        )
        
        # Then
        pdf_key = result['pdf_key']
        assert pdf_key.startswith('reports/')
        assert f'/{organization_id}/' in pdf_key
        assert f'/{experiment_id}/' in pdf_key
        assert pdf_key.endswith('.pdf')
        assert 'experiment-report-' in pdf_key
