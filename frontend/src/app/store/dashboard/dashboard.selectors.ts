import { createFeatureSelector, createSelector } from '@ngrx/store';
import { DashboardState } from './dashboard.state';

/**
 * Dashboard Selectors
 * 
 * Selectors for accessing dashboard state.
 */

/**
 * Feature selector for dashboard state
 */
export const selectDashboardState = createFeatureSelector<DashboardState>('dashboard');

/**
 * Select dashboard stats
 */
export const selectDashboardStats = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.stats
);

/**
 * Select recent experiments
 */
export const selectRecentExperiments = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.recentExperiments
);

/**
 * Select dashboard metrics
 */
export const selectDashboardMetrics = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.metrics
);

/**
 * Select dashboard loading state
 */
export const selectDashboardLoading = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.loading
);

/**
 * Select dashboard error
 */
export const selectDashboardError = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.error
);

/**
 * Select last updated timestamp
 */
export const selectDashboardLastUpdated = createSelector(
  selectDashboardState,
  (state: DashboardState) => state.lastUpdated
);

/**
 * Select if dashboard has data
 */
export const selectDashboardHasData = createSelector(
  selectDashboardState,
  (state: DashboardState) => 
    state.stats !== null || 
    state.recentExperiments.length > 0 || 
    state.metrics !== null
);

/**
 * Select active experiments count
 */
export const selectActiveExperimentsCount = createSelector(
  selectDashboardStats,
  (stats) => stats?.activeExperiments ?? 0
);

/**
 * Select success rate
 */
export const selectSuccessRate = createSelector(
  selectDashboardStats,
  (stats) => stats?.successRate ?? 0
);

/**
 * Select total problems count
 */
export const selectTotalProblemsCount = createSelector(
  selectDashboardStats,
  (stats) => stats?.totalProblems ?? 0
);
