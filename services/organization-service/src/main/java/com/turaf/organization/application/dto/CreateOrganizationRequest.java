package com.turaf.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new organization.
 */
public class CreateOrganizationRequest {
    
    @NotBlank(message = "Organization name is required")
    @Size(min = 1, max = 100, message = "Organization name must be between 1 and 100 characters")
    private String name;
    
    @NotBlank(message = "Organization slug is required")
    @Pattern(
        regexp = "^[a-z0-9-]{3,50}$",
        message = "Slug must be 3-50 characters, lowercase letters, numbers, and hyphens only"
    )
    private String slug;
    
    public CreateOrganizationRequest() {
    }
    
    public CreateOrganizationRequest(String name, String slug) {
        this.name = name;
        this.slug = slug;
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
}
