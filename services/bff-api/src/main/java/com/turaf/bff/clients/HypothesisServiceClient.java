package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateHypothesisRequest;
import com.turaf.bff.dto.HypothesisDto;
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
 * Hypothesis Service HTTP Client using Spring's declarative HTTP interface.
 */
@HttpExchange(url = "/api/v1/hypotheses", accept = "application/json", contentType = "application/json")
public interface HypothesisServiceClient {
    
    @GetExchange
    List<HypothesisDto> getHypotheses(@RequestHeader("X-User-Id") String userId,
                                      @RequestHeader("X-Organization-Id") String organizationId,
                                      @RequestParam(required = false) String problemId);
    
    @PostExchange
    HypothesisDto createHypothesis(@RequestBody CreateHypothesisRequest request,
                                   @RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-Organization-Id") String organizationId);
    
    @GetExchange("/{id}")
    HypothesisDto getHypothesis(@PathVariable String id,
                                @RequestHeader("X-User-Id") String userId,
                                @RequestHeader("X-Organization-Id") String organizationId);
    
    @PutExchange("/{id}")
    HypothesisDto updateHypothesis(@PathVariable String id,
                                   @RequestBody CreateHypothesisRequest request,
                                   @RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-Organization-Id") String organizationId);
    
    @DeleteExchange("/{id}")
    void deleteHypothesis(@PathVariable String id,
                         @RequestHeader("X-User-Id") String userId,
                         @RequestHeader("X-Organization-Id") String organizationId);
}
