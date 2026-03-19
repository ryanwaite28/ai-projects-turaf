package com.turaf.bff.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProxyEndpointsIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testOrganizationProxy_GetAll() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"org-1\",\"name\":\"Test Org\"}]")));
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("org-1"))
            .andExpect(jsonPath("$[0].name").value("Test Org"));
    }
    
    @Test
    void testOrganizationProxy_Create() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/organizations"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"org-2\",\"name\":\"New Org\"}")));
        
        mockMvc.perform(post("/api/v1/organizations")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Org\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("org-2"));
    }
    
    @Test
    void testExperimentProxy_GetAll() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"exp-1\",\"name\":\"Test Experiment\",\"status\":\"DRAFT\"}]")));
        
        mockMvc.perform(get("/api/v1/experiments")
                .param("organizationId", "org-1")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("exp-1"))
            .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }
    
    @Test
    void testExperimentProxy_Create() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/experiments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"exp-2\",\"name\":\"New Experiment\",\"status\":\"DRAFT\"}")));
        
        mockMvc.perform(post("/api/v1/experiments")
                .header("Authorization", "Bearer test-token")
                .param("organizationId", "org-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Experiment\",\"description\":\"Test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("exp-2"));
    }
    
    @Test
    void testMetricsProxy_RecordMetric() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/metrics"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"metric-1\",\"experimentId\":\"exp-1\",\"value\":42.5}")));
        
        mockMvc.perform(post("/api/v1/metrics")
                .header("Authorization", "Bearer test-token")
                .param("organizationId", "org-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"experimentId\":\"exp-1\",\"metricType\":\"CONVERSION\",\"value\":42.5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("metric-1"));
    }
    
    @Test
    void testServiceTimeout_ReturnsError() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withFixedDelay(10000)
                .withStatus(200)));
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().is5xxServerError());
    }
    
    @Test
    void testServiceUnavailable_ReturnsError() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("{\"error\":\"Service Unavailable\"}")));
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isServiceUnavailable());
    }
}
