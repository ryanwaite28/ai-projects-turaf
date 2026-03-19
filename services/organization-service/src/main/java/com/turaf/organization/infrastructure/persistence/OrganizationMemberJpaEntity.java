package com.turaf.organization.infrastructure.persistence;

import com.turaf.organization.domain.MemberRole;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationMember;
import com.turaf.organization.domain.UserId;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for OrganizationMember.
 * Maps domain model to database schema.
 */
@Entity
@Table(
    name = "organization_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"})
)
public class OrganizationMemberJpaEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;
    
    @Column(name = "added_by", nullable = false, length = 36)
    private String addedBy;
    
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected OrganizationMemberJpaEntity() {
        // Required by JPA
    }
    
    /**
     * Convert JPA entity to domain model.
     *
     * @return Domain OrganizationMember
     */
    public OrganizationMember toDomain() {
        return new OrganizationMember(
            id,
            OrganizationId.of(organizationId),
            UserId.of(userId),
            role,
            UserId.of(addedBy),
            addedAt,
            updatedAt
        );
    }
    
    /**
     * Create JPA entity from domain model.
     *
     * @param member Domain OrganizationMember
     * @return JPA entity
     */
    public static OrganizationMemberJpaEntity fromDomain(OrganizationMember member) {
        OrganizationMemberJpaEntity entity = new OrganizationMemberJpaEntity();
        entity.id = member.getId();
        entity.organizationId = member.getOrganizationId().getValue();
        entity.userId = member.getUserId().getValue();
        entity.role = member.getRole();
        entity.addedBy = member.getAddedBy().getValue();
        entity.addedAt = member.getAddedAt();
        entity.updatedAt = member.getUpdatedAt();
        return entity;
    }
    
    // Getters and setters
    
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
    
    public MemberRole getRole() {
        return role;
    }
    
    public void setRole(MemberRole role) {
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationMemberJpaEntity that = (OrganizationMemberJpaEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
