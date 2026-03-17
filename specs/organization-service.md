# Organization Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Organization Service, responsible for organization lifecycle management, membership, and multi-tenant context.

---

## Service Overview

**Purpose**: Manage organizations, memberships, and tenant isolation

**Bounded Context**: Organization and Membership Management

**Service Type**: Core microservice (ECS Fargate)

---

## Responsibilities

- Organization creation and lifecycle management
- Organization membership management
- Tenant context management
- Organization settings and configuration
- Multi-tenant data isolation enforcement

---

## Technology Stack

**Framework**: Spring Boot 3.x  
**Persistence**: Spring Data JPA  
**Database**: PostgreSQL (shared RDS instance)  
**Events**: Spring Cloud AWS (EventBridge)  
**Build Tool**: Maven  
**Java Version**: Java 17  

**Key Dependencies**:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-cloud-aws-messaging`
- `postgresql` driver

---

## API Endpoints

### POST /api/v1/organizations

**Purpose**: Create a new organization

**Headers**:
```
Authorization: Bearer {access-token}
```

**Request Body**:
```json
{
  "name": "Acme Corporation",
  "slug": "acme-corp"
}
```

**Response** (201 Created):
```json
{
  "organizationId": "uuid",
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "createdBy": "uuid",
  "createdAt": "ISO-8601"
}
```

**Validation**:
- Name: 1-100 characters, required
- Slug: 3-50 characters, lowercase, alphanumeric and hyphens only, unique
- User must be authenticated

**Business Rules**:
- Creator is automatically added as ADMIN
- Slug is auto-generated from name if not provided
- Publishes OrganizationCreated event

---

### GET /api/v1/organizations/{id}

**Purpose**: Get organization details

**Headers**:
```
Authorization: Bearer {access-token}
```

**Response** (200 OK):
```json
{
  "organizationId": "uuid",
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "memberCount": 5,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

**Authorization**:
- User must be member of organization

---

### PUT /api/v1/organizations/{id}

**Purpose**: Update organization details

**Headers**:
```
Authorization: Bearer {access-token}
```

**Request Body**:
```json
{
  "name": "Acme Corporation Inc"
}
```

**Response** (200 OK):
```json
{
  "organizationId": "uuid",
  "name": "Acme Corporation Inc",
  "slug": "acme-corp",
  "updatedAt": "ISO-8601"
}
```

**Authorization**:
- User must be ADMIN of organization

**Business Rules**:
- Slug cannot be changed after creation
- Publishes OrganizationUpdated event

---

### DELETE /api/v1/organizations/{id}

**Purpose**: Delete an organization

**Headers**:
```
Authorization: Bearer {access-token}
```

**Response** (204 No Content)

**Authorization**:
- User must be ADMIN of organization

**Business Rules**:
- Cannot delete if active experiments exist
- Soft delete (mark as deleted, don't remove data)
- All members are removed
- Publishes OrganizationDeleted event

---

### POST /api/v1/organizations/{id}/members

**Purpose**: Add a member to organization

**Headers**:
```
Authorization: Bearer {access-token}
```

**Request Body**:
```json
{
  "userId": "uuid",
  "role": "MEMBER"
}
```

**Response** (201 Created):
```json
{
  "userId": "uuid",
  "organizationId": "uuid",
  "role": "MEMBER",
  "joinedAt": "ISO-8601"
}
```

**Validation**:
- userId must be valid user
- role must be MEMBER or ADMIN

**Authorization**:
- User must be ADMIN of organization

**Business Rules**:
- User cannot be added twice to same organization
- Publishes MemberAdded event

---

### GET /api/v1/organizations/{id}/members

**Purpose**: List organization members

**Headers**:
```
Authorization: Bearer {access-token}
```

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 20)
- `role` (optional filter)

**Response** (200 OK):
```json
{
  "members": [
    {
      "userId": "uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "role": "ADMIN",
      "joinedAt": "ISO-8601"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

**Authorization**:
- User must be member of organization

---

### DELETE /api/v1/organizations/{id}/members/{userId}

**Purpose**: Remove a member from organization

**Headers**:
```
Authorization: Bearer {access-token}
```

**Response** (204 No Content)

**Authorization**:
- User must be ADMIN of organization
- Cannot remove last ADMIN

**Business Rules**:
- User loses access to all organization data
- Publishes MemberRemoved event

---

### GET /api/v1/organizations/{id}/settings

**Purpose**: Get organization settings

**Headers**:
```
Authorization: Bearer {access-token}
```

**Response** (200 OK):
```json
{
  "settings": {
    "timezone": "America/New_York",
    "dateFormat": "MM/DD/YYYY",
    "experimentRetentionDays": 365
  }
}
```

**Authorization**:
- User must be member of organization

---

### PUT /api/v1/organizations/{id}/settings

**Purpose**: Update organization settings

**Headers**:
```
Authorization: Bearer {access-token}
```

**Request Body**:
```json
{
  "timezone": "America/Los_Angeles",
  "dateFormat": "YYYY-MM-DD"
}
```

**Response** (200 OK):
```json
{
  "settings": {
    "timezone": "America/Los_Angeles",
    "dateFormat": "YYYY-MM-DD",
    "experimentRetentionDays": 365
  }
}
```

**Authorization**:
- User must be ADMIN of organization

---

## Database Schema

### organizations

```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_deleted ON organizations(deleted);
```

### organization_members

```sql
CREATE TABLE organization_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, user_id)
);

