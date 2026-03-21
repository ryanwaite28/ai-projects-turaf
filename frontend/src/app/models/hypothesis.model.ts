/**
 * Hypothesis Models
 * 
 * Type definitions for hypothesis data structures.
 */

/**
 * Hypothesis entity
 */
export interface Hypothesis {
  id: string;
  problemId: string;
  title: string;
  description: string;
  expectedOutcome: string;
  successCriteria: string;
  organizationId: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  status: HypothesisStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Hypothesis status enum
 */
export enum HypothesisStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  VALIDATED = 'VALIDATED',
  INVALIDATED = 'INVALIDATED',
  ARCHIVED = 'ARCHIVED'
}

/**
 * Create hypothesis request
 */
export interface CreateHypothesisRequest {
  problemId: string;
  title: string;
  description: string;
  expectedOutcome: string;
  successCriteria: string;
  status?: HypothesisStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Update hypothesis request
 */
export interface UpdateHypothesisRequest {
  title?: string;
  description?: string;
  expectedOutcome?: string;
  successCriteria?: string;
  status?: HypothesisStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Hypothesis list query params
 */
export interface HypothesisQueryParams {
  page?: number;
  limit?: number;
  problemId?: string;
  status?: HypothesisStatus;
  search?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * Paginated hypotheses response
 */
export interface PaginatedHypothesesResponse {
  hypotheses: Hypothesis[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

/**
 * Hypothesis with problem details
 */
export interface HypothesisWithProblem extends Hypothesis {
  problemTitle?: string;
  problemDescription?: string;
}
