/**
 * Dashboard Models
 * 
 * Type definitions for dashboard data structures matching BFF API.
 */

import { User } from './user.model';
import { Organization } from './organization.model';
import { Experiment } from './experiment.model';
import { Metric } from './metric.model';

/**
 * Dashboard overview data
 */
export interface DashboardOverview {
  user: User;
  organizations: Organization[];
  activeExperiments: Experiment[];
  totalOrganizations: number;
  totalActiveExperiments: number;
}

/**
 * Full experiment details with metrics
 */
export interface ExperimentFull {
  experiment: Experiment;
  metrics: Metric[];
  totalMetrics: number;
}

/**
 * Organization summary with members and experiments
 */
export interface OrganizationSummary {
  organization: Organization;
  members: any[]; // Member type from organization.model
  experiments: Experiment[];
  totalMembers: number;
  totalExperiments: number;
}

// ============================================================================
// LEGACY TYPES - For backward compatibility with existing components/store
// These will be deprecated once components are updated to use BFF-aligned types
// ============================================================================

/**
 * @deprecated Use DashboardOverview instead
 * Dashboard statistics summary (legacy)
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
 * @deprecated Use Experiment from experiment.model instead
 * Recent experiment summary for dashboard (legacy)
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
 * @deprecated Experiment status enum (legacy)
 */
export enum ExperimentStatus {
  DRAFT = 'DRAFT',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * @deprecated Metric data point for charts (legacy)
 */
export interface MetricDataPoint {
  date: string;
  value: number;
  metricName: string;
}

/**
 * @deprecated Dashboard metrics for visualization (legacy)
 */
export interface DashboardMetrics {
  experimentTrends: MetricDataPoint[];
  successRateTrend: MetricDataPoint[];
  problemDistribution: ProblemDistribution[];
}

/**
 * @deprecated Problem distribution data (legacy)
 */
export interface ProblemDistribution {
  problemName: string;
  experimentCount: number;
  successRate: number;
}

/**
 * @deprecated Use DashboardOverview instead
 * Complete dashboard data (legacy)
 */
export interface DashboardData {
  stats: DashboardStats;
  recentExperiments: RecentExperiment[];
  metrics: DashboardMetrics;
}
