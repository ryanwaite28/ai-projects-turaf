package com.turaf.organization.application.dto;

import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationSettings;

import java.time.Instant;

/**
 * DTO for organization data transfer.
 */
public class OrganizationDto {
    
    private String id;
    private String name;
    private String slug;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private OrganizationSettingsDto settings;
    
    public OrganizationDto() {
    }
    
    /**
     * Create DTO from domain model.
     *
     * @param org Domain Organization
     * @return DTO
     */
    public static OrganizationDto fromDomain(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.id = org.getId().getValue();
        dto.name = org.getName();
        dto.slug = org.getSlug();
        dto.createdBy = org.getCreatedBy().getValue();
        dto.createdAt = org.getCreatedAt();
        dto.updatedAt = org.getUpdatedAt();
        dto.settings = OrganizationSettingsDto.fromDomain(org.getSettings());
        return dto;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public OrganizationSettingsDto getSettings() {
        return settings;
    }
    
    public void setSettings(OrganizationSettingsDto settings) {
        this.settings = settings;
    }
    
    /**
     * DTO for organization settings.
     */
    public static class OrganizationSettingsDto {
        private boolean allowPublicExperiments;
        private int maxMembers;
        private int maxExperiments;
        
        public static OrganizationSettingsDto fromDomain(OrganizationSettings settings) {
            OrganizationSettingsDto dto = new OrganizationSettingsDto();
            dto.allowPublicExperiments = settings.isAllowPublicExperiments();
            dto.maxMembers = settings.getMaxMembers();
            dto.maxExperiments = settings.getMaxExperiments();
            return dto;
        }
        
        public boolean isAllowPublicExperiments() {
            return allowPublicExperiments;
        }
        
        public void setAllowPublicExperiments(boolean allowPublicExperiments) {
            this.allowPublicExperiments = allowPublicExperiments;
        }
        
        public int getMaxMembers() {
            return maxMembers;
        }
        
        public void setMaxMembers(int maxMembers) {
            this.maxMembers = maxMembers;
        }
        
        public int getMaxExperiments() {
            return maxExperiments;
        }
        
        public void setMaxExperiments(int maxExperiments) {
            this.maxExperiments = maxExperiments;
        }
    }
}
