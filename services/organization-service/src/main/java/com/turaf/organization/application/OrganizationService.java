package com.turaf.organization.application;

import com.turaf.organization.application.dto.CreateOrganizationRequest;
import com.turaf.organization.application.dto.OrganizationDto;
import com.turaf.organization.application.dto.UpdateOrganizationRequest;
import com.turaf.organization.application.exception.OrganizationAlreadyExistsException;
import com.turaf.organization.application.exception.OrganizationNotFoundException;
import com.turaf.organization.domain.Organization;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.OrganizationRepository;
import com.turaf.organization.domain.OrganizationSettings;
import com.turaf.organization.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for organization lifecycle management.
 * Orchestrates domain operations and publishes domain events.
 */
@Service
@Transactional
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final EventPublisher eventPublisher;
    
    public OrganizationService(
        OrganizationRepository organizationRepository,
        EventPublisher eventPublisher
    ) {
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Create a new organization.
     *
     * @param request The creation request
     * @param createdBy The user creating the organization
     * @return Created organization DTO
     * @throws OrganizationAlreadyExistsException if slug already exists
     */
    public OrganizationDto createOrganization(CreateOrganizationRequest request, UserId createdBy) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new OrganizationAlreadyExistsException(
                "Organization with slug '" + request.getSlug() + "' already exists"
            );
        }
        
        OrganizationId id = OrganizationId.generate();
        Organization organization = new Organization(
            id,
            request.getName(),
            request.getSlug(),
            createdBy
        );
        
        Organization saved = organizationRepository.save(organization);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return OrganizationDto.fromDomain(saved);
    }
    
    /**
     * Get an organization by ID.
     *
     * @param id The organization ID
     * @return Organization DTO
     * @throws OrganizationNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public OrganizationDto getOrganization(OrganizationId id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization with ID '" + id.getValue() + "' not found"
            ));
        return OrganizationDto.fromDomain(organization);
    }
    
    /**
     * Get an organization by slug.
     *
     * @param slug The organization slug
     * @return Organization DTO
     * @throws OrganizationNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationBySlug(String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization with slug '" + slug + "' not found"
            ));
        return OrganizationDto.fromDomain(organization);
    }
    
    /**
     * Get all organizations for a user.
     *
     * @param userId The user ID
     * @return List of organization DTOs
     */
    @Transactional(readOnly = true)
    public java.util.List<OrganizationDto> getOrganizationsByUser(UserId userId) {
        return organizationRepository.findByUserId(userId).stream()
            .map(OrganizationDto::fromDomain)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Update an organization.
     *
     * @param id The organization ID
     * @param request The update request
     * @return Updated organization DTO
     * @throws OrganizationNotFoundException if not found
     */
    public OrganizationDto updateOrganization(OrganizationId id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization with ID '" + id.getValue() + "' not found"
            ));
        
        if (request.hasName()) {
            organization.updateName(request.getName());
        }
        
        if (request.hasSettings()) {
            OrganizationSettings currentSettings = organization.getSettings();
            OrganizationSettings newSettings = OrganizationSettings.create(
                request.getAllowPublicExperiments() != null 
                    ? request.getAllowPublicExperiments() 
                    : currentSettings.isAllowPublicExperiments(),
                request.getMaxMembers() != null 
                    ? request.getMaxMembers() 
                    : currentSettings.getMaxMembers(),
                request.getMaxExperiments() != null 
                    ? request.getMaxExperiments() 
                    : currentSettings.getMaxExperiments()
            );
            organization.updateSettings(newSettings);
        }
        
        Organization updated = organizationRepository.save(organization);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return OrganizationDto.fromDomain(updated);
    }
    
    /**
     * Delete an organization.
     *
     * @param id The organization ID
     * @throws OrganizationNotFoundException if not found
     */
    public void deleteOrganization(OrganizationId id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization with ID '" + id.getValue() + "' not found"
            ));
        
        organizationRepository.delete(organization);
    }
}
