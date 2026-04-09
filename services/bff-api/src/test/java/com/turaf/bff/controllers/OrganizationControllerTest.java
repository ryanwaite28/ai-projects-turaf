package com.turaf.bff.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.CreateOrganizationRequest;
import com.turaf.bff.dto.MemberDto;
import com.turaf.bff.dto.OrganizationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrganizationServiceClient organizationServiceClient;
    
    @Test
    @WithMockUser(username = "user-123")
    void testGetOrganizations_Success() throws Exception {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-123")
            .name("Test Org")
            .ownerId("user-123")
            .build();
        
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(List.of(org));
        
        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("org-123"))
            .andExpect(jsonPath("$[0].name").value("Test Org"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testCreateOrganization_Success() throws Exception {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("New Org")
            .description("Test Description")
            .build();
        
        OrganizationDto org = OrganizationDto.builder()
            .id("org-456")
            .name("New Org")
            .description("Test Description")
            .build();
        
        when(organizationServiceClient.createOrganization(any(), anyString()))
            .thenReturn(org);
        
        mockMvc.perform(post("/api/v1/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("org-456"))
            .andExpect(jsonPath("$.name").value("New Org"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testGetOrganization_Success() throws Exception {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-789")
            .name("Specific Org")
            .build();
        
        when(organizationServiceClient.getOrganization(anyString(), anyString()))
            .thenReturn(org);
        
        mockMvc.perform(get("/api/v1/organizations/org-789"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("org-789"))
            .andExpect(jsonPath("$.name").value("Specific Org"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testGetMembers_Success() throws Exception {
        MemberDto member = MemberDto.builder()
            .id("member-1")
            .userId("user-1")
            .userName("John Doe")
            .role("ADMIN")
            .build();
        
        when(organizationServiceClient.getMembers(anyString(), anyString()))
            .thenReturn(List.of(member));
        
        mockMvc.perform(get("/api/v1/organizations/org-123/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("member-1"))
            .andExpect(jsonPath("$[0].userName").value("John Doe"));
    }
}
