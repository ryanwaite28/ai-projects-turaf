package com.turaf.communications.domain.exception;

public class UnauthorizedParticipantException extends RuntimeException {
    public UnauthorizedParticipantException(String message) {
        super(message);
    }
}
