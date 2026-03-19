package com.turaf.organization.interfaces.rest;

import com.turaf.organization.application.MembershipService;
import com.turaf.organization.application.dto.AddMemberRequest;
import com.turaf.organization.application.dto.MemberDto;
import com.turaf.organization.domain.MemberRole;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for organization membership management.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
@Tag(name = "Membership", description = "Organization membership management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class MembershipController {
    
    private final MembershipService membershipService;
    
    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
    
    @PostMapping
    @Operation(summary = "Add member to organization", description = "Adds a new member to the organization (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Member added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "409", description = "User is already a member")
    })
    public ResponseEntity<MemberDto> addMember(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody AddMemberRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        UserId addedBy = extractUserId(principal);
        MemberDto member = membershipService.addMember(
            OrganizationId.of(organizationId),
            request,
            addedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
    
    @GetMapping
    @Operation(summary = "Get organization members", description = "Retrieves all members of the organization (member access required)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - member access required")
    })
    public ResponseEntity<List<MemberDto>> getMembers(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        List<MemberDto> members = membershipService.getMembers(OrganizationId.of(organizationId));
        return ResponseEntity.ok(members);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get specific member", description = "Retrieves a specific member of the organization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<MemberDto> getMember(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        MemberDto member = membershipService.getMember(
            OrganizationId.of(organizationId),
            UserId.of(userId)
        );
        return ResponseEntity.ok(member);
    }
    
    @PutMapping("/{userId}/role")
    @Operation(summary = "Update member role", description = "Updates a member's role (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Role updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<MemberDto> updateMemberRole(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestBody UpdateRoleRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        MemberDto member = membershipService.updateMemberRole(
            OrganizationId.of(organizationId),
            UserId.of(userId),
            MemberRole.valueOf(request.getRole())
        );
        return ResponseEntity.ok(member);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove member from organization", description = "Removes a member from the organization (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        UserId removedBy = extractUserId(principal);
        membershipService.removeMember(
            OrganizationId.of(organizationId),
            UserId.of(userId),
            removedBy
        );
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Extract user ID from authentication principal.
     */
    private UserId extractUserId(UserDetails principal) {
        return UserId.of(principal.getUsername());
    }
    
    /**
     * Request DTO for updating member role.
     */
    public static class UpdateRoleRequest {
        private String role;
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
}
