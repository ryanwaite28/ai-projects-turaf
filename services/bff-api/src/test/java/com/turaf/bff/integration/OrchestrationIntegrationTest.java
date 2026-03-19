package com.turaf.bff.integration;

import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrchestrationIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testDashboardOverview_AggregatesMultipleServices() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/auth/me.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"user-1\",\"email\":\"test@example.com\",\"name\":\"Test User\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"org-1\",\"name\":\"Org 1\"},{\"id\":\"org-2\",\"name\":\"Org 2\"}]")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"exp-1\",\"status\":\"RUNNING\"},{\"id\":\"exp-2\",\"status\":\"RUNNING\"}]")));
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-1"))
            .andExpect(jsonPath("$.organizations[0].id").value("org-1"))
            .andExpect(jsonPath("$.organizations[1].id").value("org-2"))
            .andExpect(jsonPath("$.activeExperiments[0].id").value("exp-1"))
            .andExpect(jsonPath("$.activeExperiments[1].id").value("exp-2"))
            .andExpect(jsonPath("$.totalOrganizations").value(2))
            .andExpect(jsonPath("$.totalActiveExperiments").value(2));
    }
    
    @Test
    void testDashboardOverview_PartialFailure_GracefulDegradation() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/auth/me.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"user-1\",\"email\":\"test@example.com\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\":\"Internal Server Error\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.id").value("user-1"))
            .andExpect(jsonPath("$.organizations").isEmpty())
            .andExpect(jsonPath("$.activeExperiments").isEmpty())
            .andExpect(jsonPath("$.totalOrganizations").value(0))
            .andExpect(jsonPath("$.totalActiveExperiments").value(0));
    }
    
    @Test
    void testExperimentFull_AggregatesExperimentAndMetrics() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/experiments/exp-1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"exp-1\",\"name\":\"Test Experiment\",\"status\":\"RUNNING\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/metrics.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"id\":\"m-1\",\"value\":10.5},{\"id\":\"m-2\",\"value\":20.3}]")));
        
        mockMvc.perform(get("/api/v1/dashboard/experiments/exp-1/full")
                .header("Authorization", "Bearer test-token")
                .param("organizationId", "org-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.experiment.id").value("exp-1"))
            .andExpect(jsonPath("$.metrics[0].id").value("m-1"))
            .andExpect(jsonPath("$.metrics[1].id").value("m-2"))
            .andExpect(jsonPath("$.totalMetrics").value(2));
    }
    
    @Test
    void testOrganizationSummary_AggregatesOrgMembersExperiments() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/organizations/org-1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"org-1\",\"name\":\"Test Org\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations/org-1/members.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[{\"id\":\"u-1\",\"name\":\"User 1\"},{\"id\":\"u-2\",\"name\":\"User 2\"}]")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[{\"id\":\"exp-1\",\"status\":\"RUNNING\"}]")));
        
        mockMvc.perform(get("/api/v1/dashboard/organizations/org-1/summary")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organization.id").value("org-1"))
            .andExpect(jsonPath("$.members[0].id").value("u-1"))
            .andExpect(jsonPath("$.members[1].id").value("u-2"))
            .andExpect(jsonPath("$.totalMembers").value(2))
            .andExpect(jsonPath("$.totalExperiments").value(1));
    }
}
