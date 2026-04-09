package com.turaf.bff.clients;

import com.turaf.bff.dto.AddMemberRequest;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.dto.UpdateMemberRoleRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class OrganizationServiceClient {
    
    private final RestClient restClient;
    private static final String SERVICE_PATH = "/api/v1/organizations";
    
    public OrganizationServiceClient(@Qualifier("organizationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    
    public List<OrganizationDto> getOrganizations(String userId) {
        log.debug("Calling Organization Service: GET /organizations for user: {}", userId);
        List<OrganizationDto> organizations = restClient.get()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<OrganizationDto>>() {});
        log.debug("Retrieved organizations for user: {}", userId);
        return organizations;
    }
    
    public OrganizationDto createOrganization(CreateOrganizationRequest request, String userId) {
        log.debug("Calling Organization Service: POST /organizations");
        OrganizationDto org = restClient.post()
            .uri(SERVICE_PATH)
            .header("X-User-Id", userId)
            .body(request)
            .retrieve()
            .body(OrganizationDto.class);
        log.debug("Organization created: {}", org.getId());
        return org;
    }
    
    public OrganizationDto getOrganization(String id, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}", id);
        OrganizationDto org = restClient.get()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .retrieve()
            .body(OrganizationDto.class);
        log.debug("Retrieved organization: {}", id);
        return org;
    }
    
    public OrganizationDto updateOrganization(String id, CreateOrganizationRequest request, String userId) {
        log.debug("Calling Organization Service: PUT /organizations/{}", id);
        OrganizationDto org = restClient.put()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .body(request)
            .retrieve()
            .body(OrganizationDto.class);
        log.debug("Organization updated: {}", id);
        return org;
    }
    
    public void deleteOrganization(String id, String userId) {
        log.debug("Calling Organization Service: DELETE /organizations/{}", id);
        restClient.delete()
            .uri(SERVICE_PATH + "/{id}", id)
            .header("X-User-Id", userId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Organization deleted: {}", id);
    }
    
    public List<MemberDto> getMembers(String organizationId, String userId) {
        log.debug("Calling Organization Service: GET /organizations/{}/members", organizationId);
        List<MemberDto> members = restClient.get()
            .uri(SERVICE_PATH + "/{id}/members", organizationId)
            .header("X-User-Id", userId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<MemberDto>>() {});
        log.debug("Retrieved members for organization: {}", organizationId);
        return members;
    }
    
    public MemberDto addMember(String organizationId, AddMemberRequest request, String userId) {
        log.debug("Calling Organization Service: POST /organizations/{}/members", organizationId);
        MemberDto member = restClient.post()
            .uri(SERVICE_PATH + "/{id}/members", organizationId)
            .header("X-User-Id", userId)
            .body(request)
            .retrieve()
            .body(MemberDto.class);
        log.debug("Member added to organization: {}", organizationId);
        return member;
    }
    
    public MemberDto updateMemberRole(String organizationId, String memberId, UpdateMemberRoleRequest request, String userId) {
        log.debug("Calling Organization Service: PUT /organizations/{}/members/{}/role", organizationId, memberId);
        MemberDto member = restClient.put()
            .uri(SERVICE_PATH + "/{orgId}/members/{userId}/role", organizationId, memberId)
            .header("X-User-Id", userId)
            .body(request)
            .retrieve()
            .body(MemberDto.class);
        log.debug("Member role updated in organization: {}", organizationId);
        return member;
    }
    
    public void removeMember(String organizationId, String memberId, String userId) {
        log.debug("Calling Organization Service: DELETE /organizations/{}/members/{}", organizationId, memberId);
        restClient.delete()
            .uri(SERVICE_PATH + "/{orgId}/members/{userId}", organizationId, memberId)
            .header("X-User-Id", userId)
            .retrieve()
            .toBodilessEntity();
        log.debug("Member removed from organization: {}", organizationId);
    }
}
