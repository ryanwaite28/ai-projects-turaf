package com.turaf.bff.clients;

import com.turaf.bff.dto.MetricDto;
import com.turaf.bff.dto.RecordMetricRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * Metrics Service HTTP Client using Spring's declarative HTTP interface.
 * 
 * This interface is implemented at runtime by Spring's HttpServiceProxyFactory,
 * eliminating boilerplate code for REST client calls.
 */
@HttpExchange(url = "/api/v1/metrics", accept = "application/json", contentType = "application/json")
public interface MetricsServiceClient {
    
    @PostExchange
    MetricDto recordMetric(@RequestBody RecordMetricRequest request,
                          @RequestHeader("X-User-Id") String userId,
                          @RequestHeader("X-Organization-Id") String organizationId);
    
    @GetExchange
    List<MetricDto> getExperimentMetrics(@RequestParam String experimentId,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-Organization-Id") String organizationId);
    
    @GetExchange("/{id}")
    MetricDto getMetric(@PathVariable String id,
                       @RequestHeader("X-User-Id") String userId,
                       @RequestHeader("X-Organization-Id") String organizationId);
    
    @DeleteExchange("/{id}")
    void deleteMetric(@PathVariable String id,
                     @RequestHeader("X-User-Id") String userId,
                     @RequestHeader("X-Organization-Id") String organizationId);
}
