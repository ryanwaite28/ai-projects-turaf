import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Organization entity
 */
export interface Organization {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  settings?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

/**
 * Create organization request
 */
export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  settings?: Record<string, any>;
}

/**
 * Update organization request
 */
export interface UpdateOrganizationRequest {
  name?: string;
  description?: string;
  settings?: Record<string, any>;
}

/**
 * Organization member
 */
export interface OrganizationMember {
  id: string;
  organizationId: string;
  userId: string;
  role: OrganizationRole;
  joinedAt: string;
  user?: {
    id: string;
    email: string;
    firstName?: string;
    lastName?: string;
  };
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
 * Invite member request
 */
export interface InviteMemberRequest {
  email: string;
  role: OrganizationRole;
}

/**
 * Organization Service
 * 
 * Handles organization-related API calls.
 */
@Injectable({
  providedIn: 'root'
})
export class OrganizationService {
  
  private readonly apiUrl = `${environment.apiUrl}/organizations`;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Gets all organizations for the current user
   */
  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>(this.apiUrl);
  }
  
  /**
   * Gets a single organization by ID
   */
  getOrganization(id: string): Observable<Organization> {
    return this.http.get<Organization>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Creates a new organization
   */
  createOrganization(request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(this.apiUrl, request);
  }
  
  /**
   * Updates an organization
   */
  updateOrganization(id: string, request: UpdateOrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/${id}`, request);
  }
  
  /**
   * Deletes an organization
   */
  deleteOrganization(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Gets members of an organization
   */
  getMembers(organizationId: string): Observable<OrganizationMember[]> {
    return this.http.get<OrganizationMember[]>(`${this.apiUrl}/${organizationId}/members`);
  }
  
  /**
   * Invites a member to an organization
   */
  inviteMember(organizationId: string, request: InviteMemberRequest): Observable<OrganizationMember> {
    return this.http.post<OrganizationMember>(`${this.apiUrl}/${organizationId}/members`, request);
  }
  
  /**
   * Updates a member's role
   */
  updateMemberRole(organizationId: string, memberId: string, role: OrganizationRole): Observable<OrganizationMember> {
    return this.http.patch<OrganizationMember>(
      `${this.apiUrl}/${organizationId}/members/${memberId}`,
      { role }
    );
  }
  
  /**
   * Removes a member from an organization
   */
  removeMember(organizationId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${organizationId}/members/${memberId}`);
  }
}
