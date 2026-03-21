import { createAction, props } from '@ngrx/store';
import { 
  Experiment, 
  CreateExperimentRequest, 
  UpdateExperimentRequest, 
  ExperimentQueryParams,
  PaginatedExperimentsResponse,
  ExperimentStateTransition
} from '../../models/experiment.model';

/**
 * Experiments Actions
 * 
 * Actions for managing experiments state.
 */

/**
 * Load Experiments
 */
export const loadExperiments = createAction(
  '[Experiments] Load Experiments',
  props<{ params?: ExperimentQueryParams }>()
);

export const loadExperimentsSuccess = createAction(
  '[Experiments] Load Experiments Success',
  props<{ response: PaginatedExperimentsResponse }>()
);

export const loadExperimentsFailure = createAction(
  '[Experiments] Load Experiments Failure',
  props<{ error: string }>()
);

/**
 * Load Experiments by Hypothesis
 */
export const loadExperimentsByHypothesis = createAction(
  '[Experiments] Load Experiments By Hypothesis',
  props<{ hypothesisId: string; params?: ExperimentQueryParams }>()
);

/**
 * Load Experiment by ID
 */
export const loadExperiment = createAction(
  '[Experiments] Load Experiment',
  props<{ id: string }>()
);

export const loadExperimentSuccess = createAction(
  '[Experiments] Load Experiment Success',
  props<{ experiment: Experiment }>()
);

export const loadExperimentFailure = createAction(
  '[Experiments] Load Experiment Failure',
  props<{ error: string }>()
);

/**
 * Create Experiment
 */
export const createExperiment = createAction(
  '[Experiments] Create Experiment',
  props<{ request: CreateExperimentRequest }>()
);

export const createExperimentSuccess = createAction(
  '[Experiments] Create Experiment Success',
  props<{ experiment: Experiment }>()
);

export const createExperimentFailure = createAction(
  '[Experiments] Create Experiment Failure',
  props<{ error: string }>()
);

/**
 * Update Experiment
 */
export const updateExperiment = createAction(
  '[Experiments] Update Experiment',
  props<{ id: string; request: UpdateExperimentRequest }>()
);

export const updateExperimentSuccess = createAction(
  '[Experiments] Update Experiment Success',
  props<{ experiment: Experiment }>()
);

export const updateExperimentFailure = createAction(
  '[Experiments] Update Experiment Failure',
  props<{ error: string }>()
);

/**
 * Delete Experiment
 */
export const deleteExperiment = createAction(
  '[Experiments] Delete Experiment',
  props<{ id: string }>()
);

export const deleteExperimentSuccess = createAction(
  '[Experiments] Delete Experiment Success',
  props<{ id: string }>()
);

export const deleteExperimentFailure = createAction(
  '[Experiments] Delete Experiment Failure',
  props<{ error: string }>()
);

/**
 * Experiment State Transitions
 */
export const startExperiment = createAction(
  '[Experiments] Start Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const pauseExperiment = createAction(
  '[Experiments] Pause Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const resumeExperiment = createAction(
  '[Experiments] Resume Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const completeExperiment = createAction(
  '[Experiments] Complete Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const cancelExperiment = createAction(
  '[Experiments] Cancel Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const failExperiment = createAction(
  '[Experiments] Fail Experiment',
  props<{ id: string; transition: ExperimentStateTransition }>()
);

export const experimentStateTransitionSuccess = createAction(
  '[Experiments] State Transition Success',
  props<{ experiment: Experiment }>()
);

export const experimentStateTransitionFailure = createAction(
  '[Experiments] State Transition Failure',
  props<{ error: string }>()
);

/**
 * Select Experiment
 */
export const selectExperiment = createAction(
  '[Experiments] Select Experiment',
  props<{ experiment: Experiment | null }>()
);

/**
 * Set Filters
 */
export const setExperimentsFilters = createAction(
  '[Experiments] Set Filters',
  props<{ filters: { hypothesisId?: string; status?: string } }>()
);

/**
 * Toggle Real-Time Updates
 */
export const toggleRealTimeUpdates = createAction(
  '[Experiments] Toggle Real-Time Updates',
  props<{ enabled: boolean }>()
);

/**
 * Real-Time Update Received
 */
export const experimentUpdated = createAction(
  '[Experiments] Experiment Updated',
  props<{ experiment: Experiment }>()
);

/**
 * Clear Experiments Error
 */
export const clearExperimentsError = createAction(
  '[Experiments] Clear Error'
);
