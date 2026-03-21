import { createReducer, on } from '@ngrx/store';
import { ReportsState, initialReportsState } from './reports.state';
import * as ReportsActions from './reports.actions';

/**
 * Reports Reducer
 * 
 * Handles reports state updates.
 */
export const reportsReducer = createReducer(
  initialReportsState,
  
  // Load Reports
  on(ReportsActions.loadReports, (state): ReportsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ReportsActions.loadReportsSuccess, (state, { response }): ReportsState => ({
    ...state,
    reports: response.reports,
    pagination: {
      page: response.page,
      limit: response.limit,
      total: response.total,
      totalPages: response.totalPages
    },
    loading: false,
    error: null
  })),
  
  on(ReportsActions.loadReportsFailure, (state, { error }): ReportsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Report
  on(ReportsActions.loadReport, (state): ReportsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ReportsActions.loadReportSuccess, (state, { report }): ReportsState => ({
    ...state,
    selectedReport: report,
    loading: false,
    error: null
  })),
  
  on(ReportsActions.loadReportFailure, (state, { error }): ReportsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Create Report
  on(ReportsActions.createReport, (state): ReportsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ReportsActions.createReportSuccess, (state, { report }): ReportsState => ({
    ...state,
    reports: [report, ...state.reports],
    generationProgress: {
      ...state.generationProgress,
      [report.id]: {
        reportId: report.id,
        status: report.status,
        progress: 0,
        message: 'Report generation started'
      }
    },
    loading: false,
    error: null
  })),
  
  on(ReportsActions.createReportFailure, (state, { error }): ReportsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Delete Report
  on(ReportsActions.deleteReport, (state): ReportsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ReportsActions.deleteReportSuccess, (state, { id }): ReportsState => ({
    ...state,
    reports: state.reports.filter(r => r.id !== id),
    selectedReport: state.selectedReport?.id === id ? null : state.selectedReport,
    loading: false,
    error: null
  })),
  
  on(ReportsActions.deleteReportFailure, (state, { error }): ReportsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Download Report
  on(ReportsActions.downloadReport, (state): ReportsState => ({
    ...state,
    error: null
  })),
  
  on(ReportsActions.downloadReportSuccess, (state): ReportsState => state),
  
  on(ReportsActions.downloadReportFailure, (state, { error }): ReportsState => ({
    ...state,
    error
  })),
  
  // Load Report Preview
  on(ReportsActions.loadReportPreview, (state): ReportsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ReportsActions.loadReportPreviewSuccess, (state, { preview }): ReportsState => ({
    ...state,
    reportPreview: preview,
    loading: false,
    error: null
  })),
  
  on(ReportsActions.loadReportPreviewFailure, (state, { error }): ReportsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Update Report Progress
  on(ReportsActions.updateReportProgress, (state, { progress }): ReportsState => {
    const updatedReports = state.reports.map(report =>
      report.id === progress.reportId
        ? { ...report, status: progress.status }
        : report
    );
    
    return {
      ...state,
      reports: updatedReports,
      generationProgress: {
        ...state.generationProgress,
        [progress.reportId]: progress
      }
    };
  }),
  
  // Set Filters
  on(ReportsActions.setReportsFilters, (state, { filters }): ReportsState => ({
    ...state,
    filters: { ...state.filters, ...filters }
  })),
  
  // Clear Reports
  on(ReportsActions.clearReports, (): ReportsState => ({
    ...initialReportsState
  })),
  
  // Clear Error
  on(ReportsActions.clearReportsError, (state): ReportsState => ({
    ...state,
    error: null
  }))
);
