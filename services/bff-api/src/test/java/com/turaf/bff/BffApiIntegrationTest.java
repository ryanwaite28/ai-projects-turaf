package com.turaf.bff;

import com.turaf.bff.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "cors.allowed-origins=http://localhost:4200",
    "jwt.secret-key=test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm"
})
class BffApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testApplicationContextLoads() {
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
    
    @Test
    void testPrometheusMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain;version=0.0.4;charset=utf-8"));
    }
    
    @Test
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    
    @Test
    void testCorrelationIdGeneration() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(header().exists("X-Correlation-Id"));
    }
    
    @Test
    void testCorrelationIdPropagation() throws Exception {
        String correlationId = "test-correlation-123";
        
        mockMvc.perform(get("/actuator/health")
                .header("X-Correlation-Id", correlationId))
            .andExpect(header().string("X-Correlation-Id", correlationId));
    }
    
    @Test
    void testPublicEndpointAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().is5xxServerError());
    }
    
    @Test
    void testProtectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testInvalidJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testActuatorEndpointsExcludedFromRateLimiting() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }
    }
}
