/**
 * Organization Models
 * 
 * Type definitions for organization data structures.
 */

/**
 * Organization entity
 */
export interface Organization {
  id: string;
  name: string;
  description?: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Organization role enum
 */
export enum OrganizationRole {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  MEMBER = 'MEMBER',
  VIEWER = 'VIEWER'
}

/**
 * Organization member
 */
export interface OrganizationMember {
  userId: string;
  organizationId: string;
  role: OrganizationRole;
  joinedAt: string;
}

/**
 * Create organization request
 */
export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  slug?: string;
}

/**
 * Update organization request
 */
export interface UpdateOrganizationRequest {
  name?: string;
  description?: string;
  slug?: string;
}
