"""
Integration tests for email templates.
Tests template rendering with actual Jinja2 engine.
"""
import pytest
import os
from services.template_service import TemplateService


class TestEmailTemplates:
    """Test suite for email template rendering."""
    
    @pytest.fixture
    def template_service(self):
        """Create TemplateService with actual templates directory."""
        # Assuming tests run from service root
        template_dir = 'templates'
        if not os.path.exists(template_dir):
            pytest.skip(f"Template directory not found: {template_dir}")
        return TemplateService(template_dir=template_dir)
    
    def test_experiment_completed_template_renders(self, template_service):
        """Test experiment-completed template renders successfully."""
        data = {
            'experiment_name': 'Test Experiment',
            'experiment_id': 'exp-123',
            'completed_at': '2024-03-18T10:00:00Z',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/experiment-completed', data)
        
        assert html is not None
        assert len(html) > 0
        assert 'Experiment Completed' in html
        assert 'Test Experiment' in html
        assert 'exp-123' in html
    
    def test_experiment_completed_template_contains_required_elements(self, template_service):
        """Test experiment-completed template has all required elements."""
        data = {
            'experiment_name': 'My Experiment',
            'experiment_id': 'exp-456',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/experiment-completed', data)
        
        # Check for key content
        assert 'My Experiment' in html
        assert 'exp-456' in html
        assert 'View Experiment Results' in html
        assert 'https://app.turaf.com/experiments/exp-456' in html
    
    def test_experiment_completed_template_handles_optional_fields(self, template_service):
        """Test experiment-completed template works without optional fields."""
        data = {
            'experiment_name': 'Test',
            'experiment_id': 'exp-789',
            'frontend_url': 'https://app.turaf.com'
        }
        
        # Should render without completed_at
        html = template_service.render('email/experiment-completed', data)
        
        assert html is not None
        assert 'Test' in html
    
    def test_report_generated_template_renders(self, template_service):
        """Test report-generated template renders successfully."""
        data = {
            'experiment_name': 'Test Experiment',
            'experiment_id': 'exp-123',
            'report_id': 'rpt-456',
            'report_url': 'https://s3.amazonaws.com/reports/rpt-456.pdf',
            'generated_at': '2024-03-18T10:05:00Z',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/report-generated', data)
        
        assert html is not None
        assert 'Report Available' in html
        assert 'Test Experiment' in html
        assert 'rpt-456' in html
        assert 'https://s3.amazonaws.com/reports/rpt-456.pdf' in html
    
    def test_report_generated_template_contains_download_link(self, template_service):
        """Test report-generated template includes download link."""
        data = {
            'experiment_name': 'Experiment',
            'experiment_id': 'exp-123',
            'report_id': 'rpt-123',
            'report_url': 'https://example.com/report.pdf',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/report-generated', data)
        
        assert 'Download Report' in html
        assert 'https://example.com/report.pdf' in html
        assert 'View Report Online' in html
    
    def test_report_generated_template_handles_missing_report_url(self, template_service):
        """Test report-generated template works without report URL."""
        data = {
            'experiment_name': 'Experiment',
            'experiment_id': 'exp-123',
            'report_id': 'rpt-123',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/report-generated', data)
        
        assert html is not None
        assert 'rpt-123' in html
    
    def test_member_added_template_renders(self, template_service):
        """Test member-added template renders successfully."""
        data = {
            'member_name': 'John Doe',
            'organization_name': 'Acme Corp',
            'organization_id': 'org-123',
            'role': 'MEMBER',
            'invited_by': 'Jane Admin',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/member-added', data)
        
        assert html is not None
        assert 'Welcome to Acme Corp' in html
        assert 'John Doe' in html
        assert 'MEMBER' in html
        assert 'Jane Admin' in html
    
    def test_member_added_template_contains_organization_link(self, template_service):
        """Test member-added template includes organization dashboard link."""
        data = {
            'member_name': 'User',
            'organization_name': 'Test Org',
            'organization_id': 'org-456',
            'role': 'ADMIN',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/member-added', data)
        
        assert 'Go to Dashboard' in html
        assert 'https://app.turaf.com/organizations/org-456' in html
    
    def test_member_added_template_lists_features(self, template_service):
        """Test member-added template lists available features."""
        data = {
            'member_name': 'User',
            'organization_name': 'Org',
            'organization_id': 'org-123',
            'role': 'MEMBER',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/member-added', data)
        
        assert 'A/B testing experiments' in html
        assert 'real-time experiment metrics' in html
        assert 'comprehensive experiment reports' in html
    
    def test_member_added_template_handles_optional_invited_by(self, template_service):
        """Test member-added template works without invited_by."""
        data = {
            'member_name': 'User',
            'organization_name': 'Org',
            'organization_id': 'org-123',
            'role': 'MEMBER',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/member-added', data)
        
        assert html is not None
        assert 'User' in html
    
    def test_all_templates_include_footer(self, template_service):
        """Test all templates include standard footer."""
        templates = [
            ('email/experiment-completed', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/report-generated', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'report_id': 'rpt-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/member-added', {
                'member_name': 'User',
                'organization_name': 'Org',
                'organization_id': 'org-123',
                'role': 'MEMBER',
                'frontend_url': 'https://app.turaf.com'
            })
        ]
        
        for template_name, data in templates:
            html = template_service.render(template_name, data)
            
            assert 'Turaf' in html
            assert 'All rights reserved' in html
            assert 'Notification Settings' in html
    
    def test_all_templates_are_responsive(self, template_service):
        """Test all templates include responsive CSS."""
        templates = [
            ('email/experiment-completed', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/report-generated', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'report_id': 'rpt-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/member-added', {
                'member_name': 'User',
                'organization_name': 'Org',
                'organization_id': 'org-123',
                'role': 'MEMBER',
                'frontend_url': 'https://app.turaf.com'
            })
        ]
        
        for template_name, data in templates:
            html = template_service.render(template_name, data)
            
            # Check for responsive meta tag and media queries
            assert 'viewport' in html
            assert '@media' in html or 'max-width: 600px' in html
    
    def test_templates_escape_html_in_user_data(self, template_service):
        """Test templates properly escape HTML in user-provided data."""
        data = {
            'experiment_name': '<script>alert("xss")</script>',
            'experiment_id': 'exp-123',
            'frontend_url': 'https://app.turaf.com'
        }
        
        html = template_service.render('email/experiment-completed', data)
        
        # Should escape the script tag
        assert '<script>' not in html
        assert '&lt;script&gt;' in html or 'alert' not in html
    
    def test_templates_use_consistent_branding(self, template_service):
        """Test all templates use consistent branding colors and styles."""
        templates = [
            ('email/experiment-completed', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/report-generated', {
                'experiment_name': 'Test',
                'experiment_id': 'exp-123',
                'report_id': 'rpt-123',
                'frontend_url': 'https://app.turaf.com'
            }),
            ('email/member-added', {
                'member_name': 'User',
                'organization_name': 'Org',
                'organization_id': 'org-123',
                'role': 'MEMBER',
                'frontend_url': 'https://app.turaf.com'
            })
        ]
        
        for template_name, data in templates:
            html = template_service.render(template_name, data)
            
            # Check for consistent styling
            assert 'email-wrapper' in html
            assert 'email-header' in html
            assert 'email-body' in html
            assert 'email-footer' in html
