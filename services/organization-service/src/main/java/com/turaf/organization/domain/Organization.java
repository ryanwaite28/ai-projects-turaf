package com.turaf.organization.domain;

import com.turaf.organization.domain.common.AggregateRoot;
import com.turaf.organization.domain.event.OrganizationCreated;
import com.turaf.organization.domain.event.OrganizationUpdated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Organization aggregate root.
 * Represents a multi-tenant organization in the system.
 * Enforces all business rules and invariants for organizations.
 */
public class Organization extends AggregateRoot<OrganizationId> {
    
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_SLUG_LENGTH = 3;
    private static final int MAX_SLUG_LENGTH = 50;
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{3,50}$");
    
    private String name;
    private String slug;
    private final Instant createdAt;
    private Instant updatedAt;
    private final UserId createdBy;
    private OrganizationSettings settings;
    
    /**
     * Create a new Organization.
     *
     * @param id Organization identifier
     * @param name Organization name
     * @param slug URL-friendly slug
     * @param createdBy User who created the organization
     */
    public Organization(OrganizationId id, String name, String slug, UserId createdBy) {
        super(id);
        this.name = validateName(name);
        this.slug = validateSlug(slug);
        this.createdBy = Objects.requireNonNull(createdBy, "Created by user cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.settings = OrganizationSettings.createDefault();
        
        registerEvent(new OrganizationCreated(
            UUID.randomUUID().toString(),
            id.getValue(),
            name,
            slug,
            createdBy.getValue(),
            createdAt
        ));
    }
    
    /**
     * Reconstruct an Organization from persistence.
     * Used by repositories to rebuild the aggregate.
     */
    public Organization(OrganizationId id, String name, String slug, UserId createdBy,
                       Instant createdAt, Instant updatedAt, OrganizationSettings settings) {
        super(id);
        this.name = name;
        this.slug = slug;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.settings = settings != null ? settings : OrganizationSettings.createDefault();
    }
    
    /**
     * Update the organization name.
     *
     * @param newName The new name
     */
    public void updateName(String newName) {
        String validatedName = validateName(newName);
        if (!this.name.equals(validatedName)) {
            this.name = validatedName;
            this.updatedAt = Instant.now();
            
            registerEvent(new OrganizationUpdated(
                UUID.randomUUID().toString(),
                getId().getValue(),
                "name",
                this.name,
                updatedAt
            ));
        }
    }
    
    /**
     * Update organization settings.
     *
     * @param newSettings The new settings
     */
    public void updateSettings(OrganizationSettings newSettings) {
        this.settings = Objects.requireNonNull(newSettings, "Settings cannot be null");
        this.updatedAt = Instant.now();
    }
    
    /**
     * Validate organization name.
     *
     * @param name The name to validate
     * @return The validated name
     * @throws IllegalArgumentException if validation fails
     */
    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name cannot be null or blank");
        }
        
        String trimmedName = name.trim();
        
        if (trimmedName.length() < MIN_NAME_LENGTH || trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Organization name must be between %d and %d characters",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH)
            );
        }
        
        return trimmedName;
    }
    
    /**
     * Validate organization slug.
     *
     * @param slug The slug to validate
     * @return The validated slug
     * @throws IllegalArgumentException if validation fails
     */
    private String validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Organization slug cannot be null or blank");
        }
        
        String trimmedSlug = slug.trim().toLowerCase();
        
        if (!SLUG_PATTERN.matcher(trimmedSlug).matches()) {
            throw new IllegalArgumentException(
                String.format("Slug must be %d-%d lowercase alphanumeric characters with hyphens",
                    MIN_SLUG_LENGTH, MAX_SLUG_LENGTH)
            );
        }
        
        return trimmedSlug;
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public UserId getCreatedBy() {
        return createdBy;
    }
    
    public OrganizationSettings getSettings() {
        return settings;
    }
}
