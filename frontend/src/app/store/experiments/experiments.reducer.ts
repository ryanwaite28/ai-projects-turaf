import { createReducer, on } from '@ngrx/store';
import { ExperimentsState, initialExperimentsState } from './experiments.state';
import * as ExperimentsActions from './experiments.actions';

/**
 * Experiments Reducer
 * 
 * Handles experiments state updates.
 */
export const experimentsReducer = createReducer(
  initialExperimentsState,
  
  // Load Experiments
  on(ExperimentsActions.loadExperiments, (state): ExperimentsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ExperimentsActions.loadExperimentsSuccess, (state, { response }): ExperimentsState => ({
    ...state,
    experiments: response.experiments,
    pagination: {
      page: response.page,
      limit: response.limit,
      total: response.total,
      totalPages: response.totalPages
    },
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.loadExperimentsFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Experiments by Hypothesis
  on(ExperimentsActions.loadExperimentsByHypothesis, (state, { hypothesisId }): ExperimentsState => ({
    ...state,
    loading: true,
    error: null,
    filters: { ...state.filters, hypothesisId }
  })),
  
  // Load Experiment
  on(ExperimentsActions.loadExperiment, (state): ExperimentsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ExperimentsActions.loadExperimentSuccess, (state, { experiment }): ExperimentsState => ({
    ...state,
    selectedExperiment: experiment,
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.loadExperimentFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Create Experiment
  on(ExperimentsActions.createExperiment, (state): ExperimentsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ExperimentsActions.createExperimentSuccess, (state, { experiment }): ExperimentsState => ({
    ...state,
    experiments: [experiment, ...state.experiments],
    pagination: {
      ...state.pagination,
      total: state.pagination.total + 1
    },
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.createExperimentFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Update Experiment
  on(ExperimentsActions.updateExperiment, (state): ExperimentsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ExperimentsActions.updateExperimentSuccess, (state, { experiment }): ExperimentsState => ({
    ...state,
    experiments: state.experiments.map(e => e.id === experiment.id ? experiment : e),
    selectedExperiment: state.selectedExperiment?.id === experiment.id ? experiment : state.selectedExperiment,
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.updateExperimentFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Delete Experiment
  on(ExperimentsActions.deleteExperiment, (state): ExperimentsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ExperimentsActions.deleteExperimentSuccess, (state, { id }): ExperimentsState => ({
    ...state,
    experiments: state.experiments.filter(e => e.id !== id),
    selectedExperiment: state.selectedExperiment?.id === id ? null : state.selectedExperiment,
    pagination: {
      ...state.pagination,
      total: state.pagination.total - 1
    },
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.deleteExperimentFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // State Transitions
  on(
    ExperimentsActions.startExperiment,
    ExperimentsActions.pauseExperiment,
    ExperimentsActions.resumeExperiment,
    ExperimentsActions.completeExperiment,
    ExperimentsActions.cancelExperiment,
    ExperimentsActions.failExperiment,
    (state): ExperimentsState => ({
      ...state,
      loading: true,
      error: null
    })
  ),
  
  on(ExperimentsActions.experimentStateTransitionSuccess, (state, { experiment }): ExperimentsState => ({
    ...state,
    experiments: state.experiments.map(e => e.id === experiment.id ? experiment : e),
    selectedExperiment: state.selectedExperiment?.id === experiment.id ? experiment : state.selectedExperiment,
    loading: false,
    error: null
  })),
  
  on(ExperimentsActions.experimentStateTransitionFailure, (state, { error }): ExperimentsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Select Experiment
  on(ExperimentsActions.selectExperiment, (state, { experiment }): ExperimentsState => ({
    ...state,
    selectedExperiment: experiment
  })),
  
  // Set Filters
  on(ExperimentsActions.setExperimentsFilters, (state, { filters }): ExperimentsState => ({
    ...state,
    filters: { ...state.filters, ...filters }
  })),
  
  // Toggle Real-Time Updates
  on(ExperimentsActions.toggleRealTimeUpdates, (state, { enabled }): ExperimentsState => ({
    ...state,
    realTimeUpdates: enabled
  })),
  
  // Real-Time Update
  on(ExperimentsActions.experimentUpdated, (state, { experiment }): ExperimentsState => ({
    ...state,
    experiments: state.experiments.map(e => e.id === experiment.id ? experiment : e),
    selectedExperiment: state.selectedExperiment?.id === experiment.id ? experiment : state.selectedExperiment
  })),
  
  // Clear Error
  on(ExperimentsActions.clearExperimentsError, (state): ExperimentsState => ({
    ...state,
    error: null
  }))
);
