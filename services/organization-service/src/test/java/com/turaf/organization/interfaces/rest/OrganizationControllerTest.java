package com.turaf.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.organization.application.OrganizationService;
import com.turaf.organization.application.dto.CreateOrganizationRequest;
import com.turaf.organization.application.dto.OrganizationDto;
import com.turaf.organization.application.dto.UpdateOrganizationRequest;
import com.turaf.organization.application.exception.OrganizationAlreadyExistsException;
import com.turaf.organization.application.exception.OrganizationNotFoundException;
import com.turaf.organization.domain.OrganizationId;
import com.turaf.organization.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrganizationController.
 */
@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrganizationService organizationService;
    
    @Test
    @WithMockUser(username = "user123")
    void shouldCreateOrganization() throws Exception {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        
        OrganizationDto responseDto = new OrganizationDto();
        responseDto.setId("org-123");
        responseDto.setName("Test Org");
        responseDto.setSlug("test-org");
        responseDto.setCreatedBy("user123");
        responseDto.setCreatedAt(Instant.now());
        responseDto.setUpdatedAt(Instant.now());
        
        when(organizationService.createOrganization(any(CreateOrganizationRequest.class), any(UserId.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("org-123"))
            .andExpect(jsonPath("$.name").value("Test Org"))
            .andExpect(jsonPath("$.slug").value("test-org"));
        
        verify(organizationService).createOrganization(any(CreateOrganizationRequest.class), any(UserId.class));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldReturnConflictWhenSlugExists() throws Exception {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");
        
        when(organizationService.createOrganization(any(CreateOrganizationRequest.class), any(UserId.class)))
            .thenThrow(new OrganizationAlreadyExistsException("Organization with slug 'test-org' already exists"));
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_ALREADY_EXISTS"));
    }
    
    @Test
    @WithMockUser
    void shouldGetOrganizationById() throws Exception {
        // Given
        OrganizationDto responseDto = new OrganizationDto();
        responseDto.setId("org-123");
        responseDto.setName("Test Org");
        responseDto.setSlug("test-org");
        
        when(organizationService.getOrganization(any(OrganizationId.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("org-123"))
            .andExpect(jsonPath("$.name").value("Test Org"));
        
        verify(organizationService).getOrganization(any(OrganizationId.class));
    }
    
    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenOrganizationDoesNotExist() throws Exception {
        // Given
        when(organizationService.getOrganization(any(OrganizationId.class)))
            .thenThrow(new OrganizationNotFoundException("Organization not found"));
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }
    
    @Test
    @WithMockUser
    void shouldGetOrganizationBySlug() throws Exception {
        // Given
        OrganizationDto responseDto = new OrganizationDto();
        responseDto.setId("org-123");
        responseDto.setName("Test Org");
        responseDto.setSlug("test-org");
        
        when(organizationService.getOrganizationBySlug("test-org"))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(get("/api/v1/organizations/slug/test-org"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("test-org"));
        
        verify(organizationService).getOrganizationBySlug("test-org");
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldUpdateOrganization() throws Exception {
        // Given
        UpdateOrganizationRequest request = new UpdateOrganizationRequest("Updated Name");
        
        OrganizationDto responseDto = new OrganizationDto();
        responseDto.setId("org-123");
        responseDto.setName("Updated Name");
        responseDto.setSlug("test-org");
        
        when(organizationService.updateOrganization(any(OrganizationId.class), any(UpdateOrganizationRequest.class)))
            .thenReturn(responseDto);
        
        // When/Then
        mockMvc.perform(put("/api/v1/organizations/org-123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"));
        
        verify(organizationService).updateOrganization(any(OrganizationId.class), any(UpdateOrganizationRequest.class));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldDeleteOrganization() throws Exception {
        // Given
        doNothing().when(organizationService).deleteOrganization(any(OrganizationId.class));
        
        // When/Then
        mockMvc.perform(delete("/api/v1/organizations/org-123")
                .with(csrf()))
            .andExpect(status().isNoContent());
        
        verify(organizationService).deleteOrganization(any(OrganizationId.class));
    }
    
    @Test
    @WithMockUser(username = "user123")
    void shouldReturnBadRequestForInvalidInput() throws Exception {
        // Given - invalid request with empty name
        CreateOrganizationRequest request = new CreateOrganizationRequest("", "test-org");
        
        // When/Then
        mockMvc.perform(post("/api/v1/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
