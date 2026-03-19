package com.turaf.bff.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordServiceCall(String serviceName, long durationMs, boolean success) {
        Timer.builder("bff.service.call.duration")
            .tag("service", serviceName)
            .tag("success", String.valueOf(success))
            .description("Duration of service calls from BFF to downstream services")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void incrementAuthenticationAttempt(boolean success) {
        Counter.builder("bff.authentication.attempts")
            .tag("success", String.valueOf(success))
            .description("Number of authentication attempts")
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementOrganizationCreated() {
        Counter.builder("bff.organization.created")
            .description("Number of organizations created")
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementExperimentCreated() {
        Counter.builder("bff.experiment.created")
            .description("Number of experiments created")
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementMetricRecorded() {
        Counter.builder("bff.metric.recorded")
            .description("Number of metrics recorded")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordOrchestrationCall(String endpoint, long durationMs) {
        Timer.builder("bff.orchestration.duration")
            .tag("endpoint", endpoint)
            .description("Duration of orchestration endpoint calls")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
