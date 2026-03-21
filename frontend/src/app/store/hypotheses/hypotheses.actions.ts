import { createAction, props } from '@ngrx/store';
import { 
  Hypothesis, 
  CreateHypothesisRequest, 
  UpdateHypothesisRequest, 
  HypothesisQueryParams,
  PaginatedHypothesesResponse 
} from '../../models/hypothesis.model';

/**
 * Hypotheses Actions
 * 
 * Actions for managing hypotheses state.
 */

/**
 * Load Hypotheses
 */
export const loadHypotheses = createAction(
  '[Hypotheses] Load Hypotheses',
  props<{ params?: HypothesisQueryParams }>()
);

export const loadHypothesesSuccess = createAction(
  '[Hypotheses] Load Hypotheses Success',
  props<{ response: PaginatedHypothesesResponse }>()
);

export const loadHypothesesFailure = createAction(
  '[Hypotheses] Load Hypotheses Failure',
  props<{ error: string }>()
);

/**
 * Load Hypotheses by Problem
 */
export const loadHypothesesByProblem = createAction(
  '[Hypotheses] Load Hypotheses By Problem',
  props<{ problemId: string; params?: HypothesisQueryParams }>()
);

/**
 * Load Hypothesis by ID
 */
export const loadHypothesis = createAction(
  '[Hypotheses] Load Hypothesis',
  props<{ id: string }>()
);

export const loadHypothesisSuccess = createAction(
  '[Hypotheses] Load Hypothesis Success',
  props<{ hypothesis: Hypothesis }>()
);

export const loadHypothesisFailure = createAction(
  '[Hypotheses] Load Hypothesis Failure',
  props<{ error: string }>()
);

/**
 * Create Hypothesis
 */
export const createHypothesis = createAction(
  '[Hypotheses] Create Hypothesis',
  props<{ request: CreateHypothesisRequest }>()
);

export const createHypothesisSuccess = createAction(
  '[Hypotheses] Create Hypothesis Success',
  props<{ hypothesis: Hypothesis }>()
);

export const createHypothesisFailure = createAction(
  '[Hypotheses] Create Hypothesis Failure',
  props<{ error: string }>()
);

/**
 * Update Hypothesis
 */
export const updateHypothesis = createAction(
  '[Hypotheses] Update Hypothesis',
  props<{ id: string; request: UpdateHypothesisRequest }>()
);

export const updateHypothesisSuccess = createAction(
  '[Hypotheses] Update Hypothesis Success',
  props<{ hypothesis: Hypothesis }>()
);

export const updateHypothesisFailure = createAction(
  '[Hypotheses] Update Hypothesis Failure',
  props<{ error: string }>()
);

/**
 * Delete Hypothesis
 */
export const deleteHypothesis = createAction(
  '[Hypotheses] Delete Hypothesis',
  props<{ id: string }>()
);

export const deleteHypothesisSuccess = createAction(
  '[Hypotheses] Delete Hypothesis Success',
  props<{ id: string }>()
);

export const deleteHypothesisFailure = createAction(
  '[Hypotheses] Delete Hypothesis Failure',
  props<{ error: string }>()
);

/**
 * Select Hypothesis
 */
export const selectHypothesis = createAction(
  '[Hypotheses] Select Hypothesis',
  props<{ hypothesis: Hypothesis | null }>()
);

/**
 * Set Filters
 */
export const setHypothesesFilters = createAction(
  '[Hypotheses] Set Filters',
  props<{ filters: { problemId?: string; status?: string } }>()
);

/**
 * Clear Hypotheses Error
 */
export const clearHypothesesError = createAction(
  '[Hypotheses] Clear Error'
);
