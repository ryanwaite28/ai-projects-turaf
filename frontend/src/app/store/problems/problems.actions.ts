import { createAction, props } from '@ngrx/store';
import { 
  Problem, 
  CreateProblemRequest, 
  UpdateProblemRequest, 
  ProblemQueryParams,
  PaginatedProblemsResponse 
} from '../../models/problem.model';

/**
 * Problems Actions
 * 
 * Actions for managing problems state.
 */

/**
 * Load Problems
 */
export const loadProblems = createAction(
  '[Problems] Load Problems',
  props<{ params?: ProblemQueryParams }>()
);

export const loadProblemsSuccess = createAction(
  '[Problems] Load Problems Success',
  props<{ response: PaginatedProblemsResponse }>()
);

export const loadProblemsFailure = createAction(
  '[Problems] Load Problems Failure',
  props<{ error: string }>()
);

/**
 * Load Problem by ID
 */
export const loadProblem = createAction(
  '[Problems] Load Problem',
  props<{ id: string }>()
);

export const loadProblemSuccess = createAction(
  '[Problems] Load Problem Success',
  props<{ problem: Problem }>()
);

export const loadProblemFailure = createAction(
  '[Problems] Load Problem Failure',
  props<{ error: string }>()
);

/**
 * Create Problem
 */
export const createProblem = createAction(
  '[Problems] Create Problem',
  props<{ request: CreateProblemRequest }>()
);

export const createProblemSuccess = createAction(
  '[Problems] Create Problem Success',
  props<{ problem: Problem }>()
);

export const createProblemFailure = createAction(
  '[Problems] Create Problem Failure',
  props<{ error: string }>()
);

/**
 * Update Problem
 */
export const updateProblem = createAction(
  '[Problems] Update Problem',
  props<{ id: string; request: UpdateProblemRequest }>()
);

export const updateProblemSuccess = createAction(
  '[Problems] Update Problem Success',
  props<{ problem: Problem }>()
);

export const updateProblemFailure = createAction(
  '[Problems] Update Problem Failure',
  props<{ error: string }>()
);

/**
 * Delete Problem
 */
export const deleteProblem = createAction(
  '[Problems] Delete Problem',
  props<{ id: string }>()
);

export const deleteProblemSuccess = createAction(
  '[Problems] Delete Problem Success',
  props<{ id: string }>()
);

export const deleteProblemFailure = createAction(
  '[Problems] Delete Problem Failure',
  props<{ error: string }>()
);

/**
 * Select Problem
 */
export const selectProblem = createAction(
  '[Problems] Select Problem',
  props<{ problem: Problem | null }>()
);

/**
 * Clear Problems Error
 */
export const clearProblemsError = createAction(
  '[Problems] Clear Error'
);
