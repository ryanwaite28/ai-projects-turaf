/**
 * Experiment Models
 * 
 * Type definitions for experiment data structures.
 */

/**
 * Experiment entity
 */
export interface Experiment {
  id: string;
  hypothesisId: string;
  name: string;
  description: string;
  status: ExperimentStatus;
  startDate?: string;
  endDate?: string;
  completedAt?: string;
  organizationId: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  configuration?: ExperimentConfiguration;
  results?: ExperimentResults;
  metrics?: ExperimentMetrics;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Experiment status enum
 */
export enum ExperimentStatus {
  DRAFT = 'DRAFT',
  READY = 'READY',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

/**
 * Experiment configuration
 */
export interface ExperimentConfiguration {
  parameters?: Record<string, any>;
  environment?: string;
  sampleSize?: number;
  duration?: number;
  controlGroup?: boolean;
  variants?: ExperimentVariant[];
}

/**
 * Experiment variant
 */
export interface ExperimentVariant {
  id: string;
  name: string;
  description?: string;
  allocation?: number;
  parameters?: Record<string, any>;
}

/**
 * Experiment results
 */
export interface ExperimentResults {
  summary?: string;
  outcome?: 'SUCCESS' | 'FAILURE' | 'INCONCLUSIVE';
  confidence?: number;
  observations?: string[];
  conclusions?: string[];
  recommendations?: string[];
  data?: Record<string, any>;
}

/**
 * Experiment metrics
 */
export interface ExperimentMetrics {
  totalRuns?: number;
  successfulRuns?: number;
  failedRuns?: number;
  averageDuration?: number;
  successRate?: number;
  performanceMetrics?: Record<string, number>;
  customMetrics?: Record<string, any>;
}

/**
 * Create experiment request
 */
export interface CreateExperimentRequest {
  hypothesisId: string;
  name: string;
  description: string;
  status?: ExperimentStatus;
  startDate?: string;
  endDate?: string;
  configuration?: ExperimentConfiguration;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Update experiment request
 */
export interface UpdateExperimentRequest {
  name?: string;
  description?: string;
  status?: ExperimentStatus;
  startDate?: string;
  endDate?: string;
  configuration?: ExperimentConfiguration;
  results?: ExperimentResults;
  metrics?: ExperimentMetrics;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Experiment list query params
 */
export interface ExperimentQueryParams {
  page?: number;
  limit?: number;
  hypothesisId?: string;
  status?: ExperimentStatus;
  search?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * Paginated experiments response
 */
export interface PaginatedExperimentsResponse {
  experiments: Experiment[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

/**
 * Experiment with hypothesis details
 */
export interface ExperimentWithHypothesis extends Experiment {
  hypothesisTitle?: string;
  hypothesisDescription?: string;
  problemId?: string;
  problemTitle?: string;
}

/**
 * Experiment state transition request
 */
export interface ExperimentStateTransition {
  action: 'start' | 'pause' | 'resume' | 'complete' | 'cancel' | 'fail';
  reason?: string;
  metadata?: Record<string, any>;
}
