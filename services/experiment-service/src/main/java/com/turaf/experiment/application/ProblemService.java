package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateProblemRequest;
import com.turaf.experiment.application.dto.ProblemDto;
import com.turaf.experiment.application.dto.UpdateProblemRequest;
import com.turaf.experiment.application.exception.ProblemNotFoundException;
import com.turaf.experiment.domain.Problem;
import com.turaf.experiment.domain.ProblemId;
import com.turaf.experiment.domain.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProblemService {
    
    private final ProblemRepository problemRepository;
    private final EventPublisher eventPublisher;

    public ProblemService(ProblemRepository problemRepository, EventPublisher eventPublisher) {
        this.problemRepository = problemRepository;
        this.eventPublisher = eventPublisher;
    }

    public ProblemDto createProblem(CreateProblemRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        ProblemId id = ProblemId.generate();
        
        Problem problem = new Problem(
            id,
            organizationId,
            request.getTitle(),
            request.getDescription(),
            createdBy
        );
        
        Problem saved = problemRepository.save(problem);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return ProblemDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public ProblemDto getProblem(ProblemId id) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id.getValue()));
        return ProblemDto.fromDomain(problem);
    }

    @Transactional(readOnly = true)
    public List<ProblemDto> getAllProblems() {
        String organizationId = TenantContextHolder.getOrganizationId();
        return problemRepository.findByOrganizationId(organizationId)
            .stream()
            .map(ProblemDto::fromDomain)
            .collect(Collectors.toList());
    }

    public ProblemDto updateProblem(ProblemId id, UpdateProblemRequest request) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id.getValue()));
        
        problem.update(request.getTitle(), request.getDescription());
        Problem updated = problemRepository.save(problem);
        
        return ProblemDto.fromDomain(updated);
    }

    public void deleteProblem(ProblemId id) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id.getValue()));
        
        problemRepository.delete(problem);
    }
}
