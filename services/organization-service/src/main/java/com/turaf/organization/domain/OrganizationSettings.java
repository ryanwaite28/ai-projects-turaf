package com.turaf.organization.domain;

import java.util.Objects;

/**
 * Value object representing organization settings.
 * Immutable configuration for an organization.
 */
public final class OrganizationSettings {
    
    private final boolean allowPublicExperiments;
    private final int maxMembers;
    private final int maxExperiments;
    
    private OrganizationSettings(boolean allowPublicExperiments, int maxMembers, int maxExperiments) {
        this.allowPublicExperiments = allowPublicExperiments;
        this.maxMembers = validateMaxMembers(maxMembers);
        this.maxExperiments = validateMaxExperiments(maxExperiments);
    }
    
    /**
     * Create default organization settings.
     *
     * @return Default settings
     */
    public static OrganizationSettings createDefault() {
        return new OrganizationSettings(false, 10, 100);
    }
    
    /**
     * Create custom organization settings.
     *
     * @param allowPublicExperiments Whether to allow public experiments
     * @param maxMembers Maximum number of members
     * @param maxExperiments Maximum number of experiments
     * @return Custom settings
     */
    public static OrganizationSettings create(boolean allowPublicExperiments, int maxMembers, int maxExperiments) {
        return new OrganizationSettings(allowPublicExperiments, maxMembers, maxExperiments);
    }
    
    /**
     * Create a copy with updated public experiments setting.
     *
     * @param allowPublicExperiments New setting value
     * @return New settings instance
     */
    public OrganizationSettings withAllowPublicExperiments(boolean allowPublicExperiments) {
        return new OrganizationSettings(allowPublicExperiments, this.maxMembers, this.maxExperiments);
    }
    
    /**
     * Create a copy with updated max members.
     *
     * @param maxMembers New max members value
     * @return New settings instance
     */
    public OrganizationSettings withMaxMembers(int maxMembers) {
        return new OrganizationSettings(this.allowPublicExperiments, maxMembers, this.maxExperiments);
    }
    
    /**
     * Create a copy with updated max experiments.
     *
     * @param maxExperiments New max experiments value
     * @return New settings instance
     */
    public OrganizationSettings withMaxExperiments(int maxExperiments) {
        return new OrganizationSettings(this.allowPublicExperiments, this.maxMembers, maxExperiments);
    }
    
    private int validateMaxMembers(int maxMembers) {
        if (maxMembers < 1) {
            throw new IllegalArgumentException("Max members must be at least 1");
        }
        return maxMembers;
    }
    
    private int validateMaxExperiments(int maxExperiments) {
        if (maxExperiments < 1) {
            throw new IllegalArgumentException("Max experiments must be at least 1");
        }
        return maxExperiments;
    }
    
    // Getters
    
    public boolean isAllowPublicExperiments() {
        return allowPublicExperiments;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public int getMaxExperiments() {
        return maxExperiments;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationSettings that = (OrganizationSettings) o;
        return allowPublicExperiments == that.allowPublicExperiments &&
               maxMembers == that.maxMembers &&
               maxExperiments == that.maxExperiments;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(allowPublicExperiments, maxMembers, maxExperiments);
    }
}
