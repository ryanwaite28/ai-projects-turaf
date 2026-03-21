import { createFeatureSelector, createSelector } from '@ngrx/store';
import { HypothesesState } from './hypotheses.state';

/**
 * Hypotheses Selectors
 * 
 * Selectors for accessing hypotheses state.
 */

/**
 * Feature selector for hypotheses state
 */
export const selectHypothesesState = createFeatureSelector<HypothesesState>('hypotheses');

/**
 * Select all hypotheses
 */
export const selectAllHypotheses = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.hypotheses
);

/**
 * Select selected hypothesis
 */
export const selectSelectedHypothesis = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.selectedHypothesis
);

/**
 * Select hypotheses loading state
 */
export const selectHypothesesLoading = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.loading
);

/**
 * Select hypotheses error
 */
export const selectHypothesesError = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.error
);

/**
 * Select pagination info
 */
export const selectHypothesesPagination = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.pagination
);

/**
 * Select filters
 */
export const selectHypothesesFilters = createSelector(
  selectHypothesesState,
  (state: HypothesesState) => state.filters
);

/**
 * Select total hypotheses count
 */
export const selectHypothesesTotal = createSelector(
  selectHypothesesPagination,
  (pagination) => pagination.total
);

/**
 * Select current page
 */
export const selectHypothesesCurrentPage = createSelector(
  selectHypothesesPagination,
  (pagination) => pagination.page
);

/**
 * Select total pages
 */
export const selectHypothesesTotalPages = createSelector(
  selectHypothesesPagination,
  (pagination) => pagination.totalPages
);

/**
 * Select hypothesis by ID
 */
export const selectHypothesisById = (id: string) => createSelector(
  selectAllHypotheses,
  (hypotheses) => hypotheses.find(h => h.id === id)
);

/**
 * Select hypotheses by problem ID
 */
export const selectHypothesesByProblemId = (problemId: string) => createSelector(
  selectAllHypotheses,
  (hypotheses) => hypotheses.filter(h => h.problemId === problemId)
);