CREATE INDEX idx_org_members_org_id ON organization_members(organization_id);
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);
CREATE INDEX idx_org_members_role ON organization_members(role);
```

**Roles**: `MEMBER`, `ADMIN`

### organization_settings

```sql
CREATE TABLE organization_settings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, key)
);

CREATE INDEX idx_org_settings_org_id ON organization_settings(organization_id);
```

---

## Application Services

### OrganizationService

**Responsibilities**:
- Organization CRUD operations
- Organization validation
- Event publishing

**Methods**:
```java
public interface OrganizationService {
    OrganizationDto createOrganization(CreateOrganizationRequest request, UserId createdBy);
    OrganizationDto getOrganization(OrganizationId id, UserId requestingUser);
    OrganizationDto updateOrganization(OrganizationId id, UpdateOrganizationRequest request, UserId requestingUser);
    void deleteOrganization(OrganizationId id, UserId requestingUser);
    List<OrganizationDto> getUserOrganizations(UserId userId);
}
```

### MembershipService

**Responsibilities**:
- Member management
- Role assignment
- Authorization checks

**Methods**:
```java
public interface MembershipService {
    MemberDto addMember(OrganizationId orgId, UserId userId, Role role, UserId addedBy);
    Page<MemberDto> getMembers(OrganizationId orgId, Pageable pageable);
    void removeMember(OrganizationId orgId, UserId userId, UserId removedBy);
    boolean isMember(OrganizationId orgId, UserId userId);
    boolean isAdmin(OrganizationId orgId, UserId userId);
    Role getMemberRole(OrganizationId orgId, UserId userId);
}
```

### SettingsService

**Responsibilities**:
- Organization settings management
- Default settings initialization

**Methods**:
```java
public interface SettingsService {
    Map<String, String> getSettings(OrganizationId orgId);
    void updateSettings(OrganizationId orgId, Map<String, String> settings);
    String getSetting(OrganizationId orgId, String key);
    void setSetting(OrganizationId orgId, String key, String value);
}
```

---

## Domain Logic

### Organization Entity

```java
@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String slug;
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    // Business methods
    public void updateName(String newName) {
        validateName(newName);
        this.name = newName;
    }
    
    public void markAsDeleted() {
        this.deleted = true;
    }
    
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Name too long");
        }
    }
}
```

### OrganizationMember Entity

```java
@Entity
@Table(name = "organization_members")
public class OrganizationMember {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @CreatedDate
    private Instant joinedAt;
    
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
    
    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }
    
    public void demoteToMember() {
        this.role = Role.MEMBER;
    }
}
```

---

## Events Published

### OrganizationCreated

**Triggered When**: New organization is created

**Payload**:
```json
{
  "organizationId": "uuid",
  "name": "string",
  "slug": "string",
  "createdBy": "userId",
  "createdAt": "ISO-8601"
}
```

### OrganizationUpdated

**Triggered When**: Organization details are updated

**Payload**:
```json
{
  "organizationId": "uuid",
  "name": "string",
  "updatedBy": "userId",
  "updatedAt": "ISO-8601"
}
```

### MemberAdded

**Triggered When**: User is added to organization

**Payload**:
```json
{
  "organizationId": "uuid",
  "userId": "uuid",
  "role": "string",
  "addedBy": "userId",
  "addedAt": "ISO-8601"
}
```

### MemberRemoved

**Triggered When**: User is removed from organization

**Payload**:
```json
{
  "organizationId": "uuid",
  "userId": "uuid",
  "removedBy": "userId",
  "removedAt": "ISO-8601"
}
```

---

## Multi-Tenant Context Management

### Tenant Context Filter

**Purpose**: Ensure all queries are scoped to user's organizations

**Implementation**:
```java
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        // Extract user from JWT
        UserId userId = extractUserFromToken(request);
        
        // Load user's organizations
        List<OrganizationId> orgIds = membershipService.getUserOrganizations(userId);
        
        // Set in thread-local context
        TenantContext.setOrganizations(orgIds);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

