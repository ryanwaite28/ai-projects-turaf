import { createReducer, on } from '@ngrx/store';
import { HypothesesState, initialHypothesesState } from './hypotheses.state';
import * as HypothesesActions from './hypotheses.actions';

/**
 * Hypotheses Reducer
 * 
 * Handles hypotheses state updates.
 */
export const hypothesesReducer = createReducer(
  initialHypothesesState,
  
  // Load Hypotheses
  on(HypothesesActions.loadHypotheses, (state): HypothesesState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(HypothesesActions.loadHypothesesSuccess, (state, { response }): HypothesesState => ({
    ...state,
    hypotheses: response.hypotheses,
    pagination: {
      page: response.page,
      limit: response.limit,
      total: response.total,
      totalPages: response.totalPages
    },
    loading: false,
    error: null
  })),
  
  on(HypothesesActions.loadHypothesesFailure, (state, { error }): HypothesesState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Hypotheses by Problem
  on(HypothesesActions.loadHypothesesByProblem, (state, { problemId }): HypothesesState => ({
    ...state,
    loading: true,
    error: null,
    filters: { ...state.filters, problemId }
  })),
  
  // Load Hypothesis
  on(HypothesesActions.loadHypothesis, (state): HypothesesState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(HypothesesActions.loadHypothesisSuccess, (state, { hypothesis }): HypothesesState => ({
    ...state,
    selectedHypothesis: hypothesis,
    loading: false,
    error: null
  })),
  
  on(HypothesesActions.loadHypothesisFailure, (state, { error }): HypothesesState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Create Hypothesis
  on(HypothesesActions.createHypothesis, (state): HypothesesState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(HypothesesActions.createHypothesisSuccess, (state, { hypothesis }): HypothesesState => ({
    ...state,
    hypotheses: [hypothesis, ...state.hypotheses],
    pagination: {
      ...state.pagination,
      total: state.pagination.total + 1
    },
    loading: false,
    error: null
  })),
  
  on(HypothesesActions.createHypothesisFailure, (state, { error }): HypothesesState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Update Hypothesis
  on(HypothesesActions.updateHypothesis, (state): HypothesesState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(HypothesesActions.updateHypothesisSuccess, (state, { hypothesis }): HypothesesState => ({
    ...state,
    hypotheses: state.hypotheses.map(h => h.id === hypothesis.id ? hypothesis : h),
    selectedHypothesis: state.selectedHypothesis?.id === hypothesis.id ? hypothesis : state.selectedHypothesis,
    loading: false,
    error: null
  })),
  
  on(HypothesesActions.updateHypothesisFailure, (state, { error }): HypothesesState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Delete Hypothesis
  on(HypothesesActions.deleteHypothesis, (state): HypothesesState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(HypothesesActions.deleteHypothesisSuccess, (state, { id }): HypothesesState => ({
    ...state,
    hypotheses: state.hypotheses.filter(h => h.id !== id),
    selectedHypothesis: state.selectedHypothesis?.id === id ? null : state.selectedHypothesis,
    pagination: {
      ...state.pagination,
      total: state.pagination.total - 1
    },
    loading: false,
    error: null
  })),
  
  on(HypothesesActions.deleteHypothesisFailure, (state, { error }): HypothesesState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Select Hypothesis
  on(HypothesesActions.selectHypothesis, (state, { hypothesis }): HypothesesState => ({
    ...state,
    selectedHypothesis: hypothesis
  })),
  
  // Set Filters
  on(HypothesesActions.setHypothesesFilters, (state, { filters }): HypothesesState => ({
    ...state,
    filters: { ...state.filters, ...filters }
  })),
  
  // Clear Error
  on(HypothesesActions.clearHypothesesError, (state): HypothesesState => ({
    ...state,
    error: null
  }))
);
