package com.turaf.bff.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.bff.clients.ExperimentServiceClient;
import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
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

@WebMvcTest(ExperimentController.class)
class ExperimentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ExperimentServiceClient experimentServiceClient;
    
    @Test
    @WithMockUser(username = "user-123")
    void testGetExperiments_Success() throws Exception {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-123")
            .name("Test Experiment")
            .status("RUNNING")
            .build();
        
        when(experimentServiceClient.getExperiments(anyString(), anyString()))
            .thenReturn(List.of(experiment));
        
        mockMvc.perform(get("/api/v1/experiments?organizationId=org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("exp-123"))
            .andExpect(jsonPath("$[0].name").value("Test Experiment"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testCreateExperiment_Success() throws Exception {
        CreateExperimentRequest request = CreateExperimentRequest.builder()
            .name("New Experiment")
            .description("Test Description")
            .organizationId("org-123")
            .problemId("prob-1")
            .hypothesisId("hyp-1")
            .build();
        
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-456")
            .name("New Experiment")
            .status("DRAFT")
            .build();
        
        when(experimentServiceClient.createExperiment(any(), anyString(), anyString()))
            .thenReturn(experiment);
        
        mockMvc.perform(post("/api/v1/experiments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("exp-456"))
            .andExpect(jsonPath("$.name").value("New Experiment"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testStartExperiment_Success() throws Exception {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-789")
            .name("Started Experiment")
            .status("RUNNING")
            .build();
        
        when(experimentServiceClient.startExperiment(anyString(), anyString(), anyString()))
            .thenReturn(experiment);
        
        mockMvc.perform(post("/api/v1/experiments/exp-789/start?organizationId=org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("exp-789"))
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }
    
    @Test
    @WithMockUser(username = "user-123")
    void testCompleteExperiment_Success() throws Exception {
        ExperimentDto experiment = ExperimentDto.builder()
            .id("exp-999")
            .name("Completed Experiment")
            .status("COMPLETED")
            .build();
        
        when(experimentServiceClient.completeExperiment(anyString(), anyString(), anyString()))
            .thenReturn(experiment);
        
        mockMvc.perform(post("/api/v1/experiments/exp-999/complete?organizationId=org-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("exp-999"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
