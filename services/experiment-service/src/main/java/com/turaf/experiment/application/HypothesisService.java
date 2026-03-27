package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.tenant.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateHypothesisRequest;
import com.turaf.experiment.application.dto.HypothesisDto;
import com.turaf.experiment.application.dto.UpdateHypothesisRequest;
import com.turaf.experiment.application.exception.HypothesisNotFoundException;
import com.turaf.experiment.application.exception.ProblemNotFoundException;
import com.turaf.experiment.domain.Hypothesis;
import com.turaf.experiment.domain.HypothesisId;
import com.turaf.experiment.domain.HypothesisRepository;
import com.turaf.experiment.domain.ProblemId;
import com.turaf.experiment.domain.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HypothesisService {
    
    private final HypothesisRepository hypothesisRepository;
    private final ProblemRepository problemRepository;
    private final EventPublisher eventPublisher;

    public HypothesisService(
            HypothesisRepository hypothesisRepository,
            ProblemRepository problemRepository,
            EventPublisher eventPublisher) {
        this.hypothesisRepository = hypothesisRepository;
        this.problemRepository = problemRepository;
        this.eventPublisher = eventPublisher;
    }

    public HypothesisDto createHypothesis(CreateHypothesisRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        ProblemId problemId = ProblemId.of(request.getProblemId());
        
        // Verify problem exists
        problemRepository.findById(problemId)
            .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + request.getProblemId()));
        
        HypothesisId id = HypothesisId.generate();
        Hypothesis hypothesis = new Hypothesis(
            id,
            organizationId,
            problemId,
            request.getStatement(),
            request.getExpectedOutcome(),
            createdBy
        );
        
        Hypothesis saved = hypothesisRepository.save(hypothesis);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return HypothesisDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public HypothesisDto getHypothesis(HypothesisId id) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found with id: " + id.getValue()));
        return HypothesisDto.fromDomain(hypothesis);
    }

    @Transactional(readOnly = true)
    public List<HypothesisDto> getHypothesesByProblem(ProblemId problemId) {
        return hypothesisRepository.findByProblemId(problemId)
            .stream()
            .map(HypothesisDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HypothesisDto> getAllHypotheses() {
        String organizationId = TenantContextHolder.getOrganizationId();
        return hypothesisRepository.findByOrganizationId(organizationId)
            .stream()
            .map(HypothesisDto::fromDomain)
            .collect(Collectors.toList());
    }

    public HypothesisDto updateHypothesis(HypothesisId id, UpdateHypothesisRequest request) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found with id: " + id.getValue()));
        
        hypothesis.update(request.getStatement(), request.getExpectedOutcome());
        Hypothesis updated = hypothesisRepository.save(hypothesis);
        
        return HypothesisDto.fromDomain(updated);
    }

    public void deleteHypothesis(HypothesisId id) {
        Hypothesis hypothesis = hypothesisRepository.findById(id)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found with id: " + id.getValue()));
        
        hypothesisRepository.delete(hypothesis);
    }
}
