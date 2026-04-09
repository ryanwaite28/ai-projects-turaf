package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateExperimentRequest;
import com.turaf.bff.dto.ExperimentDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

/**
 * Experiment Service HTTP Client using Spring's declarative HTTP interface.
 */
@HttpExchange(url = "/api/v1/experiments", accept = "application/json", contentType = "application/json")
public interface ExperimentServiceClient {
    
    @GetExchange
    List<ExperimentDto> getExperiments(@RequestParam String organizationId,
                                       @RequestHeader("X-User-Id") String userId);
    
    @PostExchange
    ExperimentDto createExperiment(@RequestBody CreateExperimentRequest request,
                                   @RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-Organization-Id") String organizationId);
    
    @GetExchange("/{id}")
    ExperimentDto getExperiment(@PathVariable String id,
                                @RequestHeader("X-User-Id") String userId,
                                @RequestHeader("X-Organization-Id") String organizationId);
    
    @PutExchange("/{id}")
    ExperimentDto updateExperiment(@PathVariable String id,
                                   @RequestBody CreateExperimentRequest request,
                                   @RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-Organization-Id") String organizationId);
    
    @DeleteExchange("/{id}")
    void deleteExperiment(@PathVariable String id,
                         @RequestHeader("X-User-Id") String userId,
                         @RequestHeader("X-Organization-Id") String organizationId);
    
    @PostExchange("/{id}/start")
    ExperimentDto startExperiment(@PathVariable String id,
                                  @RequestHeader("X-User-Id") String userId,
                                  @RequestHeader("X-Organization-Id") String organizationId);
    
    @PostExchange("/{id}/complete")
    ExperimentDto completeExperiment(@PathVariable String id,
                                     @RequestHeader("X-User-Id") String userId,
                                     @RequestHeader("X-Organization-Id") String organizationId);
    
    @PostExchange("/{id}/cancel")
    ExperimentDto cancelExperiment(@PathVariable String id,
                                   @RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-Organization-Id") String organizationId);
}
