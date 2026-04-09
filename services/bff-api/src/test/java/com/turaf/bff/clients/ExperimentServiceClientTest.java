package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentServiceClientTest {
    
    private MockWebServer mockWebServer;
    private ExperimentServiceClient client;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        RestClient restClient = RestClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        
        // Create HttpExchange proxy for the interface
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();
        client = factory.createClient(ExperimentServiceClient.class);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testGetExperiments_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("[{\"id\":\"exp-123\",\"name\":\"Test Experiment\",\"status\":\"RUNNING\"}]")
            .addHeader("Content-Type", "application/json"));
        
        List<ExperimentDto> experiments = client.getExperiments("org-123", "user-123");
        
        assertNotNull(experiments);
        assertEquals(1, experiments.size());
        assertEquals("exp-123", experiments.get(0).getId());
        assertEquals("Test Experiment", experiments.get(0).getName());
        assertEquals("RUNNING", experiments.get(0).getStatus());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/v1/experiments"));
        assertTrue(recordedRequest.getPath().contains("organizationId=org-123"));
        assertEquals("user-123", recordedRequest.getHeader("X-User-Id"));
    }
    
    @Test
    void testCreateExperiment_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"exp-456\",\"name\":\"New Experiment\",\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        
        CreateExperimentRequest request = CreateExperimentRequest.builder()
            .name("New Experiment")
            .description("Test Description")
            .organizationId("org-123")
            .problemId("prob-1")
            .hypothesisId("hyp-1")
            .build();
        
        ExperimentDto experiment = client.createExperiment(request, "user-123", "org-123");
        
        assertNotNull(experiment);
        assertEquals("exp-456", experiment.getId());
        assertEquals("New Experiment", experiment.getName());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/experiments", recordedRequest.getPath());
        assertEquals("user-123", recordedRequest.getHeader("X-User-Id"));
        assertEquals("org-123", recordedRequest.getHeader("X-Organization-Id"));
    }
    
    @Test
    void testStartExperiment_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"exp-789\",\"name\":\"Started Experiment\",\"status\":\"RUNNING\"}")
            .addHeader("Content-Type", "application/json"));
        
        ExperimentDto experiment = client.startExperiment("exp-789", "user-123", "org-123");
        
        assertNotNull(experiment);
        assertEquals("exp-789", experiment.getId());
        assertEquals("RUNNING", experiment.getStatus());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/experiments/exp-789/start", recordedRequest.getPath());
    }
    
    @Test
    void testCompleteExperiment_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"exp-999\",\"name\":\"Completed Experiment\",\"status\":\"COMPLETED\"}")
            .addHeader("Content-Type", "application/json"));
        
        ExperimentDto experiment = client.completeExperiment("exp-999", "user-123", "org-123");
        
        assertNotNull(experiment);
        assertEquals("exp-999", experiment.getId());
        assertEquals("COMPLETED", experiment.getStatus());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/experiments/exp-999/complete", recordedRequest.getPath());
    }
}
