package com.andrelucs.realtimepolls.exceptions.service;

public class InvalidPollUpdateException extends RuntimeException {
    public InvalidPollUpdateException(String message) {
        super(message);
    }
}