### Repository Filtering

**Purpose**: Automatically filter queries by organization

**Implementation**:
```java
public interface OrganizationAwareRepository<T> extends JpaRepository<T, UUID> {
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.organizationId IN :orgIds")
    List<T> findAllInOrganizations(@Param("orgIds") List<UUID> orgIds);
}
```

---

## Authorization

### Role-Based Access Control

**Roles**:
- `MEMBER`: Can view organization data, create experiments
- `ADMIN`: Full control over organization

**Permission Matrix**:

| Action | MEMBER | ADMIN |
|--------|--------|-------|
| View organization | ✓ | ✓ |
| Update organization | ✗ | ✓ |
| Delete organization | ✗ | ✓ |
| View members | ✓ | ✓ |
| Add members | ✗ | ✓ |
| Remove members | ✗ | ✓ |
| Update settings | ✗ | ✓ |

### Authorization Service

```java
@Service
public class AuthorizationService {
    
    public void requireMember(OrganizationId orgId, UserId userId) {
        if (!membershipService.isMember(orgId, userId)) {
            throw new ForbiddenException("User is not a member");
        }
    }
    
    public void requireAdmin(OrganizationId orgId, UserId userId) {
        if (!membershipService.isAdmin(orgId, userId)) {
            throw new ForbiddenException("Admin role required");
        }
    }
}
```

---

## Error Handling

**Error Codes**:
- `ORG_001`: Organization not found
- `ORG_002`: Slug already exists
- `ORG_003`: User not a member
- `ORG_004`: Insufficient permissions
- `ORG_005`: Cannot remove last admin
- `ORG_006`: Cannot delete organization with active experiments

---

## Testing Strategy

### Unit Tests
- Test organization validation
- Test slug generation
- Test role assignment logic
- Test authorization checks

### Integration Tests
- Test repository methods
- Test event publishing
- Test multi-tenant filtering

### API Tests
- Test all endpoints
- Test authorization
- Test pagination
- Test error scenarios

---

## Monitoring and Observability

### Metrics to Track
- Organizations created
- Members added/removed
- Authorization failures
- API response times

### Logging
- Log all organization changes
- Log all membership changes
- Log authorization failures
- Include organization context in logs

---

## Security Considerations

### Tenant Isolation
- All queries filtered by organization
- No cross-organization data access
- Authorization checks on every request
- Audit logging of all changes

### Data Privacy
- Soft delete preserves audit trail
- Member data visible only to organization
- Settings are organization-private

---

## Future Enhancements

- Organization invitations via email
- Custom roles and permissions
- Organization billing and subscriptions
- Organization branding and customization
- SSO integration per organization
- Organization transfer ownership

---

## References

- PROJECT.md: Organization Service specification
- Multi-Tenancy Patterns
- Spring Security Authorization
