package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class OrganizationServiceClient {
    
    private final WebClient webClient;
    private static final String SERVICE_PATH = "/organization";
    
    public OrganizationServiceClient(@Qualifier("organizationWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Flux<OrganizationDto> getOrganizations(String userId) {
        log.debug("Calling Organization Service: GET /organizations for user: {}", userId);
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations")
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToFlux(OrganizationDto.class)
            .doOnComplete(() -> log.debug("Retrieved organizations for user: {}", userId))
            .doOnError(error -> log.error("Failed to get organizations for user: {}", userId, error));
    }
    
    public Mono<OrganizationDto> createOrganization(CreateOrganizationRequest request, String userId) {
        log.debug("Calling Organization Service: POST /organizations");
        return webClient.post()
            .uri(SERVICE_PATH + "/organizations")
            .header("X-User-Id", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OrganizationDto.class)
            .doOnSuccess(org -> log.debug("Organization created: {}", org.getId()))
            .doOnError(error -> log.error("Failed to create organization", error));
    }
    
    public Mono<OrganizationDto> getOrganization(String id, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}", id);
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations/{id}", id)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(OrganizationDto.class)
            .doOnSuccess(org -> log.debug("Retrieved organization: {}", id))
            .doOnError(error -> log.error("Failed to get organization: {}", id, error));
    }
    
    public Mono<OrganizationDto> updateOrganization(String id, CreateOrganizationRequest request, String userId) {
        log.debug("Calling Organization Service: PUT /organizations/{}", id);
        return webClient.put()
            .uri(SERVICE_PATH + "/organizations/{id}", id)
            .header("X-User-Id", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OrganizationDto.class)
            .doOnSuccess(org -> log.debug("Organization updated: {}", id))
            .doOnError(error -> log.error("Failed to update organization: {}", id, error));
    }
    
    public Mono<Void> deleteOrganization(String id, String userId) {
        log.debug("Calling Organization Service: DELETE /organizations/{}", id);
        return webClient.delete()
            .uri(SERVICE_PATH + "/organizations/{id}", id)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Organization deleted: {}", id))
            .doOnError(error -> log.error("Failed to delete organization: {}", id, error));
    }
    
    public Flux<MemberDto> getMembers(String organizationId, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}/members", organizationId);
        return webClient.get()
            .uri(SERVICE_PATH + "/organizations/{id}/members", organizationId)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToFlux(MemberDto.class)
            .doOnComplete(() -> log.debug("Retrieved members for organization: {}", organizationId))
            .doOnError(error -> log.error("Failed to get members for organization: {}", organizationId, error));
    }
    
    public Mono<MemberDto> addMember(String organizationId, com.turaf.bff.dto.AddMemberRequest request, String userId) {
        log.debug("Calling Organization Service: POST /organizations/{}/members", organizationId);
        return webClient.post()
            .uri(SERVICE_PATH + "/organizations/{id}/members", organizationId)
            .header("X-User-Id", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MemberDto.class)
            .doOnSuccess(member -> log.debug("Member added to organization: {}", organizationId))
            .doOnError(error -> log.error("Failed to add member to organization: {}", organizationId, error));
    }
    
    public Mono<MemberDto> updateMemberRole(String organizationId, String memberId, com.turaf.bff.dto.UpdateMemberRoleRequest request, String userId) {
        log.debug("Calling Organization Service: PUT /organizations/{}/members/{}/role", organizationId, memberId);
        return webClient.put()
            .uri(SERVICE_PATH + "/organizations/{orgId}/members/{userId}/role", organizationId, memberId)
            .header("X-User-Id", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MemberDto.class)
            .doOnSuccess(member -> log.debug("Member role updated in organization: {}", organizationId))
            .doOnError(error -> log.error("Failed to update member role in organization: {}", organizationId, error));
    }
    
    public Mono<Void> removeMember(String organizationId, String memberId, String userId) {
        log.debug("Calling Organization Service: DELETE /organizations/{}/members/{}", organizationId, memberId);
        return webClient.delete()
            .uri(SERVICE_PATH + "/organizations/{orgId}/members/{userId}", organizationId, memberId)
            .header("X-User-Id", userId)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Member removed from organization: {}", organizationId))
            .doOnError(error -> log.error("Failed to remove member from organization: {}", organizationId, error));
    }
}
