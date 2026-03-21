/**
 * Problem Models
 * 
 * Type definitions for problem data structures.
 */

/**
 * Problem entity
 */
export interface Problem {
  id: string;
  title: string;
  description: string;
  organizationId: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  status: ProblemStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Problem status enum
 */
export enum ProblemStatus {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  DRAFT = 'DRAFT'
}

/**
 * Create problem request
 */
export interface CreateProblemRequest {
  title: string;
  description: string;
  status?: ProblemStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Update problem request
 */
export interface UpdateProblemRequest {
  title?: string;
  description?: string;
  status?: ProblemStatus;
  tags?: string[];
  metadata?: Record<string, any>;
}

/**
 * Problem list query params
 */
export interface ProblemQueryParams {
  page?: number;
  limit?: number;
  status?: ProblemStatus;
  search?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * Paginated problems response
 */
export interface PaginatedProblemsResponse {
  problems: Problem[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
