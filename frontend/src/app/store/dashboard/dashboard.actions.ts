import { createAction, props } from '@ngrx/store';
import { DashboardData, DashboardStats, RecentExperiment, DashboardMetrics } from '../../models/dashboard.model';

/**
 * Dashboard Actions
 * 
 * Actions for managing dashboard state.
 */

/**
 * Load Dashboard Data
 */
export const loadDashboard = createAction(
  '[Dashboard] Load Dashboard'
);

export const loadDashboardSuccess = createAction(
  '[Dashboard] Load Dashboard Success',
  props<{ data: DashboardData }>()
);

export const loadDashboardFailure = createAction(
  '[Dashboard] Load Dashboard Failure',
  props<{ error: string }>()
);

/**
 * Load Dashboard Stats
 */
export const loadDashboardStats = createAction(
  '[Dashboard] Load Stats'
);

export const loadDashboardStatsSuccess = createAction(
  '[Dashboard] Load Stats Success',
  props<{ stats: DashboardStats }>()
);

export const loadDashboardStatsFailure = createAction(
  '[Dashboard] Load Stats Failure',
  props<{ error: string }>()
);

/**
 * Load Recent Experiments
 */
export const loadRecentExperiments = createAction(
  '[Dashboard] Load Recent Experiments'
);

export const loadRecentExperimentsSuccess = createAction(
  '[Dashboard] Load Recent Experiments Success',
  props<{ experiments: RecentExperiment[] }>()
);

export const loadRecentExperimentsFailure = createAction(
  '[Dashboard] Load Recent Experiments Failure',
  props<{ error: string }>()
);

/**
 * Load Dashboard Metrics
 */
export const loadDashboardMetrics = createAction(
  '[Dashboard] Load Metrics'
);

export const loadDashboardMetricsSuccess = createAction(
  '[Dashboard] Load Metrics Success',
  props<{ metrics: DashboardMetrics }>()
);

export const loadDashboardMetricsFailure = createAction(
  '[Dashboard] Load Metrics Failure',
  props<{ error: string }>()
);

/**
 * Refresh Dashboard
 */
export const refreshDashboard = createAction(
  '[Dashboard] Refresh Dashboard'
);

/**
 * Clear Dashboard Error
 */
export const clearDashboardError = createAction(
  '[Dashboard] Clear Error'
);
