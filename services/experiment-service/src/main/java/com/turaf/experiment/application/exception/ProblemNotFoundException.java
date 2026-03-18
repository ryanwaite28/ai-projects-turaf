package com.turaf.experiment.application.exception;

public class ProblemNotFoundException extends RuntimeException {
    
    public ProblemNotFoundException(String message) {
        super(message);
    }
    
    public ProblemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
