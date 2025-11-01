package com.andrelucs.realtimepolls.exceptions.service;

import org.springframework.http.HttpStatus;

public class InvalidPollCreationException extends Exception {
    public InvalidPollCreationException(String message) {
        super(message);
    }
}
