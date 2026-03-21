import { DashboardStats, RecentExperiment, DashboardMetrics } from '../../models/dashboard.model';

/**
 * Dashboard State
 * 
 * Manages dashboard data in NgRx store.
 */
export interface DashboardState {
  stats: DashboardStats | null;
  recentExperiments: RecentExperiment[];
  metrics: DashboardMetrics | null;
  loading: boolean;
  error: string | null;
  lastUpdated: string | null;
}

/**
 * Initial dashboard state
 */
export const initialDashboardState: DashboardState = {
  stats: null,
  recentExperiments: [],
  metrics: null,
  loading: false,
  error: null,
  lastUpdated: null
};
