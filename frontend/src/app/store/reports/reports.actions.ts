import { createAction, props } from '@ngrx/store';
import { 
  Report, 
  CreateReportRequest,
  ReportQueryParams,
  PaginatedReportsResponse,
  ReportPreview,
  ReportProgress
} from '../../models/report.model';

/**
 * Reports Actions
 * 
 * Actions for managing reports state.
 */

/**
 * Load Reports
 */
export const loadReports = createAction(
  '[Reports] Load Reports',
  props<{ params?: ReportQueryParams }>()
);

export const loadReportsSuccess = createAction(
  '[Reports] Load Reports Success',
  props<{ response: PaginatedReportsResponse }>()
);

export const loadReportsFailure = createAction(
  '[Reports] Load Reports Failure',
  props<{ error: string }>()
);

/**
 * Load Report
 */
export const loadReport = createAction(
  '[Reports] Load Report',
  props<{ id: string }>()
);

export const loadReportSuccess = createAction(
  '[Reports] Load Report Success',
  props<{ report: Report }>()
);

export const loadReportFailure = createAction(
  '[Reports] Load Report Failure',
  props<{ error: string }>()
);

/**
 * Create Report
 */
export const createReport = createAction(
  '[Reports] Create Report',
  props<{ request: CreateReportRequest }>()
);

export const createReportSuccess = createAction(
  '[Reports] Create Report Success',
  props<{ report: Report }>()
);

export const createReportFailure = createAction(
  '[Reports] Create Report Failure',
  props<{ error: string }>()
);

/**
 * Delete Report
 */
export const deleteReport = createAction(
  '[Reports] Delete Report',
  props<{ id: string }>()
);

export const deleteReportSuccess = createAction(
  '[Reports] Delete Report Success',
  props<{ id: string }>()
);

export const deleteReportFailure = createAction(
  '[Reports] Delete Report Failure',
  props<{ error: string }>()
);

/**
 * Download Report
 */
export const downloadReport = createAction(
  '[Reports] Download Report',
  props<{ id: string; format?: string }>()
);

export const downloadReportSuccess = createAction(
  '[Reports] Download Report Success',
  props<{ id: string }>()
);

export const downloadReportFailure = createAction(
  '[Reports] Download Report Failure',
  props<{ error: string }>()
);

/**
 * Load Report Preview
 */
export const loadReportPreview = createAction(
  '[Reports] Load Report Preview',
  props<{ id: string }>()
);

export const loadReportPreviewSuccess = createAction(
  '[Reports] Load Report Preview Success',
  props<{ preview: ReportPreview }>()
);

export const loadReportPreviewFailure = createAction(
  '[Reports] Load Report Preview Failure',
  props<{ error: string }>()
);

/**
 * Update Report Progress
 */
export const updateReportProgress = createAction(
  '[Reports] Update Report Progress',
  props<{ progress: ReportProgress }>()
);

/**
 * Set Filters
 */
export const setReportsFilters = createAction(
  '[Reports] Set Filters',
  props<{ filters: ReportQueryParams }>()
);

/**
 * Clear Reports
 */
export const clearReports = createAction(
  '[Reports] Clear Reports'
);

/**
 * Clear Reports Error
 */
export const clearReportsError = createAction(
  '[Reports] Clear Error'
);
