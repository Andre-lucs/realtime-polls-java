package com.andrelucs.realtimepolls.exceptions.controller;

public class PollNotFoundException extends RuntimeException{
    public PollNotFoundException(String message) {
        super(message);
    }
}
