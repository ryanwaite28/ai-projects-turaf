import { createReducer, on } from '@ngrx/store';
import { ProblemsState, initialProblemsState } from './problems.state';
import * as ProblemsActions from './problems.actions';

/**
 * Problems Reducer
 * 
 * Handles problems state updates.
 */
export const problemsReducer = createReducer(
  initialProblemsState,
  
  // Load Problems
  on(ProblemsActions.loadProblems, (state): ProblemsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ProblemsActions.loadProblemsSuccess, (state, { response }): ProblemsState => ({
    ...state,
    problems: response.problems,
    pagination: {
      page: response.page,
      limit: response.limit,
      total: response.total,
      totalPages: response.totalPages
    },
    loading: false,
    error: null
  })),
  
  on(ProblemsActions.loadProblemsFailure, (state, { error }): ProblemsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Problem
  on(ProblemsActions.loadProblem, (state): ProblemsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ProblemsActions.loadProblemSuccess, (state, { problem }): ProblemsState => ({
    ...state,
    selectedProblem: problem,
    loading: false,
    error: null
  })),
  
  on(ProblemsActions.loadProblemFailure, (state, { error }): ProblemsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Create Problem
  on(ProblemsActions.createProblem, (state): ProblemsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ProblemsActions.createProblemSuccess, (state, { problem }): ProblemsState => ({
    ...state,
    problems: [problem, ...state.problems],
    pagination: {
      ...state.pagination,
      total: state.pagination.total + 1
    },
    loading: false,
    error: null
  })),
  
  on(ProblemsActions.createProblemFailure, (state, { error }): ProblemsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Update Problem
  on(ProblemsActions.updateProblem, (state): ProblemsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ProblemsActions.updateProblemSuccess, (state, { problem }): ProblemsState => ({
    ...state,
    problems: state.problems.map(p => p.id === problem.id ? problem : p),
    selectedProblem: state.selectedProblem?.id === problem.id ? problem : state.selectedProblem,
    loading: false,
    error: null
  })),
  
  on(ProblemsActions.updateProblemFailure, (state, { error }): ProblemsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Delete Problem
  on(ProblemsActions.deleteProblem, (state): ProblemsState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(ProblemsActions.deleteProblemSuccess, (state, { id }): ProblemsState => ({
    ...state,
    problems: state.problems.filter(p => p.id !== id),
    selectedProblem: state.selectedProblem?.id === id ? null : state.selectedProblem,
    pagination: {
      ...state.pagination,
      total: state.pagination.total - 1
    },
    loading: false,
    error: null
  })),
  
  on(ProblemsActions.deleteProblemFailure, (state, { error }): ProblemsState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Select Problem
  on(ProblemsActions.selectProblem, (state, { problem }): ProblemsState => ({
    ...state,
    selectedProblem: problem
  })),
  
  // Clear Error
  on(ProblemsActions.clearProblemsError, (state): ProblemsState => ({
    ...state,
    error: null
  }))
);
