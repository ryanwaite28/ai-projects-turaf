package com.turaf.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DomainExceptionTest {
    
    @Test
    void testDomainExceptionCreation() {
        DomainException exception = new DomainException("Invalid state", "INVALID_STATE");
        
        assertThat(exception.getMessage()).isEqualTo("Invalid state");
        assertThat(exception.getErrorCode()).isEqualTo("INVALID_STATE");
    }
    
    @Test
    void testDomainExceptionCreation_WithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        DomainException exception = new DomainException("Invalid state", "INVALID_STATE", cause);
        
        assertThat(exception.getMessage()).isEqualTo("Invalid state");
        assertThat(exception.getErrorCode()).isEqualTo("INVALID_STATE");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
    
    @Test
    void testToString() {
        DomainException exception = new DomainException("Invalid state", "INVALID_STATE");
        
        assertThat(exception.toString()).contains("DomainException");
        assertThat(exception.toString()).contains("INVALID_STATE");
        assertThat(exception.toString()).contains("Invalid state");
    }
}
