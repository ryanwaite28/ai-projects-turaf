# Task: Implement REST Controllers

**Service**: Organization Service  
**Phase**: 3  
**Estimated Time**: 3 hours  

## Objective

Implement REST API endpoints for organization and membership management operations.

## Prerequisites

- [x] Task 003: Organization service implemented
- [x] Task 004: Membership service implemented

## Scope

**Files to Create**:
- `services/organization-service/src/main/java/com/turaf/organization/interfaces/rest/OrganizationController.java`
- `services/organization-service/src/main/java/com/turaf/organization/interfaces/rest/MembershipController.java`
- `services/organization-service/src/main/java/com/turaf/organization/interfaces/rest/GlobalExceptionHandler.java`

## Implementation Details

### Organization Controller

```java
@RestController
@RequestMapping("/api/v1/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {
    private final OrganizationService organizationService;
    
    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrganizationDto organization = organizationService.createOrganization(
            request,
            new UserId(principal.getUserId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable String id) {
        OrganizationDto organization = organizationService.getOrganization(new OrganizationId(id));
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<OrganizationDto> getOrganizationBySlug(@PathVariable String slug) {
        OrganizationDto organization = organizationService.getOrganizationBySlug(slug);
        return ResponseEntity.ok(organization);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("@membershipService.isAdmin(new com.turaf.organization.domain.OrganizationId(#id), new com.turaf.common.domain.UserId(principal.userId))")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationDto organization = organizationService.updateOrganization(
            new OrganizationId(id),
            request
        );
        return ResponseEntity.ok(organization);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("@membershipService.isAdmin(new com.turaf.organization.domain.OrganizationId(#id), new com.turaf.common.domain.UserId(principal.userId))")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String id) {
        organizationService.deleteOrganization(new OrganizationId(id));
        return ResponseEntity.noContent().build();
    }
}
```

### Membership Controller

```java
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
@PreAuthorize("isAuthenticated()")
public class MembershipController {
    private final MembershipService membershipService;
    
    @PostMapping
    @PreAuthorize("@membershipService.isAdmin(new com.turaf.organization.domain.OrganizationId(#organizationId), new com.turaf.common.domain.UserId(principal.userId))")
    public ResponseEntity<MemberDto> addMember(
            @PathVariable String organizationId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        MemberDto member = membershipService.addMember(
            new OrganizationId(organizationId),
            request,
            new UserId(principal.getUserId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
    
    @GetMapping
    @PreAuthorize("@membershipService.isMember(new com.turaf.organization.domain.OrganizationId(#organizationId), new com.turaf.common.domain.UserId(principal.userId))")
    public ResponseEntity<List<MemberDto>> getMembers(@PathVariable String organizationId) {
        List<MemberDto> members = membershipService.getMembers(new OrganizationId(organizationId));
        return ResponseEntity.ok(members);
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("@membershipService.isAdmin(new com.turaf.organization.domain.OrganizationId(#organizationId), new com.turaf.common.domain.UserId(principal.userId))")
    public ResponseEntity<Void> removeMember(
            @PathVariable String organizationId,
            @PathVariable String userId) {
        membershipService.removeMember(
            new OrganizationId(organizationId),
            new UserId(userId)
        );
        return ResponseEntity.noContent().build();
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationAlreadyExists(OrganizationAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("ORGANIZATION_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("ORGANIZATION_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleMemberAlreadyExists(MemberAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse("MEMBER_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse("ACCESS_DENIED", "You do not have permission");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

## Acceptance Criteria

- [x] POST /api/v1/organizations endpoint works
- [x] GET /api/v1/organizations/{id} endpoint works
- [x] GET /api/v1/organizations/slug/{slug} endpoint works
- [x] PUT /api/v1/organizations/{id} endpoint works
- [x] DELETE /api/v1/organizations/{id} endpoint works
- [x] POST /api/v1/organizations/{id}/members endpoint works
- [x] GET /api/v1/organizations/{id}/members endpoint works
- [x] DELETE /api/v1/organizations/{id}/members/{userId} endpoint works
- [x] Authorization checks enforced
- [x] All endpoints documented with OpenAPI

## Testing Requirements

**Integration Tests**:
- Test create organization
- Test get organization
- Test update organization (admin only)
- Test delete organization (admin only)
- Test add member (admin only)
- Test get members (member access)
- Test remove member (admin only)
- Test authorization failures

**Test Files to Create**:
- `OrganizationControllerTest.java`
- `MembershipControllerTest.java`

## References

- Specification: `specs/organization-service.md` (API Endpoints section)
- Related Tasks: 006-implement-event-publishing
