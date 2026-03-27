package com.turaf.common.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata for domain events supporting correlation and causation tracking.
 * 
 * This class enables distributed tracing and event causality tracking across services:
 * - Correlation ID: Groups related events from a single user action
 * - Causation ID: Links an event to the event that caused it
 * - Custom metadata: Allows services to add additional context
 * 
 * Following event-driven architecture best practices for observability.
 */
public class EventMetadata {
    
    private String correlationId;
    private String causationId;
    private Map<String, String> customMetadata;
    
    /**
     * Default constructor initializes empty metadata.
     */
    public EventMetadata() {
        this.customMetadata = new HashMap<>();
    }
    
    /**
     * Constructor with correlation and causation IDs.
     * 
     * @param correlationId the correlation ID for grouping related events
     * @param causationId the causation ID linking to the causing event
     */
    public EventMetadata(String correlationId, String causationId) {
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.customMetadata = new HashMap<>();
    }
    
    /**
     * Adds custom metadata key-value pair.
     * 
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, String value) {
        Objects.requireNonNull(key, "Metadata key cannot be null");
        customMetadata.put(key, value);
    }
    
    /**
     * Gets custom metadata value by key.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not present
     */
    public String getMetadata(String key) {
        return customMetadata.get(key);
    }
    
    /**
     * Gets all custom metadata.
     * 
     * @return unmodifiable map of custom metadata
     */
    public Map<String, String> getAllMetadata() {
        return Map.copyOf(customMetadata);
    }
    
    // Getters and setters
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getCausationId() {
        return causationId;
    }
    
    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMetadata that = (EventMetadata) o;
        return Objects.equals(correlationId, that.correlationId) &&
               Objects.equals(causationId, that.causationId) &&
               Objects.equals(customMetadata, that.customMetadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(correlationId, causationId, customMetadata);
    }
    
    @Override
    public String toString() {
        return "EventMetadata{" +
               "correlationId='" + correlationId + '\'' +
               ", causationId='" + causationId + '\'' +
               ", customMetadata=" + customMetadata +
               '}';
    }
}
