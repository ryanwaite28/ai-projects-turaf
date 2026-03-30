import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import {
  loadReports,
  createReport,
  deleteReport,
  downloadReport,
  setReportsFilters
} from '../../../store/reports/reports.actions';
import {
  selectAllReports,
  selectReportsLoading,
  selectReportsError,
  selectReportsPagination,
  selectReportsFilters,
  selectCompletedReportsCount,
  selectPendingReportsCount,
  selectGenerationProgress
} from '../../../store/reports/reports.selectors';
import { Report, ReportType, ReportFormat, ReportStatus } from '../../../models/report.model';

/**
 * Report List Component
 * 
 * Displays list of reports with filtering, download, and generation capabilities.
 */
@Component({
  selector: 'app-report-list',
  templateUrl: './report-list.component.html',
  styleUrls: ['./report-list.component.scss']
})
export class ReportListComponent implements OnInit, OnDestroy {
  
  reports$ = this.store.select(selectAllReports);
  loading$ = this.store.select(selectReportsLoading);
  error$ = this.store.select(selectReportsError);
  pagination$ = this.store.select(selectReportsPagination);
  filters$ = this.store.select(selectReportsFilters);
  completedCount$ = this.store.select(selectCompletedReportsCount);
  pendingCount$ = this.store.select(selectPendingReportsCount);
  generationProgress$ = this.store.select(selectGenerationProgress);
  
  displayedColumns: string[] = ['name', 'type', 'format', 'status', 'generatedAt', 'size', 'actions'];
  
  reportTypes = Object.values(ReportType);
  reportFormats = Object.values(ReportFormat);
  reportStatuses = Object.values(ReportStatus);
  
  selectedType: string | null = null;
  selectedFormat: string | null = null;
  selectedStatus: string | null = null;
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.loadReports();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Loads reports with current filters
   */
  loadReports(): void {
    const filters: any = {};
    if (this.selectedType) filters.type = this.selectedType;
    if (this.selectedFormat) filters.format = this.selectedFormat;
    if (this.selectedStatus) filters.status = this.selectedStatus;
    
    this.store.dispatch(loadReports({ params: filters }));
  }
  
  /**
   * Filters reports by type
   */
  filterByType(type: string | null): void {
    this.selectedType = type;
    this.store.dispatch(setReportsFilters({ filters: { type: type as ReportType | undefined } }));
    this.loadReports();
  }
  
  /**
   * Filters reports by format
   */
  filterByFormat(format: string | null): void {
    this.selectedFormat = format;
    this.store.dispatch(setReportsFilters({ filters: { format: format as ReportFormat | undefined } }));
    this.loadReports();
  }
  
  /**
   * Filters reports by status
   */
  filterByStatus(status: string | null): void {
    this.selectedStatus = status;
    this.store.dispatch(setReportsFilters({ filters: { status: status as ReportStatus | undefined } }));
    this.loadReports();
  }
  
  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.store.dispatch(loadReports({ params: { page } }));
  }
  
  /**
   * Views report preview
   */
  viewReport(report: Report): void {
    this.router.navigate(['/reports', report.id]);
  }
  
  /**
   * Downloads a report
   */
  downloadReportFile(report: Report, event: Event): void {
    event.stopPropagation();
    this.store.dispatch(downloadReport({ id: report.id, format: report.format }));
  }
  
  /**
   * Deletes a report
   */
  deleteReportItem(report: Report, event: Event): void {
    event.stopPropagation();
    if (confirm(`Delete report "${report.name}"?`)) {
      this.store.dispatch(deleteReport({ id: report.id }));
    }
  }
  
  /**
   * Gets status badge class
   */
  getStatusClass(status: string): string {
    const statusMap: Record<string, string> = {
      'PENDING': 'pending',
      'GENERATING': 'generating',
      'COMPLETED': 'completed',
      'FAILED': 'failed'
    };
    return statusMap[status] || 'pending';
  }
  
  /**
   * Gets status icon
   */
  getStatusIcon(status: string): string {
    const iconMap: Record<string, string> = {
      'PENDING': 'schedule',
      'GENERATING': 'autorenew',
      'COMPLETED': 'check_circle',
      'FAILED': 'error'
    };
    return iconMap[status] || 'help';
  }
  
  /**
   * Gets type icon
   */
  getTypeIcon(type: string): string {
    const iconMap: Record<string, string> = {
      'EXPERIMENT_SUMMARY': 'science',
      'HYPOTHESIS_ANALYSIS': 'analytics',
      'PROBLEM_OVERVIEW': 'assignment',
      'METRICS_REPORT': 'show_chart',
      'COMPARISON_REPORT': 'compare',
      'CUSTOM': 'description'
    };
    return iconMap[type] || 'description';
  }
  
  /**
   * Formats file size
   */
  formatFileSize(bytes?: number): string {
    if (!bytes) return 'N/A';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
  
  /**
   * Gets generation progress percentage
   */
  getProgress(reportId: string, progressMap: any): number {
    return progressMap[reportId]?.progress || 0;
  }
  
  /**
   * Checks if report can be downloaded
   */
  canDownload(report: Report): boolean {
    return report.status === ReportStatus.COMPLETED && !!report.fileUrl;
  }
}
