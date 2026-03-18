package com.turaf.experiment.application.exception;

public class ExperimentNotFoundException extends RuntimeException {
    
    public ExperimentNotFoundException(String message) {
        super(message);
    }
    
    public ExperimentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
