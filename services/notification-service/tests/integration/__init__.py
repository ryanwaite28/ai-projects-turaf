"""
Integration tests for notification-service.

These tests verify end-to-end functionality using moto for AWS service mocking
and requests-mock for webhook endpoint simulation.

Following the hybrid testing strategy:
- Use moto for SNS (free-tier equivalent)
- Mock SES client (limited in LocalStack free tier)
- Use requests-mock for webhook endpoints (Python equivalent of WireMock)
- Test complete notification delivery workflow
"""
