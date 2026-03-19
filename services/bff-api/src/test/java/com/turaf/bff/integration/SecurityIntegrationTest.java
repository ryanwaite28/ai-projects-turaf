package com.turaf.bff.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testCorsPreflightRequest_AllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().exists("Access-Control-Max-Age"));
    }
    
    @Test
    void testCorsActualRequest_IncludesHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testRateLimiting_PublicEndpoint() throws Exception {
        for (int i = 0; i < 11; i++) {
            var result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"));
            
            if (i >= 10) {
                result.andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("Too Many Requests"));
            }
        }
    }
    
    @Test
    void testActuatorEndpoints_ExcludedFromRateLimiting() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }
    }
    
    @Test
    void testActuatorEndpoints_ExcludedFromAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
        
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testProtectedEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/v1/experiments")
                .param("organizationId", "org-1"))
            .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/v1/dashboard/overview"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testInvalidJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCorrelationIdGeneration() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(header().exists("X-Correlation-Id"));
    }
    
    @Test
    void testCorrelationIdPropagation() throws Exception {
        String correlationId = "test-correlation-456";
        
        mockMvc.perform(get("/actuator/health")
                .header("X-Correlation-Id", correlationId))
            .andExpect(header().string("X-Correlation-Id", correlationId));
    }
}
