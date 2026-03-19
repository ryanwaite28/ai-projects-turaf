package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationSettings;
import com.turaf.organization.domain.UserId;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for Organization aggregate.
 * Maps domain model to database schema.
 */
@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 50, unique = true)
    private String slug;
    
    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "allow_public_experiments", nullable = false)
    private boolean allowPublicExperiments = false;
    
    @Column(name = "max_members", nullable = false)
    private int maxMembers = 10;
    
    @Column(name = "max_experiments", nullable = false)
    private int maxExperiments = 100;
    
    protected OrganizationJpaEntity() {
        // Required by JPA
    }
    
    /**
     * Convert JPA entity to domain model.
     *
     * @return Domain Organization
     */
    public Organization toDomain() {
        OrganizationSettings settings = OrganizationSettings.create(
            allowPublicExperiments,
            maxMembers,
            maxExperiments
        );
        
        return new Organization(
            OrganizationId.of(id),
            name,
            slug,
            UserId.of(createdBy),
            createdAt,
            updatedAt,
            settings
        );
    }
    
    /**
     * Create JPA entity from domain model.
     *
     * @param org Domain Organization
     * @return JPA entity
     */
    public static OrganizationJpaEntity fromDomain(Organization org) {
        OrganizationJpaEntity entity = new OrganizationJpaEntity();
        entity.id = org.getId().getValue();
        entity.name = org.getName();
        entity.slug = org.getSlug();
        entity.createdBy = org.getCreatedBy().getValue();
        entity.createdAt = org.getCreatedAt();
        entity.updatedAt = org.getUpdatedAt();
        entity.allowPublicExperiments = org.getSettings().isAllowPublicExperiments();
        entity.maxMembers = org.getSettings().getMaxMembers();
        entity.maxExperiments = org.getSettings().getMaxExperiments();
        return entity;
    }
    
    // Getters and setters
    
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationJpaEntity that = (OrganizationJpaEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
