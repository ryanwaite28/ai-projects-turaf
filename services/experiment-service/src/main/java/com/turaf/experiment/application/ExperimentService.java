package com.turaf.experiment.application;

import com.turaf.common.event.EventPublisher;
import com.turaf.common.security.TenantContextHolder;
import com.turaf.experiment.application.dto.CreateExperimentRequest;
import com.turaf.experiment.application.dto.ExperimentDto;
import com.turaf.experiment.application.dto.UpdateExperimentRequest;
import com.turaf.experiment.application.exception.ExperimentNotFoundException;
import com.turaf.experiment.application.exception.HypothesisNotFoundException;
import com.turaf.experiment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExperimentService {
    
    private final ExperimentRepository experimentRepository;
    private final HypothesisRepository hypothesisRepository;
    private final EventPublisher eventPublisher;

    public ExperimentService(
            ExperimentRepository experimentRepository,
            HypothesisRepository hypothesisRepository,
            EventPublisher eventPublisher) {
        this.experimentRepository = experimentRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.eventPublisher = eventPublisher;
    }

    public ExperimentDto createExperiment(CreateExperimentRequest request, String createdBy) {
        String organizationId = TenantContextHolder.getOrganizationId();
        HypothesisId hypothesisId = HypothesisId.of(request.getHypothesisId());
        
        // Verify hypothesis exists
        hypothesisRepository.findById(hypothesisId)
            .orElseThrow(() -> new HypothesisNotFoundException("Hypothesis not found with id: " + request.getHypothesisId()));
        
        ExperimentId id = ExperimentId.generate();
        Experiment experiment = new Experiment(
            id,
            organizationId,
            hypothesisId,
            request.getName(),
            request.getDescription(),
            createdBy
        );
        
        Experiment saved = experimentRepository.save(experiment);
        
        saved.getDomainEvents().forEach(eventPublisher::publish);
        saved.clearDomainEvents();
        
        return ExperimentDto.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public ExperimentDto getExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        return ExperimentDto.fromDomain(experiment);
    }

    @Transactional(readOnly = true)
    public List<ExperimentDto> getExperimentsByHypothesis(HypothesisId hypothesisId) {
        return experimentRepository.findByHypothesisId(hypothesisId)
            .stream()
            .map(ExperimentDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExperimentDto> getExperimentsByStatus(ExperimentStatus status) {
        String organizationId = TenantContextHolder.getOrganizationId();
        return experimentRepository.findByOrganizationIdAndStatus(organizationId, status)
            .stream()
            .map(ExperimentDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExperimentDto> getAllExperiments() {
        String organizationId = TenantContextHolder.getOrganizationId();
        return experimentRepository.findByOrganizationId(organizationId)
            .stream()
            .map(ExperimentDto::fromDomain)
            .collect(Collectors.toList());
    }

    public ExperimentDto updateExperiment(ExperimentId id, UpdateExperimentRequest request) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        
        experiment.update(request.getName(), request.getDescription());
        Experiment updated = experimentRepository.save(experiment);
        
        return ExperimentDto.fromDomain(updated);
    }

    public ExperimentDto startExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        
        experiment.start();
        Experiment updated = experimentRepository.save(experiment);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return ExperimentDto.fromDomain(updated);
    }

    public ExperimentDto completeExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        
        experiment.complete();
        Experiment updated = experimentRepository.save(experiment);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return ExperimentDto.fromDomain(updated);
    }

    public ExperimentDto cancelExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        
        experiment.cancel();
        Experiment updated = experimentRepository.save(experiment);
        
        updated.getDomainEvents().forEach(eventPublisher::publish);
        updated.clearDomainEvents();
        
        return ExperimentDto.fromDomain(updated);
    }

    public void deleteExperiment(ExperimentId id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new ExperimentNotFoundException("Experiment not found with id: " + id.getValue()));
        
        experimentRepository.delete(experiment);
    }
}
