package com.turaf.bff.integration;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PerformanceIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testConcurrentRequests_HealthEndpoint() throws Exception {
        int concurrentUsers = 50;
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/actuator/health"))
                        .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log error
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        
        assertTrue(successCount.get() >= 48, "At least 96% of requests should succeed");
        assertTrue(duration < 10000, "Should complete within 10 seconds");
    }
    
    @Test
    void testConcurrentRequests_ProxyEndpoint() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        
        int concurrentUsers = 30;
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/api/v1/organizations")
                            .header("Authorization", "Bearer test-token"))
                        .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log error
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(successCount.get() >= 28, "At least 93% of requests should succeed");
    }
    
    @Test
    void testOrchestrationEndpoint_ResponseTime() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/auth/me.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"id\":\"user-1\",\"email\":\"test@example.com\"}")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        wireMockServer.stubFor(get(urlMatching("/api/v1/experiments.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/v1/dashboard/overview")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk());
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(duration < 1000, "Orchestration endpoint should respond within 1 second");
    }
    
    @Test
    void testProxyEndpoint_ResponseTime() throws Exception {
        wireMockServer.stubFor(get(urlMatching("/api/v1/organizations.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[]")));
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/v1/organizations")
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk());
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(duration < 500, "Proxy endpoint should respond within 500ms");
    }
}
