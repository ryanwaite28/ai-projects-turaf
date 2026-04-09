package com.turaf.bff.controllers;

import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.clients.IdentityServiceClient;
import com.turaf.bff.clients.MetricsServiceClient;
import com.turaf.bff.clients.OrganizationServiceClient;
import com.turaf.bff.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private OrganizationServiceClient organizationServiceClient;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
    @MockBean
    private MetricsServiceClient metricsServiceClient;
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetDashboardOverview_Success() throws Exception {
        UserDto user = UserDto.builder()
            .id("user-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();
        
        OrganizationDto org = OrganizationDto.builder()
            .id("org-123")
            .name("Test Org")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-123")
            .name("Test Experiment")
            .status("RUNNING")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(user);
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenReturn(List.of(org));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(List.of(experiment));
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
            .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-123"))
            .andExpect(jsonPath("$.organizations[0].id").value("org-123"))
            .andExpect(jsonPath("$.activeExperiments[0].id").value("exp-123"))
            .andExpect(jsonPath("$.totalOrganizations").value(1))
            .andExpect(jsonPath("$.totalActiveExperiments").value(1));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetDashboardOverview_WithErrors() throws Exception {
        UserDto user = UserDto.builder()
            .id("user-123")
            .build();
        
        when(identityServiceClient.getCurrentUser(anyString()))
            .thenReturn(user);
        when(organizationServiceClient.getOrganizations(anyString()))
            .thenThrow(new RuntimeException("Service error"));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenThrow(new RuntimeException("Service error"));
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
            .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-123"))
            .andExpect(jsonPath("$.organizations").isArray())
            .andExpect(jsonPath("$.organizations").isEmpty())
            .andExpect(jsonPath("$.activeExperiments").isArray())
            .andExpect(jsonPath("$.activeExperiments").isEmpty())
            .andExpect(jsonPath("$.totalOrganizations").value(0))
            .andExpect(jsonPath("$.totalActiveExperiments").value(0));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetExperimentFull_Success() throws Exception {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-456")
            .name("Full Experiment")
            .status("RUNNING")
            .build();
        
        MetricDto metric1 = MetricDto.builder()
            .id("metric-1")
            .name("clicks")
            .value(100.0)
            .build();
        
        MetricDto metric2 = MetricDto.builder()
            .id("metric-2")
            .name("views")
            .value(1000.0)
            .build();
        
        when(experimentServiceClient.getExperiment(anyString(), anyString(), anyString()))
            .thenReturn(experiment);
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenReturn(List.of(metric1, metric2));
        
        mockMvc.perform(get("/api/v1/dashboard/experiments/exp-456/full?organizationId=org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.experiment.id").value("exp-456"))
            .andExpect(jsonPath("$.metrics[0].id").value("metric-1"))
            .andExpect(jsonPath("$.metrics[1].id").value("metric-2"))
            .andExpect(jsonPath("$.totalMetrics").value(2));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetOrganizationSummary_Success() throws Exception {
        OrganizationDto org = OrganizationDto.builder()
            .id("org-789")
            .name("Summary Org")
            .build();
        
        MemberDto member = MemberDto.builder()
            .id("member-1")
            .userName("John Doe")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-789")
            .name("Org Experiment")
            .build();
        
        when(organizationServiceClient.getOrganization(anyString(), anyString()))
            .thenReturn(org);
        when(organizationServiceClient.getMembers(anyString(), anyString()))
            .thenReturn(List.of(member));
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(List.of(experiment));
        
        mockMvc.perform(get("/api/v1/dashboard/organizations/org-789/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organization.id").value("org-789"))
            .andExpect(jsonPath("$.members[0].id").value("member-1"))
            .andExpect(jsonPath("$.experiments[0].id").value("exp-789"))
            .andExpect(jsonPath("$.totalMembers").value(1))
            .andExpect(jsonPath("$.totalExperiments").value(1));
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = {"ROLE_USER"})
    void testGetExperimentFull_WithMetricsError() throws Exception {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-999")
            .name("Experiment with Error")
            .build();
        
        when(experimentServiceClient.getExperiment(anyString(), anyString(), anyString()))
            .thenReturn(experiment);
        when(metricsServiceClient.getExperimentMetrics(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Metrics service error"));
        
        mockMvc.perform(get("/api/v1/dashboard/experiments/exp-999/full?organizationId=org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.experiment.id").value("exp-999"))
            .andExpect(jsonPath("$.metrics").isArray())
            .andExpect(jsonPath("$.metrics").isEmpty())
            .andExpect(jsonPath("$.totalMetrics").value(0));
    }
}
