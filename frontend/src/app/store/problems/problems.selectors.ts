import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ProblemsState } from './problems.state';

/**
 * Problems Selectors
 * 
 * Selectors for accessing problems state.
 */

/**
 * Feature selector for problems state
 */
export const selectProblemsState = createFeatureSelector<ProblemsState>('problems');

/**
 * Select all problems
 */
export const selectAllProblems = createSelector(
  selectProblemsState,
  (state: ProblemsState) => state.problems
);

/**
 * Select selected problem
 */
export const selectSelectedProblem = createSelector(
  selectProblemsState,
  (state: ProblemsState) => state.selectedProblem
);

/**
 * Select problems loading state
 */
export const selectProblemsLoading = createSelector(
  selectProblemsState,
  (state: ProblemsState) => state.loading
);

/**
 * Select problems error
 */
export const selectProblemsError = createSelector(
  selectProblemsState,
  (state: ProblemsState) => state.error
);

/**
 * Select pagination info
 */
export const selectProblemsPagination = createSelector(
  selectProblemsState,
  (state: ProblemsState) => state.pagination
);

/**
 * Select total problems count
 */
export const selectProblemsTotal = createSelector(
  selectProblemsPagination,
  (pagination) => pagination.total
);

/**
 * Select current page
 */
export const selectProblemsCurrentPage = createSelector(
  selectProblemsPagination,
  (pagination) => pagination.page
);

/**
 * Select total pages
 */
export const selectProblemsTotalPages = createSelector(
  selectProblemsPagination,
  (pagination) => pagination.totalPages
);

/**
 * Select problem by ID
 */
export const selectProblemById = (id: string) => createSelector(
  selectAllProblems,
  (problems) => problems.find(p => p.id === id)
);
