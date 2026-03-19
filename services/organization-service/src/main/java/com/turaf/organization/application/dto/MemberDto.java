package com.turaf.organization.application.dto;

import com.turaf.organization.domain.OrganizationMember;

import java.time.Instant;

/**
 * DTO for organization member data transfer.
 */
public class MemberDto {
    
    private String id;
    private String organizationId;
    private String userId;
    private String role;
    private String addedBy;
    private Instant addedAt;
    private Instant updatedAt;
    
    public MemberDto() {
    }
    
    /**
     * Create DTO from domain model.
     *
     * @param member Domain OrganizationMember
     * @return DTO
     */
    public static MemberDto fromDomain(OrganizationMember member) {
        MemberDto dto = new MemberDto();
        dto.id = member.getId();
        dto.organizationId = member.getOrganizationId().getValue();
        dto.userId = member.getUserId().getValue();
        dto.role = member.getRole().name();
        dto.addedBy = member.getAddedBy().getValue();
        dto.addedAt = member.getAddedAt();
        dto.updatedAt = member.getUpdatedAt();
        return dto;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getAddedBy() {
        return addedBy;
    }
    
    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
    
    public Instant getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
