import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ReportsState } from './reports.state';

/**
 * Reports Selectors
 * 
 * Selectors for accessing reports state.
 */

/**
 * Feature selector for reports state
 */
export const selectReportsState = createFeatureSelector<ReportsState>('reports');

/**
 * Select all reports
 */
export const selectAllReports = createSelector(
  selectReportsState,
  (state: ReportsState) => state.reports
);

/**
 * Select selected report
 */
export const selectSelectedReport = createSelector(
  selectReportsState,
  (state: ReportsState) => state.selectedReport
);

/**
 * Select report preview
 */
export const selectReportPreview = createSelector(
  selectReportsState,
  (state: ReportsState) => state.reportPreview
);

/**
 * Select generation progress
 */
export const selectGenerationProgress = createSelector(
  selectReportsState,
  (state: ReportsState) => state.generationProgress
);

/**
 * Select reports loading state
 */
export const selectReportsLoading = createSelector(
  selectReportsState,
  (state: ReportsState) => state.loading
);

/**
 * Select reports error
 */
export const selectReportsError = createSelector(
  selectReportsState,
  (state: ReportsState) => state.error
);

/**
 * Select reports pagination
 */
export const selectReportsPagination = createSelector(
  selectReportsState,
  (state: ReportsState) => state.pagination
);

/**
 * Select reports filters
 */
export const selectReportsFilters = createSelector(
  selectReportsState,
  (state: ReportsState) => state.filters
);

/**
 * Select report by ID
 */
export const selectReportById = (id: string) => createSelector(
  selectAllReports,
  (reports) => reports.find(r => r.id === id)
);

/**
 * Select reports by experiment ID
 */
export const selectReportsByExperiment = (experimentId: string) => createSelector(
  selectAllReports,
  (reports) => reports.filter(r => r.experimentId === experimentId)
);

/**
 * Select reports by status
 */
export const selectReportsByStatus = (status: string) => createSelector(
  selectAllReports,
  (reports) => reports.filter(r => r.status === status)
);

/**
 * Select completed reports count
 */
export const selectCompletedReportsCount = createSelector(
  selectAllReports,
  (reports) => reports.filter(r => r.status === 'COMPLETED').length
);

/**
 * Select pending reports count
 */
export const selectPendingReportsCount = createSelector(
  selectAllReports,
  (reports) => reports.filter(r => r.status === 'PENDING' || r.status === 'GENERATING').length
);
