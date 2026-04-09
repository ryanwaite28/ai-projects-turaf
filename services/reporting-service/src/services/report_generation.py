"""
Report generation service for creating experiment reports.

This module orchestrates the full report generation pipeline, coordinating
data fetching, aggregation, templating, PDF generation, S3 storage, and
event publishing.
"""

import logging
import uuid
from typing import Dict, Any, Optional

from services.data_fetching import DataFetchingService
from services.aggregation import DataAggregationService
from services.template_engine import TemplateEngine
from services.pdf_generation import PdfGenerationService
from services.s3_storage import S3StorageService
from events.event_publisher import EventPublisher

logger = logging.getLogger(__name__)


class ReportGenerationService:
    """
    Application service for generating experiment reports.

    Orchestrates the full report pipeline following DDD Application Service
    pattern: fetches data from external services, aggregates and analyzes
    metrics, renders an HTML template, converts to PDF, stores in S3, and
    publishes a ReportGenerated domain event.

    Pipeline:
        1. Fetch experiment, hypothesis, problem, and metrics data
        2. Aggregate and analyze the fetched data
        3. Render HTML report template
        4. Convert HTML to PDF
        5. Upload PDF (and HTML) to S3
        6. Publish ReportGenerated event to EventBridge

    Attributes:
        data_fetching_service: Fetches raw data from upstream services
        aggregation_service: Aggregates and derives insights from raw data
        template_engine: Renders Jinja2 HTML report template
        pdf_service: Converts HTML to PDF bytes via WeasyPrint
        s3_service: Uploads reports to S3 and generates presigned URLs
        event_publisher: Publishes domain events to EventBridge
    """

    def __init__(
        self,
        data_fetching_service: Optional[DataFetchingService] = None,
        aggregation_service: Optional[DataAggregationService] = None,
        template_engine: Optional[TemplateEngine] = None,
        pdf_service: Optional[PdfGenerationService] = None,
        s3_service: Optional[S3StorageService] = None,
        event_publisher: Optional[EventPublisher] = None
    ):
        """
        Initialize the report generation service with all pipeline dependencies.

        All dependencies default to their production implementations. Override
        them in tests via constructor injection.

        Args:
            data_fetching_service: Fetches data from Experiment and Metrics services
            aggregation_service: Aggregates raw data into report-ready format
            template_engine: Renders HTML report from aggregated data
            pdf_service: Converts HTML to PDF bytes
            s3_service: Uploads reports to S3
            event_publisher: Publishes ReportGenerated event to EventBridge
        """
        self.data_fetching_service = data_fetching_service or DataFetchingService()
        self.aggregation_service = aggregation_service or DataAggregationService()
        self.template_engine = template_engine or TemplateEngine()
        self.pdf_service = pdf_service or PdfGenerationService()
        self.s3_service = s3_service or S3StorageService()
        self.event_publisher = event_publisher or EventPublisher()

        logger.info("ReportGenerationService initialized")

    def generate_report(self, experiment_event: Dict[str, Any]) -> Dict[str, Any]:
        """
        Execute the full report generation pipeline for a completed experiment.

        Steps:
            1. Validate required fields
            2. Fetch experiment, hypothesis, problem, and metrics data
            3. Aggregate metrics and generate insights
            4. Render HTML report template
            5. Convert HTML to PDF
            6. Upload PDF and HTML to S3
            7. Publish ReportGenerated domain event

        Args:
            experiment_event: Dictionary containing:
                - experimentId: Experiment identifier
                - organizationId: Organization identifier (tenant)
                - completedAt: ISO-8601 completion timestamp
                - result: Optional experiment result (SUCCESS|FAILURE)

        Returns:
            Dictionary with report metadata:
                - id: Generated report identifier (UUID)
                - experimentId: Source experiment ID
                - organizationId: Organization ID
                - status: 'generated'
                - reportLocation: S3 URL of the PDF report
                - reportKey: S3 key of the PDF report

        Raises:
            ValueError: If required fields are missing from experiment_event
            Exception: If any pipeline step fails
        """
        # Validate required fields
        experiment_id = experiment_event.get('experimentId')
        organization_id = experiment_event.get('organizationId')

        if not experiment_id:
            raise ValueError("experimentId is required")
        if not organization_id:
            raise ValueError("organizationId is required")

        report_id = str(uuid.uuid4())

        logger.info(
            f"Starting report generation pipeline for experiment {experiment_id}, "
            f"report {report_id}"
        )

        # Step 1: Fetch all data needed for the report
        logger.info(f"[1/6] Fetching report data for experiment {experiment_id}")
        report_data = self.data_fetching_service.fetch_report_data(
            experiment_id, organization_id
        )

        # Step 2: Aggregate and derive insights from raw data
        logger.info(f"[2/6] Aggregating data for experiment {experiment_id}")
        aggregated_data = self.aggregation_service.aggregate_data(report_data)

        # Step 3: Render HTML template
        logger.info(f"[3/6] Rendering HTML template for experiment {experiment_id}")
        html_content = self.template_engine.render_report(aggregated_data)

        # Step 4: Generate PDF from HTML
        logger.info(f"[4/6] Generating PDF for experiment {experiment_id}")
        pdf_bytes = self.pdf_service.generate_pdf(html_content)

        # Step 5: Upload to S3 (PDF + HTML)
        logger.info(f"[5/6] Uploading report to S3 for experiment {experiment_id}")
        storage_result = self.s3_service.upload_report(
            pdf_bytes=pdf_bytes,
            organization_id=organization_id,
            experiment_id=experiment_id,
            html_content=html_content
        )

        # Step 6: Publish ReportGenerated domain event
        logger.info(f"[6/6] Publishing ReportGenerated event for report {report_id}")
        self.event_publisher.publish_report_generated(
            organization_id=organization_id,
            experiment_id=experiment_id,
            report_id=report_id,
            report_location=storage_result['pdf_url'],
            report_format='PDF'
        )

        logger.info(
            f"Report {report_id} generated successfully for experiment {experiment_id}. "
            f"Stored at {storage_result['pdf_url']}"
        )

        return {
            'id': report_id,
            'experimentId': experiment_id,
            'organizationId': organization_id,
            'status': 'generated',
            'completedAt': experiment_event.get('completedAt'),
            'result': experiment_event.get('result'),
            'reportLocation': storage_result['pdf_url'],
            'reportKey': storage_result['pdf_key'],
        }
