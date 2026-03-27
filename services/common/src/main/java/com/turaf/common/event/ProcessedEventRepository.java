package com.turaf.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for tracking processed events.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    /**
     * Check if an event has been processed.
     *
     * @param eventId The event ID
     * @return true if the event exists in the database
     */
    boolean existsByEventId(String eventId);
    
    /**
     * Find a processed event by its event ID.
     *
     * @param eventId The event ID
     * @return Optional containing the processed event if found
     */
    Optional<ProcessedEvent> findByEventId(String eventId);
    
    /**
     * Delete processed events older than the specified timestamp.
     * Used for cleanup of old idempotency records.
     *
     * @param before Delete events processed before this timestamp
     * @return Number of deleted records
     */
    long deleteByProcessedAtBefore(Instant before);
}
