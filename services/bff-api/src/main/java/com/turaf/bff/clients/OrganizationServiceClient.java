package com.turaf.bff.clients;

import com.turaf.bff.dto.AddMemberRequest;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import com.turaf.bff.dto.UpdateMemberRoleRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

/**
 * Organization Service HTTP Client using Spring's declarative HTTP interface.
 */
@HttpExchange(url = "/api/v1/organizations", accept = "application/json", contentType = "application/json")
public interface OrganizationServiceClient {
    
    @GetExchange
    List<OrganizationDto> getOrganizations(@RequestHeader("X-User-Id") String userId);
    
    @PostExchange
    OrganizationDto createOrganization(@RequestBody CreateOrganizationRequest request,
                                       @RequestHeader("X-User-Id") String userId);
    
    @GetExchange("/{id}")
    OrganizationDto getOrganization(@PathVariable String id,
                                    @RequestHeader("X-User-Id") String userId);
    
    @PutExchange("/{id}")
    OrganizationDto updateOrganization(@PathVariable String id,
                                       @RequestBody CreateOrganizationRequest request,
                                       @RequestHeader("X-User-Id") String userId);
    
    @DeleteExchange("/{id}")
    void deleteOrganization(@PathVariable String id,
                           @RequestHeader("X-User-Id") String userId);
    
    @GetExchange("/{id}/members")
    List<MemberDto> getMembers(@PathVariable String id,
                               @RequestHeader("X-User-Id") String userId);
    
    @PostExchange("/{id}/members")
    MemberDto addMember(@PathVariable String id,
                        @RequestBody AddMemberRequest request,
                        @RequestHeader("X-User-Id") String userId);
    
    @PutExchange("/{orgId}/members/{memberId}/role")
    MemberDto updateMemberRole(@PathVariable String orgId,
                               @PathVariable String memberId,
                               @RequestBody UpdateMemberRoleRequest request,
                               @RequestHeader("X-User-Id") String userId);
    
    @DeleteExchange("/{orgId}/members/{memberId}")
    void removeMember(@PathVariable String orgId,
                     @PathVariable String memberId,
                     @RequestHeader("X-User-Id") String userId);
}
