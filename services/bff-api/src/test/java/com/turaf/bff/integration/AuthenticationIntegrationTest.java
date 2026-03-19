package com.turaf.bff.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testLoginFlow_Success() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"user-123\",\"email\":\"test@example.com\",\"token\":\"jwt-token\",\"name\":\"Test User\"}")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.token").value("jwt-token"))
            .andExpect(header().exists("X-Correlation-Id"));
        
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v1/auth/login"))
            .withHeader("Content-Type", equalTo("application/json")));
    }
    
    @Test
    void testLoginFlow_InvalidCredentials() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Unauthorized\",\"message\":\"Invalid credentials\"}")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testRegisterFlow_Success() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/register"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"user-456\",\"email\":\"newuser@example.com\",\"token\":\"new-jwt-token\"}")));
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"newuser@example.com\",\"password\":\"password123\",\"name\":\"New User\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-456"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }
    
    @Test
    void testProtectedEndpoint_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testCorrelationIdPropagation() throws Exception {
        String correlationId = "test-correlation-123";
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"user-123\",\"email\":\"test@example.com\",\"token\":\"jwt-token\"}")));
        
        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(header().string("X-Correlation-Id", correlationId));
    }
}
