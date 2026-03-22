"""
Integration tests for reporting-service.

These tests verify end-to-end functionality using moto for AWS service mocking
(Python equivalent of Testcontainers + LocalStack for Java services).

Following the hybrid testing strategy:
- Use moto for S3 (free-tier equivalent)
- Mock EventBridge client (not in free tier)
- Test complete report generation workflow
"""
