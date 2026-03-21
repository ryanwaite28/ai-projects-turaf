import { createReducer, on } from '@ngrx/store';
import { DashboardState, initialDashboardState } from './dashboard.state';
import * as DashboardActions from './dashboard.actions';

/**
 * Dashboard Reducer
 * 
 * Handles dashboard state updates.
 */
export const dashboardReducer = createReducer(
  initialDashboardState,
  
  // Load Dashboard
  on(DashboardActions.loadDashboard, (state): DashboardState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(DashboardActions.loadDashboardSuccess, (state, { data }): DashboardState => ({
    ...state,
    stats: data.stats,
    recentExperiments: data.recentExperiments,
    metrics: data.metrics,
    loading: false,
    error: null,
    lastUpdated: new Date().toISOString()
  })),
  
  on(DashboardActions.loadDashboardFailure, (state, { error }): DashboardState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Stats
  on(DashboardActions.loadDashboardStats, (state): DashboardState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(DashboardActions.loadDashboardStatsSuccess, (state, { stats }): DashboardState => ({
    ...state,
    stats,
    loading: false,
    error: null,
    lastUpdated: new Date().toISOString()
  })),
  
  on(DashboardActions.loadDashboardStatsFailure, (state, { error }): DashboardState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Recent Experiments
  on(DashboardActions.loadRecentExperiments, (state): DashboardState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(DashboardActions.loadRecentExperimentsSuccess, (state, { experiments }): DashboardState => ({
    ...state,
    recentExperiments: experiments,
    loading: false,
    error: null,
    lastUpdated: new Date().toISOString()
  })),
  
  on(DashboardActions.loadRecentExperimentsFailure, (state, { error }): DashboardState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Load Metrics
  on(DashboardActions.loadDashboardMetrics, (state): DashboardState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  on(DashboardActions.loadDashboardMetricsSuccess, (state, { metrics }): DashboardState => ({
    ...state,
    metrics,
    loading: false,
    error: null,
    lastUpdated: new Date().toISOString()
  })),
  
  on(DashboardActions.loadDashboardMetricsFailure, (state, { error }): DashboardState => ({
    ...state,
    loading: false,
    error
  })),
  
  // Refresh Dashboard
  on(DashboardActions.refreshDashboard, (state): DashboardState => ({
    ...state,
    loading: true,
    error: null
  })),
  
  // Clear Error
  on(DashboardActions.clearDashboardError, (state): DashboardState => ({
    ...state,
    error: null
  }))
);
