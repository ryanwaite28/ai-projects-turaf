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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final OrganizationServiceClient organizationServiceClient;
    
    @GetMapping
    public Flux<OrganizationDto> getOrganizations(@AuthenticationPrincipal UserContext userContext) {
        log.info("Get organizations for user: {}", userContext.getUserId());
        return organizationServiceClient.getOrganizations(userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved organizations"))
            .doOnError(error -> log.error("Failed to get organizations", error));
    }
    
    @PostMapping
    public Mono<ResponseEntity<OrganizationDto>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Create organization: {}", request.getName());
        return organizationServiceClient.createOrganization(request, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Organization created"))
            .doOnError(error -> log.error("Failed to create organization", error));
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrganizationDto>> getOrganization(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get organization: {}", id);
        return organizationServiceClient.getOrganization(id, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnError(error -> log.error("Failed to get organization {}", id, error));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<OrganizationDto>> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update organization: {}", id);
        return organizationServiceClient.updateOrganization(id, request, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Organization updated"))
            .doOnError(error -> log.error("Failed to update organization {}", id, error));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteOrganization(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Delete organization: {}", id);
        return organizationServiceClient.deleteOrganization(id, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Organization deleted"))
            .doOnError(error -> log.error("Failed to delete organization {}", id, error));
    }
    
    @GetMapping("/{id}/members")
    public Flux<MemberDto> getMembers(
            @PathVariable String id,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Get members for organization: {}", id);
        return organizationServiceClient.getMembers(id, userContext.getUserId())
            .doOnComplete(() -> log.info("Retrieved members"))
            .doOnError(error -> log.error("Failed to get members for organization {}", id, error));
    }
    
    @PostMapping("/{id}/members")
    public Mono<ResponseEntity<MemberDto>> addMember(
            @PathVariable String id,
            @Valid @RequestBody com.turaf.bff.dto.AddMemberRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Add member to organization: {}", id);
        return organizationServiceClient.addMember(id, request, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Member added to organization"))
            .doOnError(error -> log.error("Failed to add member to organization {}", id, error));
    }
    
    @PatchMapping("/{id}/members/{memberId}")
    public Mono<ResponseEntity<MemberDto>> updateMemberRole(
            @PathVariable String id,
            @PathVariable String memberId,
            @Valid @RequestBody com.turaf.bff.dto.UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Update member role in organization: {}", id);
        return organizationServiceClient.updateMemberRole(id, memberId, request, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Member role updated"))
            .doOnError(error -> log.error("Failed to update member role in organization {}", id, error));
    }
    
    @DeleteMapping("/{id}/members/{memberId}")
    public Mono<ResponseEntity<Void>> removeMember(
            @PathVariable String id,
            @PathVariable String memberId,
            @AuthenticationPrincipal UserContext userContext) {
        log.info("Remove member from organization: {}", id);
        return organizationServiceClient.removeMember(id, memberId, userContext.getUserId())
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("Member removed from organization"))
            .doOnError(error -> log.error("Failed to remove member from organization {}", id, error));
    }
}
