import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AppState } from '../../../store/app.state';
import {
  loadReport,
  loadReportPreview,
  downloadReport,
  deleteReport
} from '../../../store/reports/reports.actions';
import {
  selectSelectedReport,
  selectReportPreview,
  selectReportsLoading,
  selectReportsError
} from '../../../store/reports/reports.selectors';
import { Report } from '../../../models/report.model';

/**
 * Report Preview Component
 * 
 * Displays preview of a generated report.
 */
@Component({
  selector: 'app-report-preview',
  templateUrl: './report-preview.component.html',
  styleUrls: ['./report-preview.component.scss']
})
export class ReportPreviewComponent implements OnInit, OnDestroy {
  
  report$ = this.store.select(selectSelectedReport);
  preview$ = this.store.select(selectReportPreview);
  loading$ = this.store.select(selectReportsLoading);
  error$ = this.store.select(selectReportsError);
  
  private destroy$ = new Subject<void>();
  private reportId: string | null = null;
  
  constructor(
    private store: Store<AppState>,
    private route: ActivatedRoute,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.reportId = params['id'];
        if (this.reportId) {
          this.store.dispatch(loadReport({ id: this.reportId }));
          this.store.dispatch(loadReportPreview({ id: this.reportId }));
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Navigates back to reports list
   */
  goBack(): void {
    this.router.navigate(['/reports']);
  }
  
  /**
   * Downloads the report
   */
  downloadReportFile(report: Report): void {
    this.store.dispatch(downloadReport({ id: report.id, format: report.format }));
  }
  
  /**
   * Deletes the report
   */
  deleteReportItem(report: Report): void {
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
}
