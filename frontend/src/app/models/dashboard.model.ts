/**
 * Dashboard Models
 * 
 * Type definitions for dashboard data structures.
 */

/**
 * Dashboard statistics summary
 */
export interface DashboardStats {
  totalProblems: number;
  activeExperiments: number;
  completedExperiments: number;
  totalMetrics: number;
  successRate: number;
  avgExperimentDuration: number;
}

/**
 * Recent experiment summary for dashboard
 */
export interface RecentExperiment {
  id: string;
  name: string;
  problemName: string;
  status: ExperimentStatus;
  startDate: string;
  endDate?: string;
  successMetrics: number;
  totalMetrics: number;
}

/**
 * Experiment status enum
 */
export enum ExperimentStatus {
  DRAFT = 'DRAFT',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * Metric data point for charts
 */
export interface MetricDataPoint {
  date: string;
  value: number;
  metricName: string;
}

/**
 * Dashboard metrics for visualization
 */
export interface DashboardMetrics {
  experimentTrends: MetricDataPoint[];
  successRateTrend: MetricDataPoint[];
  problemDistribution: ProblemDistribution[];
}

/**
 * Problem distribution data
 */
export interface ProblemDistribution {
  problemName: string;
  experimentCount: number;
  successRate: number;
}

/**
 * Complete dashboard data
 */
export interface DashboardData {
  stats: DashboardStats;
  recentExperiments: RecentExperiment[];
  metrics: DashboardMetrics;
}
