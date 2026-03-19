package com.turaf.bff.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomMetricsTest {
    
    private MeterRegistry meterRegistry;
    private CustomMetrics customMetrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        customMetrics = new CustomMetrics(meterRegistry);
    }
    
    @Test
    void testRecordServiceCall_Success() {
        customMetrics.recordServiceCall("identity", 100, true);
        
        Timer timer = meterRegistry.find("bff.service.call.duration")
            .tag("service", "identity")
            .tag("success", "true")
            .timer();
        
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
    
    @Test
    void testRecordServiceCall_Failure() {
        customMetrics.recordServiceCall("organization", 200, false);
        
        Timer timer = meterRegistry.find("bff.service.call.duration")
            .tag("service", "organization")
            .tag("success", "false")
            .timer();
        
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
    
    @Test
    void testIncrementAuthenticationAttempt_Success() {
        customMetrics.incrementAuthenticationAttempt(true);
        
        Counter counter = meterRegistry.find("bff.authentication.attempts")
            .tag("success", "true")
            .counter();
        
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testIncrementAuthenticationAttempt_Failure() {
        customMetrics.incrementAuthenticationAttempt(false);
        
        Counter counter = meterRegistry.find("bff.authentication.attempts")
            .tag("success", "false")
            .counter();
        
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testIncrementOrganizationCreated() {
        customMetrics.incrementOrganizationCreated();
        customMetrics.incrementOrganizationCreated();
        
        Counter counter = meterRegistry.find("bff.organization.created").counter();
        
        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }
    
    @Test
    void testIncrementExperimentCreated() {
        customMetrics.incrementExperimentCreated();
        
        Counter counter = meterRegistry.find("bff.experiment.created").counter();
        
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }
    
    @Test
    void testIncrementMetricRecorded() {
        customMetrics.incrementMetricRecorded();
        customMetrics.incrementMetricRecorded();
        customMetrics.incrementMetricRecorded();
        
        Counter counter = meterRegistry.find("bff.metric.recorded").counter();
        
        assertNotNull(counter);
        assertEquals(3.0, counter.count());
    }
    
    @Test
    void testRecordOrchestrationCall() {
        customMetrics.recordOrchestrationCall("dashboard-overview", 150);
        
        Timer timer = meterRegistry.find("bff.orchestration.duration")
            .tag("endpoint", "dashboard-overview")
            .timer();
        
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
}
