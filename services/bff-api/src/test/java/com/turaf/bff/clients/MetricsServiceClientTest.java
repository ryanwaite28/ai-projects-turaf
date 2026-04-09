package com.turaf.bff.clients;

import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
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

class MetricsServiceClientTest {
    
    private MockWebServer mockWebServer;
    private MetricsServiceClient client;
    
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
        client = factory.createClient(MetricsServiceClient.class);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testRecordMetric_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"metric-123\",\"experimentId\":\"exp-123\",\"name\":\"conversion_rate\",\"value\":0.25}")
            .addHeader("Content-Type", "application/json"));
        
        RecordMetricRequest request = RecordMetricRequest.builder()
            .experimentId("exp-123")
            .name("conversion_rate")
            .value(0.25)
            .unit("percentage")
            .build();
        
        MetricDto metric = client.recordMetric(request, "user-123", "org-123");
        
        assertNotNull(metric);
        assertEquals("metric-123", metric.getId());
        assertEquals("exp-123", metric.getExperimentId());
        assertEquals("conversion_rate", metric.getName());
        assertEquals(0.25, metric.getValue());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/metrics", recordedRequest.getPath());
        assertEquals("user-123", recordedRequest.getHeader("X-User-Id"));
        assertEquals("org-123", recordedRequest.getHeader("X-Organization-Id"));
    }
    
    @Test
    void testGetExperimentMetrics_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("[{\"id\":\"metric-1\",\"experimentId\":\"exp-456\",\"name\":\"clicks\",\"value\":100.0}," +
                    "{\"id\":\"metric-2\",\"experimentId\":\"exp-456\",\"name\":\"views\",\"value\":1000.0}]")
            .addHeader("Content-Type", "application/json"));
        
        List<MetricDto> metrics = client.getExperimentMetrics("exp-456", "user-123", "org-123");
        
        assertNotNull(metrics);
        assertEquals(2, metrics.size());
        assertEquals("metric-1", metrics.get(0).getId());
        assertEquals("clicks", metrics.get(0).getName());
        assertEquals(100.0, metrics.get(0).getValue());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/v1/metrics"));
        assertTrue(recordedRequest.getPath().contains("experimentId=exp-456"));
    }
    
    @Test
    void testGetMetric_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"id\":\"metric-789\",\"experimentId\":\"exp-789\",\"name\":\"revenue\",\"value\":5000.0}")
            .addHeader("Content-Type", "application/json"));
        
        MetricDto metric = client.getMetric("metric-789", "user-123", "org-123");
        
        assertNotNull(metric);
        assertEquals("metric-789", metric.getId());
        assertEquals("revenue", metric.getName());
        assertEquals(5000.0, metric.getValue());
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/api/v1/metrics/metric-789", recordedRequest.getPath());
    }
    
    @Test
    void testDeleteMetric_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(204));
        
        client.deleteMetric("metric-999", "user-123", "org-123");
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertEquals("/api/v1/metrics/metric-999", recordedRequest.getPath());
    }
}
