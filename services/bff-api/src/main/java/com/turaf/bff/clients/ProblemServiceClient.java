package com.turaf.bff.clients;

import com.turaf.bff.dto.CreateProblemRequest;
import com.turaf.bff.dto.ProblemDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

/**
 * Problem Service HTTP Client using Spring's declarative HTTP interface.
 */
@HttpExchange(url = "/api/v1/problems", accept = "application/json", contentType = "application/json")
public interface ProblemServiceClient {
    
    @GetExchange
    List<ProblemDto> getProblems(@RequestHeader("X-User-Id") String userId,
                                 @RequestHeader("X-Organization-Id") String organizationId);
    
    @PostExchange
    ProblemDto createProblem(@RequestBody CreateProblemRequest request,
                             @RequestHeader("X-User-Id") String userId,
                             @RequestHeader("X-Organization-Id") String organizationId);
    
    @GetExchange("/{id}")
    ProblemDto getProblem(@PathVariable String id,
                          @RequestHeader("X-User-Id") String userId,
                          @RequestHeader("X-Organization-Id") String organizationId);
    
    @PutExchange("/{id}")
    ProblemDto updateProblem(@PathVariable String id,
                             @RequestBody CreateProblemRequest request,
                             @RequestHeader("X-User-Id") String userId,
                             @RequestHeader("X-Organization-Id") String organizationId);
    
    @DeleteExchange("/{id}")
    void deleteProblem(@PathVariable String id,
                      @RequestHeader("X-User-Id") String userId,
                      @RequestHeader("X-Organization-Id") String organizationId);
}
