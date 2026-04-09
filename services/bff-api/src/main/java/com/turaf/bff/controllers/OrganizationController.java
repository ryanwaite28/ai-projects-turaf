package com.turaf.bff.controllers;

import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final OrganizationServiceClient organizationServiceClient;
    
    @GetMapping
    public List<OrganizationDto> getOrganizations(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get organizations for user: {}", userContext.getUserId());
        List<OrganizationDto> organizations = organizationServiceClient.getOrganizations(userContext.getUserId());
        log.info("Retrieved organizations");
        return organizations;
    }
    
    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create organization: {}", request.getName());
        OrganizationDto org = organizationServiceClient.createOrganization(request, userContext.getUserId());
        log.info("Organization created");
        return ResponseEntity.ok(org);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get organization: {}", id);
        OrganizationDto org = organizationServiceClient.getOrganization(id, userContext.getUserId());
        log.info("Retrieved organization");
        return ResponseEntity.ok(org);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update organization: {}", id);
        OrganizationDto org = organizationServiceClient.updateOrganization(id, request, userContext.getUserId());
        log.info("Organization updated");
        return ResponseEntity.ok(org);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete organization: {}", id);
        organizationServiceClient.deleteOrganization(id, userContext.getUserId());
        log.info("Organization deleted");
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/members")
    public List<MemberDto> getMembers(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get members for organization: {}", id);
        List<MemberDto> members = organizationServiceClient.getMembers(id, userContext.getUserId());
        log.info("Retrieved members");
        return members;
    }
    
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberDto> addMember(
            @PathVariable String id,
            @Valid @RequestBody com.turaf.bff.dto.AddMemberRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Add member to organization: {}", id);
        MemberDto member = organizationServiceClient.addMember(id, request, userContext.getUserId());
        log.info("Member added to organization");
        return ResponseEntity.ok(member);
    }
    
    @PatchMapping("/{id}/members/{memberId}")
    public ResponseEntity<MemberDto> updateMemberRole(
            @PathVariable String id,
            @PathVariable String memberId,
            @Valid @RequestBody com.turaf.bff.dto.UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update member role in organization: {}", id);
        MemberDto member = organizationServiceClient.updateMemberRole(id, memberId, request, userContext.getUserId());
        log.info("Member role updated");
        return ResponseEntity.ok(member);
    }
    
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String id,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Remove member from organization: {}", id);
        organizationServiceClient.removeMember(id, memberId, userContext.getUserId());
        log.info("Member removed from organization");
        return ResponseEntity.ok().build();
    }
}
