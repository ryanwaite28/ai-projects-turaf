import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ExperimentsState } from './experiments.state';
import { ExperimentStatus } from '../../models/experiment.model';

/**
 * Experiments Selectors
 * 
 * Selectors for accessing experiments state.
 */

/**
 * Feature selector for experiments state
 */
export const selectExperimentsState = createFeatureSelector<ExperimentsState>('experiments');

/**
 * Select all experiments
 */
export const selectAllExperiments = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.experiments
);

/**
 * Select selected experiment
 */
export const selectSelectedExperiment = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.selectedExperiment
);

/**
 * Select experiments loading state
 */
export const selectExperimentsLoading = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.loading
);

/**
 * Select experiments error
 */
export const selectExperimentsError = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.error
);

/**
 * Select pagination info
 */
export const selectExperimentsPagination = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.pagination
);

/**
 * Select filters
 */
export const selectExperimentsFilters = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.filters
);

/**
 * Select real-time updates status
 */
export const selectRealTimeUpdates = createSelector(
  selectExperimentsState,
  (state: ExperimentsState) => state.realTimeUpdates
);

/**
 * Select total experiments count
 */
export const selectExperimentsTotal = createSelector(
  selectExperimentsPagination,
  (pagination) => pagination.total
);

/**
 * Select current page
 */
export const selectExperimentsCurrentPage = createSelector(
  selectExperimentsPagination,
  (pagination) => pagination.page
);

/**
 * Select total pages
 */
export const selectExperimentsTotalPages = createSelector(
  selectExperimentsPagination,
  (pagination) => pagination.totalPages
);

/**
 * Select experiment by ID
 */
export const selectExperimentById = (id: string) => createSelector(
  selectAllExperiments,
  (experiments) => experiments.find(e => e.id === id)
);

/**
 * Select experiments by hypothesis ID
 */
export const selectExperimentsByHypothesisId = (hypothesisId: string) => createSelector(
  selectAllExperiments,
  (experiments) => experiments.filter(e => e.hypothesisId === hypothesisId)
);

/**
 * Select experiments by status
 */
export const selectExperimentsByStatus = (status: ExperimentStatus) => createSelector(
  selectAllExperiments,
  (experiments) => experiments.filter(e => e.status === status)
);

/**
 * Select running experiments
 */
export const selectRunningExperiments = createSelector(
  selectAllExperiments,
  (experiments) => experiments.filter(e => e.status === ExperimentStatus.RUNNING)
);

/**
 * Select completed experiments
 */
export const selectCompletedExperiments = createSelector(
  selectAllExperiments,
  (experiments) => experiments.filter(e => e.status === ExperimentStatus.COMPLETED)
);

/**
 * Select experiments statistics
 */
export const selectExperimentsStats = createSelector(
  selectAllExperiments,
  (experiments) => ({
    total: experiments.length,
    running: experiments.filter(e => e.status === ExperimentStatus.RUNNING).length,
    completed: experiments.filter(e => e.status === ExperimentStatus.COMPLETED).length,
    failed: experiments.filter(e => e.status === ExperimentStatus.FAILED).length,
    draft: experiments.filter(e => e.status === ExperimentStatus.DRAFT).length
  })
);
