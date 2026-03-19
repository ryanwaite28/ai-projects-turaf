package com.turaf.organization.application.dto;

import com.turaf.organization.domain.OrganizationSettings;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an organization.
 */
public class UpdateOrganizationRequest {
    
    @Size(min = 1, max = 100, message = "Organization name must be between 1 and 100 characters")
    private String name;
    
    private Boolean allowPublicExperiments;
    
    @Min(value = 1, message = "Max members must be at least 1")
    private Integer maxMembers;
    
    @Min(value = 1, message = "Max experiments must be at least 1")
    private Integer maxExperiments;
    
    public UpdateOrganizationRequest() {
    }
    
    public UpdateOrganizationRequest(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getAllowPublicExperiments() {
        return allowPublicExperiments;
    }
    
    public void setAllowPublicExperiments(Boolean allowPublicExperiments) {
        this.allowPublicExperiments = allowPublicExperiments;
    }
    
    public Integer getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public Integer getMaxExperiments() {
        return maxExperiments;
    }
    
    public void setMaxExperiments(Integer maxExperiments) {
        this.maxExperiments = maxExperiments;
    }
    
    public boolean hasName() {
        return name != null;
    }
    
    public boolean hasSettings() {
        return allowPublicExperiments != null || maxMembers != null || maxExperiments != null;
    }
}
