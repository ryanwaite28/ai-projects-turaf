package com.turaf.organization.interfaces.rest;

import java.time.Instant;

/**
 * Standard error response for REST API.
 */
public class ErrorResponse {
    
    private String code;
    private String message;
    private Instant timestamp;
    
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
