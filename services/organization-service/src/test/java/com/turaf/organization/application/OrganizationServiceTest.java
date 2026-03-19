package com.turaf.organization.application;

import com.turaf.organization.application.dto.CreateOrganizationRequest;
import com.turaf.organization.application.dto.OrganizationDto;
import com.turaf.organization.application.dto.UpdateOrganizationRequest;
import com.turaf.organization.application.exception.OrganizationAlreadyExistsException;
import com.turaf.organization.application.exception.OrganizationNotFoundException;
import com.turaf.organization.domain.*;
import com.turaf.organization.domain.common.DomainEvent;
import com.turaf.organization.domain.event.OrganizationCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationService.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {
    
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    private OrganizationService organizationService;
    
    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository, eventPublisher);
    }
    
    @Test
    void shouldCreateOrganizationSuccessfully() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        UserId createdBy = UserId.generate();
        
        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            return org;
        });
        
        // When
        OrganizationDto result = organizationService.createOrganization(request, createdBy);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Org", result.getName());
        assertEquals("test-org", result.getSlug());
        assertEquals(createdBy.getValue(), result.getCreatedBy());
        
        verify(organizationRepository).existsBySlug("test-org");
        verify(organizationRepository).save(any(Organization.class));
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldThrowExceptionWhenSlugAlreadyExists() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        UserId createdBy = UserId.generate();
        
        when(organizationRepository.existsBySlug("test-org")).thenReturn(true);
        
        // When/Then
        assertThrows(
            OrganizationAlreadyExistsException.class,
            () -> organizationService.createOrganization(request, createdBy)
        );
        
        verify(organizationRepository).existsBySlug("test-org");
        verify(organizationRepository, never()).save(any(Organization.class));
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldPublishOrganizationCreatedEvent() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        UserId createdBy = UserId.generate();
        
        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            return org;
        });
        
        // When
        organizationService.createOrganization(request, createdBy);
        
        // Then
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        
        boolean hasCreatedEvent = eventCaptor.getAllValues().stream()
            .anyMatch(event -> event instanceof OrganizationCreated);
        assertTrue(hasCreatedEvent, "OrganizationCreated event should be published");
    }
    
    @Test
    void shouldGetOrganizationById() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        
        // When
        OrganizationDto result = organizationService.getOrganization(id);
        
        // Then
        assertNotNull(result);
        assertEquals(id.getValue(), result.getId());
        assertEquals("Test Org", result.getName());
        assertEquals("test-org", result.getSlug());
        
        verify(organizationRepository).findById(id);
    }
    
    @Test
    void shouldThrowExceptionWhenOrganizationNotFoundById() {
        // Given
        OrganizationId id = OrganizationId.generate();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            OrganizationNotFoundException.class,
            () -> organizationService.getOrganization(id)
        );
        
        verify(organizationRepository).findById(id);
    }
    
    @Test
    void shouldGetOrganizationBySlug() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        when(organizationRepository.findBySlug("test-org")).thenReturn(Optional.of(org));
        
        // When
        OrganizationDto result = organizationService.getOrganizationBySlug("test-org");
        
        // Then
        assertNotNull(result);
        assertEquals("Test Org", result.getName());
        assertEquals("test-org", result.getSlug());
        
        verify(organizationRepository).findBySlug("test-org");
    }
    
    @Test
    void shouldThrowExceptionWhenOrganizationNotFoundBySlug() {
        // Given
        when(organizationRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            OrganizationNotFoundException.class,
            () -> organizationService.getOrganizationBySlug("nonexistent")
        );
        
        verify(organizationRepository).findBySlug("nonexistent");
    }
    
    @Test
    void shouldUpdateOrganizationName() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Old Name", "test-org", createdBy);
        
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("New Name");
        
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization updated = invocation.getArgument(0);
            return updated;
        });
        
        // When
        OrganizationDto result = organizationService.updateOrganization(id, request);
        
        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        
        verify(organizationRepository).findById(id);
        verify(organizationRepository).save(any(Organization.class));
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }
    
    @Test
    void shouldUpdateOrganizationSettings() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setAllowPublicExperiments(true);
        request.setMaxMembers(50);
        request.setMaxExperiments(200);
        
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization updated = invocation.getArgument(0);
            return updated;
        });
        
        // When
        OrganizationDto result = organizationService.updateOrganization(id, request);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getSettings().isAllowPublicExperiments());
        assertEquals(50, result.getSettings().getMaxMembers());
        assertEquals(200, result.getSettings().getMaxExperiments());
        
        verify(organizationRepository).findById(id);
        verify(organizationRepository).save(any(Organization.class));
    }
    
    @Test
    void shouldUpdateOrganizationNameAndSettings() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Old Name", "test-org", createdBy);
        
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("New Name");
        request.setMaxMembers(25);
        
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization updated = invocation.getArgument(0);
            return updated;
        });
        
        // When
        OrganizationDto result = organizationService.updateOrganization(id, request);
        
        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals(25, result.getSettings().getMaxMembers());
        
        verify(organizationRepository).findById(id);
        verify(organizationRepository).save(any(Organization.class));
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("New Name");
        
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            OrganizationNotFoundException.class,
            () -> organizationService.updateOrganization(id, request)
        );
        
        verify(organizationRepository).findById(id);
        verify(organizationRepository, never()).save(any(Organization.class));
    }
    
    @Test
    void shouldDeleteOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        UserId createdBy = UserId.generate();
        Organization org = new Organization(id, "Test Org", "test-org", createdBy);
        
        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        
        // When
        organizationService.deleteOrganization(id);
        
        // Then
        verify(organizationRepository).findById(id);
        verify(organizationRepository).delete(org);
    }
    
    @Test
    void shouldThrowExceptionWhenDeletingNonexistentOrganization() {
        // Given
        OrganizationId id = OrganizationId.generate();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(
            OrganizationNotFoundException.class,
            () -> organizationService.deleteOrganization(id)
        );
        
        verify(organizationRepository).findById(id);
        verify(organizationRepository, never()).delete(any(Organization.class));
    }
}
