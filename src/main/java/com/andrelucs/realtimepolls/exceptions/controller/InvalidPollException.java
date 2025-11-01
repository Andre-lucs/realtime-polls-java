package com.andrelucs.realtimepolls.exceptions.controller;

public class InvalidPollException extends RuntimeException {
    public InvalidPollException(String message) {
        super(message);
    }
}
