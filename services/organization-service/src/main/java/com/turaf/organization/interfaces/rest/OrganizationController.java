package com.turaf.organization.interfaces.rest;

import com.turaf.organization.application.OrganizationService;
import com.turaf.organization.application.dto.CreateOrganizationRequest;
import com.turaf.organization.application.dto.OrganizationDto;
import com.turaf.organization.application.dto.UpdateOrganizationRequest;
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

/**
 * REST controller for organization management operations.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }
    
    @PostMapping
    @Operation(summary = "Create a new organization", description = "Creates a new organization with the authenticated user as the creator")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Organization created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Organization with slug already exists")
    })
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        UserId createdBy = extractUserId(principal);
        OrganizationDto organization = organizationService.createOrganization(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Retrieves an organization by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization found"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationDto> getOrganization(
            @Parameter(description = "Organization ID") @PathVariable String id) {
        
        OrganizationDto organization = organizationService.getOrganization(OrganizationId.of(id));
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get organization by slug", description = "Retrieves an organization by its URL-friendly slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization found"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationDto> getOrganizationBySlug(
            @Parameter(description = "Organization slug") @PathVariable String slug) {
        
        OrganizationDto organization = organizationService.getOrganizationBySlug(slug);
        return ResponseEntity.ok(organization);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update organization", description = "Updates an organization (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationDto> updateOrganization(
            @Parameter(description = "Organization ID") @PathVariable String id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        OrganizationDto organization = organizationService.updateOrganization(
            OrganizationId.of(id),
            request
        );
        return ResponseEntity.ok(organization);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete organization", description = "Deletes an organization (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Organization deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<Void> deleteOrganization(
            @Parameter(description = "Organization ID") @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        
        organizationService.deleteOrganization(OrganizationId.of(id));
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Extract user ID from authentication principal.
     * This is a simplified implementation - in production, you'd use a custom UserDetails implementation.
     */
    private UserId extractUserId(UserDetails principal) {
        return UserId.of(principal.getUsername());
    }
}
