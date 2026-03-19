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
}
