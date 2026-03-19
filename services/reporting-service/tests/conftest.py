"""
Shared pytest fixtures and configuration for reporting-service tests.

This module provides reusable fixtures for testing AWS services,
HTTP mocking, and common test data.
"""

import pytest
import os
import boto3
from moto import mock_aws
from datetime import datetime, timedelta


@pytest.fixture
def aws_credentials(monkeypatch):
    """
    Mock AWS credentials for moto.
    
    Sets environment variables required by boto3 to use mock AWS services.
    """
    monkeypatch.setenv('AWS_ACCESS_KEY_ID', 'testing')
    monkeypatch.setenv('AWS_SECRET_ACCESS_KEY', 'testing')
    monkeypatch.setenv('AWS_SECURITY_TOKEN', 'testing')
    monkeypatch.setenv('AWS_SESSION_TOKEN', 'testing')
    monkeypatch.setenv('AWS_DEFAULT_REGION', 'us-east-1')


@pytest.fixture
def s3_client(aws_credentials):
    """
    Mock S3 client for testing.
    
    Yields:
        boto3 S3 client configured for moto
    """
    with mock_aws():
        yield boto3.client('s3', region_name='us-east-1')


@pytest.fixture
def s3_bucket(s3_client):
    """
    Mock S3 bucket for testing.
    
    Creates a test bucket and yields its name.
    
    Yields:
        str: Bucket name
    """
    bucket_name = 'test-reports-bucket'
    s3_client.create_bucket(Bucket=bucket_name)
    yield bucket_name


@pytest.fixture
def dynamodb_resource(aws_credentials):
    """
    Mock DynamoDB resource for testing.
    
    Yields:
        boto3 DynamoDB resource configured for moto
    """
    with mock_aws():
        yield boto3.resource('dynamodb', region_name='us-east-1')


@pytest.fixture
def dynamodb_table(dynamodb_resource):
    """
    Mock DynamoDB table for idempotency testing.
    
    Creates a table with the schema required by IdempotencyService.
    
    Yields:
        str: Table name
    """
    table_name = 'test-idempotency-table'
    
    dynamodb_resource.create_table(
        TableName=table_name,
        KeySchema=[
            {'AttributeName': 'eventId', 'KeyType': 'HASH'}
        ],
        AttributeDefinitions=[
            {'AttributeName': 'eventId', 'AttributeType': 'S'}
        ],
        BillingMode='PAY_PER_REQUEST'
    )
    
    yield table_name


@pytest.fixture
def eventbridge_client(aws_credentials):
    """
    Mock EventBridge client for testing.
    
    Yields:
        boto3 EventBridge client configured for moto
    """
    with mock_aws():
        yield boto3.client('events', region_name='us-east-1')


@pytest.fixture
def event_bus(eventbridge_client):
    """
    Mock EventBridge event bus for testing.
    
    Creates a test event bus and yields its name.
    
    Yields:
        str: Event bus name
    """
    bus_name = 'test-event-bus'
    eventbridge_client.create_event_bus(Name=bus_name)
    yield bus_name


@pytest.fixture
def sample_experiment_data():
    """
    Sample experiment data for testing.
    
    Returns:
        dict: Experiment data structure
    """
    return {
        'id': 'exp-123',
        'name': 'Performance Test Experiment',
        'organizationId': 'org-456',
        'hypothesisId': 'hyp-789',
        'startedAt': '2024-01-01T00:00:00Z',
        'completedAt': '2024-01-01T02:30:00Z',
        'status': 'COMPLETED',
        'result': 'SUCCESS'
    }


@pytest.fixture
def sample_hypothesis_data():
    """
    Sample hypothesis data for testing.
    
    Returns:
        dict: Hypothesis data structure
    """
    return {
        'id': 'hyp-789',
        'statement': 'Caching will improve response time by 50%',
        'expectedOutcome': 'Reduce average latency to under 100ms',
        'criteria': 'Average latency < 100ms',
        'problemId': 'prob-101'
    }


@pytest.fixture
def sample_problem_data():
    """
    Sample problem data for testing.
    
    Returns:
        dict: Problem data structure
    """
    return {
        'id': 'prob-101',
        'title': 'High API Response Time',
        'description': 'API endpoints are responding too slowly',
        'context': 'User complaints about performance'
    }


@pytest.fixture
def sample_metrics_data():
    """
    Sample metrics data for testing.
    
    Returns:
        list: List of metric data points
    """
    return [
        {
            'id': 'metric-1',
            'name': 'latency',
            'value': 100.0,
            'timestamp': '2024-01-01T00:30:00Z'
        },
        {
            'id': 'metric-2',
            'name': 'latency',
            'value': 150.0,
            'timestamp': '2024-01-01T01:00:00Z'
        }
    ]


@pytest.fixture
def sample_aggregated_metrics():
    """
    Sample aggregated metrics for testing.
    
    Returns:
        dict: Aggregated metrics by name
    """
    return {
        'latency': {
            'count': 100,
            'average': 125.5,
            'min': 50.0,
            'max': 200.0
        },
        'throughput': {
            'count': 100,
            'average': 1000.0,
            'min': 800.0,
            'max': 1200.0
        }
    }


@pytest.fixture
def sample_eventbridge_event():
    """
    Sample EventBridge event for testing.
    
    Returns:
        dict: EventBridge event structure
    """
    return {
        'version': '0',
        'id': 'evt-123',
        'detail-type': 'ExperimentCompleted',
        'source': 'turaf.experiment-service',
        'account': '123456789012',
        'time': '2024-01-01T12:00:00Z',
        'region': 'us-east-1',
        'resources': [],
        'detail': {
            'eventId': 'evt-123',
            'experimentId': 'exp-456',
            'organizationId': 'org-789',
            'completedAt': '2024-01-01T12:00:00Z',
            'result': 'SUCCESS'
        }
    }


@pytest.fixture
def sample_html_content():
    """
    Sample HTML content for testing.
    
    Returns:
        str: HTML string
    """
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Test Report</title>
    </head>
    <body>
        <h1>Experiment Report</h1>
        <p>This is a test report.</p>
    </body>
    </html>
    """


@pytest.fixture
def sample_pdf_bytes():
    """
    Sample PDF bytes for testing.
    
    Returns:
        bytes: PDF content
    """
    return b'%PDF-1.4\nSample PDF content for testing'
