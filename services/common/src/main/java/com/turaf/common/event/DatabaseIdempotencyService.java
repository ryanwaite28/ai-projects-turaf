package com.turaf.common.event;

import com.turaf.common.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Database-based idempotency service for event processing.
 * Alternative to DynamoDB-based IdempotencyService for environments without AWS.
 * 
 * Ensures events are processed exactly once using PostgreSQL for persistence.
 * Works across multiple service instances and survives restarts.
 */
@Service
public class DatabaseIdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseIdempotencyService.class);
    private final ProcessedEventRepository repository;
    
    public DatabaseIdempotencyService(ProcessedEventRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Checks if an event has already been processed.
     */
    public boolean isProcessed(String eventId) {
        boolean exists = repository.existsByEventId(eventId);
        if (exists) {
            logger.debug("Event {} has already been processed (idempotency check)", eventId);
        }
        return exists;
    }
    
    /**
     * Checks if a domain event has already been processed.
     */
    public boolean isProcessed(DomainEvent event) {
        return isProcessed(event.getEventId());
    }
    
    /**
     * Marks an event as processed with idempotent behavior.
     */
    @Transactional
    public void markProcessed(String eventId, String eventType, String organizationId) {
        try {
            ProcessedEvent processedEvent = new ProcessedEvent(eventId, eventType, organizationId);
            repository.save(processedEvent);
            logger.debug("Marked event {} as processed", eventId);
        } catch (DataIntegrityViolationException e) {
            logger.debug("Event {} was already marked as processed (concurrent processing)", eventId);
        }
    }
    
    /**
     * Marks a domain event as processed.
     */
    @Transactional
    public void markProcessed(DomainEvent event) {
        markProcessed(event.getEventId(), event.getEventType(), event.getOrganizationId());
    }
    
    /**
     * Cleanup old processed event records daily at 2 AM.
     * Removes records older than 30 days.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldRecords() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        long deleted = repository.deleteByProcessedAtBefore(cutoff);
        
        if (deleted > 0) {
            logger.info("Cleaned up {} old processed event records (older than 30 days)", deleted);
        }
    }
}
