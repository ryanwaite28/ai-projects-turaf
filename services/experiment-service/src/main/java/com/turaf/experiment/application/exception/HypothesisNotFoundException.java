package com.turaf.experiment.application.exception;

public class HypothesisNotFoundException extends RuntimeException {
    
    public HypothesisNotFoundException(String message) {
        super(message);
    }
    
    public HypothesisNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
