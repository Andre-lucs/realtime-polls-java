package com.andrelucs.realtimepolls.exceptions.controller;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
